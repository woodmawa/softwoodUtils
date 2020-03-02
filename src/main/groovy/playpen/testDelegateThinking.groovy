package playpen

class Bed {
    static String name
    String bedSize = "5ft by 6ft"

    static sTrySleep (arg=null) {
        println "called static trySleep() for name ($name) with arg " + arg ?: "<no arg>"
    }

    void trySleep (arg = null) {
        println "called instance.onSleep() for name (${->name}) with " + arg ?: "<no arg>"
    }

    void sayName () {
        println "beds name is $name"
    }

}

Bed mybed = new Bed(name:"wills bed")
mybed.sayName()

mybed.name = "change to: tobys's bed"
mybed.sayName()


class DataObject {
    String name = "first data object"
    int height = 10
    int length = 12
    int width = 5

    def showData () {
        String s = "name : $name, height:$height, length:$length"
        println "name : $name, height:$height, length:$length"
        s
    }
}

DataObject myData = new DataObject()
Map verbNounLookup = new HashMap()

Map buildAction (Map vnl, String verb, String noun, data, Closure method) {
    method.delegate = data
    method.resolveStrategy = Closure.DELEGATE_ONLY
    def lookup = [(verb): [(noun): method]]
    vnl.putAll(lookup)
    lookup
}

buildAction (verbNounLookup, "go", "snooze", myData , mybed::trySleep)
buildAction (verbNounLookup, "go2", "snooze", myData, Bed::sTrySleep)
buildAction (verbNounLookup, "do", "thing", myData, {println "called with $it, and sees name as: $name, " + " and show data as (${showData()})"})

def action = verbNounLookup.go.'snooze'

action ("hi")

 action = verbNounLookup.go2.'snooze'
//"static method called with "
action( "hi toby ")


action = verbNounLookup.do.thing
action( "william")