package com.softwood.rules.annotation

/*import java.lang.annotation.ElementType
import java.lang.annotation.Inherited
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target*/
import java.lang.annotation.*

/**
 * Annotation to mark a parameter as a fact.
 *
 * @author Will Woodman
 */

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Fact {
    String value()
}
