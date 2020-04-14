package com.pandaroid.springframework.annotation;

import java.lang.annotation.*;

/**
 * 业务逻辑，需要被 DI
 * @author pandaroid
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PandaroidService {
    String value() default "";
}
