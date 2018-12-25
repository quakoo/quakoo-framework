package com.quakoo.baseFramework.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class ErrorUtil {

    public static String getErrorStackTraceString(Exception e) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                e.printStackTrace(new PrintStream(baos));
            } finally {
                baos.close();
            }
            return e.getMessage() + "\r\n" + baos.toString();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return null;

    }

}
