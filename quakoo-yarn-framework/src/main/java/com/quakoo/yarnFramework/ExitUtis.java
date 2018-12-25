package com.quakoo.yarnFramework;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by 136249 on 2015/3/10.
 */
public class ExitUtis {
    static  Logger logger= LoggerFactory.getLogger(ExitUtis.class);
    public static void exit(Throwable t, int i) {
        logger.error("system exit with error ",t);
        System.err.println(ExceptionUtils.getFullStackTrace(t));
        System.exit(i);
    }

    public static void exit(String errMsg, int i) {
        logger.error("system exit with errMsg "+errMsg);
        System.err.println(errMsg);
        System.exit(i);
    }
}
