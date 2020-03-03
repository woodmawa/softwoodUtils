package com.softwood.rules.api

import com.softwood.rules.core.BasicFact
import groovy.transform.ToString

import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Stream

/**
 * Collection of fact with a name as key to retrieve a Fact, and can be iterated over
 * todo might want to use Tuple for a Fact
 */
@ToString
class Facts<String, Object>  {

    //add these methods to Facts
    @Delegate
    Map<String, Object> $map  = new ConcurrentHashMap()

    String name = "my Facts"
    String description = "some standard facts"

    // get fact using key from $map delegate
    public <T> T getFact (key) {
        return (T) $map.get (key)
    }

    Collection<Fact> asFacts () {
        List list = $map.collect {
            new BasicFact (name:(it.key), value:it.value)
        }
        list.asImmutable()
    }

    //returns a stream of Map.EntrySetView
    Stream<Map.Entry> stream () {
        //$map.entrySet().stream()
        List facts = asFacts()
        facts.asImmutable().stream()
    }

    public <T> List<T> asList () {
        //$map.iterator().toList()
        asFacts().toList()
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
