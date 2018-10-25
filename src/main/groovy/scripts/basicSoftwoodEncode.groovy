package scripts

import com.softwood.utils.JsonEncodingStyle
import com.softwood.utils.JsonUtils
import groovy.json.JsonOutput

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


println "encode int 2 : " + jsonGenerator.toJson(2)
println "encode list int [2] : " + jsonGenerator.toJson([2])
println "encode map [a:2] : " + jsonGenerator.toJson([a:2])

class SimpleSoftwood {
    String name
    List sList = []
    Map sMap =[:]
}

SimpleSoftwood s1 = new SimpleSoftwood()
println "encode simple s1 : " + jsonGenerator.toJson(s1).encodePrettily()