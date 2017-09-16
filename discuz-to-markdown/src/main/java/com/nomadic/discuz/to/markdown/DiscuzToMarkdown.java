package com.nomadic.discuz.to.markdown;

import com.nomadic.discuz.to.markdown.convert.PostParser;
import com.nomadic.discuz.to.markdown.convert.PostWriter;
import com.nomadic.discuz.to.markdown.domain.Attachment;
import com.nomadic.discuz.to.markdown.domain.Post;
import com.nomadic.discuz.to.markdown.mapper.OneMapper;
import org.apache.commons.collections4.Closure;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DiscuzToMarkdown {

    @Autowired
    private OneMapper oneMapper;


    public void convert() {
        List<Post> stringList = oneMapper.getPosts();
        List<Attachment> allAttach = oneMapper.getAllAttach();

        for (Post post : stringList) {
            PostParser postParser = new PostParser(post, allAttach);
            postParser.parse();
            String result = postParser.getResult();

            String content = "# " + post.getSubject() + "\n" + result;
            PostWriter.write(content, postParser.getDictionary(), postParser.getFileName());

            List<ImmutablePair<String, String>> attachmentList = postParser.getAttachmentList();
            IterableUtils.forEach(attachmentList, new Closure<ImmutablePair<String, String>>() {
                @Override
                public void execute(ImmutablePair<String, String> pair) {
                    PostWriter.copy(pair.getLeft(), pair.getRight());
                }
            });

        }
    }
}
