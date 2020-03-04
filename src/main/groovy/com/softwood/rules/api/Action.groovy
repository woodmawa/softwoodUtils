package com.softwood.rules.api

import java.util.concurrent.ConcurrentMap

interface Action {
    def invoke (param)
    def invoke ()
    Map getStateData()
    void setStateData (Map m)
    void clearStateData ()
}