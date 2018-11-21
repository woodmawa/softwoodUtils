package scripts

import com.softwood.utils.JsonUtils

import java.time.LocalDateTime

ClassLoader threadContextLoader = Thread.currentThread().getContextClassLoader()


class ScriptTest {
    String name
}

ScriptTest st = new ScriptTest()
ClassLoader stLoader = st.getClass().getClassLoader()

ClassLoader scriptLoader = this.getClass().getClassLoader()


JsonUtils.Options options = new JsonUtils.Options()
options.setExpandLevels (2)
options.registerTypeEncodingConverter(LocalDateTime) {it.toString()}
options.excludeFieldByNames("ci")
options.excludeNulls(true)
options.summaryClassFormEnabled(false)
options.includeVersion(true)

jsonGenerator = options.build()

println "thread loader hierarchy"
ClassLoader loader = threadContextLoader
while (loader) {
    println "\t$loader"
    loader = loader.getParent()
}

println "\nscript loader hierarchy"
 loader = scriptLoader
while (loader) {
    println "\t$loader"
    loader = loader.getParent()
}

ClassLoader genLoader = jsonGenerator.getClass().getClassLoader()

println "\njsonGenerator loader hierarchy"
loader = genLoader
while (loader) {
    println "\t$loader"
    loader = loader.getParent()
}

println "\nnew ScriptTest in script stLoader hierarchy"
loader = stLoader
while (loader) {
    println "\t$loader"
    loader = loader.getParent()
}