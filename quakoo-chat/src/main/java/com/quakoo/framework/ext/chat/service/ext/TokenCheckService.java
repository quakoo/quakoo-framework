package com.quakoo.framework.ext.chat.service.ext;

/**
 * token信息检测
 *
 * class_name: TokenCheckService
 * package: com.quakoo.framework.ext.chat.service.ext
 * creat_user: lihao
 * creat_date: 2019/1/29
 * creat_time: 17:12
 **/
public interface TokenCheckService {

    /**
     * 接收到连接检测
     * method_name: check
     * params: [token]
     * return: boolean
     * creat_user: lihao
     * creat_date: 2019/1/29
     * creat_time: 17:13
     **/
    public boolean check(String token);

}
