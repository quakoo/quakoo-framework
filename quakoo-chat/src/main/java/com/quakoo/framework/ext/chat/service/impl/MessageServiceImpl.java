package com.quakoo.framework.ext.chat.service.impl;

import com.quakoo.framework.ext.chat.dao.MessageDao;
import com.quakoo.framework.ext.chat.service.MessageService;

import javax.annotation.Resource;

public class MessageServiceImpl implements MessageService {

    @Resource
    private MessageDao messageDao;

    @Override
    public boolean updateContent(long id, String content) throws Exception {
        return messageDao.updateContent(id, content);
    }

}
