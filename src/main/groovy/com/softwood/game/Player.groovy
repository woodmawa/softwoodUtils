package com.softwood.game

import com.softwood.rules.api.Fact
import com.softwood.rules.api.Facts

class Player {
    String name

    HashMap attributes = [:]

    List<Sensor> sensors = []

    Facts getWorldState () {
        sensors.collect {
            it.sense() as Fact
        }
    }


}
