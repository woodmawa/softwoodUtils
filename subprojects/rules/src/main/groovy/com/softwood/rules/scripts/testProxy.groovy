package com.softwood.rules.scripts

class MyClass {
    String sayHello() {
        println "hello Will"
    }
}

Object something = new MyClass()
Proxy p = new Proxy()
p.wrap(something)

p.sayHello()

//create a runtime proxy for the class
def proxy = ProxyGenerator.INSTANCE.instantiateAggregateFromBaseClass(MyClass)

proxy.sayHello()

