package com.quakoo.framework.ext.chat.service;

import java.util.List;

import com.quakoo.framework.ext.chat.model.ChatGroup;

public interface ChatGroupService {
	
	public ChatGroup load(long cgid) throws Exception;
	
	public ChatGroup create(String name, List<Long> uids, String icon) throws Exception;
	
	public boolean join(long cgid, long uid, String icon) throws Exception;
	
	public boolean exit(long cgid, long uid, String icon) throws Exception;

	public boolean updateCheck(long cgid, int check) throws Exception;

	public boolean updateNotice(long cgid, String notice) throws Exception;
	
	public List<Long> userIds(long cgid) throws Exception;
	
}