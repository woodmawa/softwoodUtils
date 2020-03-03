package com.softwood.game

import com.softwood.rules.api.Fact
import com.softwood.rules.api.Facts

class SensorArray {
    String name

    List<Sensor> sensors = []

    SensorArray leftShift (Sensor sensor ) {
        assert sensor
        sensors << sensor
        this
    }

    void clearSensors () {
        sensors.clear()
    }

    Facts getPlayerWorldState (Player player) {
        Map map = [:]
        sensors.each {
            println "in getWS check sensor $it.name"
            Fact fact = it.sense(player)
            println "in getWS found fact  $fact"
            map << [(fact?.name): fact?.value]
            println "in getWS map is  $map"
        }

        Facts facts = new Facts()
        facts.$map.putAll ( map)
        facts
    }

}
