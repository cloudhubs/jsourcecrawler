package org.baylor.ecs.cloudhubs.sourcecrawler.cfg;

import jas.Pair;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.baylor.ecs.cloudhubs.sourcecrawler.helper.StackTraceMethod;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.internal.*;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.CompleteBlockGraph;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@AllArgsConstructor
public class CFG {
    @Getter
    protected SootMethod method;
    protected CompleteBlockGraph cfg;

    enum Label { Must, May, MustNot };
    protected HashMap<Block, Label> labels;

    protected HashMap<Unit, CFG> callSiteToCFG;
    protected Optional<CFG> pred;
    protected Optional<Unit> callSite;

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
                callee.ifPresent(c -> {
                    // Prevent infinite recursive expansion (don't expand recursive calls)
                    if (!c.getMethod().getSignature().equals(method.getSignature())) {
                        callSiteToCFG.put(unit, c.copyWrapper(this, unit));
                    }
                });
            });
        }
        // Recursively expand CFGs
        callSiteToCFG.forEach((call, cfg) -> cfg.connectCFGs(methods));
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
            .filter(cfg -> {
                var met = cfg.getMethod();
                return met.getSignature().equals(method.getSignature());
            })
            .collect(Collectors.toList());
        if (m.size() > 0) {
            return Optional.of(m.get(0));
        }
        return Optional.empty();
    }

    public Pair<CFG, Unit> findThrowUnitAndCFG(List<StackTraceMethod> trace) {
        for (var block : cfg.getBlocks()) {
            for (var unit : block) {
                if (callSiteToCFG.containsKey(unit) && callSiteIsStackTraceCall(unit, trace)) {
                    // Get the CFG For the callee
                    var callee = callSiteToCFG.get(unit);

                    // Found a block that must have executed since a stack trace method call was found.
                    labels.put(block, Label.Must);
                    return callee.findThrowUnitAndCFG(trace);
                } else if (unit instanceof JThrowStmt && exceptionMatches(block, (JThrowStmt)unit, trace)) {
                    return new Pair<>(this, unit);
                }
            }
        }
        return null;
    }

    private boolean callSiteIsStackTraceCall(Unit unit, List<StackTraceMethod> trace) {
        // Get the CFG For the callee
        var callee = callSiteToCFG.get(unit);

        // Find the method in the stack trace
        var methods = trace.stream()
            .filter(stm -> stm.getMethod().getSignature().equals(callee.getMethod().getSignature()))
            .collect(Collectors.toList())
            .iterator();

        if (methods.hasNext()) {
            var stackTraceMethod = methods.next();

            // Verify the line number is the same for this call site
            return stackTraceMethod.getLine() == unit.getJavaSourceStartLineNumber();
        }
        return false;
    }

    /**
     * Checks whether the `JThrowStmt` given has the same line number as the one in the stack trace, and it is
     * thrown from the same method.
     *
     * @return whether the exception matched or not
     */
    private boolean exceptionMatches(Block block, JThrowStmt throwStmt, List<StackTraceMethod> trace) {
        var exception = findExceptionType(block, throwStmt);
        if (exception.isPresent()) {
            // Find the method in the stack trace
            var methods = trace.stream()
                .filter(stm -> stm.getMethod().getSignature().equals(method.getSignature()))
                .collect(Collectors.toList())
                .iterator();

            if (methods.hasNext()) {
                var stackTraceMethod = methods.next();

                // Verify the line number is the same for this call site
                return stackTraceMethod.getLine() == throwStmt.getJavaSourceStartLineNumber();
            }
        }
        return false;
    }

    private static Optional<String> findExceptionType(Block block, Unit u) {
        var pred = block.getPredOf(u);
        if (pred instanceof JAssignStmt) {
            var right = ((JAssignStmt)pred).getRightOp();
            if (right instanceof JNewExpr) {
                return Optional.of(right.toString().split("\\s")[1]);
            }
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

// I discovered the CallGraph class exists but only after I connected the CFGs. This maybe could have
// been used for parent stuff but may be useful for telling whether a Unit is an outgoing edge or not.
//        var callsites = Scene.v().getCallGraph().edgesInto(method);
//        var edge = callsites.next(); // iterator over all callers
//        edge.getSrc().method(); // SootMethod
//        edge.srcUnit(); // Unit