package com.softwood.flow.core.flows

import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ConcurrentLinkedQueue

class FlowContext extends Expando {
    ConcurrentLinkedDeque activePromises
    ConcurrentLinkedDeque promises
    ConcurrentLinkedQueue taskActions
    ConcurrentLinkedQueue flowListeners
    List newInClosure
    Flow flow
    FlowType type
    Object[] initialArgs

    static FlowContext newProcessContext (flow) {
        FlowContext ctx = new FlowContext()
        ctx.flow = flow
        ctx.type = FlowType.Process
        ctx
    }

    static Expando newFreeStandingContext () {
        FlowContext ctx = new FlowContext()
        ctx.activePromises = new ConcurrentLinkedDeque<>()
        ctx.promises = new ConcurrentLinkedDeque<>()
        ctx.taskActions = new ConcurrentLinkedQueue<>()
        ctx.newInClosure = []
        ctx.flow = null
        ctx.type = FlowType.FreeStanding
        ctx
    }

    FlowContext() {
        activePromises = new ConcurrentLinkedDeque<>()
        promises = new ConcurrentLinkedDeque<>()
        taskActions = new ConcurrentLinkedQueue<>()
        flowListeners = new ConcurrentLinkedQueue<FlowListener>()
        newInClosure = []
        flow = null
        type = FlowType.Process

    }
}
