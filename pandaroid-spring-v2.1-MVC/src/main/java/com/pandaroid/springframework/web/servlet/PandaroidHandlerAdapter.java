package com.pandaroid.springframework.web.servlet;

import com.pandaroid.springframework.annotation.PandaroidRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author pandaroid
 */
public class PandaroidHandlerAdapter {
    public PandaroidModelAndView handler(HttpServletRequest req, HttpServletResponse resp, PandaroidHandlerMapping handlerMapping) {
        // 首先保存参数和参数 index 的对应列表
        Map<String, Integer> paramName2IndexMap = new HashMap<>();
        Method beanMethod = handlerMapping.getMethod();
        // 匹配到的 beanMethod invoke
        // 这里还需要对 Parameters 的注解 PandaroidRequestParam 进行处理
        // 获取 Parameters beanMethod.getParameters() ，然后获取带注解的参数，根据注解进行参数填充
        // Map<String, String[]> reqParameterMap = req.getParameterMap();
        Parameter[] beanMethodParameters = beanMethod.getParameters();
        // 首先组织 Parameters
        ArrayList<Object> beanMethodInvokeParameters = new ArrayList<>(beanMethodParameters.length);
        // Object[] beanMethodInvokeParameters = new Object[]{req, resp};
        // 迭代 beanMethodParameters ，看是否注解 PandaroidRequestParam
        for (Parameter beanMethodParameter : beanMethodParameters) {
            // 如果是 HttpServletRequest ，则直接设入 req 值
            if(beanMethodParameter.getType().equals(HttpServletRequest.class)) {
                beanMethodInvokeParameters.add(req);
                continue;
            }
            // 如果是 HttpServletResponse ，则直接设入 resp 值
            if(beanMethodParameter.getType().equals(HttpServletResponse.class)) {
                beanMethodInvokeParameters.add(resp);
                continue;
            }
            // 没有 PandaroidRequestParam 注解，直接取参数的名字
            // 没有开启 -parameters ，默认获取到的 reqParamName 为 arg0 、arg1 、arg2 ... 这种形式
            // 正常情况下使用 JVM 不会默认保留参数名字，因为这个可能导致 class 文件过大或其他问题
            // 所以最好还是通过注解绑定 requestParam
            /*String reqParamName = beanMethodParameter.getName();
            // 有 PandaroidRequestParam 注解，取出 value 作为请求参数名（即 reqParameterMap 的 key），从 reqParameterMap 中获取请求参数值
            if(beanMethodParameter.isAnnotationPresent(PandaroidRequestParam.class)) {
                String reqParamCustomeName = beanMethodParameter.getAnnotation(PandaroidRequestParam.class).value();
                if(null != reqParamCustomeName && !("".equals(reqParamCustomeName.trim()))) {
                    reqParamName = reqParamCustomeName;
                }
            }*/
            // 没有 PandaroidRequestParam 注解，直接跳过不处理
            if(!beanMethodParameter.isAnnotationPresent(PandaroidRequestParam.class)) {
                continue;
            }
            // 有 PandaroidRequestParam 注解，取出 value 作为请求参数名（即 reqParameterMap 的 key），从 reqParameterMap 中获取请求参数值
            String reqParamName = beanMethodParameter.getAnnotation(PandaroidRequestParam.class).value();
            // 取出请求 reqParamName 对应的 reqParamValues
            // String[] reqParamValues = reqParameterMap.get(reqParamName);
            // 将 reqParamValue 作为 args Object[] 数组的下一个值
            // 这里暂时只处理单个参数，不处理 Array 类型参数
            // String reqParamStrValue = reqParamValues[0];
            String reqParamStrValue = req.getParameter(reqParamName);
            // Start: String 类型的 reqParamStrValue 转为对应类型
            // Spring 允许自定义的类型转换器 Converter ，这里我们暂时先不支持扩展 Converter ，先暂时强制转类型，不支持配置
            Object reqParamValue = doConvertParamForceCastStringValue(reqParamStrValue, beanMethodParameter.getType());
            // End  : String 类型的 reqParamStrValue 转为对应类型
            // 存入参数数组
            beanMethodInvokeParameters.add(reqParamValue);
        }
        // 暴力访问调用 beanMethod.invoke()
        beanMethod.setAccessible(true);
        // 这里获取 beanName ，通过当前 beanMethod 找到它被声明的 Class ，然后获取到 SimpleName（与初始化 IoC 的时候 PandaroidController 的 key 一致）
        // String beanName = applicationContext.toLowerFirstCase(beanMethod.getDeclaringClass().getSimpleName());
        Object[] beanMethodInvokeParametersObjects = beanMethodInvokeParameters.toArray();
        // System.out.println("[PandaroidHandlerAdapter doDispatch] beanName: " + beanName);
        // System.out.println("[PandaroidHandlerAdapter doDispatch] beanMethodInvokeParametersObjects: " + Arrays.toString(beanMethodInvokeParametersObjects));
        try {
            // beanMethod 中通过 resp 返回结果
            // 完成对方法返回 ModelAndView 的封装，如何解析渲染？
            Object result = beanMethod.invoke(handlerMapping.getController(), beanMethodInvokeParametersObjects);
            // 1. 如果返回为空，则不用处理
            if(null == result || result instanceof Void) {
                return null;
            }
            // 2. 如果是 ModelAndView
            if(PandaroidModelAndView.class == beanMethod.getReturnType()) {
                return (PandaroidModelAndView) result;
            }
            // 3. 如果之后扩展，增加 @ResponseBody 注解，则将自定义的返回类型进行相应处理后，适配成 ModelAndView 返回
        } catch (InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Object doConvertParamForceCastStringValue(String reqParamStrValue, Class<?> type) {
        System.out.println("[PandaroidHandlerAdapter doConvertParam] reqParamStrValue: " + reqParamStrValue);
        System.out.println("[PandaroidHandlerAdapter doConvertParam] type: " + type);
        // 简单的对类型进行转化，处理用到的 Integer 类型
        if(type.equals(Integer.class)) {
            return Integer.valueOf(reqParamStrValue);
        }
        //
        return reqParamStrValue;
    }
}
