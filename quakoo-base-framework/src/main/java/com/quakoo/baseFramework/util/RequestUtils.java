package com.quakoo.baseFramework.util;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author liyongbiao
 *
 */
public class RequestUtils {

	static Logger logger = LoggerFactory.getLogger(RequestUtils.class);

	public static String addParams(String url, String key, String value) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put(key, value);

		return addParams(url, map);

	}

	public static String addParams(String url, Map<String, Object> formparams) {
		try {
			if (url.contains("?")) {
				url = url + "&";
			} else {
				url = url + "?";
			}
			StringBuilder urlBuffer=new StringBuilder(url);
			for (Map.Entry<String,Object> entry : formparams.entrySet()) {
				if (entry.getValue() == null) {
					continue;
				}
				urlBuffer.append( entry.getKey());
				urlBuffer.append( "=");
				urlBuffer.append(entry.getValue().toString());
				urlBuffer.append("&");
			}
			url=urlBuffer.toString();
			if (formparams.size() > 0) {
				url = url.substring(0, url.length() - 1);
			}
		} catch (Exception e) {
			logger.error(url);
		}
		return url;

	}

	public static String addParamAndEnCode(String url, Map<String, Object> formparams) {
		try {
			if (url.contains("?")) {
				url = url + "&";
			} else {
				url = url + "?";
			}
			StringBuilder sb=new StringBuilder(url);
			for (Map.Entry<String,Object> entry : formparams.entrySet()) {
				sb.append(entry.getKey()).append("=")
						.append(URLEncoder.encode(entry.getValue().toString(),"utf-8"))
						.append("&");
			}
			url=sb.toString();
			if (formparams.size() > 0) {
				url = url.substring(0, url.length() - 1);
			}
		} catch (Exception e) {
			logger.error(url);
		}
		return url;
	}

	private static final String http = "http://";

	/**
	 * 返回完整的url，把域名放到配置文件，不硬编码。
	 * 
	 * @param url
	 * @return
	 */
	public static String getFullUrl(HttpServletRequest request, String url) {
		String temp = request.getServerName();
		int port = request.getServerPort();
		if (port == 80) {
			return http + temp + url;
		} else {
			return http + temp + ":" + port + url;
		}

	}

	

	/**
	 * 种cookie
	 * 
	 * @param response
	 * @param name
	 * @param value
	 * @param maxAge
	 */
	public static void addCookie(HttpServletResponse response, String name,
			String value, int maxAge) {
		Cookie cookie = new Cookie(name, value);
		cookie.setPath("/");
		if (maxAge > 0) {
			cookie.setMaxAge(maxAge);
		}
		response.addCookie(cookie);
	}

	/**
	 * 根据名称读取cookie
	 * 
	 * @param request
	 * @param name
	 * @return
	 */
	public static Cookie getCookieByName(HttpServletRequest request, String name) {
		Map<String, Cookie> cookieMap = readCookieMap(request);
		if (cookieMap.containsKey(name)) {
			Cookie cookie = cookieMap.get(name);
			return cookie;
		} else {
			return null;
		}
	}

	/**
	 * 读取cookie的map
	 * 
	 * @param request
	 * @return
	 */
	public static Map<String, Cookie> readCookieMap(HttpServletRequest request) {
		Map<String, Cookie> cookieMap = new HashMap<String, Cookie>();
		Cookie[] cookies = request.getCookies();
		if (null != cookies) {
			for (Cookie cookie : cookies) {
				cookieMap.put(cookie.getName(), cookie);
			}
		}
		return cookieMap;
	}

	/**
	 * 获取完整的url
	 * 
	 * @param request
	 * @return
	 */
	public static String getCompleteUrl(HttpServletRequest request) {
		try {
			Map<String,String[]> map = request.getParameterMap();
			StringBuilder queryString = new StringBuilder();
			if (map != null) {
				for (Map.Entry<String,String[]> entry : map.entrySet()) {
					String[] values = entry.getValue();
					if (values != null) {
						for (String value : values) {
							queryString.append(entry.getKey()).append("=").append(value).append("&");
						}
					}
				}
			}
			String queryStringStr=queryString.toString();
			if (StringUtils.isNotBlank(queryStringStr)) {
				return request.getRequestURI() + "?" + queryStringStr;
			} else {
				return request.getRequestURI();

			}
		} catch (Exception e) {
			return request.getRequestURI();

		}
	}



	public static String string2Unicode(String str) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < str.length(); i++) {
			if ((str.charAt(i) >= 'A' && str.charAt(i) <= 'Z')
					|| (str.charAt(i) >= 'a' && str.charAt(i) <= 'z')
					|| str.charAt(i) == '_'
					|| (str.charAt(i) >= '0' && str.charAt(i) <= '9'))

			{
				sb.append(str.charAt(i));
			} else {
				String tmpStr = Integer.toHexString(str.charAt(i));
				if (tmpStr.length() < 4) {
					sb.append("_u00");
				} else {
					sb.append("_u");
				}
				sb.append(tmpStr);
			}
		}
		return sb.toString();
	}

	public static String unicodeCover(String str) {
		StringBuffer sb = new StringBuffer("");

		for (;;) {

			int fIndex = str.indexOf("_u");
			if (fIndex > 0) {
				String beginStr = str.substring(0, fIndex);
				str = str.substring(fIndex);
				sb.append(beginStr);
			} else if (fIndex == 0) {
			} else {
				sb.append(str);
				return sb.toString();
			}

			if (isNum4(str)) {
				sb.append((char) Integer.parseInt(str.substring(2, 6), 16));
				str = str.substring(6);
			} else {
				str = str.substring(2);
				sb.append("_u");
			}

		}

	}

	private static boolean isNum4(String str) {
		try {
			Integer.parseInt(str.substring(2, 6), 16);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static void main(String[] sdg) throws Exception {
		System.out.println(unicodeCover("_u002e"));
		System.out
				.println(unicodeCover("4_5_95_10_7_reUrlhttp_u00253A_u00252F_u00252F127_u002e0_u002e0_u002e1_u00252Faccount_u00252FafterLogin_u00253FjsonValue_u00253D6_8_3_4_3_3_0_providertqqtypeweboptclientNametqqtest"));
		System.out
				.println(unicodeCover("_uefw5584u24_218874_0012uhttp://_u6_u"));
		System.out
				.println(unicodeCover("_uefw_u5584u24__u_u218874_0012uhttp://_u6_u"));
		System.out
				.println(unicodeCover("_uefw5584u24__u_u0025_u74__u5938uhttp://_u6_u"));
	}
}
