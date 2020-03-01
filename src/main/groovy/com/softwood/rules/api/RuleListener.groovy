package com.softwood.rules.api

/**
 * A listener for rules execution events.
 *
 * @author Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)
 */
public interface RuleListener {

    /**
     * Triggered before the evaluation of a rule.
     *
     * @param rule being evaluated
     * @param facts known before evaluating the rule
     * @return true if the rule should be evaluated, false otherwise
     */
    boolean beforeEvaluate(Rule rule, Facts facts)

    /**
     * Triggered after the evaluation of a rule.
     *
     * @param rule that has been evaluated
     * @param facts known after evaluating the rule
     * @param evaluationResult true if the rule evaluated to true, false otherwise
     */
    void afterEvaluate(Rule rule, Facts facts, boolean evaluationResult)

    /**
     * Triggered before the execution of a rule.
     *
     * @param rule the current rule
     * @param facts known facts before executing the rule
     */
    void beforeExecute(Rule rule, Facts facts)

    /**
     * Triggered after a rule has been executed successfully.
     *
     * @param rule the current rule
     * @param facts known facts after executing the rule
     */
    void onSuccess(Rule rule, Facts facts)

    /**
     * Triggered after a rule has failed.
     *
     * @param rule      the current rule
     * @param facts known facts after executing the rule
     * @param exception the exception thrown when attempting to execute the rule
     */
    void onFailure(Rule rule, Facts facts, Exception exception)

}
