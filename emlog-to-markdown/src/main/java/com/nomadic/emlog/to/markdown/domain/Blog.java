package com.nomadic.emlog.to.markdown.domain;

/**
 * created at 2017/9/16 23:34
 */
public class Blog {
    private long gid;
    private String title;
    private long date;
    private String content;

    public long getGid() {
        return gid;
    }

    public void setGid(long gid) {
        this.gid = gid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
