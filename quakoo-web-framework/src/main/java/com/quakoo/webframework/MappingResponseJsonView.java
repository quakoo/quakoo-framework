package com.quakoo.webframework;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.view.AbstractView;

import redis.clients.util.SafeEncoder;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quakoo.baseFramework.json.JsonUtils;

/**
 * @author liyongbiao
 */
public class MappingResponseJsonView extends AbstractView {
	
	Logger logger = LoggerFactory.getLogger(MappingResponseJsonView.class);

    private final JsonEncoding encoding = JsonEncoding.UTF8;

    public static final String resultKey = "_jsonResult_";


    private boolean jsonp = false;

    private String jsonpKey = null;

    private boolean disableCaching = true;


    /**
     * Construct a new {@code MappingResponseJsonView}, setting the content type
     * to {@code application/json}.
     */

    public MappingResponseJsonView() {
        super();
    }


    /*
     * 设置一些返回头 (non-Javadoc)
     *
     * @see org.springframework.web.servlet.view.json.MappingJacksonJsonView#
     * prepareResponse(javax.servlet.http.HttpServletRequest,
     * javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void prepareResponse(HttpServletRequest request,
                                   HttpServletResponse response) {
        response.setContentType(getContentType());
        response.setCharacterEncoding(encoding.getJavaName());
        if (disableCaching) {
            response.addHeader("Pragma", "no-cache");
            response.addHeader("Cache-Control", "no-cache, no-store, max-age=0");
            response.addDateHeader("Expires", 1L);
        }
        jsonpKey = this.getJsonpParam(request);
        if (StringUtils.isNotEmpty(jsonpKey)) {
            jsonp = true;
        }
    }

	/*
     * 将model转换成json返回 (non-Javadoc)
	 * 
	 * @see org.springframework.web.servlet.view.json.MappingJacksonJsonView#
	 * renderMergedOutputModel(java.util.Map,
	 * javax.servlet.http.HttpServletRequest,
	 * javax.servlet.http.HttpServletResponse)
	 */

    @SuppressWarnings("deprecation")
    @Override
    protected void renderMergedOutputModel(Map<String, Object> m,
                                           HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        Object result = m.get(resultKey);
        if(result==null){
        	return;
        }
//        html_handle(result);
//        logger.info("======== "+result.toString());
    
        ObjectMapper objectMapper = JsonUtils.objectMapper;
        StringBuffer sb = new StringBuffer();
//        // 包装输出流
//        JsonGenerator generator = objectMapper.getJsonFactory()
//                .createJsonGenerator(response.getOutputStream(), encoding);
        // 支持jsonp跨域操作
        if (jsonp) {
            sb.append(jsonpKey).append("(");
        }
        sb.append(objectMapper.writeValueAsString(result));
        // 支持jsonp跨域操作
        if (jsonp) {
            sb.append(")");
        }
 //       try {
            String s = sb.toString();
            response.getOutputStream().write(SafeEncoder.encode(s));
            response.getOutputStream().flush();
//            response.getOutputStream().close();
//            // objectMapper.writeValue(generator,output);
//        } catch (Exception e) {
//        		logger.error("",e);
//        		JsonGenerator generator = objectMapper.getJsonFactory().createJsonGenerator(
//                    response.getOutputStream(), encoding);
//            objectMapper.writeValue(generator, result);
//        }

    }

    /**
     * 增加对jsonp的支持
     *
     * @param request
     * @return
     */
    private String getJsonpParam(HttpServletRequest request) {
        String param = "rt";
        return request.getParameter(param);
    }

//    private String html_encode(String content) {
//    	String res = content.replaceAll("&", "&gt;").replaceAll("<", "&lt;")
//    			.replaceAll(">", "&gt;").replaceAll(" ", "&nbsp;")
//    			.replaceAll("\'", "&#39;").replaceAll("\"", "&quot;")
//    			.replaceAll("\n", "<br>");
//    	return res;
//    }
    
//    public void html_handle(Object obj) throws Exception {
//		Class clazz = obj.getClass();
//		PropertyDescriptor[] propertyDescriptors = Introspector.getBeanInfo(clazz)
//				.getPropertyDescriptors();
//		for (PropertyDescriptor one : propertyDescriptors) {
//			String fieldName = one.getName();
//			if (!"class".equals(fieldName)) {
//				Method writeMethod = one.getWriteMethod();
//				Method readMethod = one.getReadMethod();
//				Field field = ReflectUtil
//						.getFieldByName(fieldName, clazz);
//				if(readMethod.getReturnType() == String.class) {
//					NocheckEncode nocheckEncode = field
//							.getAnnotation(NocheckEncode.class);
//					if (null == nocheckEncode){
//						Object objValue = readMethod.invoke(obj);
//						if(null != objValue){
//							String value = objValue.toString();
//						    value = html_encode(value);
//							writeMethod.invoke(obj, value);
//						}
//						
//					}
//				}
//			}
//		}
//	}

}
