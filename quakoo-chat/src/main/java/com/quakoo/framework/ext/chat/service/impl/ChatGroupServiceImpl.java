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

/**
 * 群组处理类
 * class_name: ChatGroupServiceImpl
 * package: com.quakoo.framework.ext.chat.service.impl
 * creat_user: lihao
 * creat_date: 2019/1/29
 * creat_time: 17:17
 **/
public class ChatGroupServiceImpl implements ChatGroupService {

	@Resource
	private ChatGroupDao chatGroupDao;

    @Resource
    private AbstractChatInfo chatInfo;

	public ChatGroup load(long cgid) throws Exception {
		return chatGroupDao.load(cgid);
	}

	/**
     * 创建一个群组
	 * method_name: create
	 * params: [name, uids, icon]
	 * return: com.quakoo.framework.ext.chat.model.ChatGroup
	 * creat_user: lihao
	 * creat_date: 2019/1/29
	 * creat_time: 17:17
	 **/
	public ChatGroup create(String name, List<Long> uids, String icon) throws Exception {
		String uidStr = JsonUtils.toJson(uids);
		ChatGroup chatGroup = new ChatGroup();
		chatGroup.setName(name);
		chatGroup.setUids(uidStr);
        chatGroup.setIcon(icon);
		return chatGroupDao.insert(chatGroup);
	}

	/**
     * 更新群公告
	 * method_name: updateNotice
	 * params: [cgid, notice]
	 * return: boolean
	 * creat_user: lihao
	 * creat_date: 2019/1/29
	 * creat_time: 17:18
	 **/
    @Override
    public boolean updateNotice(long cgid, String notice) throws Exception {
        ZkLock lock = null;
	    try {
            lock = ZkLock.getAndLock(chatInfo.lockZkAddress,
                    chatInfo.projectName, "chat_group_id_" + cgid + AbstractChatInfo.lock_suffix,
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
    public boolean updateIcon(long cgid, String icon) throws Exception {
        ZkLock lock = null;
        try {
            lock = ZkLock.getAndLock(chatInfo.lockZkAddress,
                    chatInfo.projectName, "chat_group_id_" + cgid + AbstractChatInfo.lock_suffix,
                    true, AbstractChatInfo.session_timout, AbstractChatInfo.lock_timeout);
            boolean res = false;
            ChatGroup chatGroup = chatGroupDao.load(cgid);
            if(null != chatGroup) {
                chatGroup.setIcon(icon);
                res = chatGroupDao.update(chatGroup);
            }
            return res;
        } finally {
            if (lock != null) lock.release();
        }
    }

    /**
     * 更新群主入群审核开关
     * method_name: updateCheck
     * params: [cgid, check]
     * return: boolean
     * creat_user: lihao
     * creat_date: 2019/1/29
     * creat_time: 17:18
     **/
    @Override
    public boolean updateCheck(long cgid, int check) throws Exception {
        ZkLock lock = null;
	    try {
            lock = ZkLock.getAndLock(chatInfo.lockZkAddress,
                    chatInfo.projectName, "chat_group_id_" + cgid + AbstractChatInfo.lock_suffix,
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

    /**
     * 添加用户到群组
     * method_name: join
     * params: [cgid, uid, icon]
     * return: boolean
     * creat_user: lihao
     * creat_date: 2019/1/29
     * creat_time: 17:19
     **/
    public boolean join(long cgid, List<Long> uids, int maxNum) throws Exception {
        ZkLock lock = null;
        try {
            lock = ZkLock.getAndLock(chatInfo.lockZkAddress,
                    chatInfo.projectName, "chat_group_id_" + cgid + AbstractChatInfo.lock_suffix,
                    true, AbstractChatInfo.session_timout, AbstractChatInfo.lock_timeout);
            boolean res = false;
            ChatGroup chatGroup = chatGroupDao.load(cgid);
            if(null != chatGroup) {
                LinkedHashSet<Long> allUids = JsonUtils.fromJson(chatGroup.getUids(),
                        new TypeReference<LinkedHashSet<Long>>() {});
                allUids.addAll(uids);
                if(allUids.size() <= maxNum) {
                    String uidStr = JsonUtils.toJson(allUids);
                    chatGroup.setUids(uidStr);
                    res = chatGroupDao.update(chatGroup);
                }
            }
            return res;
        } finally {
            if (lock != null) lock.release();
        }
	}

	/**
     * 从群组删除用户
	 * method_name: exit
	 * params: [cgid, uid, icon]
	 * return: boolean
	 * creat_user: lihao
	 * creat_date: 2019/1/29
	 * creat_time: 17:19
	 **/
    public boolean exit(long cgid, List<Long> uids) throws Exception {
        ZkLock lock = null;
	    try {
            lock = ZkLock.getAndLock(chatInfo.lockZkAddress,
                    chatInfo.projectName, "chat_group_id_" + cgid + AbstractChatInfo.lock_suffix,
                    true, AbstractChatInfo.session_timout, AbstractChatInfo.lock_timeout);
            boolean res = false;
            ChatGroup chatGroup = chatGroupDao.load(cgid);
            if(null != chatGroup) {
                LinkedHashSet<Long> allUids = JsonUtils.fromJson(chatGroup.getUids(),
                        new TypeReference<LinkedHashSet<Long>>() {});
                allUids.removeAll(uids);
                String uidStr = JsonUtils.toJson(allUids);
                chatGroup.setUids(uidStr);
                res = chatGroupDao.update(chatGroup);
            }
            return res;
        } finally {
            if (lock != null) lock.release();
        }

	}

	/**
     * 获取群组下所有UID
	 * method_name: userIds
	 * params: [cgid]
	 * return: java.util.List<java.lang.Long>
	 * creat_user: lihao
	 * creat_date: 2019/1/29
	 * creat_time: 17:19
	 **/
	public List<Long> userIds(long cgid) throws Exception {
		List<Long> res = Lists.newArrayList();
		ChatGroup chatGroup = chatGroupDao.load(cgid);
		if(null != chatGroup) {
			res = JsonUtils.fromJson(chatGroup.getUids(), new TypeReference<ArrayList<Long>>() {});
		}
		return res;
	}

}
