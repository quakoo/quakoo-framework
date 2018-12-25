package com.quakoo.yarnFramework;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by 136249 on 2015/3/11.
 */
public class DefaultScloudAm extends AbstractAm {
    static Logger logger= LoggerFactory.getLogger(DefaultScloudAm.class);
    public static void main(String[] args){
        System.out.println("DefaultScloudAm come in");
        logger.info("=====DefaultScloudAm come in");
        try {
            DefaultScloudAm defaultScloudAm = new DefaultScloudAm();
            defaultScloudAm.init(args);
            defaultScloudAm.run();
        }catch (Throwable t){
            ExitUtis.exit(t,1);
        }
    }

}
