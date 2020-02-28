package com.softwood.rules.api


/**
 * a collection of Rules for the rules engine
 */
class Rules implements Iterable<Rule> {

    private Set<Rule> rules = new TreeSet<>();

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
    public void register(Object rule) {
        Objects.requireNonNull(rule)
        //todo proxies
        //rules.add(RuleProxy.asRule(rule));
    }

    /**
     * Unregister a rule.
     *
     * @param rule to unregister
     */
    public void unregister(Rule rule) {
        Objects.requireNonNull(rule)
        rules.remove(rule)
    }
    /**
     * Unregister a rule.
     *
     * @param rule to unregister
     */
    public void unregister(Object rule) {
        Objects.requireNonNull(rule);
        //todo proxies
        //rules.remove(RuleProxy.asRule(rule));
    }

    /**
     * Unregister a rule by name.
     *
     * @param ruleName the name of the rule to unregister
     */
    public void unregister(final String ruleName){
        Objects.requireNonNull(ruleName)
        Rule rule = findRuleByName(ruleName)
        if(rule != null) {
            unregister(rule)
        }
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

    private Rule findRuleByName(String ruleName){
        for(Rule rule : rules){
            if(rule.getName().equalsIgnoreCase(ruleName))
                return rule
        }
        return null
    }

}
