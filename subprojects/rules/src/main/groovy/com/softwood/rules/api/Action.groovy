package com.softwood.rules.api

interface Action {
    def invoke (param)
    def invoke ()
    Map getStateData()
    void setStateData (Map m)
    void clearStateData ()
}