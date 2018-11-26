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
    List simpleList
    List complexList
    Map simpleMap
    Map complexMap
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


SimpleChild sc = new SimpleChild(id:10, name:"child")
//SimpleOriginal so = new SimpleOriginal(id:1, name:"ok", simpleList:[1,2], complexList:[true, sc], simpleMap: [a:1, b:true], complexMap:[a:sc])
SimpleOriginal so = new SimpleOriginal(id:1, name:"ok", simpleList:[1,2])
so.child = sc

//println jsonGenerator.toJsonApi(so).encodePrettily()

//System.exit(0)

jsonSoftwoodText = """{
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

jsonTmfText = """{
  "@type" : "scripts.SimpleOriginal",
  "id" : 1,
  "name" : "ok",
  "child" : {
    "@type" : "scripts.SimpleChild",
    "id" : 10,
    "name" : "child"
  }
}
"""

jsonApiText = """{
  "data" : {
    "type" : "scripts.SimpleOriginal",
    "id" : 1,
    "attributes" : {
      "id" : 1,
      "name" : "ok"
    },
    "relationships" : {
      "child" : {
        "data" : {
          "type" : "scripts.SimpleChild",
          "id" : 1
        }
      },
      "simpleList" : {
        "data" : [ 1, 2 ]
      }
    }
  }
}"""


//um wont accept a null class -
def result
/*result = jsonGenerator.toObject (SimpleOriginal, jsonSoftwoodText, JsonEncodingStyle.softwood)
println "decoded from softwood formatted json : " + result
assert result.child.name == "child"

result = jsonGenerator.toObject (SimpleOriginal, jsonTmfText, JsonEncodingStyle.tmf)
println "decoded from tmf formatted json : " + result
assert result.child.name == "child"*/

result = jsonGenerator.toObject (SimpleOriginal, jsonApiText, JsonEncodingStyle.jsonApi)
println "decoded from jsonApi formatted  json : " + result
assert result.child.name == "child"