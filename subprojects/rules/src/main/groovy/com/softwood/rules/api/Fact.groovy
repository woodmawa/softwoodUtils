package com.softwood.rules.api

/**
 * Fact is a disguised Map.Entry
 */
interface Fact {
    def getValue()
    def getName()
}
