package com.softwood.rules.core

import com.softwood.rules.api.Condition
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import groovy.util.logging.Slf4j
import org.codehaus.groovy.reflection.stdclasses.CachedClosureClass
import org.codehaus.groovy.runtime.ComposedClosure
import org.codehaus.groovy.runtime.ConvertedClosure
import org.codehaus.groovy.runtime.MethodClosure
import org.codehaus.groovy.runtime.*;

import java.util.function.Predicate

@MapConstructor
@Slf4j
//@CompileStatic
class ConditionClosure<V> extends Closure<V> implements Condition {

    /**
     * add static builder to create ConditionClosure from original source closure
     * @param predicate
     * @return
     *
     */
    static Condition from (Predicate predicate) {
         new ConditionClosure (predicate, predicate)
    }

    static Condition from (Closure closPredicate) {
        //could clone here
        new ConditionClosure (closPredicate, closPredicate)
    }

    static Condition from (MethodClosure methodReference) {
        new ConditionClosure (methodReference, methodReference)
    }

    /*
     * lowerLimit, upperLimit and measure are values that be for a condition and
     * used in the test closure
     *
     * measure can be set as absolute value to test against
     */
    def lowerLimit  = 0
    def upperLimit = 0
    def measure = 0

    //A condition can have a name, and a description
    final String UNNAMED = "unnamed"

    String name = UNNAMED
    String description = "anonymous"

    ConditionClosure(Object owner, Object thisObject ) {
        super(owner, thisObject)
    }

    ConditionClosure(Object owner) {
        super(owner, null)
    }

    public  Closure leftShift(final Closure other) {
        ComposedClosure composed =  new ComposedClosure(other, this)
        System.out.println "return composedClosure ${composed.toString()}  from ${other.toString()}"
        composed
    }

    /**
     * implement a doCall on this class. call() on inherited Closure will route to this doCall() here
     * @param args
     * @return
     */

    def doCall( args) {

        def result
        if (args) {
            if (maximumNumberOfParameters > 0)
                result = delegate.call(args)  //todo should this be delegate ?
            else
                result = delegate.call()
        }
        else
            result = delegate.call()

        result
    }


    boolean test (fact = null) {
        log.debug "evaluated test with <$fact> as input to the test"
        if (fact) {
            def result = call (fact)
         }
        else
            return call()    //just invoke the no args test
    }

    Condition and (Condition other) {
        Closure combined = {test(it) && other.test(it)}
        Condition condition =  ConditionClosure<Boolean>.from (combined)
        String thisName, otherName, compositeName
        thisName = getName()
        otherName = other.getName()
        condition.setName ( ( name == UNNAMED && other.getName()==UNNAMED) ?"($UNNAMED)": "($name & ${other.getName()})" )
        condition.setDescription  ("logical AND")
        condition
    }

    /**
     * generate a condition from the inverse of the current closure logic
     * @return
     */
    Condition  negate() {
        Condition negative =  ConditionClosure.from {!test ()}
        negative.setName ( "(NOT $name)" )
        negative.setDescription ("Negate $description")
        negative
    }

    Condition or (final Condition other) {
        //return super.or(other)
        Closure combined = {test(it) || other.test(it)}
        Condition condition =  ConditionClosure<Boolean>.from (combined)
        condition.setName ( ( name == UNNAMED && other.getName()==UNNAMED) ?"($UNNAMED)": "($name | ${other.getName()})" )
        condition.setDescription ("logical OR")
        condition
    }

    /*
     * if coerced to boolean evaluate the test and return it
     * otherwise just use the default groovy truth for this
     */
    boolean asType (Class clazz, param=null) {
        assert clazz
        if (clazz == Boolean)
            if (param)
                test(param)
            else
                test()
        else
            this //just use groovy truth here - return this condition, would normally be 'true'
    }

    String toString() {
        "${this.getClass().name} ($name, $description)"
    }

    //expect this doesnt work
    @Override
    Condition setConditionTest(Predicate predicate) {
        ConditionClosure condition = ConditionClosure.from (predicate::test)
        //having created a condition from the predicate, essentially clone the current into the new, and return new conditionClosure
        condition.name = name
        condition.description = name
        condition.lowerLimit = lowerLimit
        condition.upperLimit = upperLimit
        condition.measure = measure


        condition.delegate = condition
        condition.resolveStrategy = Closure.DELEGATE_FIRST
        condition
    }

    @Override
    Condition setConditionTest(Closure closurePredicate) {
        ConditionClosure condition = ConditionClosure.from (closurePredicate)
        //having created a condition from the predicate, essentially clone the current into the new, and return new conditionClosure
        condition.setName(name)
        condition.setDescription (description)
        condition.setLowerLimit (lowerLimit)
        condition.setUpperLimit (upperLimit)
        condition.setMeasure (measure)

        condition.resolveStrategy = Closure.DELEGATE_FIRST
        condition
    }

    public Object clone() {
        try {
            return super.clone()
        } catch (final CloneNotSupportedException e) {
            return null
        }
    }

    /**
     *because abstract parent implements getProperty - we have to overide to add our new properties
     */
    @Override
    public Object getProperty(final String property) {
        if ("name".equals(property)) {
            return getName();
        } else if ("description".equals(property)) {
            return getDescription();
        } else if ("lowerLimit".equals(property)) {
            return getLowerLimit();
        } else if ("upperLimit".equals(property)) {
            return getUpperLimit();
        } else if ("measure".equals(property)) {
            return getMeasure();
        } else super.getProperty(property )
    }

    @Override
    public void setProperty(final String property, Object newValue) {
        if ("name".equals(property)) {
            setName (newValue);
        } else if ("description".equals(property)) {
             setDescription(newValue);
        } else if ("lowerLimit".equals(property)) {
             getLowerLimit(Optional.ofNullable(newValue));
        } else if ("upperLimit".equals(property)) {
             getUpperLimit(Optional.ofNullable(newValue));
        } else if ("measure".equals(property)) {
             getMeasure(Optional.ofNullable(newValue));
        } else super.setProperty(property, newValue )
    }
}