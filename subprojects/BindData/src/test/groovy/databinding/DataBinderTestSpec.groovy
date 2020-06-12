package databinding

import databinding.*
import spock.lang.Shared
import spock.lang.Specification



class DataBinderTestSpec extends Specification {

    @Shared DataBinder dbind
    @Shared TargetClass target

    def setupSpec() {
        //build a binder with defaults
        dbind  = new DataBinder.Options().build()
    }

    def setup() {
        target = new TargetClass()
    }


    def cleanupSpec () {
        dbind = null
    }

    def "test DataBinder defaults" () {
        expect:
        dbind.options.excludedFieldNames.isEmpty()
        dbind.options.excludedFieldTypes.isEmpty()
        dbind.options.excludeClass == true
        dbind.options.excludeNulls == true
        dbind.options.excludePrivateFields == false
        dbind.options.excludeStaticFields == true
        dbind.options.expandLevels == 1
        dbind.options.includeVersion == false
    }

    def "one level bind from a map " () {
            given :
            Map sourceMap = [someString:"hi", id:1, someInt:10, refClass:[refString:'my refString', refInt: 100]]

            when:
            TargetClass tc = dbind.bind (target, sourceMap)

            then:
            tc.someString == "hi"
            tc.id == 1
    }
}
