package com.softwood.rules.api

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * Collection of fact with a name as key to retrieve a Fact, and can be iterated over
 */
class Facts extends ConcurrentHashMap<String, Fact> {

    String name = ""
    String description = ""

}
