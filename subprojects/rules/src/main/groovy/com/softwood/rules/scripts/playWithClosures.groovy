package com.softwood.rules.scripts

import org.codehaus.groovy.reflection.CachedClass
import org.codehaus.groovy.reflection.CachedMethod
import org.codehaus.groovy.runtime.MethodClosure

def clos = {println "hi "}

def methods  = clos.metaClass.getMethods()

methods.each {println "$it"}

def metaMethods = clos.metaClass.getMetaMethods()



MetaMethod code = clos.metaClass.pickMethod("doCall", Object)
//clos.metaClass.getMetaMethod("doCall", [])

CachedClass clazz = code.getDeclaringClass()
CachedMethod[] listCachedMethods = clazz.getMethods()
CachedMethod cm = listCachedMethods[5]

def out = System.out::println

MethodClosure mr = clos::call

MetaMethod codeClone = code.clone()

assert code

//chaneg to new closure
clos = {println "William "}
//codeClone.doMethodInvoke(clos, [])
mr()
clos()