package com.quakoo.framework.ext.chat.service.ext;

import com.quakoo.framework.ext.chat.model.ext.ChatCheckRes;

public interface ChatCheckService {

    public ChatCheckRes check(long uid, int type, long thirdId, String word);

}
