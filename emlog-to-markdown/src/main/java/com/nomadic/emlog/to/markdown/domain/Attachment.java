package com.nomadic.emlog.to.markdown.domain;

/**
 * created at 2017/9/16 23:34
 */
public class Attachment {

    private long aid;
    private long blogid;
    private String filename;
    private String filepath;
    private long addtime;

    public long getAid() {
        return aid;
    }

    public void setAid(long aid) {
        this.aid = aid;
    }

    public long getBlogid() {
        return blogid;
    }

    public void setBlogid(long blogid) {
        this.blogid = blogid;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getFilepath() {
        return filepath;
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    public long getAddtime() {
        return addtime;
    }

    public void setAddtime(long addtime) {
        this.addtime = addtime;
    }
}
