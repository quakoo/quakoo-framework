package com.quakoo.baseFramework.secure;

import com.quakoo.baseFramework.util.QueryStringDecoder;
import redis.clients.util.SafeEncoder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Created by 136249 on 2015/2/27.
 */
public class HmacSha1Utils {

    private static final String HMAC_SHA1 = "HmacSHA1";

    private static byte[]  hmacSha1(byte[] data, byte[] key) throws InvalidKeyException, NoSuchAlgorithmException {
        SecretKeySpec signingKey = new SecretKeySpec(key, HMAC_SHA1);
        Mac mac = Mac.getInstance(HMAC_SHA1);
        mac.init(signingKey);
        byte[] rawHmac = mac.doFinal(data);
        return rawHmac;
    }

    public static String encodeSingByUri(String action,long timestamp,String uri,String accessKeySecret,String... filterStrings) {
        return  encodeSing(action,timestamp,getParamsMapFromRequest(uri,filterStrings),accessKeySecret);
    }

    /**
     *
     * @param action API名字，比如  request_download
     * @param getParams 所有get的请求的数据
     * @param accessKeySecret  accessKeySecret
     * @return 签名
     */
    public static String encodeSing(String action,long timestamp,Map<String,List<String>> getParams,String accessKeySecret)  {
        try {
            TreeMap<String, List<String>> map = new TreeMap<>();
            map.putAll(getParams);
            StringBuilder sb = new StringBuilder(action);
            sb.append("\n").append(timestamp);
            for (Map.Entry<String,List<String>> entry : map.entrySet()) {
                List<String> result = entry.getValue();
                for (String r : result) {
                    sb.append("\n").append(r);
                }
            }
            byte[] singBytes = SafeEncoder.encode(sb.toString());
            byte[] hmacBytes = hmacSha1(singBytes, accessKeySecret.getBytes());
            return Base64Util.encode(hmacBytes);
        }catch (Exception e){
            throw  new RuntimeException("encodeSing error",e);
        }
    }

    /**
     * 根据uri获取 Map<String,List<String>>
     * @param uri
     * @param filterStrings 需要过滤的字符串
     * @return
     */
    public static Map<String,List<String>> getParamsMapFromRequest(String uri,String... filterStrings){
        QueryStringDecoder queryStringDecoder=new QueryStringDecoder(uri);
        Map<String,List<String>> result= queryStringDecoder.getParameters();
        if(null!=filterStrings){
            for(String filterString:filterStrings){
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
    public static  Map<String,List<String>> getParamsMapFromObject(Map<String,Object> paramsMap,String... filterStrings){
        Map<String,List<String>> result= new HashMap<>();
        for(Map.Entry<String,Object> entry:paramsMap.entrySet()){
            Object objValue=entry.getValue();
            if (objValue == null) {
                continue;
            }
            if (objValue instanceof List) {
                List values = (List) objValue;
                List<String> target = new ArrayList<>();
                for (Object value : values) {
                    target.add(value.toString());
                }
                result.put(entry.getKey(),target);
            } else {
                result.put(entry.getKey(), Arrays.asList(objValue.toString()));
            }
        }

        if(null!=filterStrings){
            for(String filterString:filterStrings){
                result.remove(filterString);
            }
        }
        return result;
    }


    public static void main(String[] fwe)throws Exception{
        Map<String,List<String>> getParams=new HashMap<>();
        getParams.put("123", Arrays.asList("123124124"));
        getParams.put("1234",Arrays.asList("fwefwef"));
        getParams.put("1233",Arrays.asList("fwefwfwefef"));
        getParams.put("0133",Arrays.asList("fwefwwefef"));
        getParams.remove("111");
        long time=1000;
        System.out.println(encodeSing("request_upload", time, getParams, "testAccessKeySecret"));
        System.out.println(getParamsMapFromRequest("/dfw?sfe=1fwe&fweg=123&fwe=124","fwe"));
    }

}
