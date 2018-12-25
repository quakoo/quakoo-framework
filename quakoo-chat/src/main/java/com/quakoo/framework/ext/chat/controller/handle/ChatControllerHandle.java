package com.quakoo.framework.ext.chat.controller.handle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.quakoo.framework.ext.chat.service.ext.ChatCheckService;
import com.quakoo.framework.ext.chat.model.ext.ChatCheckRes;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.google.common.collect.Sets;
import com.quakoo.baseFramework.model.pagination.Pager;
import com.quakoo.framework.ext.chat.context.ApiChatContextHandle;
import com.quakoo.framework.ext.chat.model.UserInfo;
import com.quakoo.framework.ext.chat.model.UserPrompt;
import com.quakoo.framework.ext.chat.model.UserStream;
import com.quakoo.framework.ext.chat.model.back.ConnectBack;
import com.quakoo.framework.ext.chat.model.back.PromptBack;
import com.quakoo.framework.ext.chat.model.back.StreamBack;
import com.quakoo.framework.ext.chat.model.param.UserLongConnection;
import com.quakoo.framework.ext.chat.service.ChatService;
import com.quakoo.framework.ext.chat.service.ConnectService;
import com.quakoo.framework.ext.chat.service.UserInfoService;
import com.quakoo.framework.ext.chat.service.UserPromptService;
import com.quakoo.framework.ext.chat.service.UserStreamService;
import com.quakoo.webframework.BaseController;

public class ChatControllerHandle extends BaseController {

    Logger logger = LoggerFactory.getLogger(ChatControllerHandle.class);
    
    private static final Map<String, Object> successResult = new HashMap<String, Object>();
    private static final Map<String, Object> erroResult = new HashMap<String, Object>();
    static {
        erroResult.put("success", false);
        successResult.put("success", true);
    }
	
	@Resource
	private UserInfoService userInfoService;
	
	@Resource
	private UserStreamService userStreamService;
	
	@Resource
	private UserPromptService userPromptService;
	
	@Resource
	private ConnectService connectService;
	
	@Resource
	private ChatService chatService;

	@Resource
	private ChatCheckService chatCheckService;

    @RequestMapping("/directoryTree")
    public ModelAndView directoryTree(@RequestParam(value = "uid") long uid,
                                      @RequestParam(required = true, value = "lastIndex") double lastIndex,
                                      HttpServletRequest request, HttpServletResponse response, Model model) throws Exception {
        List<UserStream> userStreams = userStreamService.getDirectoryStream(uid, lastIndex);
        List<StreamBack> streams = userStreamService.transformBack(userStreams);
        ConnectBack connectBack = connectService.transformBack(streams, null);
        return this.viewNegotiating(request, response, connectBack);
    }

	
	@RequestMapping("/pager")
    public ModelAndView pager(@RequestParam(value = "uid") long uid,
    		@RequestParam(value = "type") int type,
    		@RequestParam(value = "thirdId") long thirdId,
    		@ModelAttribute("_pager") Pager pager,
    		HttpServletRequest request, HttpServletResponse response,
    		Model model) throws Exception {
		pager = userStreamService.getPager(uid, type, thirdId, pager);
		return this.viewNegotiating(request, response, pager.toModelAttribute());
	}
	
	@RequestMapping("/chat")
	public ModelAndView chat(
			@RequestParam(required = true, value = "uid") long uid,
			@RequestParam(required = true, value = "clientId") String clientId,
			@RequestParam(required = true, value = "type") int type,
			@RequestParam(required = true, value = "thirdId") long thirdId,
			@RequestParam(value = "word", required = false) String word,
			@RequestParam(value = "voice", required = false) String voice,
			@RequestParam(value = "voiceDuration", required = false) String voiceDuration,
			@RequestParam(value = "video", required = false) String video,
			@RequestParam(value = "videoDuration", required = false) String videoDuration,
			@RequestParam(value = "picture", required = false) String picture,
            @RequestParam(value = "ext", required = false) String ext,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
        if(StringUtils.isBlank(word) && StringUtils.isBlank(voice) && StringUtils.isBlank(picture) && StringUtils.isBlank(video)
                && StringUtils.isBlank(ext))
            return super.viewNegotiating(request, response, erroResult);
        ChatCheckRes chatCheckRes = chatCheckService.check(uid, type, thirdId, word);
        if(!chatCheckRes.isSuccess()) {
            Map<String, Object> res = new HashMap<String, Object>(erroResult);
            res.put("msg", chatCheckRes.getMsg());
            return super.viewNegotiating(request, response, res);
        }
		boolean sign = chatService.chat(uid, clientId, type, thirdId, word, picture, 
				voice, voiceDuration, video, videoDuration, ext);
		if(sign) return super.viewNegotiating(request, response, successResult);
		else return super.viewNegotiating(request, response, erroResult);
	}
	
	@RequestMapping("/delete")
	public ModelAndView delete(
			@RequestParam(required = true, value = "uid") long uid,
			@RequestParam(required = true, value = "type") int type,
			@RequestParam(required = true, value = "thirdId") long thirdId,
			@RequestParam(required = true, value = "mid") long mid,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		boolean sign = userStreamService.delete(uid, type, thirdId, mid);
		if(sign) return super.viewNegotiating(request, response, successResult);
		else return super.viewNegotiating(request, response, erroResult);
	}
	
	@RequestMapping("/checkChat")
	public ModelAndView checkChat(
			@RequestParam(required = true, value = "uid") long uid,
			@RequestParam(required = true, value = "clientId") String clientId,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		boolean sign = chatService.checkChat(uid, clientId);
		if(sign) return super.viewNegotiating(request, response, successResult);
		else return super.viewNegotiating(request, response, erroResult);
	}
	
	
	@RequestMapping("/connect")
	public ModelAndView connect(@RequestParam(value = "uid") long uid,
			@RequestParam(required = true, value = "lastIndex") double lastIndex,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		response.setHeader("connection", "close");
		lastIndex += 0.001;
        UserInfo userInfo = userInfoService.load(uid);
        if(null != userInfo && userInfo.getLastIndex() > lastIndex) lastIndex = userInfo.getLastIndex();
		userInfo = userInfoService.syncUserInfo(uid, lastIndex, userInfo);
		userStreamService.init(uid);
		
		double lastPromptIndex = userInfo.getPromptIndex();
		List<UserPrompt> newPrompt = userPromptService.newPrompt(uid, lastPromptIndex);
		if(newPrompt.size() > 0) {
			double currentPromptIndex = newPrompt.get(0).getSort() + 0.001;
			userInfoService.updatePromptIndex(uid, currentPromptIndex);
		}
		List<UserStream> newStream = userStreamService.newStream(uid, lastIndex);
		
		List<PromptBack> prompts = userPromptService.transformBack(newPrompt);
		List<StreamBack> streams = userStreamService.transformBack(newStream);
		
		ConnectBack connectBack = connectService.transformBack(streams, prompts);
		if(connectBack.isSend()) {
			return super.viewNegotiating(request, response, connectBack);
		} else {
			AsyncContext asyncContext = request.startAsync(request, response);
		    asyncContext.setTimeout(-1);
		    UserLongConnection userLongConnection = new UserLongConnection(uid, lastIndex, 
		    		asyncContext, System.currentTimeMillis());
		    Set<UserLongConnection> list = ApiChatContextHandle.LongConnectionContext.
		    		get_connection_pool(uid).get(uid);
		    if(null == list){
				list = Sets.newConcurrentHashSet();
				ApiChatContextHandle.LongConnectionContext.get_connection_pool(uid).put(uid, list);
			}
		    list.add(userLongConnection);
		}
		return null;
	}
	
}
