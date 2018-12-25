package com.quakoo.framework.ext.chat.service.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import javax.annotation.Resource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.quakoo.baseFramework.jackson.JsonUtils;
import com.quakoo.baseFramework.lock.ZkLock;
import com.quakoo.framework.ext.chat.AbstractChatInfo;
import com.quakoo.framework.ext.chat.dao.ChatGroupDao;
import com.quakoo.framework.ext.chat.model.ChatGroup;
import com.quakoo.framework.ext.chat.service.ChatGroupService;

public class ChatGroupServiceImpl implements ChatGroupService {

	@Resource
	private ChatGroupDao chatGroupDao;

    @Resource
    private AbstractChatInfo chatInfo;

	public ChatGroup load(long cgid) throws Exception {
		return chatGroupDao.load(cgid);
	}
	
	public ChatGroup create(String name, List<Long> uids, String icon) throws Exception {
		String uidStr = JsonUtils.toJson(uids);
		ChatGroup chatGroup = new ChatGroup();
		chatGroup.setName(name);
		chatGroup.setUids(uidStr);
        chatGroup.setIcon(icon);
		return chatGroupDao.insert(chatGroup);
	}

    @Override
    public boolean updateNotice(long cgid, String notice) throws Exception {
        ZkLock lock = null;
	    try {
            lock = ZkLock.getAndLock(chatInfo.lockZkAddress,
                    chatInfo.projectName, "chat_group" + AbstractChatInfo.lock_suffix,
                    true, AbstractChatInfo.session_timout, AbstractChatInfo.lock_timeout);
            boolean res = false;
            ChatGroup chatGroup = chatGroupDao.load(cgid);
            if(null != chatGroup) {
                chatGroup.setNotice(notice);
                res = chatGroupDao.update(chatGroup);
            }
            return res;
        } finally {
            if (lock != null) lock.release();
        }
    }

    @Override
    public boolean updateCheck(long cgid, int check) throws Exception {
        ZkLock lock = null;
	    try {
            lock = ZkLock.getAndLock(chatInfo.lockZkAddress,
                    chatInfo.projectName, "chat_group" + AbstractChatInfo.lock_suffix,
                    true, AbstractChatInfo.session_timout, AbstractChatInfo.lock_timeout);
            boolean res = false;
            ChatGroup chatGroup = chatGroupDao.load(cgid);
            if(null != chatGroup) {
                chatGroup.setCheck(check);
                res = chatGroupDao.update(chatGroup);
            }
            return res;
        } finally {
            if (lock != null) lock.release();
        }
    }



    public boolean join(long cgid, long uid, String icon) throws Exception {
        ZkLock lock = null;
        try {
            lock = ZkLock.getAndLock(chatInfo.lockZkAddress,
                    chatInfo.projectName, "chat_group" + AbstractChatInfo.lock_suffix,
                    true, AbstractChatInfo.session_timout, AbstractChatInfo.lock_timeout);
            boolean res = false;
            ChatGroup chatGroup = chatGroupDao.load(cgid);
            if(null != chatGroup) {
                LinkedHashSet<Long> uids = JsonUtils.fromJson(chatGroup.getUids(),
                        new TypeReference<LinkedHashSet<Long>>() {});
                uids.add(uid);
                String uidStr = JsonUtils.toJson(uids);
                chatGroup.setUids(uidStr);
                chatGroup.setIcon(icon);
                res = chatGroupDao.update(chatGroup);
            }
            return res;
        } finally {
            if (lock != null) lock.release();
        }
	}

	public boolean exit(long cgid, long uid, String icon) throws Exception {
        ZkLock lock = null;
	    try {
            lock = ZkLock.getAndLock(chatInfo.lockZkAddress,
                    chatInfo.projectName, "chat_group" + AbstractChatInfo.lock_suffix,
                    true, AbstractChatInfo.session_timout, AbstractChatInfo.lock_timeout);
            boolean res = false;
            ChatGroup chatGroup = chatGroupDao.load(cgid);
            if(null != chatGroup) {
                LinkedHashSet<Long> uids = JsonUtils.fromJson(chatGroup.getUids(),
                        new TypeReference<LinkedHashSet<Long>>() {});
                for(Iterator<Long> it = uids.iterator(); it.hasNext();){
                    if(it.next().longValue() == uid) it.remove();
                }
                String uidStr = JsonUtils.toJson(uids);
                chatGroup.setUids(uidStr);
                chatGroup.setIcon(icon);
                res = chatGroupDao.update(chatGroup);
            }
            return res;
        } finally {
            if (lock != null) lock.release();
        }

	}

	public List<Long> userIds(long cgid) throws Exception {
		List<Long> res = Lists.newArrayList();
		ChatGroup chatGroup = chatGroupDao.load(cgid);
		if(null != chatGroup) {
			res = JsonUtils.fromJson(chatGroup.getUids(), new TypeReference<ArrayList<Long>>() {});
		}
		return res;
	}

}
