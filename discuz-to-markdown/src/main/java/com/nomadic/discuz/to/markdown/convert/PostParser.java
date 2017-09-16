package com.nomadic.discuz.to.markdown.convert;

import com.nomadic.discuz.to.markdown.domain.Attachment;
import com.nomadic.discuz.to.markdown.domain.Post;
import com.nomadic.discuz.to.markdown.util.TraceLogger;
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


    private static final String REGEX = "\\[%s(.*?)\\](.*?)\\[/%s\\]";

    private List<Parser> parsers = new LinkedList<Parser>() {
        {
            add(new FilenameParser(PostParser.this));
            add(new AttachmentParser(PostParser.this));
            add(new IParser(PostParser.this));
            add(new UrlParser(PostParser.this));
        }
    };
    private String dictionary;
    private String fileName;
    private String result;

    private List<ImmutablePair<String, String>> attachmentList = new ArrayList<>();

    private Post post;
    private Map<Long, Attachment> allAttachMap;

    public PostParser(Post post, List<Attachment> allAttach) {
        this.post = post;
        result = post.getMessage();
        IterableUtils.forEach(allAttach, attachment -> {
            if (allAttachMap == null) {
                allAttachMap = new HashMap<>();
            }
            allAttachMap.put(attachment.getAid(), attachment);
        });
    }

    public void parse() {
        for (Parser parser : parsers) {
            parser.parse();
        }
    }

    private interface Parser {

        void parse();
    }

    private static class FilenameParser implements Parser {

        private static final String SPLIT = "_";
        private static final String FILE_NAME_POST = ".md";

        private static final Map<String, Integer> SEQUENCE = new HashMap<>();

        private PostParser postParser;

        private FilenameParser(PostParser postParser) {
            this.postParser = postParser;
        }

        public void parse() {
            long dateline = postParser.post.getDateline();
            postParser.dictionary = FastDateFormat.getInstance("yyyyMM").format(new Date(dateline * 1000));
            String fileNamePrefix = FastDateFormat.getInstance("yyyyMMdd").format(new Date(dateline * 1000));
            Integer index = (SEQUENCE.get(fileNamePrefix) == null ? 1 : SEQUENCE.get(fileNamePrefix) + 1);
            postParser.fileName = fileNamePrefix + SPLIT + String.format("%02d", index) + FILE_NAME_POST;
            SEQUENCE.put(fileNamePrefix, index);
            TraceLogger.LOGGER.info("filename is " + postParser.dictionary + "/" + postParser.fileName);
        }
    }

    private static class AttachmentParser implements Parser {
        private static final String IMG_SPLIT = "_img_";
        private static final String TAG_ATTACH = "attach";
        private static final Pattern PATTERN_TAG_ATTACH = Pattern.compile(String.format(REGEX, TAG_ATTACH, TAG_ATTACH), Pattern.CASE_INSENSITIVE);
        private PostParser postParser;

        private AttachmentParser(PostParser postParser) {
            this.postParser = postParser;
        }

        public void parse() {
            Matcher matcher = PATTERN_TAG_ATTACH.matcher(postParser.result);
            while (matcher.find()) {
                String matchAll = matcher.group();
                String match = matcher.group(2);
                Attachment attachment = postParser.allAttachMap.get(Long.valueOf(match));

                String imgExt = attachment.getAttachment().substring(attachment.getAttachment().lastIndexOf("."));
                String imageFileName = postParser.fileName.substring(0, postParser.fileName.length() - 3) + IMG_SPLIT + String.format("%03d", postParser.attachmentList.size() + 1) + imgExt;
                String targetAttachment = new File(postParser.dictionary, imageFileName).getPath();

                String markdownImg = String.format("![%s](%s)", attachment.getFilename(), imageFileName);

                TraceLogger.LOGGER.info(String.format("attachement %s --> %s", attachment.getAttachment(), targetAttachment));
                postParser.result = postParser.result.replace(matchAll, markdownImg);

                postParser.attachmentList.add(ImmutablePair.of(attachment.getAttachment(), targetAttachment));
            }
        }
    }

    private static class IParser implements Parser {
        private static final String TAG_I_START = "i=s";
        private static final String TAG_I_END = "i";
        private static final Pattern PATTERN_TAG_I = Pattern.compile(String.format(REGEX, TAG_I_START, TAG_I_END), Pattern.CASE_INSENSITIVE);

        private PostParser postParser;

        private IParser(PostParser postParser) {
            this.postParser = postParser;
        }

        public void parse() {
            Matcher matcher = PATTERN_TAG_I.matcher(postParser.result);
            while (matcher.find()) {
                String matchAll = matcher.group();
                String match = matcher.group(2);
                TraceLogger.LOGGER.info("delete content " + matchAll);
                postParser.result = postParser.result.replace(matchAll, "");
            }
        }
    }

    private static class UrlParser implements Parser {
        private static final String TAG_URL_START = "url=";
        private static final String TAG_URL_END = "url";
        private static final Pattern PATTERN_TAG_URL = Pattern.compile(String.format(REGEX, TAG_URL_START, TAG_URL_END), Pattern.CASE_INSENSITIVE);

        private static final String TEMPLATE = "[%s](%s)";

        private PostParser postParser;

        private UrlParser(PostParser postParser) {
            this.postParser = postParser;
        }

        public void parse() {
            Matcher matcher = PATTERN_TAG_URL.matcher(postParser.result);
            while (matcher.find()) {
                String matchAll = matcher.group();
                String url = matcher.group(1);
                String text = matcher.group(2);
                String markdownUrl = String.format(TEMPLATE, text, url);
                TraceLogger.LOGGER.info("[url replace] " + matchAll + " --> " + markdownUrl);
                postParser.result = postParser.result.replace(matchAll, markdownUrl);
            }
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
