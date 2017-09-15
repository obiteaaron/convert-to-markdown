package com.nomadic.discuz.to.markdown;

import com.nomadic.discuz.to.markdown.domain.Post;
import com.nomadic.discuz.to.markdown.mapper.OneMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DiscuzToMarkdown {

    @Autowired
    private OneMapper oneMapper;

    public void convert() {
        List<Post> stringList = oneMapper.getPosts();
        for (Post s : stringList) {
            System.out.println(s.getSubject());
        }
    }
}
