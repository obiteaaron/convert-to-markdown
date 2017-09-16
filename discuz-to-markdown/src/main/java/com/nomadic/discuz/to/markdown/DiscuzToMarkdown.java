package com.nomadic.discuz.to.markdown;

import com.nomadic.discuz.to.markdown.convert.PostParser;
import com.nomadic.discuz.to.markdown.convert.PostWriter;
import com.nomadic.discuz.to.markdown.domain.Attachment;
import com.nomadic.discuz.to.markdown.domain.Post;
import com.nomadic.discuz.to.markdown.mapper.OneMapper;
import org.apache.commons.collections4.Closure;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Component
public class DiscuzToMarkdown {

    @Autowired
    private OneMapper oneMapper;


    public void convert() {
        List<Post> stringList = oneMapper.getPosts();
        Map<Long, Post> postMap = new HashMap<>();
        List<Attachment> allAttach = oneMapper.getAllAttach();

        // 预处理，将回帖内容连接到帖子后面
        for (Post post : stringList) {
            if (post.getFirst() == 1) {
                Post post1 = postMap.put(post.getTid(), post);
                if (post1 != null) {
                    throw new RuntimeException();
                }
            } else if (post.getFirst() == 0) {
                Post post1 = postMap.get(post.getTid());
                post1.setMessage(post1.getMessage() + "\n\n\n" + post.getMessage());
            } else {
                throw new RuntimeException();
            }
        }

        // 用于生成README.md文件
        Map<String, List<PostParser>> blogMap = new HashMap<>();
        // 处理
        for (Post post : postMap.values()) {
            PostParser postParser = new PostParser(post, allAttach);
            postParser.parse();
            String result = postParser.getResult();

            String content = "# " + postParser.getSubject() + "\n" + result;
            PostWriter.write(content, postParser.getDictionary(), postParser.getFileName());

            List<ImmutablePair<String, String>> attachmentList = postParser.getAttachmentList();
            IterableUtils.forEach(attachmentList, new Closure<ImmutablePair<String, String>>() {
                @Override
                public void execute(ImmutablePair<String, String> pair) {
                    PostWriter.copy(pair.getLeft(), pair.getRight());
                }
            });

            // 按目录分组
            List<PostParser> list = blogMap.computeIfAbsent(postParser.getDictionary(), k -> new LinkedList<>());
            list.add(postParser);
        }

        // 生成README.md目录
        List<String> ALL_README = new LinkedList<>();
        ALL_README.add("# 文章列表");
        ALL_README.add("");

        String template = "%s [%s](%s)";
        for (Map.Entry<String, List<PostParser>> entry : blogMap.entrySet()) {
            List<String> README = new LinkedList<>();
            README.add("# 文章列表");
            README.add("");

            ALL_README.add("## " + String.format(template, "", entry.getKey(), entry.getKey()));

            for (PostParser postParser : entry.getValue()) {
                String blogLink = String.format(template, postParser.getFileName(), postParser.getSubject(), postParser.getFileName());
                README.add(blogLink);

                String blogPathLink = String.format(template, postParser.getFileName(), postParser.getSubject(), entry.getKey() + "/" + postParser.getFileName());
                ALL_README.add(blogPathLink);
            }
            String README_MD = StringUtils.join(README, "\n");
            PostWriter.write(README_MD, entry.getKey(), "README.md");
        }
        String ALL_README_MD = StringUtils.join(ALL_README, "\n");
        PostWriter.write(ALL_README_MD, "", "README.md");
    }
}
