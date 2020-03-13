package groovyx.runtime

import groovy.transform.InheritConstructors
import io.reactivex.rxjava3.functions.Consumer
import io.reactivex.rxjava3.functions.Function
import org.codehaus.groovy.runtime.MethodClosure

/**
 * class to wrap a closure and convert it into a RxJava Consumer
 * @param <T>  expected type of the arg that that the closure will be called with
 */
@InheritConstructors
class FunctionalClosure<T, R> extends Closure implements Consumer<T>, Function<T,R> {

    private Closure action = {}

    //maximumNumberOfParameters = 1
    //parameterTypes = EMPTY_CLASS_ARRAY

    FunctionalClosure() {
        super(null)
    }

    FunctionalClosure (final Closure clos) {
        //setup the abstract closure with the owner of the closure
        //super(clos?.owner)
        super (clos.clone())

        maximumNumberOfParameters = clos.getMaximumNumberOfParameters()
        action = clos.clone()
   }

    //implement doCall to direct the call() to the action closure
    protected Object doCall(Object arguments) {
        return action(arguments)
    }

    Closure<T> leftShift (final Closure clos) {
        action = clos.clone()

    }

    /**
     * as we have an embedded action closure, make sure when setting the closure delegate
     * that this is set on the action.
     * @param delegate - the object you want to provide the context for the action
     */
    //
    void setDelegate (Object delegate) {
        action.delegate = delegate
    }

    /**
     * implements the RxJava Consumer contract, takes a generic arg of type T,
     * an invokes the closure call () with the arg
     * @param arg
     */
    void accept (T arg) {
        call (arg)
    }

    /**
     * implements the RxJava Function contract, takes a generic arg of type T,
     * an invokes the closure call () with the arg, and returns the result of the call
     * @param arg
     */
    R apply (T arg) {
        return call (arg)
    }


        /**
     * static from method, accepts a closure and assigns a clone of it
     * and returns result as Consumer<T>
     * @param clos pass some closure to convert to Functional type
     * @return Consumer<T>
     */
    static <T> Consumer<T>  consumerFrom (Closure clos ) {
        assert clos

        if (clos.maximumNumberOfParameters == 0){
            throw new IncorrectClosureArgumentsException("from: closure must accept at least one argument")
        }
        Closure cons = new FunctionalClosure<>(clos.clone())
        cons
    }

    /**
     * static from method, accepts a closure and assigns a clone of it
     * and returns result as Function<T, R>
     * @param clos pass some closure to convert to Functional type
     * @return Consumer<T>
     */
    static <T,R> Function<T, R>  functionFrom (Closure clos ) {
        assert clos

        if (clos.maximumNumberOfParameters == 0){
            throw new IncorrectClosureArgumentsException("from: closure must accept at least one argument")
        }
        Closure cons = new FunctionalClosure<>(clos.clone())
        cons
    }

    /**
     * static from method, accepts a closure and assigns a clone of it
     * and returns result as Consumer<T>
     * @param clos pass some closure to convert to Functional type
     * @return Consumer<T>
     */

    static MethodClosure asMethodClosure (Closure clos ) {
        assert clos

        Closure cons = new FunctionalClosure<>(clos.clone())
        cons::accept
    }

}

