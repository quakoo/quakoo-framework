package com.quakoo.framework.ext.chat.service.ext;

import com.quakoo.framework.ext.chat.model.ext.OtherChatRes;

/**
 * 其他类型消息预处理
 * class_name: OtherChatPrevService
 * package: com.quakoo.framework.ext.chat.service.ext
 * creat_user: lihao
 * creat_date: 2019/1/29
 * creat_time: 17:11
 **/
public interface OtherChatPrevService {

    public OtherChatRes handle(long uid, int type, long thirdId,
                               String word, String picture, String voice, String voiceDuration,
                               String video, String videoDuration, String ext);

}
