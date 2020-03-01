package com.softwood.rules.api


import groovy.transform.ToString

/**
 * Collection of fact with a name as key to retrieve a Fact, and can be iterated over
 * todo might want to use Tuple for a Fact
 */
@ToString
class Facts<String, Object>  {

    //add these methods to Facts
    @Delegate
    Map<String, Object> $map  = new HashMap()

    String name = "my Facts"
    String description = "some standard facts"

    // get fact using key from $map delegate
    public <T> T getFact (key) {
        return (T) $map.get (key)
    }


    public <T> List<T> asList () {
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
