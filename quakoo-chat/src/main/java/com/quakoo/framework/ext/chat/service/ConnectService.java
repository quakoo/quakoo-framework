package com.quakoo.framework.ext.chat.service;

import java.util.List;

import com.quakoo.framework.ext.chat.model.back.ConnectBack;
import com.quakoo.framework.ext.chat.model.back.PromptBack;
import com.quakoo.framework.ext.chat.model.back.StreamBack;

public interface ConnectService {

	public ConnectBack transformBack(List<StreamBack> streams,
			List<PromptBack> prompts) throws Exception;
	
}
