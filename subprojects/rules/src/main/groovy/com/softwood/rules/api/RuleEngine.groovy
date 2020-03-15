package com.softwood.rules.api

interface RuleEngine {

    //normally expect the engine to run the Facts through att the com.softwood.rules in the set
    Collection<Boolean> check (Facts facts, RuleSet rules)
    Collection<Boolean> check (Facts facts, RuleSet rules, arg)
    def run (Facts facts, RuleSet rules)
    def run (Facts facts, RuleSet rules, arg)


    //engine can just also run a single rule is required
    boolean check (Facts facts, Rule rule)
    boolean check (Facts facts, Rule rule, arg)
    def run (Facts facts, Rule rule)
    def run (Facts facts, Rule rule, arg)

    void registerRuleListener (RuleListener listener)
    void registerRuleListeners (List<RuleListener> list)
    void removeRuleListener (RuleListener listener)

    void registerRuleEngineListener (RuleEngineListener listener)
    void registerRuleEngineListeners (List<RuleEngineListener> list)
    void removeRuleEngineListener (RuleEngineListener listener)

    void leftShift (Map mapOfAttributes)
    void addAttribute (String key, value)
    void removeAttribute (String key)
}