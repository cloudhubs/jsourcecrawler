package org.baylor.ecs.cloudhubs.sourcecrawler.cfg;

import soot.SootMethod;
import soot.SootMethodRef;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JStaticInvokeExpr;
import soot.toolkits.graph.pdg.EnhancedBlockGraph;

import java.util.HashMap;

public class CFG {
    private SootMethod method;
    private EnhancedBlockGraph cfg;

    public CFG(SootMethod m) throws RuntimeException {
        method = m;

        // Throws RunTimeException if a body can't be retrieved
        var body = method.getActiveBody();

        cfg = new EnhancedBlockGraph(body);

        cfg.getBody().getUnits().forEach(u -> {
            if (u instanceof JAssignStmt) {
                var right = ((JAssignStmt)u).getRightOpBox();
                var value = right.getValue();
                if (value instanceof JStaticInvokeExpr) {
                   var method = ((JStaticInvokeExpr)value).getMethod().getActiveBody();
//                   System.out.println("kek");
                }
            }
        });

        // cfg.iterator().next().getHead().
        // Note: it actually appears it is unnecessary to do any sort of wrapping
        // on this block graph due to the fact predecessors of blocks can
        // already be accessed.
        // It also appears I do not need to directly in-line function calls as
        // part of the work is already done with InvokeStmt -> InvokeExpr -> SootMethod.
        // I can simply get the SootMethod back and keep a Flyweight/cache of CFGs.
    }

}
