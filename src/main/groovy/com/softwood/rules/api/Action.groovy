package com.softwood.rules.api

import java.util.concurrent.ConcurrentMap

interface Action {
    def execute (param)
    def execute ()
    Map getStateData()
    void setStateData (Map m)
    void clearStateData ()
}