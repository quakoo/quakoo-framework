package com.quakoo.webframework;

import com.quakoo.baseFramework.exception.ServerBusyException;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author liyongbiao
 */
public class ServerBusyInterceptor extends HandlerInterceptorAdapter {
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response, Object handler) throws Exception {
        if (ThreadMonitor.isBusy()) {
            throw new ServerBusyException(ThreadMonitor.getServerInfo());
        }
        return true;
    }
}
