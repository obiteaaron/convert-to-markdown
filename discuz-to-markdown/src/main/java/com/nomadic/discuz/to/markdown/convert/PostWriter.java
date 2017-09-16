package com.nomadic.discuz.to.markdown.convert;

import com.nomadic.discuz.to.markdown.util.TraceLogger;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 写入md文件
 */
public class PostWriter {
    private static final String GLOBAL_FILE_PATH = "D:/Workspaces/IntelliJIdea/discuz_blog";
    private static final String GLOBAL_ATTACHMENT_PATH = "D:/webroot/bbs/data/attachment/forum";

    public static void mkdirs(String path) {
        File file = new File(PostWriter.GLOBAL_FILE_PATH, path);
        if (!file.exists()) {
            TraceLogger.LOGGER.info("create dictionary " + file.getPath());
            file.mkdirs();
        }
    }

    /**
     * 写文件，写内容
     *
     * @param content
     * @param fileName
     */
    public static void write(String content, String path, String fileName) {
        mkdirs(path);
        File file = new File(new File(GLOBAL_FILE_PATH, path), fileName);
        try {
            TraceLogger.LOGGER.info("write file " + file.getPath());
            IOUtils.write(replaceCRLFToLF(content), new FileOutputStream(file), "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 复制文件，复制图片文件
     *
     * @param sourceFileName
     * @param targetFileName
     * @throws IOException
     */
    public static void copy(String sourceFileName, String targetFileName) {
        try {
            IOUtils.copy(new FileInputStream(new File(GLOBAL_ATTACHMENT_PATH, sourceFileName)), new FileOutputStream(new File(GLOBAL_FILE_PATH, targetFileName)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String replaceCRLFToLF(String content) {
        return content.replace("\r\n", "\n")
                // 两个空格加换行，才是gitlab里面的换行，唉~
                .replace("\n", "  \n");
    }
}
