package com.quakoo.framework.ext.chat.service.impl;

import com.quakoo.framework.ext.chat.dao.MessageDao;
import com.quakoo.framework.ext.chat.service.MessageService;

import javax.annotation.Resource;

/**
 * 消息处理类
 * class_name: MessageServiceImpl
 * package: com.quakoo.framework.ext.chat.service.impl
 * creat_user: lihao
 * creat_date: 2019/1/29
 * creat_time: 18:20
 **/
public class MessageServiceImpl implements MessageService {

    @Resource
    private MessageDao messageDao;

    /**
     * 更新消息内容
     * method_name: updateContent
     * params: [id, content]
     * return: boolean
     * creat_user: lihao
     * creat_date: 2019/1/29
     * creat_time: 18:20
     **/
    @Override
    public boolean updateContent(long id, String content) throws Exception {
        return messageDao.updateContent(id, content);
    }

}
