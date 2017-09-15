package com.nomadic.discuz.to.markdown.mapper;

import com.nomadic.discuz.to.markdown.domain.Attachment;
import com.nomadic.discuz.to.markdown.domain.Post;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface OneMapper {

    List<Post> getPosts();

    /**
     * 来源是9张表
     */
    List<Attachment> getAllAttach();
}
