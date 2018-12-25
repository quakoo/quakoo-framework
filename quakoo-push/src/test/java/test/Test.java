package test;

import com.xiaomi.xmpush.server.Constants;
import com.xiaomi.xmpush.server.Message;
import com.xiaomi.xmpush.server.Result;
import com.xiaomi.xmpush.server.Sender;

import com.quakoo.baseFramework.redis.JedisX;
import com.quakoo.baseFramework.redis.RedisDecrParam;
import com.quakoo.baseFramework.redis.RedisIncrParam;

public class Test {

//	private static ConfigurableApplicationContext context = new ClassPathXmlApplicationContext("test-servlet.xml");
//
//	public static void main(String[] args) throws Exception {
//		System.out.println("==============================");
//
//		JedisX cache = context.getBean("cachePool", JedisX.class);
//
//		List<RedisDecrParam> params = Lists.newArrayList();
//		RedisDecrParam one = new RedisDecrParam("a");
//		RedisDecrParam two = new RedisDecrParam("b");
//		params.add(one);
//		params.add(two);
//		Map<RedisDecrParam, Long> res = cache.pipDecr(params);
//		System.out.println(res.toString());
//
//		System.exit(1);
//	}

    private static void sendMessage() throws Exception {
        Constants.useOfficial();
        Sender sender = new Sender("NsYPHggkDg8xt3N2Blr4DQ==");
//        String messagePayload = “This is a message”;
        String title = "2";
        String description = "2";
        Message message = new Message.Builder()
                .title(title).passThrough(0)
                .description(description)
                .restrictedPackageName("com.queke.im")
                .notifyType(1).notifyId(1)     // 使用默认提示音提示
                .build();
        Result result = sender.sendToAlias(message, "1026", 3);
        System.out.println(result.toString());

    }

    public static void main(String[] args) throws Exception {
//        sendMessage();

//        String out_trade_no = "11111111";
//        // 订单名称，必填
//        String subject = "测试";
//        // 付款金额，必填
//        String total_amount="1.00";
//        // 商品描述，可空
//        String body = "测试";
//        // 超时时间 可空
//        String timeout_express="2m";
//        // 销售产品码 必填
//        String product_code="QUICK_WAP_WAY";
//        /**********************/
//        // SDK 公共请求类，包含公共请求参数，以及封装了签名与验签，开发者无需关注签名与验签
//        //调用RSA签名方式
//        AlipayClient client = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do",
//                "2016091701912250", "MIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAoIBAQDNmSjtpY3HM6JQWaMYFr4EQphQIAzFhwndM8d2zLBst22wQOsE+CpIVOqCtLhBFwt0g7+TPhR5kt6qPuqVF2O0PduptGIVwujs9ZSuOHxSlKMJDPTuHEkDiHcPlNy0VWnpZJHT1RP1sEv+tnCvOayxjJe008rMLVM+8BQRDhqA2rVwgoa4/Fit7d6TLbmUrYq+NeGNg6jJ5dOT68wieINGhSVSAfIvV+Gq66tNPtde/Ea4OFG4aN87JahLQM32PZxUW/l+B1Lw97Ab6iQrT0+z4VszLQtUkPc0IAlbjDI3uYnR0Ou36mja3x8bz3tf8aQK/5wAQOnGinKRhRD3LorBAgMBAAECggEBAJ03qJ+JgJA6gL1WvivWgDl7v0NOLtYBfF6FkNrcjXClF6C3E8C6EIM9RBLtJRTJDX5snNt4lmmdEN44vGOD/j8/KjFsebDQWhORe795/D+d38o0+Tr3sDGecgUdPWQ9CH9r40OMZfXzeTluw16LCO7AZuXlKufVJRo0tfOQyF2NAN81VQgBXOr97E0vuKE32fJLRoXRVu9DfTZASlYtVhZNf9JIdTsa/MsGYrnBsIDxbHd9XItHfm/StdwE0B8aliSkHHxcHxc7+tEui191TgIYtFQ767gFm3rez9IYOOJKYdvhvJBD+jZrKlZTFl3FhAl11nlZbQ+17XiFOnx0ORECgYEA+7Ahaww/gE7NiemoovPaqjUS2T33IDZgOJuI1OzOxcYkhKfv70NnLzS8Ro8p/pUrCZqKvDXIpmoFeMlcVVpu+M79ZdhGwGvWq7+Eo8QPm8Km02mfuM6dLU9EbMl7kqGYTxg+8dcOMJ0L/aYS4Kduva6Kjdc9PrDbhgKLKBydUtUCgYEA0R7i02gXStNDYH4Jz9k97gvYe8wvlcSNx/7m8xxEOP18WXTYl5BSl5eaNqPK0RT1x/QaVqkBA7hZlq9rOy6tDkwuZ8FFu1+/FiRU4v9mVY9phi8CxtAYzjp7Gl3o0zpuI3H66u/Mkhf3Lk3+DDoP8drXVAO8HWwHMuWWkHUFlj0CgYB9sjbeWV2VXMW1rKX1y8dW3e6t5A55Xe6sFrE9dY7GovA1+BER8x2jK7kjm/gjFqhJwY6r9EhjI+vbqOSeE+38AJP4uwyKqNCrMZiymQYaihn613NXPoisyjuoAO/gCSghyEAXU4nYqXYFlV00KSpbPMIhmG8T0guwX7DTtYa0iQKBgQCx99UWEXnxJgYrMlhrWPiJg+9CvCyrCtGWqOonb6Dn12JiRzylECAZ/t0xLwfGFE5vUuCva7j/T35+7N8XSMviZRcBGcycgAmBcH4FiVGv8xLdLVjxldvmdoKZl94zFYDyDWh7VIUGt/qWn61a/nbfX1ztlRgc+fjOafFPEygkCQKBgQC/4KEM2TDQzIjv60c4DZD99H8QI/i86uzocP3wede9L3d5O+/WzYpck/Oybvuh+hYlJcREjeJMwiCDmGGvo4CXwrA5ZrybSeZolxMyeEN/cddo4Gg61yIgy2kLL7Dqs+qthNcCBt+8VLDLusvdygNh68cX6bIgCO9uglN+NIYrfA==",
//                "json", "UTF-8", "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAoi3o3DQ8IOnKCY3J4WbOaqDlYDwmkjFBqOOdyqnAkvJ0OeKlh+7MwyRNw2+aacswDRv9HiqYlnmCS4dZ8R0dl0VK4gSWL34wSagHSbjWdTVShMTfqJj+LY/b5xHB6wNDyAek1vARYVJr343iE72vAGPaPZlxI26QC91Lsn1c4zfSr7Ak1I3RjsuwMvwcgVSqMcNpTbmoW7vZlB1U9fdDBkQlVRrNGdl9zFRMo8rklUznfu2dWi9d3rHTVxYVo/tFIaE4FriFzjJkmX+txuTq5yDh81heyF18mDVNAM6BN+NfARCgDkohUxERHh+0Pu9bRQ3s7FLmcRKv/Qg++tgWRwIDAQAB", "RSA2");
//
//        AlipayTradeWapPayRequest alipay_request=new AlipayTradeWapPayRequest();
//
//        // 封装请求支付信息
//        AlipayTradeWapPayModel model=new AlipayTradeWapPayModel();
//        model.setOutTradeNo(out_trade_no);
//        model.setSubject(subject);
//        model.setTotalAmount(total_amount);
//        model.setBody(body);
//        model.setTimeoutExpress(timeout_express);
//        model.setProductCode(product_code);
//        alipay_request.setBizModel(model);
//        // 设置异步通知地址
//        alipay_request.setNotifyUrl("http://39.107.247.82:21113/paynotify/ali");
//        // 设置同步地址
//        alipay_request.setReturnUrl("http://192.168.0.6:8020/backend/test/success.html");
//
//        // form表单生产
//        String form = client.pageExecute(alipay_request).getBody();
//        System.out.println(form);
    }

}
