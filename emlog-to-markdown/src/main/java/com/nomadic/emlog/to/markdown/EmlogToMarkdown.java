package com.nomadic.emlog.to.markdown;

import com.nomadic.emlog.to.markdown.convert.BlogParser;
import com.nomadic.emlog.to.markdown.convert.BlogWriter;
import com.nomadic.emlog.to.markdown.domain.Attachment;
import com.nomadic.emlog.to.markdown.domain.Blog;
import com.nomadic.emlog.to.markdown.domain.Comment;
import com.nomadic.emlog.to.markdown.domain.Twitter;
import com.nomadic.emlog.to.markdown.mapper.OneMapper;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class EmlogToMarkdown {

    @Autowired
    private OneMapper oneMapper;


    public void convert() {
        List<Blog> blogList = oneMapper.getBlog();
        List<Attachment> attachList = oneMapper.getAttach();
        List<Comment> commentList = oneMapper.getComment();
        List<Twitter> twitterList = oneMapper.getTwitter();

        // 将回复转成map
        Map<Long, List<Comment>> commentMap = new HashMap<>();
        IterableUtils.forEach(commentList, comment -> {
            List<Comment> comments = commentMap.computeIfAbsent(comment.getGid(), k -> new LinkedList<>());
            comments.add(comment);
        });
        // 将twitter追加到blog后面，当成blog处理
        IterableUtils.forEach(twitterList, twitter -> {
            Blog blog = new Blog();
            blog.setContent(twitter.getContent());
            blog.setDate(twitter.getDate());
            blog.setTitle("无标题");
            blog.setGid(Long.MAX_VALUE);
            blogList.add(blog);
        });

        // 预处理，将回帖内容连接到帖子后面
        for (Blog blog : blogList) {
            List<Comment> comments = commentMap.get(blog.getGid());
            IterableUtils.forEach(comments, comment -> blog.setContent(blog.getContent() + "\n\n\n" + comment.getComment()));
        }

        // 用于生成README.md文件
        Map<String, List<BlogParser>> blogMap = new LinkedHashMap<>();
        // 处理
        for (Blog blog : blogList) {
            if (StringUtils.isBlank(blog.getContent())) {
                continue;
            }
            BlogParser blogParser = new BlogParser(blog, attachList);
            blogParser.parse();
            String result = blogParser.getResult();

            String content = "# " + blogParser.getSubject() + "\n\n" + result;
            BlogWriter.write(content, blogParser.getDictionary(), blogParser.getFileName());

            List<ImmutablePair<String, String>> attachmentList = blogParser.getAttachmentList();
            IterableUtils.forEach(attachmentList, pair -> BlogWriter.copy(pair.getLeft(), pair.getRight()));

            // 按目录分组
            List<BlogParser> list = blogMap.computeIfAbsent(blogParser.getDictionary(), k -> new LinkedList<>());
            list.add(blogParser);
        }

        // 生成README.md目录
        List<String> ALL_README = new LinkedList<>();
        ALL_README.add("# 文章列表");
        ALL_README.add("");

        String template = "%s [%s](%s)";
        for (Map.Entry<String, List<BlogParser>> entry : blogMap.entrySet()) {
            List<String> README = new LinkedList<>();
            README.add("# 文章列表");
            README.add("");

            ALL_README.add("## " + String.format(template, "", entry.getKey(), entry.getKey()));

            for (BlogParser postParser : entry.getValue()) {
                String blogLink = String.format(template, postParser.getFileName(), postParser.getSubject(), postParser.getFileName());
                README.add(blogLink);

                String blogPathLink = String.format(template, postParser.getFileName(), postParser.getSubject(), entry.getKey() + "/" + postParser.getFileName());
                ALL_README.add(blogPathLink);
            }
            String README_MD = StringUtils.join(README, "\n");
            BlogWriter.write(README_MD, entry.getKey(), "README.md");
        }
        String ALL_README_MD = StringUtils.join(ALL_README, "\n");
        BlogWriter.write(ALL_README_MD, "", "README.md");
    }
}
