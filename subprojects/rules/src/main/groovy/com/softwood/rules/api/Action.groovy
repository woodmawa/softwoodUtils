package com.softwood.rules.api

interface Action {
    def invoke(param)

    def invoke()

    Map getStateData()

    void setStateData(Map m)

    void clearStateData()

    //allows groovy () call semantics, just invokes correct invoke() above
    def call(param)

    def call()

    //declare gettser/setters in public APi
    void setName(String n)

    String getName()

    void setDescription(String p)

    String getDescription()

    void setAction(Closure action)

    Closure getAction()
}