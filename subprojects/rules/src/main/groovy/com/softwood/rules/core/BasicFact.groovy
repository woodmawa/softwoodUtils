package com.softwood.rules.core

import com.softwood.rules.api.Fact
import groovy.transform.EqualsAndHashCode
import groovy.transform.MapConstructor
import groovy.transform.ToString

/**
 * had problems coercing Map.Entry  to Fact interface
 * so introduced concrete class that can be extended
 *
 */
@ToString
@EqualsAndHashCode
@MapConstructor
class BasicFact implements Fact {

    String name
    def value


    BasicFact(Map.Entry e) {
        name = e.getKey()
        value = e.getValue()
    }

    BasicFact leftShift(Map.Entry e) {
        assert e
        name = e.getKey()
        value = e.getValue()
        this
    }

}
