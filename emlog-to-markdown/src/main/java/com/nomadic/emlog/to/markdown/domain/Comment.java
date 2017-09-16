package com.nomadic.emlog.to.markdown.domain;

/**
 * created at 2017/9/16 23:34
 */
public class Comment {
    private long gid;
    private long date;
    private String comment;

    public long getGid() {
        return gid;
    }

    public void setGid(long gid) {
        this.gid = gid;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
