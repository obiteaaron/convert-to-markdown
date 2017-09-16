package com.nomadic.emlog.to.markdown.mapper;

import com.nomadic.emlog.to.markdown.domain.Attachment;
import com.nomadic.emlog.to.markdown.domain.Blog;
import com.nomadic.emlog.to.markdown.domain.Comment;
import com.nomadic.emlog.to.markdown.domain.Twitter;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface OneMapper {

    List<Blog> getBlog();

    List<Attachment> getAttach();

    List<Comment> getComment();

    List<Twitter> getTwitter();
}
