package com.softwood.rules.api

/**
 * A listener for rules engine execution events.
 *
 * @author Will Woodman
 */
public interface RuleEngineListener {

    /**
     * Triggered before evaluating the rule set.
     * <strong>When this listener is used with a {@link InferenceRulesEngine}, this method will be triggered once before the evaluation of each candidate rule set in each iteration.</strong>
     *
     * @param rules to fire
     * @param facts present before firing rules
     */
    void beforeEvaluate(RuleSet rules, Facts facts)

    /**
     * Triggered after executing the rule set
     * <strong>When this listener is used with a {@link InferenceRulesEngine}, this method will be triggered once after the execution of each candidate rule set in each iteration.</strong>
     *
     * @param rules fired
     * @param facts present after firing rules
     */
    void afterExecute(RuleSet rules, Facts facts, resultList)


    //if only processing a single Rule in the rule engine these will be triggered if defined
    /**
     * Triggered before evaluating the rule set.
     * <strong>When this listener is used with a {@link InferenceRulesEngine}, this method will be triggered once before the evaluation of the specified  rule set in each iteration.</strong>
     *
     * @param rules to fire
     * @param facts present before firing rules
     */
    void beforeRuleEvaluate(Rule rules, Facts facts)

    /**
     * Triggered after executing the rule set
     * <strong>When this listener is used with a {@link InferenceRulesEngine}, this method will be triggered once after the execution of each candidate rule set in each iteration.</strong>
     *
     * @param rules fired
     * @param facts present after firing rules
     */
    void afterRuleExecute(Rule rule, Facts facts, result)
}