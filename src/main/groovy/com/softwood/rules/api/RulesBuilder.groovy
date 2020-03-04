package com.softwood.rules.api

import com.softwood.rules.core.BasicCondition
import com.softwood.rules.core.BasicRule
import groovy.transform.builder.Builder
import groovy.transform.builder.ExternalStrategy

import com.softwood.rules.api.RuleFactory


class RulesBuilder extends FactoryBuilderSupport {
    {
        registerFactory('rule', new BuilderRuleFactory())
        registerFactory('condition', new BuilderConditionFactory())
        registerFactory ('action', new BuilderActionFactory())
    }
}

class BuilderRuleFactory extends AbstractFactory {
    def newInstance (FactoryBuilderSupport builder, name, value, Map attributes) {
        Rule rule = RuleFactory.newRule(name: value)
    }

    void setChild (FactoryBuilderSupport builder, Object parent, Object child) {
        parent.preConditions << child
    }

    boolean onHandleNodeAttributes (FactoryBuilderSupport builder, Object node, Map attributes) {
        if (attributes.x) {
            node.x = attributes.x
            attributes.remove ('x')
        }
    }
}

class BuilderConditionFactory extends AbstractFactory {

    boolean isLeaf () {true}

    def newInstance (FactoryBuilderSupport builder, name, value, Map attributes) {
        Condition condition  = new BasicCondition (name:value)
    }

}

class BuilderActionFactory extends AbstractFactory {

    boolean isLeaf () {true}

    def newInstance (FactoryBuilderSupport builder, name, value, Map attributes) {
        Action action  = RuleFactory.newAction(name: value)
    }

}