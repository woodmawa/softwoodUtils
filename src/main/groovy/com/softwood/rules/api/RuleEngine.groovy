package com.softwood.rules.api

interface RuleEngine {

    Collection<Boolean> check (Facts facts, RuleSet rules)
    Collection<Boolean> check (Facts facts, RuleSet rules, arg)
    def run (Facts facts, RuleSet rules)
    def run (Facts facts, RuleSet rules, arg)

}