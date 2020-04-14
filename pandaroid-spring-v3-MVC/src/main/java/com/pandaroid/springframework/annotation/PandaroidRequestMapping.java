package com.pandaroid.springframework.annotation;

import java.lang.annotation.*;

/**
 * 请求 url 和 handler 映射
 * @author pandaroid
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PandaroidRequestMapping {
    String value() default "";
}
