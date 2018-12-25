package com.quakoo.framework.ext.push.controller.handle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.quakoo.framework.ext.push.service.PushHandleAllService;
import com.quakoo.framework.ext.push.service.PushHandleService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.quakoo.baseFramework.jackson.JsonUtils;
import com.quakoo.baseFramework.transform.ListTransformUtils;
import com.quakoo.baseFramework.transform.ListTransformerStringToLong;
import com.quakoo.webframework.BaseController;

public class PushControllerHandle extends BaseController {

    Logger logger = LoggerFactory.getLogger(PushControllerHandle.class);
    
    private static final Map<String, Object> successResult = new HashMap<String, Object>();
    private static final Map<String, Object> erroResult = new HashMap<String, Object>();
    static {
        erroResult.put("success", false);
        successResult.put("success", true);
    }
    
    @Resource
	private PushHandleService pushHandleService;
    
    @Resource
    private PushHandleAllService pushHandleAllService;
    
    @RequestMapping("/allPush")
	public ModelAndView allPush(
			@RequestParam(required = true, value = "title") String title,
			@RequestParam(required = true, value = "content") String content,
			@RequestParam(required = false, value = "extra", defaultValue = "{}") String extra,
            @RequestParam(required = false, value = "platform", defaultValue = "0") int platform,
			HttpServletRequest request, HttpServletResponse response) throws Exception {
		try {
			Map<String, String> extraMap = JsonUtils.fromJson(extra, new TypeReference<Map<String, String>>() {});
			pushHandleAllService.push(title, content, extraMap, platform);
			return super.viewNegotiating(request, response, successResult);
		} catch (Exception e) {
			e.printStackTrace();
			return super.viewNegotiating(request, response, erroResult);
		}
	}

    @RequestMapping("/push")
	public ModelAndView push(
			@RequestParam(required = true, value = "uid") long uid,
			@RequestParam(required = true, value = "title") String title,
			@RequestParam(required = true, value = "content") String content,
			@RequestParam(required = false, value = "extra", defaultValue = "{}") String extra,
            @RequestParam(required = false, value = "platform", defaultValue = "0") int platform,
			HttpServletRequest request, HttpServletResponse response) throws Exception {
		try {
			Map<String, String> extraMap = JsonUtils.fromJson(extra, new TypeReference<Map<String, String>>() {});
			pushHandleService.push(uid, title, content, extraMap, platform);
			return super.viewNegotiating(request, response, successResult);
		} catch (Exception e) {
			e.printStackTrace();
			return super.viewNegotiating(request, response, erroResult);
		}
	}
    
    @RequestMapping("/batchPush")
	public ModelAndView batchPush(
			@RequestParam(required = true, value = "uids") String uids,
			@RequestParam(required = true, value = "title") String title,
			@RequestParam(required = true, value = "content") String content,
			@RequestParam(required = false, value = "extra", defaultValue = "{}") String extra,
            @RequestParam(required = false, value = "platform", defaultValue = "0") int platform,
			HttpServletRequest request, HttpServletResponse response) throws Exception {
		try {
			Map<String, String> extraMap = JsonUtils.fromJson(extra, new TypeReference<Map<String, String>>() {});
			List<String> strList = Lists.
					newArrayList(StringUtils.split(uids, ","));
			List<Long> uidList = ListTransformUtils.
					transformedList(strList, new ListTransformerStringToLong());
			pushHandleService.batchPush(uidList, title, content, extraMap, platform);
			return super.viewNegotiating(request, response, successResult);
		} catch (Exception e) {
			e.printStackTrace();
			return super.viewNegotiating(request, response, erroResult);
		}
	}

    public static void main(String[] args) {

    }
    
}
