package scripts

import org.codehaus.groovy.runtime.callsite.GroovySunClassLoader

class Test {
    String name
}

def t = new Test()

/*import java.lang.reflect.Field

Field f = ClassLoader.class.getDeclaredField("classes");
f.setAccessible(true);

ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
Vector<Class> classes =  (Vector<Class>) f.get(classLoader);*/

GroovyClassLoader cl = new GroovyClassLoader(Thread.currentThread().getContextClassLoader())
List<Class> classList = cl.getLoadedClasses()

println "classes loaded : " + classList.size()
classList.each {
    println "loaded : " + it.canonicalName
}


ClassLoader classLoader = Thread.currentThread().getContextClassLoader()

def clazz = classLoader.findSystemClass("scripts.Test")

println clazz.canonicalName