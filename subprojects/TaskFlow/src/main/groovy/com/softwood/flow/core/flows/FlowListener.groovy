package com.softwood.flow.core.flows

/**
 * does this need methods to listen to the flow as well
 */
interface FlowListener {
    def flowEventUpdate(FlowContext flowContext, FlowEvent event)

    def beforeFlowNodeExecuteState(Expando flowContext, AbstractFlowNode)

    def afterFlowNodeExecuteState(Expando flowContext, AbstractFlowNode)
}
