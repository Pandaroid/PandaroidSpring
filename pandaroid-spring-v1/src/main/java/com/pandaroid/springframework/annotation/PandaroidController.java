package com.pandaroid.springframework.annotation;


import java.lang.annotation.*;

/**
 * 控制器
 * @author pandaroid
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PandaroidController {
    String value() default "";
}
