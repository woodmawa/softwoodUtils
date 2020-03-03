package com.softwood.game

import com.softwood.rules.api.Fact
import com.softwood.rules.core.BasicFact
import org.codehaus.groovy.runtime.MethodClosure

class Sensor {
    String name
    Player owner
    Closure detect = {key, attmap-> attmap.get(key) }

    Fact sense() {
        assert owner
        if  (owner.attributes.get (name)) {
            new BasicFact (name:name, value:detect(name,  owner.attributes))
        }

    }
}
