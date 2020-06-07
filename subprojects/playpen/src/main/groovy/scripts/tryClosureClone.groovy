package scripts

def c = { func(); println "hello" }  //wont resolve func()  without setting delegate as instance of Wills
Closure cloned = c.clone()

def assignee = cloned

class Wills {
    def func() {
        println "will says "
    }
}

Wills w = new Wills()

def addWater = cloned.rehydrate(w, w, w)

addWater()