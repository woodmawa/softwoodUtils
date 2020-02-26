package com.softwood.rules.scripts

import com.softwood.rules.api.Condition
import com.softwood.rules.core.BasicCondition

def param = "william"

Condition c = new BasicCondition(name: "c1#", description : "my first condition ", dynamicTest: {it == "william"})

def res = c.test(param)

assert res

