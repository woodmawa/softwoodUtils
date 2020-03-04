package com.softwood.game

import com.softwood.rules.api.Fact
import com.softwood.rules.core.BasicFact
import org.codehaus.groovy.runtime.MethodClosure

class Sensor {
    String name
    Closure $detect = {key, attmap->
        def val = attmap.get(key)
        val}

    //try and sense state on arg - nominally player in this example
    Fact sense(arg) {
        println "sense for $name in $arg"
        assert arg
        def attVal = arg.attributes.get (name)
        println "\tsense for $name in $arg, found $attVal"

        if  (name) {
            Fact fact = new BasicFact (name:name, value:$detect(name,  arg.attributes))
            println "\tbuilt a Fact with $fact"
            fact
        }

    }
}
