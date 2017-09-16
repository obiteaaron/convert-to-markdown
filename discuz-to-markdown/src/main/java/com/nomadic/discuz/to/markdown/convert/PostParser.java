package com.nomadic.discuz.to.markdown.convert;

import com.nomadic.discuz.to.markdown.domain.Attachment;
import com.nomadic.discuz.to.markdown.domain.Post;
import com.nomadic.discuz.to.markdown.util.TraceLogger;
import org.apache.commons.collections4.Closure;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * created at 2017/9/16 8:22
 * 解析原文中的标签等等，用于替换
 */
public class PostParser {

    private static final String SPLIT = "_";
    private static final String IMG_SPLIT = "_img_";
    private static final String FILE_NAME_POST = ".md";

    private static final String TAG_ATTACH = "attach";
    private static final String TAG_I_START = "i=s";
    private static final String TAG_I_END = "i";

    private static final String REGEX = "\\[(%s.*?)\\](.*?)\\[/%s\\]";

    private static final Pattern PATTERN_TAG_ATTACH = Pattern.compile(String.format(REGEX, TAG_ATTACH, TAG_ATTACH), Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_TAG_I = Pattern.compile(String.format(REGEX, TAG_I_START, TAG_I_END), Pattern.CASE_INSENSITIVE);

    private static Map<String, Integer> sequence = new HashMap<>();

    private String dictionary;
    private String fileName;
    private String result;

    private List<ImmutablePair<String, String>> attachmentList = new ArrayList<>();

    private Post post;
    private Map<Long, Attachment> allAttachMap;

    public PostParser(Post post, List<Attachment> allAttach) {
        this.post = post;
        IterableUtils.forEach(allAttach, new Closure<Attachment>() {
            @Override
            public void execute(Attachment attachment) {
                if (allAttachMap == null) {
                    allAttachMap = new HashMap<>();
                }
                allAttachMap.put(attachment.getAid(), attachment);
            }
        });
    }

    public PostParser parse() {
        result = post.getMessage();
        parseFileName();
        parseAttach();
        parseI();
        return this;
    }

    private void parseFileName() {

        long dateline = post.getDateline();
        dictionary = FastDateFormat.getInstance("yyyyMM").format(new Date(dateline * 1000));
        String fileNamePrefix = FastDateFormat.getInstance("yyyyMMdd").format(new Date(dateline * 1000));
        Integer index = (sequence.get(fileNamePrefix) == null ? 1 : sequence.get(fileNamePrefix) + 1);
        fileName = fileNamePrefix + SPLIT + String.format("%02d", index) + FILE_NAME_POST;
        sequence.put(fileNamePrefix, index);
        TraceLogger.LOGGER.info("filename is " + dictionary + "/" + fileName);
    }

    public void parseAttach() {
        Matcher matcher = PATTERN_TAG_ATTACH.matcher(result);
        while (matcher.find()) {
            String matchAll = matcher.group();
            String match = matcher.group(2);
            Attachment attachment = allAttachMap.get(Long.valueOf(match));

            String imgExt = attachment.getAttachment().substring(attachment.getAttachment().lastIndexOf("."));
            String imageFileName = fileName.substring(0, fileName.length() - 3) + IMG_SPLIT + String.format("%03d", attachmentList.size() + 1) + imgExt;
            String targetAttachment = new File(dictionary, imageFileName).getPath();

            String markdownImg = String.format("![%s](%s)", attachment.getFilename(), imageFileName);

            TraceLogger.LOGGER.info(String.format("attachement %s --> %s", attachment.getAttachment(), targetAttachment));
            result = result.replace(matchAll, markdownImg);

            attachmentList.add(ImmutablePair.of(attachment.getAttachment(), targetAttachment));
        }
    }

    public void parseI() {
        Matcher matcher = PATTERN_TAG_I.matcher(result);
        while (matcher.find()) {
            String matchAll = matcher.group();
            String match = matcher.group(2);
            System.out.println(match);
            result = result.replace(matchAll, "");
        }
    }

    public String getDictionary() {
        return dictionary;
    }

    public String getFileName() {
        return fileName;
    }

    public String getResult() {
        return result;
    }

    public List<ImmutablePair<String, String>> getAttachmentList() {
        return attachmentList;
    }
}
