package com.quakoo.baseFramework.util;

public class UnicodeUtils {

    public static void main(String[] args) throws Exception {
        String str = "wefwee\\u7531fwef\\u4e8e\\u60a8\\u767b\\u5f55\\u5f02\\u5e38\\uff0c\\u8bf7\\u586b\\u5199\\u9a8c\\u8bc1\\u7801";
        // "\\u8bf7\\u5148\\u767b\\u5f55";
        // "\\u4e0d\\u80fd\\u9080\\u8bf7\\u4f60\\u81ea\\u5df1";
        System.out.println(unicodeCover(str));
    }

    public static String unicodeCover(String str) {
        StringBuffer sb = new StringBuffer("");
        str = str.trim();
        int fIndex = str.indexOf("\\u");
        if (fIndex > 0) {
            String beginStr = str.substring(0, fIndex);
            str = str.substring(fIndex);
            sb.append(beginStr);
        }
        String strArr[] = str.split("\\\\u");

        if (strArr.length == 1) {
            return str;
        }

        for (int i = 0; i < strArr.length; i++) {

            if (strArr[i] != null && !strArr[i].equals("")) {
                sb.append((char) Integer.parseInt(strArr[i].substring(0, 4), 16));
                sb.append(strArr[i].substring(4));
            }
        }
        return sb.toString();
    }

}
