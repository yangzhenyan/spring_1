package com.yang.framework.annotation;

import java.lang.annotation.*;

/**
 * @author yzy
 * @date 2020/8/23
 * @describe
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface YController {
    String value() default "";
}
