package com.quakoo.framework.ext.chat.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Resource;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.quakoo.framework.ext.chat.dao.UserPromptDao;
import com.quakoo.framework.ext.chat.model.UserPrompt;
import com.quakoo.framework.ext.chat.model.back.PromptBack;
import com.quakoo.framework.ext.chat.model.back.PromptItemBack;
import com.quakoo.framework.ext.chat.service.UserPromptService;


public class UserPromptServiceImpl implements UserPromptService {
	
	@Resource
	private UserPromptDao userPromptDao;
	
	@Override
	public boolean insert(UserPrompt prompt) throws Exception {
		return userPromptDao.insert(prompt);
	}

	@Override
	public int batchInsert(List<UserPrompt> prompts) throws Exception {
		return userPromptDao.insert(prompts);
	}

	public List<UserPrompt> newPrompt(long uid, double lastPromptIndex)
			throws Exception {
		return userPromptDao.new_data(uid, lastPromptIndex);
	}

	@Override
	public List<PromptBack> transformBack(List<UserPrompt> list) throws Exception {
		List<PromptBack> res = Lists.newArrayList();
		if(null != list && list.size() > 0) {
			Map<Integer, Map<Long, Integer>> map = Maps.newHashMap();
			for(UserPrompt one : list) {
				int type = one.getType();
				Map<Long, Integer> sub_map = map.get(type);
				if(null == sub_map) {
					sub_map = Maps.newHashMap();
					map.put(type, sub_map);
				}
				long thirdId = one.getThirdId();
				Integer num = sub_map.get(thirdId);
				if(null == num) num = 1;
			    else num = num + 1;
				sub_map.put(thirdId, num);
			}
			for(Entry<Integer, Map<Long, Integer>> entry : map.entrySet()) {
				Integer type = entry.getKey();
				List<PromptItemBack> items = Lists.newArrayList();
				Map<Long, Integer> sub_map = entry.getValue();
				for(Entry<Long, Integer> sub_entry : sub_map.entrySet()) {
					Long thirdId = sub_entry.getKey();
					Integer num = sub_entry.getValue();
					PromptItemBack item = new PromptItemBack(thirdId, num);
					items.add(item);
				}
				PromptBack back = new PromptBack(type, items);
				res.add(back);
			}
		}
		return res;
	}

}
