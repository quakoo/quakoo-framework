package com.quakoo.baseFramework.mail;

import java.io.File;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;


import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.dm.model.v20151123.SingleSendMailRequest;
import com.aliyuncs.dm.model.v20151123.SingleSendMailResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;

/**
 * 
 * @author LiYongbiao1
 *
 */
public class MailUtil {


    public static void sendEmail(String accessKeyId,String secret,String aliAccount,
                                 String name,
                                 String reveiveMailAddress,
                                 String subject, String content){

        IClientProfile profile = DefaultProfile.getProfile("cn-hangzhou", accessKeyId, secret);
        IAcsClient client = new DefaultAcsClient(profile);
        SingleSendMailRequest request = new SingleSendMailRequest();
        try {
            request.setAccountName(aliAccount);
            request.setFromAlias(name);
            request.setAddressType(1);
            request.setTagName(subject);
            request.setReplyToAddress(true);
            request.setToAddress(reveiveMailAddress);
            request.setSubject(subject);
            request.setHtmlBody(content);
            SingleSendMailResponse httpResponse = client.getAcsResponse(request);
        } catch (ServerException e) {
            e.printStackTrace();
        }
        catch (ClientException e) {
            e.printStackTrace();
        }
    }



    /**
     * <br>
     * 用阿里云的新方法发送邮件
     * 方法说明：发送邮件 <br>
     * 输入参数： <br>
     * 返回类型：boolean 成功为true，反之为false
     * 
     * "system@51book.com", "sysOF51b0ok", "system@51book.com", mailAddress,
     * "smtp.263xmail.com", title, e + e.getMessage() + baos.toString(), null
     * 
     * @return
     */
    @Deprecated
    public static void sendEmail(String from, final String password, final String username, String reveiveMailAddress,
            String host, String subject, String content, Vector<File> file) throws Exception {

        String fileName = null;
        // 构造mail session
        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.localhost", host);
        props.put("mail.smtp.auth", "true");
        Session session = Session.getDefaultInstance(props, new Authenticator() {
            @Override
            public PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
        // Session session = Session.getDefaultInstance(props);
        // Session session = Session.getDefaultInstance(props, null);

        // 构造MimeMessage 并设定基本的值
        MimeMessage msg = new MimeMessage(session);
        // MimeMessage msg = new MimeMessage();
        msg.setFrom(new InternetAddress(from));

        // msg.addRecipients(Message.RecipientType.TO, address);
        // //这个只能是给一个人发送email
        msg.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(reveiveMailAddress));

        msg.setSubject(subject);

        // 构造Multipart
        Multipart mp = new MimeMultipart();

        // 向Multipart添加正文
        MimeBodyPart mbpContent = new MimeBodyPart();
        mbpContent.setContent(content, "text/html;charset=utf-8");

        // 向MimeMessage添加（Multipart代表正文）
        mp.addBodyPart(mbpContent);

        // 向Multipart添加附件
        if (file != null) {
            Enumeration efile = file.elements();
            while (efile.hasMoreElements()) {

                MimeBodyPart mbpFile = new MimeBodyPart();
                fileName = efile.nextElement().toString();
                FileDataSource fds = new FileDataSource(fileName);
                mbpFile.setDataHandler(new DataHandler(fds));
                String filename = new String(fds.getName().getBytes(), "ISO-8859-1");

                mbpFile.setFileName(filename);
                // 向MimeMessage添加（Multipart代表附件）
                mp.addBodyPart(mbpFile);

            }

            file.removeAllElements();
        }
        // 向Multipart添加MimeMessage
        msg.setContent(mp);
        msg.setSentDate(new Date());
        msg.saveChanges();
        // 发送邮件

        Transport transport = session.getTransport("smtp");
        transport.connect(host, username, password);
        transport.sendMessage(msg, msg.getAllRecipients());
        transport.close();

    }

    /**
     * <br>
     * 方法说明：主方法，用于测试 <br>
     * 输入参数： <br>
     * 返回类型：
     */
    public static void main(String[] args) {
        try {
            MailUtil.sendEmail("j7aHxtaMnUS4aZJG",
                    "4mghcK0qx4zn5oDdNKUvRfyniCcDA2",
                    "kf@automail.quakoo.com",
                    "雀科科技",
                    "125503048@qq.com",
                    "测试11111",
                    "测试222222222222222222222222222");
        } catch (Exception e) {

            e.printStackTrace();
        }


//        try {
//            MailUtil.sendEmail("kf@quakoo.com", "6SHAXsyc", "kf@quakoo.com", "125503048@qq.com",
//                    "smtp.mxhichina.com", "测试11111", "测试222222222222222222222222222", null);
//        } catch (Exception e) {
//
//            e.printStackTrace();
//        }


    }
}
