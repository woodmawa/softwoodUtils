package com.softwood.rules.api

import com.softwood.rules.core.BasicCondition
import com.softwood.rules.core.BasicRule
import groovy.transform.builder.Builder
import groovy.transform.builder.ExternalStrategy

import com.softwood.rules.api.RuleFactory


class RulesBuilder extends FactoryBuilderSupport {
    {
        registerFactory('ruleSet', new BuilderRuleSetFactory())
        registerFactory('rule', new BuilderRuleFactory())
        registerFactory('preCondition', new BuilderConditionFactory())
        registerFactory ('action', new BuilderActionFactory())
        registerFactory ('effect', new BuilderEffectFactory())
    }
}

class BuilderRuleSetFactory extends AbstractFactory {
    def newInstance (FactoryBuilderSupport builder, name, value, Map attributes) {
        RuleSet ruleSet = new RuleSet()
        ruleSet.name = value
        ruleSet
    }

    //add rule to treeSet using leftShift
    void setChild (FactoryBuilderSupport builder, Object parent, Object child) {
        parent << child
    }

    boolean onHandleNodeAttributes (FactoryBuilderSupport builder, Object node, Map attributes) {
        //RuleSet doesnt have a description yet - set this in case later
        if (attributes.description) {
            if (node.hasProperty('description'))
                node.description = attributes.description
            attributes.remove ('description')
        }
    }
}

class BuilderRuleFactory extends AbstractFactory {
    def newInstance (FactoryBuilderSupport builder, name, value, Map attributes) {
        Rule rule = RuleFactory.newRule(name: value)
   }

    //child will be BasicAction
    void setChild (FactoryBuilderSupport builder, Object parent, Object child) {
        if (child instanceof Action)
            parent.action = child
        else if (child instanceof Condition)
            parent.preConditions  << child
        else if (child instanceof Closure)
            parent.postActionEffects << child
    }

    boolean onHandleNodeAttributes (FactoryBuilderSupport builder, Object node, Map attributes) {
        if (attributes.name) {
            node.name = attributes.name
            attributes.remove ('name')
        }
        if (attributes.description) {
            node.description = attributes.description
            attributes.remove ('description')
        }
        if (attributes.priority) {
            node.priority = attributes.priority
            attributes.remove ('priority')
        }
    }
}

class BuilderConditionFactory extends AbstractFactory {

    boolean isLeaf () {true}

    def newInstance (FactoryBuilderSupport builder, name, value, Map attributes) {
        Condition condition  = new BasicCondition (name:value)
    }

    boolean onHandleNodeAttributes (FactoryBuilderSupport builder, Object node, Map attributes) {
        if (attributes.lowerLimit) {
            node.lowerLimit = attributes.lowerLimit
            attributes.remove ('lowerLimit')
        }
        if (attributes.upperLimit) {
            node.upperLimit = attributes.upperLimit
            attributes.remove ('upperLimit')
        }
        if (attributes.measure) {
            node.measure = attributes.measure
            attributes.remove ('measure')
        }
        if (attributes.description) {
            node.description = attributes.description
            attributes.remove ('description')
        }
        //closure test for condition to check
        if (attributes.test) {
            node.test = attributes.test
            attributes.remove ('test')
        }
    }
}

class BuilderActionFactory extends AbstractFactory {

    //boolean isLeaf () {true}

    def newInstance (FactoryBuilderSupport builder, name, value, Map attributes) {
        Action action  = RuleFactory.newAction(name: value)
    }

    boolean onHandleNodeAttributes (FactoryBuilderSupport builder, Object node, Map attributes) {
        if (attributes.name) {
            node.name = attributes.name
            attributes.remove ('name')
        }
        if (attributes.stateDate) {
            node.stateDate = attributes.stateDate
            attributes.remove ('stateDate')
        }
        //this allows for either version when declaring an action
        if (attributes.doAction) {
            node.doAction = attributes.doAction
            attributes.remove ('doAction')
        }
        if (attributes.action) {
            node.action = attributes.action
            attributes.remove ('action')
        }
    }

    //if child it must be the closure for the action
    void setChild (FactoryBuilderSupport builder, Object parent, Object child) {
        parent.action = child as Closure
    }

}

class BuilderEffectFactory extends AbstractFactory {

    boolean isLeaf () {true}

    def newInstance (FactoryBuilderSupport builder, name, value, Map attributes) {
        Condition condition  = new BasicCondition (name:value)
    }

}