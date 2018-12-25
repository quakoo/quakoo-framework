package com.quakoo.space.annotation.mq;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.quakoo.space.enums.mq.HyperspaceSendToMqType;

/**
 * 暂时不支持
 * @author LiYongbiao1
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface HyperspaceSendToMq {
    /**
     * 选择在哪些时候发送mq
     * 
     * @return
     */
    HyperspaceSendToMqType[] types();

    /**
     * 对应的topicName
     * 
     * @return
     */
    String[] topicNames();

    /**
     * 是否同步发送
     * 
     * @return
     */
    boolean[] synchros();

}
