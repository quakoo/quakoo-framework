package com.quakoo.yarnFramework;

import com.fasterxml.jackson.core.type.TypeReference;
import com.quakoo.baseFramework.json.JsonUtils;
import com.quakoo.baseFramework.secure.Base64Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLDecoder;
import java.util.Map;

/**
 * Created by 136249 on 2015/3/11.
 */
public class TaskHelper {
    static final Logger logger = LoggerFactory.getLogger(TaskHelper.class);

    public static Map<String, String> getParams(String[] args) {
        try {
            if(args==null||args.length==0){
                return null;
            }
            Map<String, String> map = JsonUtils.parse(new String(Base64Util.decode(URLDecoder.decode(args[0],"utf-8"))),
                    new TypeReference<Map<String, String>>() {
                    });
            return map;
        } catch (Throwable t) {
            logger.error("getParams error", t);
            ExitUtis.exit(t, 1);
        }
        return null;
    }

}
