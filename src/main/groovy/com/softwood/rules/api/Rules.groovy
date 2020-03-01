package com.softwood.rules.api

import groovy.util.Proxy

/**
 * a collection of Rules for the rules engine
 */
class Rules implements Iterable<Rule> {

    private Set<Rule> rules = new TreeSet<>()

    /**
     * Create a new {@link Rules} object.
     *
     * @param rules to register
     */
    Rules(Set<Rule> rules) {
        this.rules = new TreeSet<>(rules)
    }

    /**
     * Create a new {@link Rules} object.
     *
     * @param rules to register
     */
    Rules(Rule... rules ) {
        Collections.addAll(this.rules, rules)
    }


    /**
     * Register a new rule.
     *
     * @param rule to register
     */
    void register(Rule rule) {
        Objects.requireNonNull(rule)
        rules.add(rule)
    }

    /**
     * Register a new rule.
     *
     * @param rule to register
     */
    public void register(Object ruleObj) {
        assert ruleObj, "object $ruleObj cannot be null"  //groovy way
        //todo proxies
        rules.add (new Proxy().wrap (ruleObj) as Rule)
        //rules.add(RuleProxy.asRule(rule));
    }

    /**
     * Unregister a rule.
     *
     * @param rule to unregister
     */
    public void unregister(Rule rule) {
        Objects.requireNonNull(rule)        //java utils way
        rules.remove(rule)
    }
    /**
     * Unregister a rule.
     *
     * @param rule to unregister
     */
    public void unregister(Object ruleObj) {
        Objects.requireNonNull(ruleObj);
        //- uses groovy proxy todo proxies
        rules.remove (new Proxy().wrap (ruleObj) as Rule)
    }

    /**
     * Unregister a rule by name.
     *
     * @param ruleName the name of the rule to unregister
     */
    public void unregister(final String ruleName){
        Objects.requireNonNull(ruleName)
        Optional<Rule> rule = findRuleByName(ruleName)
        rule.ifPresent(r -> unregister(r))      //use lambda
        }


    /**
     * Check if the rule set is empty.
     *
     * @return true if the rule set is empty, false otherwise
     */
    public boolean isEmpty() {
        return rules.isEmpty()
    }

    /**
     * Clear rules.
     */
    public void clear() {
        rules.clear()
    }

    @Override
    public Iterator<Rule> iterator() {
        return rules.iterator()
    }

    //todo replace null return with Optional
    private Optional<Rule> findRuleByName(String ruleName){
        for(Rule rule : rules){
            if(rule.getName().equalsIgnoreCase(ruleName))
                return Optional.of(rule)
        }
        return Optional.ofNullable(null)
    }

}
