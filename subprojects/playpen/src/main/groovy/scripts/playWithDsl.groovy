package scripts


enum Direction {
    left,
    right,
    forward,
    backward
}

//use spread map operator *:
this.binding.properties << Direction.values().collectEntries { [(it.name()): it] }

println this.getBinding().properties

def left = Direction.left

//ok the format here is that the spread opeator in the key position acts to spread/flatten the entries in an iterable to fold them
//in as map entries
def move(Direction dir) {
    [by                    : { String mess ->
        print "message : hi $mess "
        [at: { println "at $it hours apart" }]
    },
     /* spread operator*/ *: Direction.values().collectEntries { [(it.name()): it] }
    ]
}

def greet(String mess) {
    println "Hi $mess"
}

greet "william"

//equive move(Direction.left).by ("william).at (10)
move left by "william" at 10


def map = move Direction.left

println map

map

