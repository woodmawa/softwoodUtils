package com.softwood.rules.core

import com.softwood.rules.api.RuleListener
import groovy.beans.Bindable
import com.softwood.rules.api.RulesEngineListener

/**
 * enable class to add rule listeners
 *
 *
 */

class AbstractRuleEngine {
    @Bindable List<RulesEngineListener> ruleEngineListeners = []
    @Bindable List<RuleListener> ruleListeners = []
}
