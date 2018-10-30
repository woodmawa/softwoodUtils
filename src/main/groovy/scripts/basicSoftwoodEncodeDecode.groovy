package scripts

import com.softwood.utils.JsonEncodingStyle
import com.softwood.utils.JsonUtils
import io.vertx.core.json.JsonObject

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

class TestClass {
    int id
    String toString() {
        "TestClass (id:$id)"
    }
}

TestClass tc = new TestClass(id:1)

JsonObject enc = jsonGenerator.toSoftwoodJson(tc)
println enc.encodePrettily()

TestClass dec = jsonGenerator.toObject(TestClass, enc)
println "decoded json as object : " + dec.dump()
