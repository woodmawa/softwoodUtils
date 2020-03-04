package com.softwood.game

import com.softwood.rules.api.Fact
import com.softwood.rules.api.Facts

class Player {
    String name

    //this would need to be concurrent safe - right now its not
    HashMap<String, Object> attributes = [:]


    Map applyAttributeStateUpdates (Map updateAtts) {
        updateAtts.each {String key, value ->
            if (attributes.containsKey (key))
                attributes.remove(key)
            attributes.put (key, value)
        }
        attributes.asImmutable()
    }

}
