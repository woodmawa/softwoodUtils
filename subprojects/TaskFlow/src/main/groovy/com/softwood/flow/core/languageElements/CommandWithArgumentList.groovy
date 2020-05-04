package com.softwood.flow.core.languageElements

import com.softwood.flow.core.flows.FlowContext

public class CommandWithArgumentList {
    @Delegate
    List list  = []
    FlowContext ctx
    String name = ""

    static CommandWithArgumentList newShellCommand (FlowContext ctx, String name,  String[] arglist=null) {

        //String name = arglist?[0]
        //List cmdArgs = arglist.toList()?.sublist (1)

        CommandWithArgumentList cmdWithArgList = new CommandWithArgumentList(ctx:ctx, name:name)
        //add to items generated within the running closure
        cmdWithArgList.addAll(arglist)

        if (cmdWithArgList.ctx.newInClosure != null) {
            cmdWithArgList.ctx.newInClosure.add(cmdWithArgList)  //add to items generated within the running closure
        }

        cmdWithArgList
    }

    static CommandWithArgumentList newShellCommand (FlowContext ctx, String... args) {

        String name = args ?[0]
        List cmdArgs = []
        if (args.size() > 1)
            cmdArgs = args?.toList()?.subList(1, args?.size())

        CommandWithArgumentList cmdWithArgList = new CommandWithArgumentList(ctx: ctx, name: name)
        //add to items generated within the running closure
        cmdWithArgList.addAll(cmdArgs)

        if (cmdWithArgList.ctx.newInClosure != null) {
            cmdWithArgList.ctx.newInClosure.add (cmdWithArgList)  //add to items generated within the running closure
        }

        cmdWithArgList

    }


        static CommandWithArgumentList newShellCommand (FlowContext ctx, String name, Object item) {

        CommandWithArgumentList cmdWithArgList = new CommandWithArgumentList(ctx: ctx, name: name)
        if (item != null)
            cmdWithArgList.add ("$item")

        if (cmdWithArgList.ctx.newInClosure != null) {
            cmdWithArgList.ctx.newInClosure.add (cmdWithArgList)  //add to items generated within the running closure
        }

        cmdWithArgList
    }

    static CommandWithArgumentList newShellCommand (FlowContext ctx, String name, ArrayList alist) {
        newShellCommand (ctx, name, alist.toList())
    }
}
