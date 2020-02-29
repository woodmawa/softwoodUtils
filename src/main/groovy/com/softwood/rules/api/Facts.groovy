package com.softwood.rules.api

import com.softwood.rules.core.BasicFact
import groovy.transform.ToString

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * Collection of fact with a name as key to retrieve a Fact, and can be iterated over
 */
@ToString
class Facts<String, Fact>  {

    //add these methods to Facts
    @Delegate
    Map<String, Fact> $map  = new HashMap()

    String name = "my Facts"
    String description = "some standard facts"

    // create a Fact from the key, and value from the $map delegate for same key
    Fact getFact (key) {
        //create a new map with one  entry
        BasicFact fact = new BasicFact((key),  $map.get(key))
        return fact
    }


    List<Fact> asList () {
        $map.iterator().toList()
    }


    //need to intercept as otherwise it defaults all properties to the map delegate
    def getProperty(java.lang.String name) {
        if (name == 'name')
            return getName()
        else if (name =='description')
            return getDescription()
         else
            return $map.(name)     //just invoke on map delegate
    }

}
