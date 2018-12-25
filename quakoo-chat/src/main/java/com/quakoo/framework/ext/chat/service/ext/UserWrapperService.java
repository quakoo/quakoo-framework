package com.quakoo.framework.ext.chat.service.ext;

import java.util.List;

import com.quakoo.framework.ext.chat.model.back.UserBack;

public interface UserWrapperService {
	
	public List<UserBack> getUsers(List<Long> uids) throws Exception;

	public List<UserBack> getRemarkUsers(long uid, List<Long> uids) throws Exception;

}
