package org.baylor.ecs.cloudhubs.sourcecrawler.cfg;

import lombok.AllArgsConstructor;
import lombok.Getter;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.internal.AbstractInvokeExpr;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JInvokeStmt;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.CompleteBlockGraph;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@AllArgsConstructor
public class CFG {
    @Getter
    private SootMethod method;
    private CompleteBlockGraph cfg;

    enum Label { Must, May, MustNot };
    private HashMap<Block, Label> labels;

    private HashMap<Unit, CFG> callSiteToCFG;
    private Optional<CFG> pred;
    private Optional<Unit> callSite;

    public CFG(SootMethod m) throws RuntimeException {
        method = m;
        labels = new HashMap<>();
        callSiteToCFG = new HashMap<>();
        pred = Optional.empty();
        callSite = Optional.empty();

        // Throws RunTimeException if a body can't be retrieved
        var body = method.getActiveBody();
        cfg = new CompleteBlockGraph(body);
    }

    public CFG copyWrapper(CFG predCFG, Unit callSiteUnit) {
        return new CFG(method, cfg, new HashMap<>(), new HashMap<>(), Optional.of(predCFG), Optional.of(callSiteUnit));
    }

    public void connectCFGs(List<CFG> methods) {
        var unitIters = cfg.getBlocks()
            .stream()
            .map(Block::iterator)
            .collect(Collectors.toList());
        for (var it : unitIters) {
            it.forEachRemaining(unit -> {
                var callee = findUnitCallee(unit, methods);
                callee.ifPresent(c -> callSiteToCFG.put(unit, c.copyWrapper(this, unit)));
            });
        }
    }

    private static Optional<CFG> findUnitCallee(Unit unit, List<CFG> methods) {
        Optional<CFG> callee = Optional.empty();
        if (unit instanceof AbstractInvokeExpr) {
            callee = findCalleeCFG((AbstractInvokeExpr)unit, methods);
        } else if (unit instanceof JAssignStmt) {
            var right = ((JAssignStmt) unit).getRightOp();
            if (right instanceof AbstractInvokeExpr) {
                callee = findCalleeCFG((AbstractInvokeExpr)right, methods);
            }
        } else if (unit instanceof JInvokeStmt) {
            var invokeExpr = ((JInvokeStmt) unit).getInvokeExpr();
            callee = findCalleeCFG((AbstractInvokeExpr)invokeExpr, methods);
        }
        return callee;
    }

    private static Optional<CFG> findCalleeCFG(AbstractInvokeExpr invokeExpr, List<CFG> methods) {
        var method = invokeExpr.getMethod();
        var m = methods.stream()
            .filter(cfg -> cfg.cfg.getBody().getMethod() == method)
            .collect(Collectors.toList());
        if (m.size() > 0) {
            return Optional.of(m.get(0));
        }
        return Optional.empty();
    }
}


//        cfg.getBody().getUnits().forEach(u -> {
//            if (u instanceof JAssignStmt) {
//                var right = ((JAssignStmt)u).getRightOpBox();
//                var value = right.getValue();
//                if (value instanceof JStaticInvokeExpr) {
//                   var method = ((JStaticInvokeExpr)value).getMethod().getActiveBody();
////                   System.out.println("kek");
//                }
//            }
//        });

// cfg.iterator().next().getHead().
// Note: it actually appears it is unnecessary to do any sort of wrapping
// on this block graph due to the fact predecessors of blocks can
// already be accessed.
// It also appears I do not need to directly in-line function calls as
// part of the work is already done with InvokeStmt -> InvokeExpr -> SootMethod.
// I can simply get the SootMethod back and keep a Flyweight/cache of CFGs.