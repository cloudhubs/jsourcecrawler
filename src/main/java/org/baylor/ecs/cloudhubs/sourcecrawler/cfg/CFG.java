package org.baylor.ecs.cloudhubs.sourcecrawler.cfg;

import jas.Pair;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.baylor.ecs.cloudhubs.sourcecrawler.helper.LogParser;
import org.baylor.ecs.cloudhubs.sourcecrawler.helper.ProjectParser;
import org.baylor.ecs.cloudhubs.sourcecrawler.helper.StackTraceMethod;
import soot.Scene;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.internal.*;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.CompleteBlockGraph;

import java.util.ArrayList;
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
        // Find the call(s) of the current method in the stack trace
        var methods = trace.stream()
            .filter(stackMethod -> stackMethod.getMethod().getSignature().equals(method.getSignature()))
            .collect(Collectors.toList());

        // Find a call where the exception line matches the Unit method call line
        for (var stackMethod : methods) {
            if (stackMethod.getLine() == unit.getJavaSourceStartLineNumber()) {
                return true;
            }
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
                .filter(stackMethod -> stackMethod.getMethod().getSignature().equals(method.getSignature()))
                .collect(Collectors.toList());

            for (var stackMethod : methods) {
                // Verify the line number is the same for this call site
                if (stackMethod.getLine() == throwStmt.getJavaSourceStartLineNumber()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Optional<String> findExceptionType(Block block, Unit u) {
        for (var pred = block.getPredOf(u); pred != null; pred = block.getPredOf(pred)) {
            if (pred instanceof JAssignStmt) {
                var right = ((JAssignStmt)pred).getRightOp();
                if (right instanceof JNewExpr) {
                    return Optional.of(right.toString().split("\\s")[1]);
                }
            }
        }
        return Optional.empty();
    }

    // CFG Labeling stuff

    private Optional<Block> findBlockContainingUnit(Unit u) {
        for (var block : cfg.getBlocks()) {
            for (var unit : block) {
                if (unit.equals(u)) {
                    return Optional.of(block);
                }
            }
        }
        return Optional.empty();
    }

    // Unit is either the exception line or a call site in the stack trace
    public void beginLabelingAt(Unit unit, LogParser logParser) {
        var b = findBlockContainingUnit(unit);
        if (b.isEmpty()) {
            return;
        }
        labelBlockRecur(b.get(), logParser);
    }

    // TODO: Label inside of any method calls in these blocks.
    private void labelBlockRecur(Block block, LogParser logParser) {
        var preds = block.getPreds();

        for (var pred : preds) {
            var predLabel = labels.get(pred);
            if (predLabel == null) {
                // If a printed log statement is here, label must
                if (blockContainsPrintedLog(pred, logParser)) {
                    labels.put(block, Label.Must);
                }

                // If one of its children is must, pred is must
                if (anySuccOfBlockEquals(pred, Label.Must)) {
                    var mustSucc = block.getSuccs()
                        .stream()
                        .filter(s -> labels.get(s).equals(Label.Must))
                        .iterator()
                        .next();

                    if (mustSucc.getPreds().size() == 1) {
                        labels.put(pred, Label.Must);
                        labelMaybeSuccsAsMustNot(pred);
                    } else {
                        labels.put(pred, Label.May);
                    }
                } else if (allSuccOfBlockEquals(pred, Label.May)) {
                    labels.put(pred, Label.May);
                } else if (allSuccOfBlockEquals(pred, Label.MustNot)) {
                    labels.put(pred, Label.MustNot);
                }
            }
            labelBlockRecur(pred, logParser);
        }
        // Recurse to the next method up in the stack trace
        if (preds.size() < 1 && pred.isPresent() && callSite.isPresent()) {
            pred.get().beginLabelingAt(callSite.get(), logParser);
        }
    }

    private boolean anySuccOfBlockEquals(Block block, Label label) {
        return block.getSuccs()
            .stream()
            .anyMatch(succ -> labels.get(succ).equals(label));
    }

    private boolean allSuccOfBlockEquals(Block block, Label label) {
        return block.getSuccs()
            .stream()
            .allMatch(succ -> labels.get(succ).equals(label));
    }

    private boolean blockContainsPrintedLog(Block block, LogParser logParser) {
        List<Unit> units = new ArrayList<>();
        block.iterator().forEachRemaining(units::add);
        var filePath = block.getBody().getMethod().getDeclaringClass().getFilePath();
        var signature = block.getBody().getMethod().getSignature();
        var logs = ProjectParser.findLogs(units, filePath, signature);
        return logs
            .stream()
            .anyMatch(logParser::wasLogExecuted);
    }

    // I think this is fine since the call site will always be must, no need for continuing to next call I think
    private void labelMaybeSuccsAsMustNot(Block block) {
        var label = labels.get(block);
        if (label != Label.Must) {
            labels.put(block, Label.MustNot);
            for (var succ : block.getSuccs()) {
                labelMaybeSuccsAsMustNot(succ);
            }
        }
    }
}

// I discovered the CallGraph class exists but only after I connected the CFGs. This maybe could have
// been used for parent stuff but may be useful for telling whether a Unit is an outgoing edge or not.
//        var callsites = Scene.v().getCallGraph().edgesInto(method);
//        var edge = callsites.next(); // iterator over all callers
//        edge.getSrc().method(); // SootMethod
//        edge.srcUnit(); // Unit