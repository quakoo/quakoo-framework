package com.quakoo.framework.ext.push;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.quakoo.baseFramework.http.HttpPoolParam;
import com.quakoo.baseFramework.http.HttpResult;
import com.quakoo.baseFramework.http.MultiHttpPool;
import com.quakoo.baseFramework.jackson.JsonUtils;

import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

/**
 * 华为推送客户端
 * class_name: HuaWeiSender
 * package: com.quakoo.framework.ext.push
 * creat_user: lihao
 * creat_date: 2019/1/30
 * creat_time: 15:15
 **/
public class HuaWeiSender {

    public static final int type_trans = 1;
    public static final int type_notice = 3;

    private String appSecret;

    private String appId;

    private String appPkgName;

    private String activityName;

    private String scheme;
    private String tokenUrl = "https://login.cloud.huawei.com/oauth2/v2/token"; //获取认证Token的URL
    private String apiUrl = "https://api.push.hicloud.com/pushsend.do"; //应用级消息下发API
    private String accessToken;//下发通知消息的认证Token
    private long tokenExpiredTime;  //accessToken的过期时间

    private MultiHttpPool httpPool;

    public HuaWeiSender(String appSecret, String appId, String appPkgName, String activityName, String scheme) {
        this.appSecret = appSecret;
        this.appId = appId;
        this.appPkgName = appPkgName;
        this.activityName = activityName;
        this.scheme = scheme;
        HttpPoolParam httpPoolParam = new HttpPoolParam(5000, 5000, 1);
        httpPool = new MultiHttpPool(httpPoolParam);
    }

    private void refreshToken() throws Exception {
        Map<String, Object> params = Maps.newHashMap();
        params.put("grant_type", "client_credentials");
        params.put("client_secret", URLEncoder.encode(appSecret, "UTF-8"));
        params.put("client_id", appId);
        HttpResult httpResult = httpPool.httpQuery(tokenUrl, null, "post", null,
                params, null, false, false, false);
        Map<String, Object> res = JsonUtils.fromJson(httpResult.getResult(), new TypeReference<Map<String, Object>>() {});
        accessToken = res.get("access_token").toString();
        tokenExpiredTime = System.currentTimeMillis() + Integer.parseInt(res.get("expires_in").toString()) * 1000 - 5*60*1000;
    }

    public void send(List<String> huaWeiTokens, String title, String content, int type, Map<String, String> ext) throws Exception {
        if (tokenExpiredTime <= System.currentTimeMillis()) refreshToken();
        String device_token_list = JsonUtils.toJson(huaWeiTokens);
        Map<String, String> body = Maps.newHashMap();
        body.put("title", title);
        body.put("content", content);
        Map<String, String> param = Maps.newHashMap();
        param.put("intent", "#Intent;scheme=" + scheme + ";component="+appPkgName+"/"+activityName+";end");
        Map<String, Object> action = Maps.newHashMap();
        action.put("type", 1);//类型3为打开APP，其他行为请参考接口文档设置
        action.put("param", param);//消息点击动作参数
        Map<String, Object> msg = Maps.newHashMap();
        msg.put("type", type);//3: 通知栏消息，异步透传消息请根据接口文档设置
        msg.put("action", action);//消息点击动作
        msg.put("body", body);//通知栏消息body内容
        Map<String, String> extra = Maps.newHashMap();
        extra.put("biTag", "Trump");//设置消息标签，如果带了这个标签，会在回执中推送给CP用于检测某种类型消息的到达率和状态
        if(null != ext && ext.size() > 0) {
            extra.putAll(ext);
        }
        Map<String, Object> hps = Maps.newHashMap();
        hps.put("msg", msg);
        hps.put("ext", ext);
        Map<String, Object> payload = Maps.newHashMap();
        payload.put("hps", hps);
        String payloadStr = JsonUtils.toJson(payload);

        String postBody = String.format("access_token=%s&nsp_svc=%s&nsp_ts=%s&device_token_list=%s&payload=%s",
                URLEncoder.encode(accessToken,"UTF-8"), URLEncoder.encode("openpush.message.api.send","UTF-8"),
                URLEncoder.encode(String.valueOf(System.currentTimeMillis() / 1000),"UTF-8"),
                URLEncoder.encode(device_token_list,"UTF-8"), URLEncoder.encode(payloadStr,"UTF-8"));
        String postUrl = apiUrl + "?nsp_ctx=" + URLEncoder.encode("{\"ver\":\"1\", \"appId\":\"" + appId + "\"}", "UTF-8");
        HttpResult httpResult = httpPool.httpQuery(postUrl, null, "post", null,
                null, postBody.getBytes("UTF-8"), false, false, false);
//        System.out.println(httpResult.getResult());
    }


    public static void main(String[] args) throws  Exception{
//        HuaWeiSender huaWaiSender = new HuaWeiSender("65b358ecc93ac90a140770856b962b1a",
//                "100543099", "com.queke.minglian",
//                "com.queke.minglian.MainActivity"
//        ,"customscheme");
//        List<String> tokens = Lists.newArrayList("AFF2RSBCeM3GlNCZFPbdJ0hKZ8B4Z70elXOXburRjsV_FaT-g1pKe0O8HHa3rCpZdi4pnRtXXk5a3XteQDO2g_GwC_A8pBvUcHd_v86d-xXQIpw8AWda5t-MuNLB8qtFjg");
//        huaWaiSender.send(tokens, "111", "111", HuaWeiSender.type_notice, null);
        HttpPoolParam httpPoolParam = new HttpPoolParam(5000, 5000, 1);
        String tokenUrl = "https://login.cloud.huawei.com/oauth2/v2/token";
        MultiHttpPool httpPool = new MultiHttpPool(httpPoolParam);

        Map<String, Object> params = Maps.newHashMap();
        params.put("grant_type", "client_credentials");
        params.put("client_secret", URLEncoder.encode("4c8077f22886696dbe84b4d46f3c0ade", "UTF-8"));
        params.put("client_id", "100412763");
        HttpResult httpResult = httpPool.httpQuery(tokenUrl, null, "post", null,
                params, null, false, false, false);
        Map<String, Object> res = JsonUtils.fromJson(httpResult.getResult(), new TypeReference<Map<String, Object>>() {});
        System.out.println(res.toString());

    }

//    private String httpPost(String httpUrl, String data, int connectTimeout, int readTimeout) throws IOException {
//        OutputStream outPut = null;
//        HttpURLConnection urlConnection = null;
//        InputStream in = null;
//        try {
//            URL url = new URL(httpUrl);
//            urlConnection = (HttpURLConnection)url.openConnection();
//            urlConnection.setRequestMethod("POST");
//            urlConnection.setDoOutput(true);
//            urlConnection.setDoInput(true);
//            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
//            urlConnection.setConnectTimeout(connectTimeout);
//            urlConnection.setReadTimeout(readTimeout);
//            urlConnection.connect();
//            // POST data
//            outPut = urlConnection.getOutputStream();
//            outPut.write(data.getBytes("UTF-8"));
//            outPut.flush();
//            // read response
//            if (urlConnection.getResponseCode() < 400) {
//                in = urlConnection.getInputStream();
//            } else {
//                in = urlConnection.getErrorStream();
//            }
//            List<String> lines = IOUtils.readLines(in, urlConnection.getContentEncoding());
//            StringBuffer strBuf = new StringBuffer();
//            for (String line : lines) {
//                strBuf.append(line);
//            }
//            return strBuf.toString();
//        } finally {
//            IOUtils.closeQuietly(outPut);
//            IOUtils.closeQuietly(in);
//            if (urlConnection != null) {
//                urlConnection.disconnect();
//            }
//        }
//    }

}
