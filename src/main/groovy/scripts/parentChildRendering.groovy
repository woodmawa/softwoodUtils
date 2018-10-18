package scripts

import com.softwood.utils.JsonUtils

import java.time.LocalDateTime

class Parent {
    String name
    List<Child> children = new LinkedList<Child>()

    String toString () {
        "Parent (name:$name) "
    }
}

class Child {
    String name
    Parent parent

    List<Subchild> children = new LinkedList<Subchild>()

    String toString() {
        "Child (name:$name) "
    }
}

class Subchild {
    String name

    Child directParent

    String toString() {
        "Subchild (name:$name)"
    }
}

Parent parent = new Parent (name:"root")

Child child1 = new Child (name:"child#1",parent:parent)
Child child2 = new Child (name:"child#2",parent:parent)

parent.children << child1
parent.children << child2

Subchild sub1 = new Subchild (name:"subchild#1", directParent: child1)
Subchild sub2 = new Subchild (name:"subchild#2", directParent: child2)

child1.children << sub1
child1.children << sub2

JsonUtils.Options options = new JsonUtils.Options()
options.registerConverter(LocalDateTime) {it.toString()}
options.excludeFieldByNames("ci")
options.excludeNulls(true)
options.setExpandLevels(1)
options.summaryClassFormEnabled(false)

jsonGenerator = options.build()

println "from parent : " + jsonGenerator.toJson (parent).encodePrettily()
println "----"
println "from child1  : " + jsonGenerator.toJson (child1).encodePrettily()