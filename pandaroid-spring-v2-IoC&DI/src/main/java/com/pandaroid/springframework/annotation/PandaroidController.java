package com.pandaroid.springframework.annotation;


import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.annotation.*;

/**
 * 控制器
 * @author pandaroid
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PandaroidController {
    String value() default "";
}
