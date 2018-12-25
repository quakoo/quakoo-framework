package com.quakoo.framework.ext.chat.service.ext;

import com.quakoo.framework.ext.chat.model.ext.OtherChatRes;

public interface OtherChatPrevService {

    public OtherChatRes handle(long uid, int type, long thirdId,
                               String word, String picture, String voice, String voiceDuration,
                               String video, String videoDuration, String ext);

}
