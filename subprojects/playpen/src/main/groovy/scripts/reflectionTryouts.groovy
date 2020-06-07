package scripts

import org.apache.commons.lang3.reflect.ConstructorUtils
import playpen.GroovyClassUtils

import java.lang.reflect.Modifier

class Empty {}

def stdMeth = Empty.metaClass.methods.name
def stdProp = Empty.metaClass.properties.name


class A {
    static String ref = "ref "
    String name
    int count

    String greet() {
        println "hello william"
    }
}

class B {
    int num = 10
    String name = ""

    B(int someNumber, String name) {
        if (someNumber)
            num = someNumber
        this.name = name
    }
}


def methList = A.metaClass.methods.name
def aPropList = A.metaClass.properties.name

def metaMethList = A.metaClass.metaMethods.name

List staticPropNames = GroovyClassUtils.getStaticProperties(A)


def isWrapperOf = GroovyClassUtils.isPrimitiveWrapperOf(Integer, int.class)
def canbeAssigned = GroovyClassUtils.isAssignableTo(Integer, int.class)
//just use apache library for this
def newB // = ConstructorUtils.invokeConstructor(B, 20)

//now working
newB = GroovyClassUtils.instanceOf(B, 30, "will")
assert newB

newB = GroovyClassUtils.instanceOf(B, 20 as int)

//static property returns a  ExpandoMetaClass$ExpandoMetaProperty
//cant get this to work
ExpandoMetaClass.ExpandoMetaProperty statPropRef = A.metaClass.'static'
println statPropRef.getProperty('ref').dump()
def aCons = A.metaClass.'constructors'


println "static prop ref : " + statPropRef
println statPropRef.getProperty("ref")

//this sort of works but doesnt tell you if the property is static, need to mask the modifiers
List<MetaProperty> props = A.metaClass.getProperties()
int modifiers = props[1].modifiers
def stat = Modifier.isStatic(modifiers)
println "prop name is " + props[1].name + " and is static : $stat"

println "A #meta methods : ${metaMethList.size()} and A #methods : ${methList.size()}"
println "and A proprties : $aPropList"


def diff = metaMethList - methList
println "std methods from metaClass are : $methList"
println "class A specific methods  : " + (methList - stdMeth)

println "diff size : ${diff.size()} with entries : $diff"

MetaMethod m = A.metaClass.pickMethod('greet', null)

m.doMethodInvoke(A.newInstance(), MetaClassImpl.EMPTY_ARGUMENTS)
println ""

def greetRef = A.newInstance()::greet
print "just invoke method ref > "
greetRef()