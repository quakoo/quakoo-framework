package com.quakoo.baseFramework.http;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.quakoo.baseFramework.util.StreamUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.ByteArrayBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.quakoo.baseFramework.util.StreamUtils;

/**
 * Created by 136249 on 2015/3/30.
 * 比httpClient更快
 */
public class HttpUtils {

    static final Logger logger= LoggerFactory.getLogger(HttpUtils.class);


    /**
     * sotime:1000
     * connectTime:500
     * retry:3
     */
    public static HttpParam defaultHttpParam=new HttpParam(5000, 500, 3);


    public static HttpParam logResultHttpParam=new HttpParam(5000, 500, 3,true,true,false);

    public static HttpParam commonPostDataHttpParam=new HttpParam(10000, 1000, 3,true,false,false);


    /**
     *
     * @param httpParam httpParam
     * @param url url
     * @param getParamsMap get方式的参数
     * @param methodType 方法类型
     * @param headMap 头文件
     * @param postFromParamsMap 以form表单形式提交
     * @param postData 直接post字节流
     * @return
     * @throws Exception
     */
    public static HttpResult httpQuery(HttpParam httpParam,String url, Map<String, Object> getParamsMap, String methodType,
                                Map<String, String> headMap, Map<String, Object> postFromParamsMap, byte[] postData) throws Exception {
        HttpResult result = new HttpResult();
        result.setCharset(httpParam.getResCharSet());
        HttpURLConnection conn=null;
        InputStream inputStream=null;
        try {
            long ctime = System.currentTimeMillis();
            int statusCode = 0;
            List<NameValuePair> formparams = getList(getParamsMap);

            if (url.contains("?")) {
                url = url + (formparams.size() == 0 ? "" : "&") + format(formparams, httpParam.isUrlEncode(), httpParam.getReqCharSet());
            } else {
                url = url + (formparams.size() == 0 ? "" : "?") + format(formparams, httpParam.isUrlEncode(), httpParam.getReqCharSet());
            }
            StringBuilder logUrl=new StringBuilder(url);
            URL _url=new URL(url);
            conn = (HttpURLConnection)_url.openConnection();
            if("post".equalsIgnoreCase(methodType)) {
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
            }
            conn.setConnectTimeout(httpParam.getConnectioneTime());
            conn.setReadTimeout(httpParam.getSoTime());
            if (headMap != null) {
                for (Map.Entry<String,String> entry : headMap.entrySet()) {
                    conn.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
            byte[] postBodys=null;
            if(postFromParamsMap!=null&&postFromParamsMap.size()>0){
                if(postData!=null&&postData.length>0){
                    throw new RuntimeException("conflict postFromParamsMap and postData ");
                }
                List<NameValuePair> postNameValuePairs = getList(postFromParamsMap);
                String bodyString=format(postNameValuePairs, httpParam.isPostFromEncode(), httpParam.getReqCharSet());
                logUrl.append(bodyString);
                postBodys=bodyString.getBytes(httpParam.getReqCharSet());
            } else if(postData!=null&&postData.length>0){
                conn.setRequestProperty("Content-Type", "x-application/bytes");
            	conn.setRequestProperty("Content-Length", "" + 
                            Integer.toString(postData.length));
                postBodys=postData;
            }

            String uid = UUID.randomUUID().toString();
            if (httpParam.isLogResult()||httpParam.isLogTime()) {
                logger.info("http query:{},uid:{}", logUrl.toString(), uid);
            }

            conn.connect();
            
            if(postBodys!=null){
            	OutputStream out=conn.getOutputStream();
            	out.write(postBodys);
            	out.flush();
            }
            statusCode=conn.getResponseCode();
            Map<String, String> responseHead = new HashMap<>();
            Map<String, List<String>> header = conn.getHeaderFields();
            for (Map.Entry<String, List<String>> entry : header.entrySet()) {
                String key = entry.getKey();
                for (String value : entry.getValue()) {
                    // list???
                    responseHead.put(key, value);
                }
            }
            result.setHeaders(responseHead);
            result.setCode(statusCode);
            if(statusCode==200||statusCode==206){
	            if(httpParam.isReturnStream()){
	                result.setInputStream(new HttpInputStream(conn.getInputStream(),null));
	            }else {
	            	inputStream=conn.getInputStream();
	                byte[] bodys = toByteArray(inputStream,conn.getContentLength());
	                result.setBody(bodys);
	            }
            }
            String resultString =""+statusCode+","+result.getResult();
            if (resultString!=null&&resultString.trim().length() > 200) {
                resultString = resultString.trim().substring(0, 200);
            }
            if (httpParam.isLogResult()) {
                logger.info("http return time:{},uid:{},result{}", new Object[]{System.currentTimeMillis() - ctime,
                        uid, resultString});
            }else if(httpParam.isLogTime()){
                logger.info("http return time:{},uid:{},result{}", new Object[]{System.currentTimeMillis() - ctime,
                        uid, statusCode});
            }
            return result;
        } catch(Exception e){
            throw  new Exception("url"+url,e);
        } finally {
        	if (inputStream != null&&!httpParam.isReturnStream()) {
                try {
                	inputStream.close();
                } catch (Exception e) {

                }
            }
            if (conn != null&&!httpParam.isReturnStream()) {
                try {
                    conn.disconnect();
                } catch (Exception e) {

                }
            }
            
        }

    }


    public static List<NameValuePair> getList(Map<String,Object> paramsMap){
        List<NameValuePair> formparams = new ArrayList<>();
        if (paramsMap != null) {
            for (Map.Entry<String,Object> entry : paramsMap.entrySet()) {
                Object objValue = entry.getValue();
                if (objValue == null) {
                    continue;
                }
                if (objValue instanceof List) {
                    List values = (List) objValue;
                    for (Object value : values) {
                        formparams.add(new BasicNameValuePair(entry.getKey(), value.toString()));
                    }
                } else {
                    formparams.add(new BasicNameValuePair(entry.getKey(), objValue.toString()));

                }
            }
        }
        return formparams;
    }


    private static final String PARAMETER_SEPARATOR = "&";
    private static final String NAME_VALUE_SEPARATOR = "=";

    public static String format (final List <? extends NameValuePair> parameters,boolean enCode,String charSet) {
        if(enCode){
            return URLEncodedUtils.format(parameters, charSet);
        }
        final StringBuilder result = new StringBuilder();
        for (final NameValuePair parameter : parameters) {
            final String encodedName = parameter.getName();
            final String encodedValue = parameter.getValue();
            if (result.length() > 0) {
                result.append(PARAMETER_SEPARATOR);
            }
            result.append(encodedName);
            if (encodedValue != null) {
                result.append(NAME_VALUE_SEPARATOR);
                result.append(encodedValue);
            }
        }
        return result.toString();
    }


    public static byte[] toByteArray(InputStream instream,int contentLength) throws IOException {
        if (instream == null) {
            return null;
        }
        int i = contentLength;
        if (i < 0) {
            i = 4096;
            ByteArrayBuffer buffer = new ByteArrayBuffer(i);
            byte[] tmp = new byte[4096];
            int l;
            while((l = instream.read(tmp)) != -1) {
                buffer.append(tmp, 0, l);
            }
            return buffer.toByteArray();
        }else{
            byte[] result = new byte[i];
            StreamUtils.readFully(instream, result, 0, result.length);
            return result;
        }
       
    }
    // 下载网络文件
    public static void downloadNet(String netUrl, String localPath) throws Exception {
        int byteread = 0;
        URL url = new URL(netUrl);
        try {
            URLConnection conn = url.openConnection();
            InputStream inStream = conn.getInputStream();
            FileOutputStream fs = new FileOutputStream(localPath);
            byte[] buffer = new byte[1204];
            while ((byteread = inStream.read(buffer)) != -1) {
                fs.write(buffer, 0, byteread);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
