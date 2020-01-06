package com.quakoo.baseFramework.http;

import com.quakoo.baseFramework.util.StreamUtils;
import com.quakoo.baseFramework.secure.Base64Util;
import com.quakoo.baseFramework.util.StreamUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SchemeSocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.*;
import org.apache.http.util.ByteArrayBuffer;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * @author liyongbiao
 *         <p>
 *         线程安全的http连接池实现
 *         </p>
 *         4:44:57 PM Jul 3, 2012
 */

public class MultiHttpPool {
    Logger logger = LoggerFactory.getLogger(MultiHttpPool.class);

    static Random random= new Random();

    static Map<HttpPoolParam, MultiHttpPool> multiHttpUtilsMap = new HashMap<HttpPoolParam, MultiHttpPool>();

    static List<HttpPoolParam> proxyList;

    private static final int commonTimeOut = 10000;

    private int reTryTimes=3;

    DefaultHttpClient httpClient = null;

    private String defaultCharSet = Charset.defaultCharset().name();

    private MultiHttpPool() {
    }


    public MultiHttpPool(final HttpPoolParam param) {
        try {
            this.reTryTimes = param.getReTryTimes();
            PoolingClientConnectionManager cm = new PoolingClientConnectionManager();
            // 最大连接数
            cm.setMaxTotal(param.getConnManagerMaxTotal());
            // 每个站点最大连接数
            cm.setDefaultMaxPerRoute(param.getConnManagerMaxPerRoute());

            /*** ssl支持 begin **/
            // ssl支持
            X509TrustManager tm = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };

            SSLContext ctx = SSLContext.getInstance(SSLSocketFactory.TLS);
            ctx.init(null, new TrustManager[]{tm}, null);
            SSLSocketFactory ssf = new SSLSocketFactory(ctx, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            SchemeRegistry sr = cm.getSchemeRegistry();
            sr.register(new Scheme("https", 443, ssf));
            /*** ssl支持 end **/

            /**** socket代理 begin */
            if (StringUtils.isNotBlank(param.getProxyAddress())
                    && param.getProxyType() == HttpPoolParam.ProxyType.socket) {
                // 443 ssl的socket代理 暂时不弄
                sr.register(new Scheme("http", 80, new SchemeSocketFactory() {
                    @Override
                    public boolean isSecure(Socket sock) throws IllegalArgumentException {
                        if (sock == null) {
                            throw new IllegalArgumentException("Socket may not be null.");
                        }
                        if (sock.isClosed()) {
                            throw new IllegalArgumentException("Socket is closed.");
                        }
                        return false;
                    }

                    // 创建的时候绑定代理服务
                    @Override
                    public Socket createSocket(HttpParams httpparams) throws IOException {
                        Socket socket = new Socket(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(param
                                .getProxyAddress(), param.getProxyPort())));
                        if (StringUtils.isNotBlank(param.getProxyUserName())) {
                            Authenticator.setDefault(new Authenticator() {
                                @Override
                                protected PasswordAuthentication getPasswordAuthentication() {
                                    return new PasswordAuthentication(param.getProxyUserName(), param
                                            .getProxyPassword().toCharArray());
                                }
                            });
                        }
                        return socket;
                    }

                    @Override
                    public Socket connectSocket(Socket socket, InetSocketAddress remoteAddress,
                                                InetSocketAddress localAddress, HttpParams params) throws IOException,
                            UnknownHostException, ConnectTimeoutException {
                        if (remoteAddress == null) {
                            throw new IllegalArgumentException("Remote address may not be null");
                        }
                        if (params == null) {
                            throw new IllegalArgumentException("HTTP parameters may not be null");
                        }
                        Socket sock = socket;
                        if (sock == null) {
                            sock = createSocket(params);
                        }
                        if (localAddress != null) {
                            sock.setReuseAddress(HttpConnectionParams.getSoReuseaddr(params));
                            sock.bind(localAddress);
                        }
                        int connTimeout = HttpConnectionParams.getConnectionTimeout(params);
                        int soTimeout = HttpConnectionParams.getSoTimeout(params);
                        try {
                            sock.setSoTimeout(soTimeout);
                            sock.connect(remoteAddress, connTimeout);
                        } catch (SocketTimeoutException ex) {
                            throw new ConnectTimeoutException((new StringBuilder()).append("Connect to ")
                                    .append(remoteAddress.getHostName()).append("/").append(remoteAddress.getAddress())
                                    .append(" timed out").toString());
                        }
                        return sock;

                    }
                }));

            }
            /**** socket代理 end */

            HttpParams params = new BasicHttpParams();
            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
            HttpProtocolParams.setContentCharset(params, "utf-8");
            HttpProtocolParams.setUseExpectContinue(params, true);
            HttpProtocolParams.setHttpElementCharset(params, "utf-8");
            httpClient = new DefaultHttpClient(cm, params);
            // 服务器响应时间
            httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, param.getSoTime());
            // 连接服务器时间
            httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, param.getConnectioneTime());
            httpClient.getParams().setParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, true);

            if (param.isCookie()) {
                // 设置是否支持cookie
                httpClient.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);
            }
            if(!param.isFollowRedirects()){
                httpClient.getParams().setParameter(ClientPNames.HANDLE_REDIRECTS, false);
            }

            // http代理
            if (StringUtils.isNotBlank(param.getProxyAddress()) && param.getProxyType() == HttpPoolParam.ProxyType.http) {
                org.apache.http.HttpHost proxy = new org.apache.http.HttpHost(param.getProxyAddress(),
                        param.getProxyPort());
                httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);

                // 非匿名代理
                if (StringUtils.isNotBlank(param.getProxyUserName())) {
                    CredentialsProvider credsProvider = new BasicCredentialsProvider();
                    UsernamePasswordCredentials creds = new UsernamePasswordCredentials(param.getProxyUserName(),
                            param.getProxyPassword());
                    credsProvider.setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT), creds);
                    httpClient.setCredentialsProvider(credsProvider);
                }
            }
        } catch (Exception e) {
            logger.error("",e);
        } catch (Error e) {
            logger.error("",e);
        }
    }


    //===============MultiHttpPool初始化的一些方式=================================

    /**
     * 根据HttpPoolParam来获取httpClientPool实例
     *
     * @return
     */
    public static MultiHttpPool getMultiHttpPool(HttpPoolParam httpParam) {
        if (multiHttpUtilsMap.get(httpParam) == null) {
            multiHttpUtilsMap.put(httpParam, new MultiHttpPool(httpParam));
        }
        return multiHttpUtilsMap.get(httpParam);
    }

    public static MultiHttpPool getMultiHttpPool(int time) {
        HttpPoolParam param = new HttpPoolParam(time);
        return getMultiHttpPool(param);
    }

    public static MultiHttpPool getMultiHttpPool() {
        return getMultiHttpPool(commonTimeOut);
    }

    /**
     * 支持cookie的连接池
     *
     * @return
     */
    public static MultiHttpPool getMultiHttpPoolWithCookie() {
        HttpPoolParam param = new HttpPoolParam(commonTimeOut);
        param.setCookie(true);
        param.setConnectioneTime(20000);
        return getMultiHttpPool(param);
    }

    /**
     * 从代理池 随机取出代理配置进行连接
     *
     * @return
     */
    public static MultiHttpPool getMultiHttpPoolWithRandomProxy(List<HttpPoolParam> thisproxyList) {
        proxyList = thisproxyList;
        int maxSize = proxyList.size();
        HttpPoolParam httpPoolParam = proxyList.get(random.nextInt(maxSize));
        return getMultiHttpPool(httpPoolParam);
    }

    /**
     * 初始化代理池(443https,暂时不支持socket代理)
     *
     * @param thisproxyList
     */
    public static void setProxyList(List<HttpPoolParam> thisproxyList) {
        proxyList = thisproxyList;
    }


    //================================================

    public DefaultHttpClient getHttpClientInit() {
        return httpClient;
    }

    public HttpResult httpQuery(String url,Map<String, Object> getParamsMap, String methodType,
                                boolean log,boolean logResult) throws Exception{
        return httpQuery(url, getParamsMap, methodType, null, null, null,null,
                defaultCharSet, defaultCharSet, null, true, false, log, logResult);
    }

    public HttpResult httpQuery(String url,Map<String, Object> getParamsMap, String methodType,
                                Map<String, String> headMap, Map<String, Object> postFromParamsMap,
                                byte[] postData,boolean returnStream,boolean log,boolean logResult) throws Exception{
        return httpQuery(url, getParamsMap, methodType, headMap, postFromParamsMap, postData,null,
                defaultCharSet, defaultCharSet, null, true, returnStream, log, logResult);
    }

    public HttpResult httpQueryWithRety(String url,Map<String, Object> getParamsMap, String methodType,
                                        Map<String, String> headMap, Map<String, Object> postFromParamsMap,
                                        byte[] postData,boolean returnStream,boolean log,boolean logResult) throws Exception{
        Exception exception = null;
        HttpResult httpResult=null;
    	if(reTryTimes==0){
    		reTryTimes=1;
    	}
        for(int i=0;i<reTryTimes;i++){
            try{
                httpResult=httpQuery(url, getParamsMap, methodType, headMap, postFromParamsMap, postData,null,
                        defaultCharSet, defaultCharSet, null, true, returnStream, log, logResult);
                if(httpResult.getCode()<400||i==reTryTimes-1){
                    break;
                }else{
                    if(returnStream&&httpResult.getInputStream()!=null){
                        httpResult.getInputStream().close();
                    }
                }
            }catch(Exception  e){
                exception=e;
                if(httpResult!=null&&returnStream&&httpResult.getInputStream()!=null){
                    httpResult.getInputStream().close();
                }
            }
        }
        if(httpResult==null){
            throw exception;
        }else{
            return httpResult;
        }
    }





    public HttpResult httpQuery(
            String url,Map<String, Object> getParamsMap, String methodType,
            Map<String, String> headMap, Map<String, Object> postFromParamsMap,
            byte[] postData,File formPostFile,String reqCharSet, String resCharSet,ContentType contentType,
            boolean urlEncode,boolean returnStream,boolean log,boolean logResult) throws IOException, IllegalStateException {
    	HttpResult result = new HttpResult();
        HttpRequestBase method = null;
        result.setCharset(resCharSet);
        try{
            if(postFromParamsMap!=null&&postFromParamsMap.size()>0&&postData!=null&&postData.length>0){
                throw new RuntimeException("postFromParamsMap and postData all exist");
            }
            url = getFullUrl(url, getNameValuePair(getParamsMap), urlEncode, defaultCharSet);

            HttpEntity entity=null;
            if(postFromParamsMap!=null&&postFromParamsMap.size()>0) {
                List<NameValuePair> postFromParams= getNameValuePair(postFromParamsMap);
                entity = new UrlEncodedFormEntity(postFromParams, reqCharSet);

            }else if(postData!=null&&postData.length>0){
                entity = new ByteArrayEntity(postData);

            }else if(formPostFile!=null){
            	   entity=MultipartEntityBuilder.
            			   create().addBinaryBody("file", formPostFile).build();
            }


            if ("post".equalsIgnoreCase(methodType)) {
                method = new HttpPost(url);
                ((HttpPost) method).setEntity(entity);
            } else if ("put".equalsIgnoreCase(methodType)) {
                method = new HttpPut(url);
                ((HttpPut) method).setEntity(entity);
            } else if ("delete".equalsIgnoreCase(methodType)) {
                method = new HttpDelete(url);
            } else {
                method = new HttpGet(url);
            }

            if (headMap != null) {
                for (Map.Entry<String,String> entry : headMap.entrySet()) {
                    method.setHeader(entry.getKey(), entry.getValue());
                }
            } else {
                method.setHeader("Connection", "keep-alive");
            }

            String uid = UUID.randomUUID().toString();
            if (log) {
                logger.info("http query:{},uid:{}", url, uid);
            }
            long ctime = System.currentTimeMillis();
            HttpResponse response = getHttpClientInit().execute(method);

            HttpEntity entity1 = response.getEntity();
            Header ceheader = entity1.getContentEncoding();
            if (ceheader != null) {
                for (HeaderElement element : ceheader.getElements()) {
                    if (element.getName().equalsIgnoreCase("gzip")) {
                        response.setEntity(new GzipDecompressingEntity(response.getEntity()));
                    }
                }
            }

            int statusCode = response.getStatusLine().getStatusCode();
            Header[] headers = response.getAllHeaders();


            Map<String, String> responseHead = new HashMap<String, String>();
            if (headers != null) {
                for (Header header : headers) {
                    responseHead.put(header.getName(), header.getValue());
                }
            }
            result.setHeaders(responseHead);
            result.setCode(statusCode);

            if(returnStream){
                result.setInputStream(new HttpInputStream(response.getEntity().getContent(),method));
            }else {
                byte[] bodys = toByteArray(response.getEntity());
                result.setBody(bodys);

                ContentType reContentType =ContentType.get(entity);
                Charset charset = null;
                if (reContentType != null) {
                    charset = reContentType.getCharset();
                    if (charset == null) {
                        final ContentType defaultContentType = ContentType.getByMimeType(reContentType.getMimeType());
                        charset = defaultContentType != null ? defaultContentType.getCharset() : null;
                    }
                }
                if(charset==null){
                    result.setCharset(CharsetDetector.guessEncoding(bodys));
                }else{
                    result.setCharset(charset.name());
                }

            }




            String resultString =""+statusCode+","+result.getResult();
            if (resultString!=null&&resultString.length() > 200) {
                resultString = resultString.trim().substring(0, 200);
            }
            if (log) {
                if(logResult) {
                    logger.info("http return time:{},uid:{},result{}", new Object[]{System.currentTimeMillis() - ctime,
                            uid, resultString});
                }else{
                    logger.info("http return time:{},uid:{},result{}", new Object[]{System.currentTimeMillis() - ctime,
                            uid, statusCode});
                }
            }

            return result;
        }finally{
            if (method != null&&!returnStream) {
                try {
                    method.abort();
                } catch (Exception e) {

                }
            }
        }
    }


    private static final String PARAMETER_SEPARATOR = "&";
    private static final String NAME_VALUE_SEPARATOR = "=";

    public static String format (final List <? extends NameValuePair> parameters,boolean enCode,String charSet) {
        if(enCode){
            return URLEncodedUtils.format(parameters,charSet);
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


    /**
     * EntityUtils.toByteArray
     * @param entity
     * @return
     * @throws IOException
     */
    public  byte[] toByteArray(final HttpEntity entity) throws IOException {
        if (entity == null) {
            throw new IllegalArgumentException("HTTP entity may not be null");
        }
        InputStream instream = entity.getContent();
        if (instream == null) {
            return null;
        }
        try {
            if (entity.getContentLength() > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("HTTP entity too large to be buffered in memory");
            }
            int i = (int)entity.getContentLength();
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
        } finally {
            instream.close();
        }
    }


    public static  List<NameValuePair>  getNameValuePair(Map<String,Object> paramsMap){
        List<NameValuePair> formparams = new ArrayList<NameValuePair>();
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

    public static String getFullUrl(String url,List<NameValuePair> formparams,boolean urlEncode,String reqCharSet){
        if (url.contains("?")) {
            url = url + (formparams.size() == 0 ? "" : "&") + format(formparams, urlEncode, reqCharSet);
        } else {
            url = url + (formparams.size() == 0 ? "" : "?") + format(formparams, urlEncode, reqCharSet);
        }
        return url;
    }

    public static void main(String[] fwe) throws Exception {
    		
//    		String base64=Base64Util.encode("sdfljwelkfjwe".getBytes());
//    		HttpPoolParam httpParam=new HttpPoolParam(10000, 10000, 3);
//    		MultiHttpPool pool=MultiHttpPool.getMultiHttpPool(httpParam);
//    		Map<String, Object> postMap=new HashMap<>();
//    		postMap.put("file",base64);
//    		postMap.put("suffix", "txt");
//    		HttpResult httpResult=pool.httpQuery("http://store.quakoo.com/storage/guagua/handle64", postMap, "get", null, null, null, false, false, true);
//    		System.out.println(httpResult.getResult());


        HttpPoolParam httpPoolParam = new HttpPoolParam(5000, 5000, 1);
        httpPoolParam.setFollowRedirects(false);
        MultiHttpPool httpPool = MultiHttpPool.getMultiHttpPool(httpPoolParam);
        HttpResult httpResult = httpPool.httpQuery("http://jtt.ln.gov.cn/zc/yjaq/201308/t20130819_2515599.html", null, "get", null, null, null, false, true, false);
        System.out.println(httpResult.getCharset());
        httpResult = httpPool.httpQuery("http://www.baidu.com", null, "get", null, null, null, false, true, false);
        System.out.println(httpResult.getCharset());
        httpResult = httpPool.httpQuery("http://www.haixiangjiaoyu.com/", null, "get", null, null, null, false, true, false);
        System.out.println(httpResult.getCharset());
    }
}
