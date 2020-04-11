package com.softwood.flow.core.flows

import groovyx.gpars.dataflow.Promise

import java.util.concurrent.ConcurrentLinkedDeque

enum FlowStatus {
    ready, running, suspended, completed, forcedExit
}

enum FlowType {
    Process, Subprocess, DefaultSubflow, Subflow, FreeStanding
}

abstract class AbstractFlow {

    static final String DEFAULT = "Default"

    String name

    FlowContext ctx  //subtype of Expando
    ConcurrentLinkedDeque<Promise> promises = new ConcurrentLinkedDeque<>()
    FlowStatus status = FlowStatus.ready
    FlowType flowType

}
