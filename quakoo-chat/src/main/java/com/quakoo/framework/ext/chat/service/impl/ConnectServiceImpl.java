package com.quakoo.framework.ext.chat.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.quakoo.framework.ext.chat.model.back.ConnectBack;
import com.quakoo.framework.ext.chat.model.back.PromptBack;
import com.quakoo.framework.ext.chat.model.back.StreamBack;
import com.quakoo.framework.ext.chat.service.ConnectService;

/**
 * 连接消息封装类
 * class_name: ConnectServiceImpl
 * package: com.quakoo.framework.ext.chat.service.impl
 * creat_user: lihao
 * creat_date: 2019/1/29
 * creat_time: 18:18
 **/
@Service
public class ConnectServiceImpl implements ConnectService {

	@Override
	public ConnectBack transformBack(List<StreamBack> streams,
			List<PromptBack> prompts) throws Exception {
		ConnectBack res = new ConnectBack();
		if((null != prompts && prompts.size() > 0) || (null != streams && streams.size() > 0)) {
			if(null != streams && streams.size() > 0) {
				double maxIndex = 0;
				for(StreamBack stream : streams){
					if(stream.getMaxIndex() > maxIndex) maxIndex = stream.getMaxIndex();
				}
				res.setMaxStreamIndex(maxIndex);
				res.setStreams(streams);
			}
			if(null != prompts && prompts.size() > 0) {
				res.setPrompts(prompts);
			}
			res.setSend(true);
		} else {
			res.setSend(false);
		}
		return res;
	}

}
