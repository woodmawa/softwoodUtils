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
            Fact fact = it.sense(player)
            map << [(fact?.name): fact?.value]
       }

     Facts facts = new Facts()
        facts.putAll ( map)
        facts
    }

}
