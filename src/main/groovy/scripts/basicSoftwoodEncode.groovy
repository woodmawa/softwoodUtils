package scripts

import com.softwood.utils.JsonEncodingStyle
import com.softwood.utils.JsonUtils

import java.time.LocalDateTime

JsonUtils.Options options = new JsonUtils.Options()
options.registerConverter(LocalDateTime) {it.toString()}
options.excludeFieldByNames("ci")
options.excludeNulls(true)
options.setExpandLevels(1)
options.setJsonEncodingStyle(JsonEncodingStyle.softwood)
options.setIncludeVersion(true)
options.summaryClassFormEnabled(false)


jsonGenerator = options.build()


println "encode int 2 : " + jsonGenerator.toSoftwoodJson(2)
println "encode list int [2] : " + jsonGenerator.toSoftwoodJson([2])
println "encode map [a:2] : " + jsonGenerator.toSoftwoodJson([a:2])

class SimpleSoftwood {
    String name
    List sList = []
    Map sMap =[:]
}

class SoftwoodChild {
    String name
    SimpleSoftwood parent
}


SimpleSoftwood s1 = new SimpleSoftwood()
SoftwoodChild sc1 = new SoftwoodChild(name: "myChild", parent:s1)

s1.sMap << [a:s1, b:sc1]
println "encode simple s1 : " + jsonGenerator.toSoftwoodJson(s1).encodePrettily()