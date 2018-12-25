package com.quakoo.baseFramework.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.MatchResult;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternCompiler;
import org.apache.oro.text.regex.PatternMatcher;
import org.apache.oro.text.regex.PatternMatcherInput;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;

public class StringUtil {

    static String pattern = "http[s]*://[^\\s]+";

    static String shortPattern = "http[s]*://[^/]+/[0-9A-Za-z]+";

    static Pattern linePattern = null;

    static Pattern shortUrlPattern = null;
    static {
        PatternCompiler compiler = new Perl5Compiler();
        try {
            linePattern = compiler.compile(pattern);
            shortUrlPattern = compiler.compile(shortPattern);
        } catch (MalformedPatternException e) {
            e.printStackTrace();
        }
    }

    public static List<String> getUrlFromText(String text, boolean shortUrl) {
        List<String> result = new ArrayList<String>();
        PatternMatcher match = new Perl5Matcher();
        PatternMatcherInput msgString = new PatternMatcherInput(text);
        while (match.contains(msgString, shortUrl ? shortUrlPattern : linePattern)) {
            MatchResult matcher = match.getMatch();
            result.add(matcher.group(0));
        }
        return result;
    }


    public static String cleanShortUrl(String text) {
        PatternMatcher match = new Perl5Matcher();
        PatternMatcherInput msgString = new PatternMatcherInput(text);
        while (match.contains(msgString,  shortUrlPattern )) {
            MatchResult matcher = match.getMatch();
            text=text.replace(matcher.group(0),"");
        }
        return text;
    }

    public static String replaceLast(String org, String target, String replacement) {
        int i = org.length() - target.length();
        for (; i >= 0; i--) {
            if (org.substring(i, i + target.length()).equals(target)) {
                return org.substring(0, i) + replacement + org.substring(i + target.length(), org.length());
            }
        }
        return org;
    }
    
    public static String htmlEncode(String html){
    	 	 html = html.replaceAll( "&", "&amp;");
         html = html.replaceAll( "\"", "&quot;");  //"
         html = html.replaceAll( "\t", "&nbsp;&nbsp;");// 替换跳格
         html = html.replaceAll( " ", "&nbsp;");// 替换空格
         html = html.replaceAll("<", "&lt;");
         html = html.replaceAll( ">", "&gt;");
         html = html.replaceAll( "\r\n", "<br/>");
         html = html.replaceAll( "\r", "<br/>");
         html = html.replaceAll( "\n", "<br/>");
         return html;
    }
    
    public static final char UNDERLINE='_';  

    public static String camelToUnderline(String param){  
        if (param==null||"".equals(param.trim())){  
            return "";  
        }  
        int len=param.length();  
        StringBuilder sb=new StringBuilder(len);  
        for (int i = 0; i < len; i++) {  
            char c=param.charAt(i);  
            if (Character.isUpperCase(c)){  
                sb.append(UNDERLINE);  
                sb.append(Character.toLowerCase(c));  
            }else{  
                sb.append(c);  
            }  
        }  
        return sb.toString();  
    }  

    public static void main(String[] sdf) {
        System.out.println(replaceLast("testetste11ygr", "gr", ""));
        System.out.println(replaceLast("testetste11ygr", "11", ""));
        System.out.println(replaceLast("testetste11ygr", "r", ""));
        System.out.println(replaceLast("dwtestetste11ygr", "dw", ""));

        System.out.println(getUrlFromText("asdfsd http://www.youku.com/abc/xxh第三方 sddfdf"
                + " http://www.youku.com/abcsdf/xxh sdfefe http://www.abc.youku/abcfet无法 dfdf", false));
        System.out.println(getUrlFromText("asdfsd http://t.cn/zlvAwiZ第三方 sddfdf"
                + " http://www.youku.com/abcsdf/xxh sdfefe http://www.abc.youku/abcfet无法 dfdf", true));
        System.out
                .println(getUrlFromText(
                        "【三沙市首任市长:将重点关注环保民生等工作】三沙市第一届人民代表大会第一次会议今日下午闭幕，肖杰当选第一任市长。肖杰接受采访时表示将努力工作，践行诺言，把三沙各项工作做好，上任后将重点关注三沙行政管理、开发建设、环境保护和民生工作。http://t.cn/zWaTVjU视频：http://t.cn/zWaTGA",
                        true));

        String url = "jsonp1364287660888({result: {status: {msg: channel ty newsid 1-334-858129 is not exist, code: 1}, data: {comments: []}, encoding: utf-8}})";
        System.out.println(url.replace("jsonp1364287660888(", ""));
        System.out.println(url.replace("jsonp1364287660888\\(", ""));

    }
}
