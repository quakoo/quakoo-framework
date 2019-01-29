package com.quakoo.framework.ext.chat.service.ext;

import com.quakoo.framework.ext.chat.model.ext.ChatCheckRes;

/**
 * 消息检测(接收到消息进行检测)
 * class_name: ChatCheckService
 * package: com.quakoo.framework.ext.chat.service.ext
 * creat_user: lihao
 * creat_date: 2019/1/29
 * creat_time: 17:10
 **/
public interface ChatCheckService {

    public ChatCheckRes check(long uid, int type, long thirdId, String word);

}
