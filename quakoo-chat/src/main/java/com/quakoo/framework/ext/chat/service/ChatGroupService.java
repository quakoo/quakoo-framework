package com.quakoo.framework.ext.chat.service;

import java.util.List;

import com.quakoo.framework.ext.chat.model.ChatGroup;

public interface ChatGroupService {
	
	public ChatGroup load(long cgid) throws Exception;

	public List<ChatGroup> load(List<Long> cgids) throws Exception;
	
	public ChatGroup create(String name, List<Long> uids, String icon) throws Exception;
	
	public boolean join(long cgid, List<Long> uids, int maxNum) throws Exception;
	
	public boolean exit(long cgid, List<Long> uids) throws Exception;

	public boolean updateCheck(long cgid, int check) throws Exception;

	public boolean updateNotice(long cgid, String notice) throws Exception;

	public boolean updateIcon(long cgid, String icon) throws Exception;

	public List<Long> userIds(long cgid) throws Exception;
	
}