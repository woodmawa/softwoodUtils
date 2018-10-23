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


println "encode int 2 : " + jsonGenerator.toTmfJson(2)
println "encode list int [2] : " + jsonGenerator.toTmfJson([2])
println "encode map [a:2] : " + jsonGenerator.toTmfJson([a:2])