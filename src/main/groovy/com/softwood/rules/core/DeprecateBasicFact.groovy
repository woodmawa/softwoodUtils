package com.softwood.rules.core

import com.softwood.rules.api.Fact
import groovy.transform.MapConstructor
import groovy.transform.ToString

/**
 * may not be necessary or maybe we just use Tuple as base
 */
@MapConstructor
@ToString
class DeprecateBasicFact implements Fact {

    String name = ""
    String description = ""

    DeprecateBasicFact() {}

    DeprecateBasicFact(String name, value) {
        this.name = name
        if (!description)
            description = name
        $entry = [(name):value]
    }

    @Delegate
    private Map $entry = [:]

    void setName(Map e) {
        $entry = e
        name = $entry.entrySet()?[0].key
    }

    def getName() {
        name = $entry.entrySet()?[0].key
    }

    def getDescription() {
        description
    }

    def getValue() {
        def lov = $entry.values()
        return $entry.values()?[0]
    }

    void setEntry(Map arg) {
        assert arg.size() == 1
        $entry = arg
        name = $entry.entrySet()?[0].key
        description = name
    }


    //need to intercept as otherwise it defaults all properties to the map delegate
    def getProperty(String name) {
        if (name == 'name')
            return getName()
        else if (name =='description')
            return getDescription()
        else if (name == 'value')
            return getDescription()
        else
            return $entry.(name)     //just invoke on map delegate
    }

}
