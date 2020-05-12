package scripts

import java.lang.reflect.Modifier

class MyClass {
    String name

    def method () {
        "method"
    }

    //defined within my class
    Closure clos = {
        "closure"
    }

    def updateProps (Map m) {

        //properties is read-only
        properties.each { k, v ->
            //field may not be accessible
            try {
                if (m.containsKey(k)) {
                    def val = m.(k)
                    setProperty(k, val)
                }
            } catch (ReadOnlyPropertyException roe){
                println "read only exception <$roe> updating field : [$k] "
            } catch (Exception e) {
                println "exception <$e> updating field : [$k] "
                throw e
            }
        }
    }
}

MyClass mc = new MyClass()

Map props = mc.properties

def metaMethods = mc.metaClass.getMetaMethods().name
def methods = mc.metaClass.getMethods().name
MetaMethod dmp = mc.metaClass.getMetaMethod('dump')

println "dump via MetaMethod invoke " + dmp.invoke(mc)

List mprops = mc.metaClass.properties

def propNames = props.collect {k, v -> k}
println "prop names " + propNames

def mpropNames = mprops.collect {it -> it.name}
println "meta prop names " + mpropNames

assert mc.clos.getThisObject() == mc
assert mc.clos.getOwner() == mc

println "run mc.clos()  " + mc.clos.getThisObject().method()
println "mc closure class  " + mc.clos.getThisObject().getClass()

mc.updateProps([name: "william"])
assert mc.name == 'william'
