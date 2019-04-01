package com.quakoo.framework.ext.chat.service.impl;

import java.util.*;
import java.util.Map.Entry;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.quakoo.baseFramework.jackson.JsonUtils;
import com.quakoo.baseFramework.model.pagination.Pager;
import com.quakoo.baseFramework.model.pagination.PagerSession;
import com.quakoo.baseFramework.model.pagination.service.PagerRequestService;
import com.quakoo.framework.ext.chat.AbstractChatInfo;
import com.quakoo.framework.ext.chat.dao.ChatGroupDao;
import com.quakoo.framework.ext.chat.dao.MessageDao;
import com.quakoo.framework.ext.chat.dao.UserDirectoryDao;
import com.quakoo.framework.ext.chat.dao.UserStreamDao;
import com.quakoo.framework.ext.chat.model.ChatGroup;
import com.quakoo.framework.ext.chat.model.Message;
import com.quakoo.framework.ext.chat.model.UserDirectory;
import com.quakoo.framework.ext.chat.model.UserStream;
import com.quakoo.framework.ext.chat.model.back.MessageBack;
import com.quakoo.framework.ext.chat.model.back.StreamBack;
import com.quakoo.framework.ext.chat.model.back.UserBack;
import com.quakoo.framework.ext.chat.model.constant.Type;
import com.quakoo.framework.ext.chat.model.param.UserOneStreamParam;
import com.quakoo.framework.ext.chat.model.param.UserStreamParam;
import com.quakoo.framework.ext.chat.service.UserStreamService;
import com.quakoo.framework.ext.chat.service.ext.UserWrapperService;

/**
 * 用户信息流处理类
 * class_name: UserStreamServiceImpl
 * package: com.quakoo.framework.ext.chat.service.impl
 * creat_user: lihao
 * creat_date: 2019/1/29
 * creat_time: 18:24
 **/
public class UserStreamServiceImpl implements UserStreamService {

	@Resource
	private UserDirectoryDao userDirectoryDao;
	
	@Resource
	private UserStreamDao userStreamDao;
	
	@Resource
	private MessageDao messageDao;
	
	@Resource
	private ChatGroupDao chatGroupDao;
	
	@Autowired(required = false)
    @Qualifier("userWrapperService")
	private UserWrapperService userWrapperService;

	private Logger logger = LoggerFactory.getLogger(UserStreamServiceImpl.class);

	/**
     * 初始化一个用户的信息流
	 * method_name: init
	 * params: [uid]
	 * return: void
	 * creat_user: lihao
	 * creat_date: 2019/1/29
	 * creat_time: 18:24
	 **/
	public void init(long uid) throws Exception {
		userStreamDao.init(uid, false);
		List<UserDirectory> directories = userDirectoryDao.list_all(uid);
		for(UserDirectory directory : directories) {
			int type = directory.getType();
			long thirdId = directory.getThirdId();
			userStreamDao.init_sub(uid, type, thirdId, false);
		}
	}

	/**
     * 获取用户消息目录的一条信息
	 * method_name: getDirectoryStream
	 * params: [uid, lastIndex]
	 * return: java.util.List<com.quakoo.framework.ext.chat.model.UserStream>
	 * creat_user: lihao
	 * creat_date: 2019/1/29
	 * creat_time: 18:25
	 **/
    @Override
    public List<UserStream> getDirectoryStream(long uid, double lastIndex) throws Exception {
        UserStreamParam hotParam = new UserStreamParam(uid, lastIndex);
        userStreamDao.new_hot_data(Lists.newArrayList(hotParam));
        List<UserStream> hotList = hotParam.getDataList();
        if(null == hotList) hotList = Lists.newArrayList();

        List<UserStream> list = Lists.newArrayList();
        Set<String> filterStrs = Sets.newHashSet();
        for(UserStream one : hotList) {
            String filterStr = String.format("uid_%d_type_%d_thirdId_%d", one.getUid(), one.getType(), one.getThirdId());
            if(!filterStrs.contains(filterStr)) {
                list.add(one);
            }
            filterStrs.add(filterStr);
        }
        List<UserDirectory> directories = userDirectoryDao.list_all(uid);
        for(Iterator<UserDirectory> it = directories.iterator(); it.hasNext();) {
            UserDirectory directory = it.next();
            String key = String.format("uid_%d_type_%d_thirdId_%d", directory.getUid(), directory.getType(), directory.getThirdId());
            if(filterStrs.contains(key)) {
                it.remove();
            }
        }
        List<UserOneStreamParam> subParams = Lists.newArrayList();
        for(UserDirectory directory : directories) {
            UserOneStreamParam subParam = new UserOneStreamParam(uid,
                    directory.getType(), directory.getThirdId(), lastIndex);
            subParam.setCount(1);
            subParams.add(subParam);
        }
        userStreamDao.one_new_cold_data(subParams);
        for(UserOneStreamParam subParam : subParams){
            List<UserStream> subList = subParam.getDataList();
            if(null != subList) list.addAll(subList);
        }
        if(list.size() > 0) Collections.sort(list);
        return list;
    }

    /**
     * 获取单个用户新的消息
     * method_name: newStream
     * params: [uid, lastIndex]
     * return: java.util.List<com.quakoo.framework.ext.chat.model.UserStream>
     * creat_user: lihao
     * creat_date: 2019/1/29
     * creat_time: 18:25
     **/
    public List<UserStream> newStream(long uid, double lastIndex)
			throws Exception {
		UserStreamParam hotParam = new UserStreamParam(uid, lastIndex);
		userStreamDao.new_hot_data(Lists.newArrayList(hotParam));
        List<UserStream> hotList = hotParam.getDataList();
        if(null == hotList) hotList = Lists.newArrayList();

        UserStreamParam coldParam = new UserStreamParam(uid, lastIndex);
		userStreamDao.new_cold_data(Lists.newArrayList(coldParam));
		List<UserStream> coldList = coldParam.getDataList();
		if(null != coldList && coldList.size() >= AbstractChatInfo.pull_length) {
			List<UserDirectory> directories = userDirectoryDao.list_all(uid);
			List<UserOneStreamParam> subParams = Lists.newArrayList();
			for(UserDirectory directory : directories) {
				UserOneStreamParam subParam = new UserOneStreamParam(uid,
						directory.getType(), directory.getThirdId(), lastIndex);
				subParams.add(subParam);
			}
			userStreamDao.one_new_cold_data(subParams);
            coldList.clear();
			for(UserOneStreamParam subParam : subParams){
				List<UserStream> subList = subParam.getDataList();
				if(null != subList) coldList.addAll(subList);
			}
			if(coldList.size() > 0) Collections.sort(coldList);
		}
		if(null == coldList) coldList = Lists.newArrayList();

		Set<Long> mids = Sets.newHashSet();
		List<UserStream> res = Lists.newArrayList();
		for(UserStream one : hotList) {
		    mids.add(one.getMid());
		    res.add(one);
        }
        for(UserStream one : coldList) {
		    if(!mids.contains(one.getMid())) {
                res.add(one);
            }
        }
		return res;
	}

    private Map<Long, List<UserStream>> newHotDataStream(Map<Long, Double> lastIndexMap) throws Exception {
        Map<Long, List<UserStream>> res = Maps.newHashMap();
        List<UserStreamParam> params = Lists.newArrayList();
        for(Entry<Long, Double> entry : lastIndexMap.entrySet()){
            long uid = entry.getKey();
            double lastIndex = entry.getValue();
            UserStreamParam param = new UserStreamParam(uid, lastIndex);
            params.add(param);
        }
        userStreamDao.new_hot_data(params);
        for(UserStreamParam param : params) {
            long uid = param.getUid();
            List<UserStream> list = param.getDataList();
            if(null == list) list = Lists.newArrayList();
            res.put(uid, list);
        }
        return res;
    }

	private Map<Long, List<UserStream>> newColdDataStream(Map<Long, Double> lastIndexMap) throws Exception {
        Map<Long, List<UserStream>> res = Maps.newHashMap();
        List<UserStreamParam> params = Lists.newArrayList();
        for(Entry<Long, Double> entry : lastIndexMap.entrySet()){
            long uid = entry.getKey();
            double lastIndex = entry.getValue();
            UserStreamParam param = new UserStreamParam(uid, lastIndex);
            params.add(param);
        }
        userStreamDao.new_cold_data(params);
        for(UserStreamParam param : params) {
            long uid = param.getUid();
            double lastIndex = lastIndexMap.get(uid);
            List<UserStream> list = param.getDataList();
            if(null != list && list.size() >= AbstractChatInfo.pull_length){
                List<UserDirectory> directories = userDirectoryDao.list_all(uid);
                List<UserOneStreamParam> subParams = Lists.newArrayList();
                for(UserDirectory directory : directories) {
                    UserOneStreamParam subParam = new UserOneStreamParam(uid,
                            directory.getType(), directory.getThirdId(), lastIndex);
                    subParams.add(subParam);
                }
                userStreamDao.one_new_cold_data(subParams);
                list.clear();
                for(UserOneStreamParam subParam : subParams){
                    List<UserStream> subList = subParam.getDataList();
                    if(null != subList) list.addAll(subList);
                }
                if(list.size() > 0) Collections.sort(list);
            }
            if(null == list) list = Lists.newArrayList();
            res.put(uid, list);
        }
        return res;
    }

	/**
     * 获取多个用户新的消息
	 * method_name: newStream
	 * params: [lastIndexMap]
	 * return: java.util.Map<java.lang.Long,java.util.List<com.quakoo.framework.ext.chat.model.UserStream>>
	 * creat_user: lihao
	 * creat_date: 2019/1/29
	 * creat_time: 18:26
	 **/
	@Override
	public Map<Long, List<UserStream>> newStream(Map<Long, Double> lastIndexMap)
			throws Exception {
        Map<Long, List<UserStream>> hotMap = newHotDataStream(lastIndexMap);
        Map<Long, List<UserStream>> coldMap = newColdDataStream(lastIndexMap);
		Map<Long, List<UserStream>> res = Maps.newHashMap();
		for(Entry<Long, List<UserStream>> entry : hotMap.entrySet()) {
		    long uid = entry.getKey();
		    List<UserStream> hotList = entry.getValue();
            Set<Long> mids = Sets.newHashSet();
            List<UserStream> list = Lists.newArrayList();
            for(UserStream one : hotList) {
                mids.add(one.getMid());
                list.add(one);
            }
            List<UserStream> coldList = coldMap.get(uid);
            for(UserStream one : coldList) {
                if(!mids.contains(one.getMid())) {
                    list.add(one);
                }
            }
            res.put(uid, list);
        }
		return res;
	}

	/**
     * 删除一条消息
	 * method_name: delete
	 * params: [uid, type, thirdId, mid]
	 * return: boolean
	 * creat_user: lihao
	 * creat_date: 2019/1/29
	 * creat_time: 18:26
	 **/
	public boolean delete(long uid, int type, long thirdId, long mid)
			throws Exception {
		boolean res = false;
		UserStream item = new UserStream();
		item.setUid(uid);
		item.setType(type);
		item.setThirdId(thirdId);
		item.setMid(mid);
//		item = userStreamDao.load(item);
//		if(null != item) {
        res = userStreamDao.delete(item);
//		}
		return res;
	}

	/**
     * 获取一个消息分页列表
	 * method_name: getPager
	 * params: [uid, type, thirdId, pager]
	 * return: com.quakoo.baseFramework.model.pagination.Pager
	 * creat_user: lihao
	 * creat_date: 2019/1/29
	 * creat_time: 18:26
	 **/
	public Pager getPager(final long uid, final int type, final long thirdId, Pager pager)
			throws Exception {
		return new PagerRequestService<UserStream>(pager, 0) {
			@Override
			public List<UserStream> step1GetPageResult(String cursor, int size)
					throws Exception {
				double rightCursor = Double.parseDouble(cursor);
				if(0 != rightCursor) rightCursor -= 0.001;
				return userStreamDao.page_list(uid, type, thirdId, rightCursor, size);
			}
			@Override
			public int step2GetTotalCount() throws Exception {
				return 0;
			}
			@Override
			public List<UserStream> step3FilterResult(
					List<UserStream> unTransformDatas, PagerSession session)
					throws Exception {
				return unTransformDatas;
			}
			@Override
			public List<?> step4TransformData(
					List<UserStream> unTransformDatas, PagerSession session)
					throws Exception {
				List<Long> mids = Lists.newArrayList();
				for(UserStream one : unTransformDatas) {
					mids.add(one.getMid());
				}
				List<Message> messages = messageDao.load(mids);
				Map<Long, Message> message_map = Maps.newHashMap();
				for(Message message : messages){
					if(null != message) message_map.put(message.getId(), message);
				}
				List<MessageBack> res = Lists.newArrayList();
				Set<Long> uids = Sets.newHashSet();
				for(UserStream one : unTransformDatas){
					long mid = one.getMid();
					Message message = message_map.get(mid);
					if(null != message) {
                        uids.add(message.getAuthorId());
                        MessageBack messageBack = new MessageBack(message, one.getSort());
                        res.add(messageBack);
                    }
				}

                Map<Long, UserBack> remarkUserMap = Maps.newHashMap();
                if(uids.size() > 0) {
                    List<UserBack> users = userWrapperService.getRemarkUsers(uid, Lists.newArrayList(uids));
                    for(UserBack user : users) {
                        remarkUserMap.put(user.getId(), user);
                    }
                }

				Map<Long, UserBack> userMap = Maps.newHashMap();
				if(uids.size() > 0) {
					List<UserBack> users = userWrapperService.getUsers(Lists.newArrayList(uids));
					for(UserBack user : users) {
						userMap.put(user.getId(), user);
					}
				}

				for(MessageBack one : res) {
					long uid = one.getAuthorId();
                    UserBack remarkUser = remarkUserMap.get(uid);
                    UserBack user = userMap.get(uid);
                    if(null != remarkUser) one.setAuthorRemark(remarkUser.getNick());
                    if(null != user) {
                        one.setAuthorNick(user.getNick());
                        one.setAuthorIcon(user.getIcon());
                    } else {
                        one.setAuthorNick("用户");
                        one.setAuthorIcon("");
                    }
				}
				return res;
			}
		}.getPager();
	}

	/**
     * 批量插入消息流
	 * method_name: batchInsert
	 * params: [streams]
	 * return: int
	 * creat_user: lihao
	 * creat_date: 2019/1/29
	 * creat_time: 18:27
	 **/
	@Override
	public int batchInsertColdData(List<UserStream> streams) throws Exception {
		return userStreamDao.insert_cold_data(streams);
	}

    @Override
    public int batchInsertHotData(List<UserStream> streams) throws Exception {
        return userStreamDao.insert_hot_data(streams);
    }

    @Override
    public void clearHotData(long uid, double sort) throws Exception {
        userStreamDao.clear_hot_data_by_sort(uid, sort);
    }

    @Override
    public void createSort(List<UserStream> streams) throws Exception {
        userStreamDao.create_sort(streams);
    }

    /**
     * 封装信息流
	 * method_name: transformBack
	 * params: [list]
	 * return: java.util.List<com.quakoo.framework.ext.chat.model.back.StreamBack>
	 * creat_user: lihao
	 * creat_date: 2019/1/29
	 * creat_time: 18:27
	 **/
	@Override
	public List<StreamBack> transformBack(List<UserStream> list)
			throws Exception {
		List<StreamBack> res = Lists.newArrayList();
		if(null != list && list.size() > 0) {
			Map<UserDirectory, List<UserStream>> map = Maps.newHashMap();
			List<Long> mids = Lists.newArrayList();
			for(UserStream one : list){
				mids.add(one.getMid());
				UserDirectory directory = new UserDirectory();
				directory.setUid(one.getUid());
				directory.setType(one.getType());
				directory.setThirdId(one.getThirdId());
				List<UserStream> streams = map.get(directory);
				if(null == streams) {
					streams = Lists.newArrayList();
					map.put(directory, streams);
				}
				streams.add(one);
			}
			List<Message> messages = messageDao.load(mids);
			Map<Long, Message> message_map = Maps.newHashMap();
			for(Message message : messages){
				if(null != message) message_map.put(message.getId(), message);
			}
			
			for(Entry<UserDirectory, List<UserStream>> entry:map.entrySet()) {
				UserDirectory directory = entry.getKey();
				List<UserStream> streams = entry.getValue();
				boolean more = false;
				int size = streams.size();
				double maxIndex = 0;
				if(size >= AbstractChatInfo.pull_length){
					more = true;
					size = AbstractChatInfo.pull_length - 1;
				}
				List<MessageBack> data = Lists.newArrayList();
				Set<Long> uids = Sets.newHashSet();
			    Set<Long> cgids = Sets.newHashSet(); 
				for(int i = 0; i < size; i++){ 
					UserStream one = streams.get(i);
					if(i == 0) maxIndex = one.getSort();
					Message message = message_map.get(one.getMid());
					if(message != null) {
                        int type = one.getType();
                        long thirdId = one.getThirdId();
                        long authorId = one.getAuthorId();
                        uids.add(authorId);
                        if(type == Type.type_single_chat || type == Type.type_notice) {
                            uids.add(thirdId);
                        } else if(type == Type.type_many_chat) {
                            cgids.add(thirdId);
                        }
                        MessageBack messageBack = new MessageBack(message, one.getSort());
                        data.add(messageBack);
                    }
				}

                Map<Long, UserBack> remarkUserMap = Maps.newHashMap();
                if(uids.size() > 0) {
                    List<UserBack> users = userWrapperService.getRemarkUsers(directory.getUid(), Lists.newArrayList(uids));
                    for(UserBack user : users) {
                        remarkUserMap.put(user.getId(), user);
                    }
                }

				Map<Long, UserBack> userMap = Maps.newHashMap();
				if(uids.size() > 0) {
					List<UserBack> users = userWrapperService.getUsers(Lists.newArrayList(uids));
					for(UserBack user : users) {
						userMap.put(user.getId(), user);
					}
				}
				Map<Long, ChatGroup> chatGroupMap = Maps.newHashMap();
				if(cgids.size() > 0) {
					List<ChatGroup> chatGroups = chatGroupDao.load(Lists.newArrayList(cgids));
					for(ChatGroup chatGroup : chatGroups) {
						chatGroupMap.put(chatGroup.getId(), chatGroup);
					}
				}
				
				for(MessageBack one : data) {
					long uid = one.getAuthorId();
                    UserBack remarkUser = remarkUserMap.get(uid);
                    UserBack user = userMap.get(uid);
                    if(null != remarkUser) one.setAuthorRemark(remarkUser.getNick());
                    if(null != user) {
                        one.setAuthorNick(user.getNick());
                        one.setAuthorIcon(user.getIcon());
                    } else {
                        one.setAuthorNick("用户");
                        one.setAuthorIcon("");
                    }
				}
				
				StreamBack streamBack = new StreamBack();
				streamBack.setUid(directory.getUid());
				streamBack.setType(directory.getType());
				streamBack.setThirdId(directory.getThirdId());
				streamBack.setMore(more);
				streamBack.setMaxIndex(maxIndex);
				streamBack.setData(data);
				
				int type = streamBack.getType();
				long thirdId = streamBack.getThirdId();
				if(type == Type.type_single_chat || type == Type.type_notice) {
                    String authorNick = null;
                    String authorIcon = null;
                    UserBack remarkUser = remarkUserMap.get(thirdId);
                    UserBack user = userMap.get(thirdId);
                    if(null != remarkUser && StringUtils.isNotBlank(remarkUser.getNick())) authorNick = remarkUser.getNick();
                    if(null != remarkUser && StringUtils.isNotBlank(remarkUser.getIcon())) authorIcon = remarkUser.getIcon();
                    if(null == authorNick && null != user && StringUtils.isNotBlank(user.getNick())) authorNick = user.getNick();
                    if(null == authorIcon && null != user && StringUtils.isNotBlank(user.getIcon())) authorIcon = user.getIcon();
                    if(null == authorNick) authorNick = "用户";
                    if(null == authorIcon) authorIcon = "";
                    streamBack.setThirdNick(authorNick);
                    streamBack.setThirdIcon(authorIcon);
				} else {
					ChatGroup chatGroup = chatGroupMap.get(thirdId);
					if(null != chatGroup){
						String chatGroupName = chatGroup.getName();
						if(StringUtils.isBlank(chatGroupName)) {
							List<Long> chatGroupUids = JsonUtils.fromJson(chatGroup.getUids(),
									new TypeReference<List<Long>>() {});
							chatGroupName = "讨论组" + chatGroupUids.size() + "人";
						}
						streamBack.setThirdNick(chatGroupName);
                        streamBack.setThirdIcon(chatGroup.getIcon());
					} else{
						streamBack.setThirdNick("讨论组");
					}
				}
				res.add(streamBack);
			}
		}
		return res;
	}

}
