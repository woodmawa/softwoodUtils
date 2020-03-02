package com.softwood.rules.api

import java.beans.PropertyChangeListener

interface RuleEngine {

    Collection<Boolean> check (Facts facts, Rules rules)
    def run (Facts facts, Rules rules)

}