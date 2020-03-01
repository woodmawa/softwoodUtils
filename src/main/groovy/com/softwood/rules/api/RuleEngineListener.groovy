package com.softwood.rules.api

/**
 * A listener for rules engine execution events.
 *
 * @author Will Woodman
 */
public interface RuleEngineListener {

    /**
     * Triggered before evaluating the rule set.
     * <strong>When this listener is used with a {@link InferenceRulesEngine}, this method will be triggered before the evaluation of each candidate rule set in each iteration.</strong>
     *
     * @param rules to fire
     * @param facts present before firing rules
     */
    void beforeEvaluate(Rules rules, Facts facts)

    /**
     * Triggered after executing the rule set
     * <strong>When this listener is used with a {@link InferenceRulesEngine}, this method will be triggered after the execution of each candidate rule set in each iteration.</strong>
     *
     * @param rules fired
     * @param facts present after firing rules
     */
    void afterExecute(Rules rules, Facts facts)
}