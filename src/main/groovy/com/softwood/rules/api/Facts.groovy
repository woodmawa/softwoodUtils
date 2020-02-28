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
    Map<String, Fact> map  = new HashMap()

    String name = ""
    String description = ""

    // get entry entry ref based on key
    Fact fact (key) {
        BasicFact f = new BasicFact()
        //create a new map with one  entry
        f.entry = [(key): map.get(key)]
        return f
    }


    Iterator<Fact> iterator() {
        return map.iterator()
    }

    List<Fact> asList () {
        map.iterator().toList()
    }


}
