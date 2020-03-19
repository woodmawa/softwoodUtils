package com.softwood.rules.api

import java.util.function.Predicate

interface Condition extends Predicate {
    Condition or (Condition c)
    Condition and (Condition c)
    Condition negate ()

    String getName()
    void setName (String name)
    String getDescription()
    void setDescription(String description)
}