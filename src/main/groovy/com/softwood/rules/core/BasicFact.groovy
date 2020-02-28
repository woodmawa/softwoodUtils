package com.softwood.rules.core

import com.softwood.rules.api.Fact
import groovy.transform.ToString

@ToString
class BasicFact implements Fact {

    String name = ""
    String description = ""

    @Delegate Map entry = [:]

    void setEntry (Map e) {
        entry = e
        name = entry.entrySet()?[0].key
    }

    def getName() {
        name = entry.entrySet()?[0].key
    }

    def getDescription() {
        description
    }

    def getValue() {
        def lov = entry.values()
        return entry.values()?[0]
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
            return entry.(name)     //just invoke on map delegate
    }

}
