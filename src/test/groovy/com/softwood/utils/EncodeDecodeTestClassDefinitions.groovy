package com.softwood.utils

class EncodeDecodeTestClassDefinitions {
}

class Simple {
    String name
    int age
}

class Parent {
    UUID id
    String name
    List<Child> children = new LinkedList<Child>()

    String toString () {
        "Parent (name:$name) "
    }
}

class Child {
    Long id
    String name
    Parent parent

    List<SubChild> children = new LinkedList<SubChild>()

    String toString() {
        "Child (name:$name) "
    }
}

class SubChild {
    String name
    Parent parent

    String toString() {
        "SubChild (name:$name) "
    }
}

class Demo {
    transient transientInt
    int realIntField
    public int publicIntField
    private int privateIntField
    int getImaginaryFieldAccessor () { 1}


}
