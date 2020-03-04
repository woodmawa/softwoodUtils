package com.softwood.game

import com.softwood.rules.api.Fact
import com.softwood.rules.core.BasicFact
import org.codehaus.groovy.runtime.MethodClosure

class Sensor {
    String name
    Closure $detect = {key, attmap-> attmap.get(key)}

    //try and sense state on arg - nominally player in this example
    Fact sense(arg) {
       assert arg
        def attVal = arg.attributes.get (name)

        if  (name) {
            new BasicFact (name:name, value:$detect(name,  arg.attributes))
        }

    }
}
