package scripts

import com.softwood.flow.core.languageElements.Condition

import com.softwood.flow.core.nodes.ChoiceAction
import com.softwood.flow.core.nodes.TaskAction

import static com.softwood.flow.core.languageElements.Condition.when

action = TaskAction::newAction
choice = ChoiceAction::newChoiceAction
condition = Condition::newCondition

Closure condClosOut = {arg ->
    def ans = "william" == arg;
    println "in condition closure received $arg,  returning : $ans";
    ans}

Condition c1 = condition (condClosOut)
def res2 = c1.test('william')

Closure cl = {args ->
    println "choice closure got arg : $args"

    //get args as 'it'
    Closure condClos = {
        def ans = "william" == it;
        println "in condition closure recived $it,  returning : $ans";
        ans}


//    def res = c1.test(arg)

    when (condition (args) {
        def ans = "william" == it;
        println "in condition closure recived $it,  returning : $ans";
        ans}) {
        println "when: hello there $args"}
}

ChoiceAction choice = choice ('my choice', cl)

choice.run('william')

sleep 1000
choice