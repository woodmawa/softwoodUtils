package com.softwood.flow.core.flows

class FlowEvent<F> {
    String messsage
    F<? extends AbstractFlow> flow
    Optional<? extends Object> referencedObject = Optional.ofNullable()

}
