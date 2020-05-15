package com.softwood.utils

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

@Target([ElementType.TYPE])
@Retention(RetentionPolicy.RUNTIME)
@interface TmfEntity {
    String baseType () default ''
    String domain () default '<Undefined Domain>'
    String version() default 'v1'
}