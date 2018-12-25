package com.quakoo.space.annotation.mq;

import java.io.Serializable;
/**
 * 暂时不支持
 * @author LiYongbiao1
 *
 */
public interface MqMessage extends Serializable {

    public String myTopic();
}
