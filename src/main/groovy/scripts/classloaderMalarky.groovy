package scripts

import com.softwood.utils.JsonUtils

import java.time.LocalDateTime

ClassLoader threadContextLoader = Thread.currentThread().getContextClassLoader()


class ScriptNewClassTest {
    String name
}

ScriptNewClassTest st = new ScriptNewClassTest()
ClassLoader stLoader = st.getClass().getClassLoader()
ClassLoader rootLoader = stLoader.getRootLoader()

//Class clazz = scripts.classloaderMalarky
//ClassLoader scriptLoader = scripts.classloaderMalarky.getClass().getClassLoader()


JsonUtils.Options options = new JsonUtils.Options()
options.setExpandLevels (2)
options.registerTypeEncodingConverter(LocalDateTime) {it.toString()}
options.excludeFieldByNames("ci")
options.excludeNulls(true)
options.summaryClassFormEnabled(false)
options.includeVersion(true)

jsonGenerator = options.build()

println "root loader is  $rootLoader "

println "\nthread loader hierarchy"
ClassLoader loader = threadContextLoader
while (loader) {
    println "\t$loader"
    loader = loader.getParent()
}


/*println "\nscript loader hierarchy"
 loader = scriptLoader
while (loader) {
    println "\t$loader"
    loader = loader.getParent()
}*/

ClassLoader genLoader = jsonGenerator.getClass().getClassLoader()

println "\njsonGenerator loader hierarchy"
loader = genLoader
while (loader) {
    println "\t$loader"
    loader = loader.getParent()
}

ClassLoader jgo = jsonGenerator.options.defaultClassLoader
println "\njsonGenerator loader defaultClassLoder hierarchy"
loader = jgo
while (loader) {
    println "\t$loader"
    loader = loader.getParent()
}


println "\nnew ScriptNewClassTest in script stLoader hierarchy"
loader = stLoader
while (loader) {
    println "\t$loader"
    loader = loader.getParent()
}

def newInstance = jsonGenerator.getNewInstanceFromClass(ScriptNewClassTest)
println "\nnew ScriptNewClassTest created by jsonGenerator  class loader  hierarchy"
loader = newInstance.class.getClassLoader()
while (loader) {
    println "\t$loader"
    loader = loader.getParent()
}