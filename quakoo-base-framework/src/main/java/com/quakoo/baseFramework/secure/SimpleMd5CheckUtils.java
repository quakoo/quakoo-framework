package com.quakoo.baseFramework.secure;

import java.net.URLDecoder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Pattern;

import com.quakoo.baseFramework.util.QueryStringDecoder;
/**
 * Created by 136249 on 2015/2/27.
 */
public class SimpleMd5CheckUtils {

    public static String regx = "^((-?\\d+.?\\d*)[Ee]{1}(-?\\d+))$";

    public static Pattern pattern = Pattern.compile(regx);

    public static String encodeSingByUri(String uri, String... filterStrings) {
        return encodeSing(getParamsMapFromRequest(uri, filterStrings));
    }

    public static String encodeStringByParamMap_decodeToken(Map<String, String[]> map, String... filterStrings) {
        Map<String, List<String>> result = new HashMap<String, List<String>>();
        for (Entry<String, String[]> entry : map.entrySet()) {
            boolean has = false;
            if (filterStrings != null) {
                for (String key : filterStrings) {
                    if (key.equals(entry.getKey())) {
                        has = true;
                        break;
                    }
                }
            }
            if (!has) {
                result.put(entry.getKey(), Arrays.asList(entry.getValue()));
            }
        }
        return encodeSing_decodeToken(result);
    }

    public static String encodeSing_decodeToken(Map<String, List<String>> getParams) {
        try {
            TreeMap<String, List<String>> map = new TreeMap<>();
            map.putAll(getParams);
            StringBuilder sb = new StringBuilder("paihangbangmiyao");
            for (Map.Entry<String, List<String>> entry : map.entrySet()) {
                List<String> result = entry.getValue();
                for (String r : result) {
                    if (entry.getKey().equalsIgnoreCase("token")) {
                        sb.append(URLDecoder.decode(r, "utf-8"));
                    } else if (entry.getKey().equalsIgnoreCase("cursor") && pattern.matcher(r).matches()) {
                        DecimalFormat df = new DecimalFormat("##################.###");
                        sb.append(df.format(Double.parseDouble(r)));
                    } else {
                        sb.append(r);
                    }
                }
            }
            // System.out.println(sb.toString());
            return MD5Utils.md5ReStr(sb.toString().getBytes());
        } catch (Exception e) {
            throw new RuntimeException("encodeSing error", e);
        }
    }

    public static String encodeStringByParamMap(Map<String, String[]> map, String... filterStrings) {
        Map<String, List<String>> result = new HashMap<String, List<String>>();
        for (Entry<String, String[]> entry : map.entrySet()) {
            boolean has = false;
            if (filterStrings != null) {
                for (String key : filterStrings) {
                    if (key.equals(entry.getKey())) {
                        has = true;
                        break;
                    }
                }
            }
            if (!has) {
                result.put(entry.getKey(), Arrays.asList(entry.getValue()));
            }
        }
        return encodeSing(result);
    }

    /**
     * @param action
     *            API名字，比如 request_download
     * @param getParams
     *            所有get的请求的数据
     * @param accessKeySecret
     *            accessKeySecret
     * @return 签名
     */
    public static String encodeSing(Map<String, List<String>> getParams) {
        try {
            TreeMap<String, List<String>> map = new TreeMap<>();
            map.putAll(getParams);
            StringBuilder sb = new StringBuilder("paihangbangmiyao");
            for (Map.Entry<String, List<String>> entry : map.entrySet()) {
                List<String> result = entry.getValue();
                for (String r : result) {
                    if (entry.getKey().equalsIgnoreCase("cursor") && pattern.matcher(r).matches()) {
                        DecimalFormat df = new DecimalFormat("##################.###");
                        sb.append(df.format(Double.parseDouble(r)));
                    } else {
                        r = r.replaceAll("[\\ud800\\udc00-\\udbff\\udfff\\ud800-\\udfff]", "*");
                        sb.append(r);
                    }
                }
            }
            // System.out.println(sb.toString());
            return MD5Utils.md5ReStr(sb.toString().getBytes());
        } catch (Exception e) {
            throw new RuntimeException("encodeSing error", e);
        }
    }

    /**
     * 根据uri获取 Map<String,List<String>>
     * 
     * @param uri
     * @param filterStrings
     *            需要过滤的字符串
     * @return
     */
    public static Map<String, List<String>> getParamsMapFromRequest(String uri, String... filterStrings) {
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(uri);
        Map<String, List<String>> result = queryStringDecoder.getParameters();
        if (null != filterStrings) {
            for (String filterString : filterStrings) {
                result.remove(filterString);
            }
        }
        return result;
    }

    /**
     * 将Map<String,Object> 类型转换成 Map<String,List<String>>类型
     * 
     * @param paramsMap
     * @param filterStrings
     * @return
     */
    public static Map<String, List<String>> getParamsMapFromObject(Map<String, Object> paramsMap,
            String... filterStrings) {
        Map<String, List<String>> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : paramsMap.entrySet()) {
            Object objValue = entry.getValue();
            if (objValue == null) {
                continue;
            }
            if (objValue instanceof List) {
                List values = (List) objValue;
                List<String> target = new ArrayList<>();
                for (Object value : values) {
                    target.add(value.toString());
                }
                result.put(entry.getKey(), target);
            } else {
                result.put(entry.getKey(), Arrays.asList(objValue.toString()));
            }
        }

        if (null != filterStrings) {
            for (String filterString : filterStrings) {
                result.remove(filterString);
            }
        }
        return result;
    }

    public static void main(String[] fwe) throws Exception {
        Map<String, List<String>> getParams = new HashMap<>();
        getParams.put("123", Arrays.asList("123124124"));
        getParams.put("1234", Arrays.asList("fwefwef"));
        getParams.put("1233", Arrays.asList("fwefwfwefef"));
        getParams.put("0133", Arrays.asList("fwefwwefef"));
        getParams.remove("111");
        long time = 1000;
        System.out.println(getParamsMapFromRequest("/dfw?sfe=1fwe&fweg=123&fwe=124", "fwe"));
    }

}
