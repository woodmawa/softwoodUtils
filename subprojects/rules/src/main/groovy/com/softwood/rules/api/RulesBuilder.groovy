package com.softwood.rules.api

import com.softwood.rules.core.BasicCondition

/**
 * uses abstract FactoryBuilderSupport
 *
 * use looks like this
 * a ruleSet consists of one or more com.softwood.rules
 * a rule has optional 0..many preConditions
 * a rule has 1 action
 * a rule has 0..many postAction effects that will be triggered if provided
 *
 *
 * def com.softwood.rules = builder.ruleSet ('myRules') {*      rule ('myRule', description:'first rule', priority:0) {*          preCondition ('isTired', description:'must meet',lowerLimit:-1, upperLimit:10 , test:{ fact-> fact.value < 60})
 *          action ('act#1', description:"do something", stateData:[:], doAction : {println "your tired"})
 *          effect (action: {})  //do nothing effect
 *}*      rule (...
 *}*
 */


class RulesBuilder extends FactoryBuilderSupport {
    {
        registerFactory('ruleSet', new BuilderRuleSetFactory())
        registerFactory('rule', new BuilderRuleFactory())
        registerFactory('preCondition', new BuilderConditionFactory())
        registerFactory('action', new BuilderActionFactory())
        registerFactory('effect', new BuilderEffectFactory())
    }
}

/**
 * factory for top level level ruleSet
 */
class BuilderRuleSetFactory extends AbstractFactory {
    def newInstance(FactoryBuilderSupport builder, name, value, Map attributes) {
        RuleSet ruleSet = new RuleSet()
        ruleSet.name = value
        ruleSet
    }

    //add rule to treeSet using leftShift
    void setChild(FactoryBuilderSupport builder, Object parent, Object child) {
        parent << child
    }

    boolean onHandleNodeAttributes(FactoryBuilderSupport builder, Object node, Map attributes) {
        //RuleSet doesnt have a description yet - set this in case later
        if (attributes.description) {
            if (node.hasProperty('description'))
                node.description = attributes.description
            attributes.remove('description')
        }
    }
}

/**
 * factory for a rule
 */
class BuilderRuleFactory extends AbstractFactory {
    def newInstance(FactoryBuilderSupport builder, name, value, Map attributes) {
        Rule rule = RuleFactory.newRule(name: value)
    }

    //child will be BasicAction
    void setChild(FactoryBuilderSupport builder, Object parent, Object child) {
        if (child instanceof Action)
            parent.action = child
        else if (child instanceof Condition)
            parent.preConditions << child
        else if (child instanceof Closure)
            parent.postActionEffects << child
    }

    boolean onHandleNodeAttributes(FactoryBuilderSupport builder, Object node, Map attributes) {
        if (attributes.name) {
            node.name = attributes.name
            attributes.remove('name')
        }
        if (attributes.description) {
            node.description = attributes.description
            attributes.remove('description')
        }
        if (attributes.priority) {
            node.priority = attributes.priority
            attributes.remove('priority')
        }
    }
}


/**
 * factory for a condition
 */
class BuilderConditionFactory extends AbstractFactory {

    def newInstance(FactoryBuilderSupport builder, name, value, Map attributes) {
        Condition condition = new BasicCondition(name: value)
    }

    boolean onHandleNodeAttributes(FactoryBuilderSupport builder, Object node, Map attributes) {
        if (attributes.lowerLimit) {
            node.lowerLimit = attributes.lowerLimit
            attributes.remove('lowerLimit')
        }
        if (attributes.upperLimit) {
            node.upperLimit = attributes.upperLimit
            attributes.remove('upperLimit')
        }
        if (attributes.measure) {
            node.measure = attributes.measure
            attributes.remove('measure')
        }
        if (attributes.description) {
            node.description = attributes.description
            attributes.remove('description')
        }
        //closure test for condition to check
        if (attributes.test) {
            node.conditionTest = attributes.test
            attributes.remove('test')
        }
    }

    //if child it must be the closure for the condition
    void setChild(FactoryBuilderSupport builder, Object parent, Object child) {
        parent.conditionAction = child as Closure
    }

}

/**
 * factory for an action
 *
 */
class BuilderActionFactory extends AbstractFactory {

    //boolean isLeaf () {true}

    def newInstance(FactoryBuilderSupport builder, name, value, Map attributes) {
        Action action = RuleFactory.newAction(name: value)
        action
    }

    boolean onHandleNodeAttributes(FactoryBuilderSupport builder, Object node, Map attributes) {
        if (attributes.name) {
            node.name = attributes.name
            attributes.remove('name')
        }
        if (attributes.stateDate) {
            node.stateDate = attributes.stateDate
            attributes.remove('stateDate')
        }
        //this allows for either version when declaring an action
        if (attributes.doAction) {
            node.action = attributes.doAction
            attributes.remove('doAction')
        }
        if (attributes.action) {
            node.action = attributes.action
            attributes.remove('action')
        }
    }

    //if child it must be the closure for the action
    void setChild(FactoryBuilderSupport builder, Object parent, Object child) {
        println "action: set child called with $child"
        parent.action = child as Closure
    }

}


/**
 * factory for an effect
 */
class BuilderEffectFactory extends AbstractFactory {

    class EffectContainer {
        Closure effectAction = {}
    }

    boolean isLeaf() { true }

    def newInstance(FactoryBuilderSupport builder, name, value, Map attributes) {
        new EffectContainer()
    }

    boolean onHandleNodeAttributes(FactoryBuilderSupport builder, Object node, Map attributes) {
        if (attributes.action) {
            node.effectAction = attributes.action.clone()
            attributes.remove('action')
        }
    }

    void setParent(FactoryBuilderSupport builder, Object parent, Object node) {
        parent.postActionEffects << node.effectAction
    }
}