//package com.quakoo.baseFramework.sensitive.processor;
//
//import java.util.Map;
//
//import com.quakoo.baseFramework.sensitive.ProcessRes;
//import org.apache.commons.lang3.StringUtils;
//
//import com.quakoo.baseFramework.sensitive.KeyWord;
//import com.quakoo.baseFramework.sensitive.util.AnalysisUtil;
//
//
///**
// * 对文本进行高亮处理。
// *
// * @author hailin0@yeah.net
// * @createDate 2016年5月22日
// *
// */
//@SuppressWarnings({ "unchecked", "rawtypes" })
//public class Highlight implements Processor {
//    /**
//     * 将文本中的关键词提取出来。
//     *
//     * @param wordsTree 关键词库树
//     * @param text 待处理的文本
//     * @return 返回提取的关键词或null
//     */
//    public ProcessRes process(Map<String, Map> wordsTree, String text, AbstractFragment fragment,
//                              int minLen) {
//        StringBuffer result = new StringBuffer("");
//        int num = 0;
//        String pre = null;// 词的前面一个字
//        while (true) {
//            if (wordsTree == null || wordsTree.isEmpty() || StringUtils.isEmpty(text)) {
//                ProcessRes processRes = new ProcessRes();
//                processRes.setContent(result.append(text).toString());
//                processRes.setNum(num);
//                return processRes;
//            }
//            if (text.length() < minLen) {
//                ProcessRes processRes = new ProcessRes();
//                processRes.setContent(result.append(text).toString());
//                processRes.setNum(num);
//                return processRes;
//            }
//            String chr = text.substring(0, 1);
//            text = text.substring(1);
//            Map<String, Map> nextWord = wordsTree.get(chr);
//            // 没有对应的下一个字，表示这不是关键词的开头，进行下一个循环
//            if (nextWord == null) {
//                result.append(chr);
//                pre = chr;
//                continue;
//            }
//
//            KeyWord kw = AnalysisUtil.getSensitiveWord(chr, pre, nextWord, text);
//            // 没有匹配到完整关键字，下一个循环
//            if (kw == null) {
//                result.append(chr);
//                pre = chr;
//                continue;
//            }
//
//            // 处理片段
//            result.append(fragment.format(kw));
//            num++;
//            // 从text中去除当前已经匹配的内容，进行下一个循环匹配
//            text = text.substring(kw.getWordLength() - 1);
//            pre = kw.getWord().substring(kw.getWordLength() - 1, kw.getWordLength());
//        }
//    }
//
//}
