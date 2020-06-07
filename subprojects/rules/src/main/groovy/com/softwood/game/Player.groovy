package com.softwood.game

import com.softwood.rules.api.Fact
import com.softwood.rules.api.Facts

class Player {
    String name

    //this would need to be concurrent safe - right now its not
    HashMap<String, Object> attributes = [:]

    /**
     * given a map of changes, check if attributes exists, if so remove and add, else just add
     * returns new
     * @param updateAtts
     * @return
     */
    List<Map.Entry> applyAttributeStateUpdates(Map updateAtts) {
        updateAtts.each { String key, value ->
            if (attributes.containsKey(key))
                attributes.remove(key)
            attributes.put(key, value)
        }
        attributes.asImmutable().iterator().toList()
    }

    List<Map.Entry> getAttributeList() {
        attributes.asImmutable().iterator().toList()
    }

}
