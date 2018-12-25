package com.quakoo.framework.ext.chat.service;

import java.util.List;

public interface NoticeService {
	
	public boolean noticeAll(long authorId, String title, String desc, 
			String cover, String redirect) throws Exception;
	
	public boolean noticeRange(long authorId, List<Long> uids, String title, 
			String desc, String cover, String redirect) throws Exception;
	
}
