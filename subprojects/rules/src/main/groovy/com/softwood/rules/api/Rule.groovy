package com.softwood.rules.api

interface Rule extends Comparable<Rule> {
    /**
     * Default rule name.
     */
    String DEFAULT_NAME = "rule"

    /**
     * Default rule description.
     */
    String DEFAULT_DESCRIPTION = "description"

    /**
     * Default rule priority.
     */
    int DEFAULT_PRIORITY = Integer.MAX_VALUE - 1

    /**
     * Getter for rule name.
     * @return the rule name
     */
    String getName()

    /**
     * Getter for rule description.
     * @return rule description
     */
    String getDescription()

    /**
     * Getter for rule priority.
     * @return rule priority
     */
    int getPriority()

    /**
     * methods for adding and removing preCondition guards before actioning a rule
     * @return
     */

    void addAllPreConditions(Collection<Condition> conditions)

    void AddPreCondition(Condition condition)

    void removePreCondition(Condition condition)

    void clearAllPreConditions()

    /*
     * get the collection of preConditions
     */

    Collection<Condition> getPreConditions()

    /*
    * get the collection of effects as a list of closures
    */

    List<Closure> getPostActionEffectsList()

    /*
     * returns reference to internal action effects queue
     */

    Collection<Closure> getPostActionEffects()

    /*
    * returns reference to internal action effects queue
    */

    void clearPostActionEffects()

    /**
     * Rule conditions abstraction : this method encapsulates the rule's conditions.
     * <strong>Implementations should handle any runtime exception and return true/false accordingly</strong>
     *
     * @return true if the rule should be applied given the provided facts, false otherwise
     */
    boolean evaluate(Facts facts)

    boolean evaluate(Facts facts, arg)

    /**
     * Rule actions abstraction : this method encapsulates the rule's actions.
     * @throws Exception thrown if an exception occurs during actions performing
     */
    def execute(Facts facts) throws Exception

    def execute(Facts facts, arg) throws Exception

    /**
     * Rule actions abstraction : this method just directly calls the action - no conditions check, no effects invoked
     * @throws Exception thrown if an exception occurs during actions performing
     */
    def justExecute() throws Exception

    def justExecute(arg) throws Exception

}