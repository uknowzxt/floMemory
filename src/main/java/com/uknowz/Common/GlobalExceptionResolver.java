package com.uknowz.Common;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;

/**
 * @Author: tengyun
 * @Date:Create in  2018/8/20 下午3:11
 * @description:
 */
@Slf4j
@Component
public class GlobalExceptionResolver implements HandlerExceptionResolver {

    private static Logger errorLog = LoggerFactory.getLogger("error");

    @Override
    @ExceptionHandler(Exception.class)
    public ModelAndView resolveException(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, Exception exception) {
        BizResult bizResult;
        if (exception instanceof ExceptionHlz) {
            bizResult = BizResult.create(((ExceptionHlz) exception).getCode(), exception.getMessage());
        } else if (exception.getClass().getName().equals("java.lang.NullPointerException")) {
            logException(httpServletRequest, exception);
            bizResult = BizResult.create(ExcsEnum.INTERNAL_NullPointerException.getCode(), ExcsEnum.INTERNAL_NullPointerException.getMessage());
        } else {
            logException(httpServletRequest, exception);
            //bizResult = BizResult.create(ExcsEnum.INTERNAL_ERROR.getCode() + "", ExcsEnum.INTERNAL_ERROR.getMessage());
            bizResult = BizResult.create(ExcsEnum.INTERNAL_ERROR.getCode(), exception.getMessage());
        }
        PrintWriter out = null;
        try {
            httpServletResponse.setContentType("application/json; charset=utf-8");
            //这里必须返回sc_ok=200
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            out = httpServletResponse.getWriter();
            out.println(handleJsonOrJsonp(httpServletRequest.getParameter("callback"), JSON.toJSONString(bizResult)));
            out.flush();
        } catch (Exception e) {
            errorLog.error("Output Response ExceptionHlz", e);
        } finally {
            if (out != null) {
                out.close();
            }
        }
        return new ModelAndView();
    }

    private static void logException(HttpServletRequest request, Exception ex) {

        errorLog.error(String.format("ExceptionHlz#Input parameters: url=%s", getRequestInfo(request)), ex);
    }

    private String handleJsonOrJsonp(String jsonpCallback, String result) {
        // 将tab换成空格
        if (!StringUtils.isEmpty(result)) {
            result = result.replaceAll("\t", " ");
        }

//        if (jsonpCallback != null) {
//            jsonpCallback = StringEscapeUtils.escapeHtml(jsonpCallback);
//            StringBuilder stringBuilder = new StringBuilder(jsonpCallback).append("(").append(result).append(")");
//            result = stringBuilder.toString();
//        }

        return result;
    }

    private static String getRequestInfo(HttpServletRequest req) {
        StringBuilder bufMsg = new StringBuilder();
        bufMsg.append(req.getRequestURI()).append(",params:{");

        Map paraMap = req.getParameterMap();
        Iterator it = paraMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            String key = (String) entry.getKey();
            String[] values = (String[]) entry.getValue();
            String value = "[";
            for (String v : values) {
                value += v + ",";
            }
            value = (value.length() > 1 ? value.substring(0, value.length() - 1) : value) + "]";
            bufMsg.append(key).append(":").append(value).append(",");
        }
        return (paraMap.size() > 0 ? bufMsg.substring(0, bufMsg.length() - 1) : bufMsg) + "}";
    }
}
