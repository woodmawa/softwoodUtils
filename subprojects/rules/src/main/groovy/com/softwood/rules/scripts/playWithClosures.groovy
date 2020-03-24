package com.softwood.rules.scripts

import groovy.transform.InheritConstructors
import org.codehaus.groovy.reflection.CachedClass
import org.codehaus.groovy.reflection.CachedMethod
import org.codehaus.groovy.runtime.InvokerHelper
import org.codehaus.groovy.runtime.MethodClosure

Closure scriptClos = {println "within closure scriptClos this is $this, hi "}

class A {
    String name = "my A instance"
    Closure aClos = {println " toby "}
    def greeting() {
        println "hi from my A"
        "done"
    }
}
aInst = new A()


class ConcreteClosure extends Closure {


    MethodClosure mc
    Closure cc

    ConcreteClosure(Object owner, Object thisObject, ctx) {
        super(owner, thisObject)
        mc = (Closure) owner::doCall
        cc = owner
        if (ctx){
            mc.delegate = ctx
            mc.resolveStrategy= Closure.DELEGATE_FIRST
            cc.delegate = ctx
            cc.resolveStrategy= Closure.DELEGATE_FIRST

        }
        assert mc
    }

    ConcreteClosure(Object owner, Object thisObject) {
        super(owner, thisObject)
        mc = (Closure) owner::doCall
        cc = owner

        assert mc
    }

    ConcreteClosure(Object owner) {
        super(owner)
        mc = (Closure) owner::doCall
        cc = owner

        assert mc

    }

    MethodClosure getMc() {
        mc
    }

    MethodClosure getCc() {
        cc
    }


    def doCall() {
        //println "call() called, invoke metaClass docall() " + InvokerHelper.invokeMethod(this, "doCall", null)
        cc()

    }
}

def constructorClos = {"return with, my concrete Closure, with this = $this, and owner = $owner, and name = ${name}"}
constructorClos.delegate = aInst
constructorClos.resolveStrategy= Closure.DELEGATE_FIRST
//this will resolve a - if we set first, however if we set delegate inside Concrete closure it fails to resolve name!
// is this because you have to set the super() first, and then updating after makes no difference?

ConcreteClosure cc = new ConcreteClosure (constructorClos, constructorClos)

println cc.call()

MethodClosure aMc = aInst::greeting
aMc()

//scriptClos owned by instance of script, and aClos owner is instance a
println "scriptclos owner : " + scriptClos.owner.toString() + " and aClos owner : " + aInst.aClos.owner.toString() + " a: ($aInst)"
//scriptClos thisObject is instance of script, and aClos thisObject is instance a
println "scriptclos thisObject : " + scriptClos.thisObject + " and aClos thisObject : " + aInst.aClos.thisObject.toString()


def methods  = scriptClos.metaClass.getMethods().findAll {it.getName() =~ /doCall/}

//methods for closure Does have a doCall() and doCall(Object)
println "doCall method from methods "
methods.each {println "$it"}

def metaMethods = scriptClos.metaClass.getMetaMethods().findAll { it.getName()  =~ /doCall/}

//metaMethods for closure Doesnt have a doCall()
println "doCall method from metaMethods "
metaMethods.each {println "$it"}

MetaMethod code = scriptClos.metaClass.pickMethod("doCall", Object)
//clos.metaClass.getMetaMethod("doCall", [])

CachedClass clazz = code.getDeclaringClass()
CachedMethod[] listCachedMethods = clazz.getMethods()
CachedMethod cm = listCachedMethods[5]

def out = System.out::println

MethodClosure mr = scriptClos::call

MetaMethod codeClone = code.clone()

assert code

//chaneg to new closure
scriptClos = {println "William "}
//codeClone.doMethodInvoke(clos, [])
mr()
scriptClos()

