package scripts

import com.softwood.utils.JsonEncodingStyle
import com.softwood.utils.JsonUtils
import io.vertx.core.json.JsonObject

import java.time.LocalDateTime

JsonUtils.Options options = new JsonUtils.Options()
options.setExpandLevels (2)
options.registerTypeEncodingConverter(LocalDateTime) {it.toString()}
options.excludeFieldByNames("ci")
options.excludeNulls(true)
options.summaryClassFormEnabled(false)
options.includeVersion(true)

jsonGenerator = options.build()

//get rootloader  for generator and create a gc loader from that
def gloader = jsonGenerator.class.getClassLoader()
GroovyClassLoader groovyCL = new GroovyClassLoader(gloader)

ClassLoader root = jsonGenerator.class.getClassLoader()
println "script classloader is " + this.class.getClassLoader() + " whilst jsonGenerator loader is " + jsonGenerator.class.getClassLoader()


    class BasicParent {
        String name
        List<Child> children = new LinkedList<Child>()

        String toString() {
            "BasicParent (name:$name) "
        }
    }


    class BasicChild {
        String name
        BasicParent parent

        //List<Subchild> children = new LinkedList<Subchild>()

        String toString() {
            "BasicChild (name:$name) "
        }
    }




Class grBasicParentClass = groovyCL.loadClass(BasicParent.class.canonicalName)
Class grBasicChildClass = groovyCL.loadClass(BasicChild.class.canonicalName)
def inst = grBasicParentClass.newInstance()

println "inst classloader is " + inst.class.getClassLoader()


//one parent two children who have same parent - 2 level deep
def p =grBasicParentClass.newInstance()  //= new BasicParent(name: "Dad", children: [])
p.name = "Dad"
p.children = []

println "BasicParent p classloader is " + p.class.getClassLoader() + " whilst jsonGenerator loader is " + jsonGenerator.class.getClassLoader()


def c1 = grBasicChildClass.newInstance()//new BasicChild(name: "Jack", parent: p)
def c2 = grBasicChildClass.newInstance() //new BasicChild(name: "Jill", parent: p)
c1.name = "Jack"
c2.name = "Jill"
c1.parent = p
c2.parent = p

p.children << c1
p.children << c2


    JsonObject p2ljson = jsonGenerator.toSoftwoodJson(p)

    println "encoded parent root of graph as : " + p2ljson.encodePrettily()

//JsonObject l2pjson = jsonGenerator.toSoftwoodJson ( c1)

//println "\nencoded first child as root of graph as : " + l2pjson.encodePrettily()

    BasicParent decodedObject = jsonGenerator.toObject(BasicParent, p2ljson, JsonEncodingStyle.softwood)
