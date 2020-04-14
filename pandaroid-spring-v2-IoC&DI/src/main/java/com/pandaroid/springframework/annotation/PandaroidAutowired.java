package com.pandaroid.springframework.annotation;

import java.lang.annotation.*;

/**
 * 自动注入
 * @author pandaroid
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PandaroidAutowired {
    String value() default "";
}
