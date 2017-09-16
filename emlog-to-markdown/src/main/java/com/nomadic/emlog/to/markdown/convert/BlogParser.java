package com.nomadic.emlog.to.markdown.convert;

import com.nomadic.emlog.to.markdown.domain.Attachment;
import com.nomadic.emlog.to.markdown.domain.Blog;
import com.nomadic.emlog.to.markdown.util.TraceLogger;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.StringUtils;
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
public class BlogParser {


    private static final String REGEX = "<%s(.*?)>([\\s\\S]*?)</%s>";

    private List<Parser> parsers = new LinkedList<Parser>() {
        {
            add(new FilenameParser(BlogParser.this));
            add(new AttachmentParser(BlogParser.this));
            add(new IParser(BlogParser.this));
            add(new UrlParser(BlogParser.this));

            // 顺序不同，可能结果不同
            add(new RemoveTagParser(RemoveTagParser.TAG_A, BlogParser.this));
            add(new RemoveTagParser(RemoveTagParser.TAG_B, BlogParser.this));
            add(new RemoveTagParser(RemoveTagParser.TAG_P, BlogParser.this));
            add(new RemoveTagParser(RemoveTagParser.TAG_SPAN, BlogParser.this));
            add(new RemoveTagParser(RemoveTagParser.TAG_DIV, BlogParser.this));
            add(new RemoveTagParser(RemoveTagParser.TAG_U, BlogParser.this));
            add(new RemoveTagParser(RemoveTagParser.TAG_BLOCKQUOTE, BlogParser.this));
            add(new RemoveTagParser(RemoveTagParser.TAG_H1, BlogParser.this));
            add(new RemoveTagParser(RemoveTagParser.TAG_H2, BlogParser.this));
            add(new RemoveTagParser(RemoveTagParser.TAG_H3, BlogParser.this));
            add(new RemoveTagParser(RemoveTagParser.TAG_WBR, BlogParser.this));
            add(new RemoveTagParser(RemoveTagParser.TAG_STRONG, BlogParser.this));
            add(new RemoveTagParser(RemoveTagParser.TAG_META, BlogParser.this));
            add(new RemoveTagParser(RemoveTagParser.TAG_UL, BlogParser.this));
            add(new RemoveTagParser(RemoveTagParser.TAG_SECTION, BlogParser.this));


            add(new CodeParser(BlogParser.this));
            add(new TableParser(BlogParser.this));

            add(new SimpleReplaceParser("\n---", "\n\n---", BlogParser.this));
            add(new SimpleReplaceParser("[*]", "", BlogParser.this));
            add(new SimpleReplaceParser("*", "&#42;", BlogParser.this));
            add(new SimpleReplaceParser("&nbsp;", "", BlogParser.this));
            add(new SimpleReplaceParser("<br />", "", BlogParser.this));
            add(new SimpleReplaceParser("<wbr>", "", BlogParser.this));
            add(new SimpleReplaceParser("<p>", "", BlogParser.this));

        }
    };
    private String dictionary;
    private String fileName;
    private String subject;
    private String result;

    private List<ImmutablePair<String, String>> attachmentList = new ArrayList<>();

    private Blog blog;
    private Map<Long, Attachment> allAttachMap;

    public BlogParser(Blog blog, List<Attachment> allAttach) {
        this.blog = blog;
        subject = blog.getTitle();
        result = blog.getContent();
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

        private BlogParser postParser;

        private FilenameParser(BlogParser postParser) {
            this.postParser = postParser;
        }

        public void parse() {
            long dateline = postParser.blog.getDate();
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
        private static final Pattern PATTERN_TAG_ATTACH = Pattern.compile("<img([\\s\\S]*?)src=([\\s\\S]*?)(\\s([\\s\\S]*?))?/>", Pattern.CASE_INSENSITIVE);
        private BlogParser postParser;

        private AttachmentParser(BlogParser postParser) {
            this.postParser = postParser;
        }

        public void parse() {
            Matcher matcher = PATTERN_TAG_ATTACH.matcher(postParser.result);
            while (matcher.find()) {
                String matchAll = matcher.group();
                String match = matcher.group(2);
                match = match.trim().replace("\"", "").replace("thum-", "");
                Attachment attachment = new Attachment();
                attachment.setFilename("");
                attachment.setFilepath(match);

                String imgExt = attachment.getFilepath().substring(attachment.getFilepath().lastIndexOf("."));
                String imageFileName = postParser.fileName.substring(0, postParser.fileName.length() - 3) + IMG_SPLIT + String.format("%03d", postParser.attachmentList.size() + 1) + imgExt;
                String targetAttachment = new File(postParser.dictionary, imageFileName).getPath();

                String template = "![%s](%s)";
                if (imgExt.equals(".zip")) {
                    template = template.substring(1);
                }

                String markdownImg = String.format(template, attachment.getFilename(), imageFileName);

                TraceLogger.LOGGER.info(String.format("attachement %s --> %s", attachment.getFilepath(), targetAttachment));
                postParser.result = postParser.result.replace(matchAll, markdownImg);

                postParser.attachmentList.add(ImmutablePair.of(attachment.getFilepath(), targetAttachment));
            }
        }
    }

    private static class IParser implements Parser {
        private static final Pattern PATTERN_TAG_I = Pattern.compile("<!--[\\S\\s]*?-->", Pattern.CASE_INSENSITIVE);

        private BlogParser postParser;

        private IParser(BlogParser postParser) {
            this.postParser = postParser;
        }

        public void parse() {
            Matcher matcher = PATTERN_TAG_I.matcher(postParser.result);
            while (matcher.find()) {
                String matchAll = matcher.group();
                TraceLogger.LOGGER.info("delete content " + matchAll);
                postParser.result = postParser.result.replace(matchAll, "");
            }
        }
    }

    private static class UrlParser implements Parser {
        private static final String TAG_URL_START = "url=";
        private static final String TAG_URL_END = "url";
        private static final Pattern PATTERN_TAG_URL = Pattern.compile(String.format(REGEX, TAG_URL_START, TAG_URL_END), Pattern.CASE_INSENSITIVE);
        private static final Pattern PATTERN_TAG_URL2 = Pattern.compile(String.format(REGEX, TAG_URL_END, TAG_URL_END), Pattern.CASE_INSENSITIVE);

        private static final String TEMPLATE = "[%s](%s)";

        private BlogParser postParser;

        private UrlParser(BlogParser postParser) {
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

            matcher = PATTERN_TAG_URL2.matcher(postParser.result);
            while (matcher.find()) {
                String matchAll = matcher.group();
                String text = matcher.group(2);
                String markdownUrl = String.format(TEMPLATE, text, text);
                TraceLogger.LOGGER.info("[url replace] " + matchAll + " --> " + markdownUrl);
                postParser.result = postParser.result.replace(matchAll, markdownUrl);
            }
        }
    }

    private static class RemoveTagParser implements Parser {
        private static final String TAG_FONT = "font";
        private static final String TAG_A = "a";
        private static final String TAG_B = "b";
        private static final String TAG_P = "p";
        private static final String TAG_SPAN = "span";
        private static final String TAG_DIV = "div";
        private static final String TAG_BLOCKQUOTE = "blockquote";
        private static final String TAG_U = "u";
        private static final String TAG_H1 = "h1";
        private static final String TAG_H2 = "h2";
        private static final String TAG_H3 = "h3";
        private static final String TAG_WBR = "wbr";
        private static final String TAG_STRONG = "strong";
        private static final String TAG_META = "meta";
        private static final String TAG_UL = "ul";
        private static final String TAG_SECTION = "section";

        private Pattern PATTERN_TAG;

        private String tag;

        private BlogParser postParser;

        private RemoveTagParser(String tag, BlogParser postParser) {
            this.postParser = postParser;
            this.tag = tag;
            PATTERN_TAG = Pattern.compile(String.format(REGEX, tag, tag), Pattern.CASE_INSENSITIVE);
        }

        public void parse() {
            boolean find = true;
            while (find) {
                find = false;
                Matcher matcher = PATTERN_TAG.matcher(postParser.result);
                while (matcher.find()) {
                    find = true;
                    String matchAll = matcher.group();
                    String tag = matcher.group(1);
                    String text = matcher.group(2);
                    TraceLogger.LOGGER.info("[tag " + this.tag + " delete] ");
                    // 只保留文字
                    postParser.result = postParser.result.replace(matchAll, text);
                }
            }
        }
    }

    private static class CodeParser implements Parser {
        private static final String TAG_START = "mw_shl_code";
        private static final String TAG_END = "mw_shl_code";
        private static final Pattern PATTERN_TAG = Pattern.compile(String.format(REGEX, TAG_START, TAG_END), Pattern.CASE_INSENSITIVE);

        private static final String TEMPLATE = "\n```\n%s\n```\n";

        private BlogParser postParser;

        private CodeParser(BlogParser postParser) {
            this.postParser = postParser;
        }

        public void parse() {
            Matcher matcher = PATTERN_TAG.matcher(postParser.result);
            while (matcher.find()) {
                String matchAll = matcher.group();
                String tag = matcher.group(1);
                String text = matcher.group(2);
                String markdownUrl = String.format(TEMPLATE, text);
                TraceLogger.LOGGER.info("[code replace] " + matchAll + " --> " + markdownUrl);
                postParser.result = postParser.result.replace(matchAll, markdownUrl);
            }
        }
    }

    private static class TableParser implements Parser {

        private static final Pattern PATTERN_TAG_TABLE = Pattern.compile(String.format(REGEX, "table", "table"), Pattern.CASE_INSENSITIVE);
        private static final Pattern PATTERN_TAG_TR = Pattern.compile(String.format(REGEX, "tr", "tr"), Pattern.CASE_INSENSITIVE);
        private static final Pattern PATTERN_TAG_TD = Pattern.compile(String.format(REGEX, "td", "td"), Pattern.CASE_INSENSITIVE);
        private static final Pattern PATTERN_TAG_TH = Pattern.compile(String.format(REGEX, "th", "th"), Pattern.CASE_INSENSITIVE);

        private static final String TEMPLATE = "\n```\n%s\n```\n";

        private BlogParser postParser;

        private TableParser(BlogParser postParser) {
            this.postParser = postParser;
        }

        public void parse() {


            Matcher tableMatcher = PATTERN_TAG_TABLE.matcher(postParser.result);
            // 匹配到表格
            while (tableMatcher.find()) {

                TraceLogger.LOGGER.info(String.format("blog %s has table", postParser.fileName));

                // 存储最大的列数据，用于生成表头
                int maxCol = 0;
                // 将行的数据存储起来，用于最后生成表格
                List<List<String>> trList = new LinkedList<>();

                String matchAll = tableMatcher.group();
                String text = tableMatcher.group(2);

                Matcher trMatcher = PATTERN_TAG_TR.matcher(text);
                // 匹配到行tr
                while (trMatcher.find()) {
                    String trText = trMatcher.group(2);
                    Matcher thMatcher = PATTERN_TAG_TH.matcher(trText);
                    if (thMatcher.find()) {
                        System.out.println("不支持，打断点");
                    }
                    List<String> tdList = new LinkedList<>();
                    Matcher tdMatcher = PATTERN_TAG_TD.matcher(trText);
                    // 匹配到列td
                    while (tdMatcher.find()) {
                        String tdText = tdMatcher.group(2);
                        if (StringUtils.isNotBlank(tdText)) {
                            // 避免和markdown语法冲突
                            tdList.add(tdText.replace("|", "&#124;")
                                    .replace("\r\n", "")
                                    .replace("\n", ""));
                            System.out.println("");
                        }
                    }
                    if (tdList.size() > 0) {
                        trList.add(tdList);
                    }
                    maxCol = maxCol > tdList.size() ? maxCol : tdList.size();
                }

                List<String> markdownTable = new LinkedList<>();

                // 生成表头，由于获取不到表头，生成空表头
                String[] heads = new String[maxCol];
                Arrays.fill(heads, "None");
                String head = StringUtils.join("|", StringUtils.join(heads, "|"), "|");
                markdownTable.add(head);

                // 生成分割符行
                String[] headSplits = new String[maxCol];
                Arrays.fill(headSplits, "-");
                String headSplit = StringUtils.join("|", StringUtils.join(headSplits, "|"), "|");
                markdownTable.add(headSplit);

                // 生成每行数据
                for (List<String> strings : trList) {
                    String[] lines = new String[strings.size()];
                    Arrays.fill(lines, "%s");
                    String line = StringUtils.join("|", StringUtils.join(lines, "|"), "|");

                    String lineString = String.format(line, strings.toArray());
                    markdownTable.add(lineString);
                }
                // 生成最终表格数据
                String lineResult = StringUtils.join("\n\n", StringUtils.join(markdownTable, "\n"), "\n\n");

                TraceLogger.LOGGER.info("[table replace] " + matchAll + " --> " + lineResult);
                postParser.result = postParser.result.replace(matchAll, lineResult);
            }
        }
    }

    private static class SimpleReplaceParser implements Parser {

        private BlogParser postParser;
        private String source;
        private String target;

        private SimpleReplaceParser(String source, String target, BlogParser postParser) {
            this.source = source;
            this.target = target;
            this.postParser = postParser;
        }

        public void parse() {
            postParser.result = postParser.result.replace(source, target);
        }
    }

    public String getDictionary() {
        return dictionary;
    }

    public String getFileName() {
        return fileName;
    }

    public String getSubject() {
        return subject;
    }

    public String getResult() {
        return result;
    }

    public List<ImmutablePair<String, String>> getAttachmentList() {
        return attachmentList;
    }
}
