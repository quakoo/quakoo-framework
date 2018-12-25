package com.quakoo.baseFramework.util;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Random;

import com.quakoo.baseFramework.secure.MD5Utils;

public class YeHuoEnCodeUtil {

    public static String decode(String msg) {
        char[] chars = msg.toCharArray();
        char begin = (char) 48;
        char step = (char) 58;
        char max = (char) 68;
        char[] dechars = new char[chars.length];
        for (int i = 0; i < chars.length; i++) {
            dechars[i] = (char) getdecodeChar(chars[i], i, begin, step, max);
        }
        return new String(dechars);
    }

    public static String encode(String msg) {
        char begin = (char) 48;
        char step = (char) 58;
        char max = (char) 68;
        char[] chars = msg.toCharArray();
        char[] list = new char[chars.length];
        for (int i = 0; i < chars.length; i++) {
            list[i] = ((char) (getAndAdd((chars[i]), i, begin, step, max)));
        }
        String result = new String(list);
        return result;
    }

    public static int getAndAdd(char c, int index, int begin, int step, int max) {
        int result = begin + index * step;
        if (result > max) {
            result = result % max;
        }
        result = c + result;
        if (result > 126) {
            result = result - 126 + 33;
        }
        if (result < 33 || result > 126) {
            throw new RuntimeException();
        }
        return result;
    }

    public static int getdecodeChar(char c, int index, int begin, int step, int max) {
        int result = begin + index * step;
        if (result > max) {
            result = result % max;
        }
        result = c - result;
        if (result < 33) {
            result = result + 126 - 33;
        }
        if (result < 33 || result > 126) {
            throw new RuntimeException();
        }
        return result;
    }

    public static void main(String[] sdg) throws Exception {

        long bf = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            String msg = MD5Utils.md5ReStr(("" + new Random().nextInt(10000000) + new Random().nextInt(10000000)).getBytes());
            String incode = encode(msg);
            String in = URLDecoder.decode(URLEncoder.encode(incode));
            String de = decode(in);
            // System.out.println(in);
            System.out.println(msg + "===" + in + "===" + URLEncoder.encode(incode));
            if (!msg.equals(de)) {
                System.out.println(msg);
                System.out.println(in);
                System.out.println(de);
            }
        }
        System.out.println(MD5Utils.md5ReStr(("" + 29 + 13869).getBytes()));
        System.out.println(encode(MD5Utils.md5ReStr(("" + 29 + 13869).getBytes())));
        System.out.println(decode("7XUGk{A`+JBiw=eZyBeuj`(HEgs7^TzA"));
        System.out.println(System.currentTimeMillis() - bf);
    }

}
