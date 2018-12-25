package com.quakoo.webframework;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author liyongbiao
 */
public class DefaultController implements Controller {
    @SuppressWarnings("unused")
    @Override
    public ModelAndView handleRequest(HttpServletRequest request,
                                      HttpServletResponse response) throws Exception {
//        String uri = request.getRequestURI();
//        String query = request.getQueryString();
//        if (uri != null && uri.length() > 1 && !(uri.indexOf("/") < 0)) {
//            String redirect = null;
//            if (redirect != null) {
//                ModelAndView view = new ModelAndView();
//                view.setViewName("redirect:" + redirect);
//                return view;
//            }
//        }
//
//        response.sendError(HttpServletResponse.SC_NOT_FOUND);
        return null;
    }
}
