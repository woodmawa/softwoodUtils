package com.softwood.rules.api

import java.util.function.Predicate

interface Condition extends Predicate {
    Condition or (Condition c)
    Condition and (Condition c)
    Condition negate ()
}