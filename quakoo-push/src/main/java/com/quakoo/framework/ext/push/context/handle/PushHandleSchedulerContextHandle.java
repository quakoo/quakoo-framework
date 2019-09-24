package com.quakoo.framework.ext.push.context.handle;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.quakoo.baseFramework.jackson.JsonUtils;
import com.quakoo.baseFramework.secure.Base64Util;
import com.quakoo.baseFramework.util.StringUtil;
import com.quakoo.framework.ext.push.model.PushMsg;
import com.quakoo.framework.ext.push.model.constant.Platform;
import com.quakoo.framework.ext.push.service.PushMsgHandleService;
import com.quakoo.framework.ext.push.util.SleepUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Uninterruptibles;
import com.quakoo.baseFramework.transform.ListTransformUtils;
import com.quakoo.baseFramework.transform.ListTransformerStringToLong;
import com.quakoo.framework.ext.push.distributed.DistributedConfig;

/**
 * 推送单个或者批量用户的通知上下文
 * class_name: PushHandleSchedulerContextHandle
 * package: com.quakoo.framework.ext.push.context.handle
 * creat_user: lihao
 * creat_date: 2019/1/30
 * creat_time: 11:13
 **/
public class PushHandleSchedulerContextHandle extends PushBasePushHandleContextHandle {

    Logger logger = LoggerFactory.getLogger(PushHandleSchedulerContextHandle.class);

    private int handle_size = 5000;

    private int max_time = 1000 * 60 * 10; //10分钟

    @Resource
    private PushMsgHandleService pushMsgHandleService;

    @Override
    public void afterPropertiesSet() throws Exception {
        for(String queueName : pushInfo.push_msg_queue_names) {
            Thread thread = new Thread(new Processer(queueName));
            thread.start();
        }
    }

    public static void main(String[] args) {

        String title = "我是";
        String extraStr = "{\"type\":\"5\",\"extra\":\"{\\\"type\\\":\\\"group_video\\\",\\\"friendNick\\\":\\\"小打小闹  一块麻将娱乐群\\\",\\\"friendicon\\\":\\\"http:\\\\/\\\\/store.jimaoxinkeji.com\\\\/storage\\\\/600*600*afe75d49b321f56dbaacfb527474646a.png\\\",\\\"joinChannel\\\":\\\"1568854442124\\\",\\\"userList\\\":\\\"[{\\\\\\\"id\\\\\\\":\\\\\\\"178491\\\\\\\",\\\\\\\"icon\\\\\\\":\\\\\\\"http:\\\\\\\\\\\\/\\\\\\\\\\\\/store.jimaoxinkeji.com\\\\\\\\\\\\/storage\\\\\\\\\\\\/540*540*d0302eba283368177c22a2908186d818.jpg\\\\\\\"}, {\\\\\\\"id\\\\\\\":\\\\\\\"283386\\\\\\\",\\\\\\\"icon\\\\\\\":\\\\\\\"http:\\\\\\\\\\\\/\\\\\\\\\\\\/store.jimaoxinkeji.com\\\\\\\\\\\\/storage\\\\\\\\\\\\/259*259*ec0b7a5a168019701f534f30827174c7.jpg\\\\\\\"}]\\\"}\"}";
        title = Base64Util.encode(title.getBytes());
        extraStr = Base64Util.encode(extraStr.getBytes());

        String key = title + "_" + extraStr;
        title = key.split("_")[0];
        extraStr = key.split("_")[1];
        extraStr = new String( Base64Util.decode(extraStr));
        Map<String, String> extra = JsonUtils.fromJson(extraStr, new TypeReference<Map<String, String>>() {});
        System.out.println(extra.toString());

    }

    class Processer implements Runnable {

        private String queueName;

        public Processer(String queueName) {
            this.queueName = queueName;
        }

        private void send(List<Long> uids, PushMsg pushMsg) {
            if(uids.size() <= 500) {
                handleBatch(uids, pushMsg); //多个用户通知推送
            } else {
                List<List<Long>> uidsList =  Lists.partition(uids, 500);
                for(List<Long> one : uidsList) {
                    handleBatch(one, pushMsg); //多个用户通知推送
                }
            }
        }

        @Override
        public void run() {
            while(true) {
                if(DistributedConfig.canRunPushMsgQueue.contains(queueName)) {
                    SleepUtils.sleep(10, 2);
                    try {
                        List<PushMsg> list = pushMsgHandleService.getHandlePushMsgs(queueName, handle_size); //获取要推送的通知
                        if(list.size() > 0) {
                            long currentTime = System.currentTimeMillis();
                            long lastTime = list.get(list.size() - 1).getTime();
                            if(currentTime - lastTime > max_time) {
                                Map<String, Set<Long>> titleUidsMap = Maps.newHashMap();
                                for(PushMsg pushMsg : list) {
                                    Set<Long> uids = titleUidsMap.get(pushMsg.getTitle());
                                    if(uids == null) {
                                        uids = Sets.newHashSet();
                                        titleUidsMap.put(pushMsg.getTitle(), uids);
                                    }
                                    if(pushMsg.getType() == PushMsg.type_single) {
                                        uids.add(pushMsg.getUid());
                                    } else {
                                        String uidStr = pushMsg.getUids();
                                        List<String> strList = Lists.
                                                newArrayList(StringUtils.split(uidStr, ","));
                                        List<Long> uidList = ListTransformUtils.
                                                transformedList(strList, new ListTransformerStringToLong());
                                        uids.addAll(uidList);
                                    }
                                }
                                for(Map.Entry<String, Set<Long>> entry : titleUidsMap.entrySet()) {
                                    PushMsg pushMsg = new PushMsg();
                                    pushMsg.setTitle(entry.getKey());
                                    pushMsg.setContent("您收到了新的消息");
                                    pushMsg.setPlatform(Platform.all);
                                    send(Lists.newArrayList(entry.getValue()), pushMsg);
                                }
                            } else {
                                String uid_key_format = "%d_%d_%s_%s"; //uid_platform_title_ext
                                Map<String, List<PushMsg>> map = Maps.newHashMap();
                                for(PushMsg pushMsg : list) {
                                    int platform = pushMsg.getPlatform();
                                    String title = pushMsg.getTitle();
                                    title = Base64Util.encode(title.getBytes());
                                    Map<String, String> extra = pushMsg.getExtra();
                                    String extraStr = "";
                                    if(extra.size() > 0) {
                                        extraStr = JsonUtils.toJson(extra);
                                        extraStr = Base64Util.encode(extraStr.getBytes());
                                    }
                                    if(pushMsg.getType() == PushMsg.type_single) {
                                        long uid = pushMsg.getUid();
                                        String key = String.format(uid_key_format, uid, platform, title, extraStr);
                                        List<PushMsg> pushMsgs = map.get(key);
                                        if(pushMsgs == null) {
                                            pushMsgs = Lists.newArrayList();
                                            map.put(key, pushMsgs);
                                        }
                                        pushMsgs.add(pushMsg);
                                    } else {
                                        String uidStr = pushMsg.getUids();
                                        List<String> strList = Lists.
                                                newArrayList(StringUtils.split(uidStr, ","));
                                        List<Long> uids = ListTransformUtils.
                                                transformedList(strList, new ListTransformerStringToLong());
                                        for(long uid : uids) {
                                            String key = String.format(uid_key_format, uid, platform, title, extraStr);
                                            List<PushMsg> pushMsgs = map.get(key);
                                            if(pushMsgs == null) {
                                                pushMsgs = Lists.newArrayList();
                                                map.put(key, pushMsgs);
                                            }
                                            pushMsgs.add(pushMsg);
                                        }
                                    }
                                }
                                String num_key_format = "%d_%d_%s_%s"; //num_platform_title_ext
                                Map<String, Set<Long>> map2 = Maps.newHashMap(); // UIDS
                                Map<Long, Set<Long>> pmidUidsMap = Maps.newHashMap();
                                Map<Long, PushMsg> pushMsgMap = Maps.newHashMap();
                                for(Map.Entry<String, List<PushMsg>> entry : map.entrySet()) {
                                    String str = entry.getKey();
                                    long uid = Long.parseLong(str.split("_")[0]);
                                    int platform = Integer.parseInt(str.split("_")[1]);
                                    String title = str.split("_")[2];
                                    String extraStr = str.split("_")[3];
                                    List<PushMsg> pushMsgs = entry.getValue();
                                    int num = pushMsgs.size();
                                    if(num > 1) {
                                        String key = String.format(num_key_format, num, platform, title, extraStr);
                                        Set<Long> uids = map2.get(key);
                                        if(uids == null) {
                                            uids = Sets.newHashSet();
                                            map2.put(key, uids);
                                        }
                                        uids.add(uid);
                                    } else {
                                        PushMsg pushMsg = pushMsgs.get(0);
                                        Set<Long> uids = pmidUidsMap.get(pushMsg.getId());
                                        if(uids == null) {
                                            uids = Sets.newHashSet();
                                            pmidUidsMap.put(pushMsg.getId(), uids);
                                        }
                                        uids.add(uid);
                                        pushMsgMap.put(pushMsg.getId(), pushMsg);
                                    }
                                }

                                if(map2.size() > 0) {
                                    for(Map.Entry<String, Set<Long>> entry : map2.entrySet()) {
                                        String key = entry.getKey();
                                        Set<Long> uids = entry.getValue();
                                        int num = Integer.parseInt(key.split("_")[0]);
                                        int platform = Integer.parseInt(key.split("_")[1]);
                                        String title = key.split("_")[2];
                                        title = new String(Base64Util.decode(title));
                                        String extraStr = key.split("_")[3];
                                        Map<String, String> extra = Maps.newHashMap();
                                        if(StringUtils.isNotBlank(extraStr)) {
                                            try {
                                                extraStr = new String(Base64Util.decode(extraStr));
                                                extra = JsonUtils.fromJson(extraStr, new TypeReference<Map<String, String>>() {});
                                            } catch (Exception e) {
                                                logger.error(key, e);
                                            }
                                        }
                                        PushMsg pushMsg = new PushMsg();
                                        pushMsg.setTitle(title);
                                        pushMsg.setContent("您收到了" + num + "条新消息");
                                        pushMsg.setExtra(extra);
                                        pushMsg.setPlatform(platform);

//                                        logger.info(" ========= " + pushMsg.toString() + " uids : " + uids.toString());

                                        send(Lists.newArrayList(uids), pushMsg);
                                    }
                                }

                                if(pmidUidsMap.size() > 0) {
                                    String key_format = "%d_%s_%s_%s"; //platform_title_content_ext
                                    Map<String, Set<Long>> map3 = Maps.newHashMap(); //UIDS
                                    for(Map.Entry<Long, Set<Long>> entry : pmidUidsMap.entrySet()) {
                                        long pmid = entry.getKey();
                                        PushMsg pushMsg = pushMsgMap.get(pmid);
                                        if(pushMsg != null) {
                                            int platform = pushMsg.getPlatform();
                                            String title = pushMsg.getTitle();
                                            title = Base64Util.encode(title.getBytes());
                                            String content = pushMsg.getContent();
                                            content = Base64Util.encode(content.getBytes());
                                            Map<String, String> extra = pushMsg.getExtra();
                                            String extraStr = "";
                                            if(extra.size() > 0) {
                                                extraStr = JsonUtils.toJson(extra);
                                                extraStr = Base64Util.encode(extraStr.getBytes());
                                            }
                                            String key = String.format(key_format, platform, title, content, extraStr);
                                            Set<Long> uids = map3.get(key);
                                            if(uids == null) {
                                                uids = Sets.newHashSet();
                                                map3.put(key, uids);
                                            }
                                            uids.addAll(entry.getValue());
                                        }
                                    }
                                    for(Map.Entry<String, Set<Long>> entry : map3.entrySet()) {
                                        String key = entry.getKey();
                                        int platform = Integer.parseInt(key.split("_")[0]);
                                        String title = key.split("_")[1];
                                        title = new String(Base64Util.decode(title));
                                        String content = key.split("_")[2];
                                        content = new String(Base64Util.decode(content));
                                        String extraStr = key.split("_")[3];
                                        Map<String, String> extra = Maps.newHashMap();
                                        if(StringUtils.isNotBlank(extraStr)) {
                                            try {
                                                extraStr = new String(Base64Util.decode(extraStr));
                                                extra = JsonUtils.fromJson(extraStr, new TypeReference<Map<String, String>>() {});
                                            } catch (Exception e) {
                                                logger.error(key, e);
                                            }
                                        }
                                        PushMsg pushMsg = new PushMsg();
                                        pushMsg.setTitle(title);
                                        pushMsg.setContent(content);
                                        pushMsg.setPlatform(platform);
                                        pushMsg.setExtra(extra);
//                                        logger.info(" ========= " + pushMsg.toString() + " uids : " + entry.getValue().toString());
                                        send(Lists.newArrayList(entry.getValue()), pushMsg);
                                    }

                                }

                            }
                            pushMsgHandleService.finishHandlePushMsgs(queueName, list); //推送完成更新
                        }
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                } else {
                    Uninterruptibles.sleepUninterruptibly(2, TimeUnit.SECONDS);
                }
            }
        }
    }

}
