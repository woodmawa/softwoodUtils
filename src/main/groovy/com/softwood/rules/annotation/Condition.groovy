package com.softwood.rules.annotation

import java.lang.annotation.ElementType
import java.lang.annotation.Inherited
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Annotation to mark a method as a rule condition.
 * Must annotate any public method with no arguments and that returns a boolean value.
 *
 * @author Will Woodman
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface Condition {
}