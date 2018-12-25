package com.quakoo.framework.ext.chat.service;

import java.util.List;

import com.quakoo.framework.ext.chat.model.UserPrompt;
import com.quakoo.framework.ext.chat.model.back.PromptBack;

public interface UserPromptService {
	
	public boolean insert(UserPrompt prompt) throws Exception;
	
	public int batchInsert(List<UserPrompt> prompts) throws Exception;
	
	public List<UserPrompt> newPrompt(long uid, double lastPromptIndex) throws Exception;
	
	public List<PromptBack> transformBack(List<UserPrompt> list) throws Exception;
	
}
