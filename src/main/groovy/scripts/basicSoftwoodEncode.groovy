package scripts

import com.softwood.utils.JsonEncodingStyle
import com.softwood.utils.JsonUtils
import com.softwood.utils.UuidUtil

import java.time.LocalDateTime

JsonUtils.Options options = new JsonUtils.Options()
options.registerTypeEncodingConverter(LocalDateTime) {it.toString()}
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
    UUID id
    String name
    List sList = []
    Map sMap =[:]
}

class SoftwoodChild {
    long id
    String name
    SimpleSoftwood parent
}


SimpleSoftwood s1 = new SimpleSoftwood(id: UuidUtil.getTimeBasedUuid(), name:"root")
SoftwoodChild sc1 = new SoftwoodChild(id:10, name: "myChild", parent:s1)

s1.sMap << [a:s1, b:sc1]
println "encode simple s1 : " + jsonGenerator.toSoftwoodJson(s1).encodePrettily()