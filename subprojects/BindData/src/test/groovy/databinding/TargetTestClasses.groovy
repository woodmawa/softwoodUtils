package databinding

import groovy.transform.ToString


class TargetBase {
    protected long id
}

@ToString
class TargetClass extends TargetBase {

    String someString       //simple property
    private int  someInt    //no setters or getters
    public Float someFloat
    TargetRefClass refClass       //model property
}

@ToString
class TargetRefClass extends TargetBase {

    String          refString
    private int     refInt
}