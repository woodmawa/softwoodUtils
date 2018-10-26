package scripts

import com.softwood.utils.JsonUtils
import groovy.json.JsonOutput

import java.time.LocalDateTime

JsonUtils.Options options = new JsonUtils.Options()
options.registerConverter(LocalDateTime) {it.toString()}
options.excludeFieldByNames("ci")
options.excludeNulls(true)
options.setExpandLevels(1)
options.summaryClassFormEnabled(false)

jsonGenerator = options.build()


//println "encode int 2 : " + jsonGenerator.toTmfJson(2)
//println "encode list int [1,2] : " + jsonGenerator.toTmfJson([1,2])
//println "encode map [a:2, b:3] : " + jsonGenerator.toTmfJson([a:2, b:3])

class SimpleTmf {
    String id = "1"
    String name = "simpleInst"
    Point locationRef
    List sList = []
    Map sMap =[:]
}

class Point {
    int x, y,z
}

SimpleTmf s1 = new SimpleTmf()
s1.locationRef = new Point (x:1, y:1, z:1)
println "encode simple s1 : " + jsonGenerator.toTmfJson(s1).encodePrettily()
