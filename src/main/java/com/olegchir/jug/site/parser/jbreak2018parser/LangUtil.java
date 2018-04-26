package com.olegchir.jug.site.parser.jbreak2018parser;
import org.apache.commons.text.StringEscapeUtils;

public class LangUtil {
    public static String escapeHtml(String input) {
        input = StringEscapeUtils.escapeHtml4(input);
        input = escapeChevronQuotes(input);
        return input;
    }

    public static String escapeChevronQuotes(String src) {
        return src.replaceAll("&quot;(.*?)&quot;","«$1»");
    }
}
