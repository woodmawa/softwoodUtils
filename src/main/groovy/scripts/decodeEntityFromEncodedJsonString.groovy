package scripts

import com.softwood.utils.JsonEncodingStyle
import com.softwood.utils.JsonUtils

import java.time.LocalDateTime


JsonUtils.Options options = new JsonUtils.Options()
options.registerTypeEncodingConverter(LocalDateTime) {it.toString()}
options.excludeFieldByNames("ci")
options.excludeNulls(true)
options.setExpandLevels(1)
options.summaryClassFormEnabled(false)

jsonGenerator = options.build()

class SimpleOriginal {
    long id
    String name
    SimpleChild child

    String toString() {
        "SimpleOriginal (name:$name)"
    }
}

class SimpleChild {
    long id
    String name

    String toString() {
        "SimpleChild (name:$name)"
    }
}

SimpleOriginal so = new SimpleOriginal(id:1, name:"ok")
SimpleChild sc = new SimpleChild(id:10, name:"child")
so.child = sc

//println jsonGenerator.toSoftwoodJson(so).encodePrettily()

jsonText = """{
  "entityData" : {
    "entityType" : "scripts.SimpleOriginal",
    "id" : 1,
    "name" : "ok",
    "attributes" : {
      "id" : {
        "type" : "Long",
        "value" : 1
      },
      "name" : {
        "type" : "String",
        "value" : "ok"
      },
      "child" : {
        "type" : "scripts.SimpleChild",
        "value" : {
          "entityData" : {
            "entityType" : "scripts.SimpleChild",
            "id" : 10,
            "name" : "child",
            "attributes" : {
              "id" : {
                "type" : "Long",
                "value" : 10
              },
              "name" : {
                "type" : "String",
                "value" : "child"
              }
            }
          }
        }
      }
    }
  }
}
"""


//um wont accept a null class -
def result = jsonGenerator.toObject (SimpleOriginal, jsonText, JsonEncodingStyle.softwood)
println result