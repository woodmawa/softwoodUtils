package com.softwood.rules.scripts

import com.softwood.rules.api.Condition
import com.softwood.rules.core.BasicCondition

import java.util.function.Predicate

def param = "william"

Predicate c = new BasicCondition(name: "c1#", description : "my first condition ", dynamicTest: {it == "william"})

def res = c.test(param)

assert res

