package com.softwood.rules.api

interface Action {
    def execute (param)
    def execute ()
}