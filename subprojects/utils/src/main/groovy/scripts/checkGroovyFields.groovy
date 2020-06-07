package scripts

class WillsClass {
    int id
    String name
}

class WillsSubClass extends WillsClass {
    String subClassName
}

WillsSubClass wsc = new WillsSubClass()

def allFields = []

def clazz = wsc.getClass()

while (clazz) {
    allFields.addAll(clazz.declaredFields*.name)
    clazz = clazz.superclass
}

println allFields

