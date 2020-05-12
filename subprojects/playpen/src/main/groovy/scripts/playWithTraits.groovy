package scripts

trait MyTrait {
    String name

    def whatsThis () {
        println "trait : $this"
        this
    }
}

class WillsClass implements MyTrait {

}

WillsClass wc = new WillsClass()

wc.whatsThis()

MyTrait mt = wc

assert mt.whatsThis().is (wc)



