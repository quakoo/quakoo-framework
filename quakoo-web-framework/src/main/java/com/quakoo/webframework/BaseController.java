package com.quakoo.webframework;

import java.io.EOFException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.io.EofException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.AbstractView;

import com.quakoo.baseFramework.exception.BaseBusinessException;
import com.quakoo.baseFramework.exception.ServerBusyException;
import com.quakoo.baseFramework.util.RequestUtils;

/**
 * @author liyongbiao
 */
public class BaseController {

    Logger logger = LoggerFactory.getLogger(BaseController.class);

    protected final static String DATA_FORMAT = "rt";

    protected final static String DATA_FORMAT_JSON = "json";

    public static final Map<String, String> successResult = new HashMap<String, String>();

    public static final Map<String, String> erroResult = new HashMap<String, String>();

    static {
        erroResult.put("success", "false");
        successResult.put("success", "true");
    }

    /**
     * 返回页面
     *
     * @param viewName
     * @param request
     * @param response
     * @param model
     * @return
     */
    protected ModelAndView viewNegotiating(String viewName, final HttpServletRequest request,
                                           final HttpServletResponse response, final Model model) {
        ModelAndView modelAndView = new ModelAndView(viewName, model.asMap());
        return modelAndView;
    }

    /**
     * 直接返回json
     *
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    protected ModelAndView viewNegotiating(final HttpServletRequest request, final HttpServletResponse response,
                                           Object result) throws Exception {

        logger.info("@@@@@@@@@URI:" + request.getRequestURI() + ",method:" + request.getMethod());

        ModelAndView modelAndView = null;
        String f = request.getParameter(DATA_FORMAT);

        // 默认为json
        if (StringUtils.isBlank(f)) {
            f = DATA_FORMAT_JSON;
        }

        if (DATA_FORMAT_JSON.equalsIgnoreCase(f)) {
            modelAndView = jsonView(request, response, result);
        }
        return modelAndView;
    }

    protected ModelAndView jsonView(final HttpServletRequest request, final HttpServletResponse response,
                                    Object result) {
        ModelAndView modelAndView = null;
        AbstractView view = new MappingResponseJsonView();
        Map<String, Object> model = new HashMap<>();
        if (result != null) {
            model.put(MappingResponseJsonView.resultKey, result);
        }
        modelAndView = new ModelAndView(view, model);
        return modelAndView;
    }

    protected void printAsJavaScript(HttpServletResponse response, String message) throws IOException {
        response.setHeader("Cache-Control", "no-cache");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/javascript;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.write(message);
        out.close();
    }

    @ExceptionHandler
    public ModelAndView exp(Exception ex, HttpServletRequest request, HttpServletResponse response) {

        int httpCode = HttpStatus.SC_INTERNAL_SERVER_ERROR;
        String completeUrl = RequestUtils.getCompleteUrl(request);
        if (ex instanceof MissingServletRequestParameterException) {// param-missing
            httpCode = HttpStatus.SC_BAD_REQUEST;
            logger.error("spring http params missing. request URL:" + completeUrl + ",message:" + ex.getMessage(), ex);
        } else if (ex instanceof EofException || ex instanceof EOFException) {
            logger.error("eof exception request URL:" + completeUrl + ",message:" + ex.getMessage(), ex);
            return null;
        } else if (ex instanceof ServerBusyException || ex instanceof java.util.concurrent.RejectedExecutionException) {
            httpCode = HttpStatus.SC_SERVICE_UNAVAILABLE;
            logger.error("server busy:" + completeUrl + ",message:" + ex.getMessage(), ex);
        } else if (ex instanceof BaseBusinessException) {
            String errorCode = ((BaseBusinessException) ex).getErrorCode();
            if (org.apache.commons.lang.StringUtils.isNumeric(errorCode)) {
                httpCode = Integer.parseInt(errorCode);
            }
            if (((BaseBusinessException) ex).isPrintStack()) {
                logger.error("error:" + completeUrl + ",code:" + ((BaseBusinessException) ex).getErrorCode()
                        + ",message:" + ex.getMessage(), ex);
            } else {
                logger.error("error:" + completeUrl + ",code:" + ((BaseBusinessException) ex).getErrorCode()
                        + ",message:" + ex.getMessage());
            }
        } else {
            String exClassName = ex.getClass().getName();
            if (exClassName.contains("QuakooSystemException")) {
                String msg = ex.getMessage();
                QuakooSystemExceptionResult result = new QuakooSystemExceptionResult(msg);
                return jsonView(request, response, result);
            } else {
                logger.error("[response code=]" + httpCode + ",[request=]" + completeUrl, ex);
            }
        }
        response.setStatus(httpCode);
        return null;
    }

    public static void main(String[] args) {
        Exception e = new BaseBusinessException();
        System.out.println(e.getClass().getName());
    }

}
