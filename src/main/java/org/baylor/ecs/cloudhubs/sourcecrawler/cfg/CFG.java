package org.baylor.ecs.cloudhubs.sourcecrawler.cfg;

import jas.Pair;
import jas.Var;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.baylor.ecs.cloudhubs.sourcecrawler.helper.*;
import org.baylor.ecs.cloudhubs.sourcecrawler.model.LogType;
import org.baylor.ecs.cloudhubs.sourcecrawler.model.PathCondition;
import org.baylor.ecs.cloudhubs.sourcecrawler.model.VarDependency;
import soot.*;
import soot.jimple.ConditionExpr;
import soot.jimple.internal.*;
import soot.toolkits.graph.*;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.FlowSet;

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

    @Getter
    protected HashMap<Unit, CFG> callSiteToCFG;
    protected Optional<CFG> pred;
    protected Optional<Unit> callSite;

    @Getter
    FlowSet<Local> reqIDs = new ArraySparseSet<>();
    FlowSet<Local> writes = new ArraySparseSet<>();

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
        FlowSet<Local> emptySet = new ArraySparseSet<>();
        return new CFG(method, cfg,
                new HashMap<>(),
                new HashMap<>(),
                Optional.of(predCFG),
                Optional.of(callSiteUnit),
                emptySet.clone(),
                emptySet.clone());
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

    public Optional<Block> findBlockContainingUnit(Unit u) {
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
        labels.put(b.get(), Label.Must);
        labelBlockRecur(b.get(), logParser);
    }

    // TODO: Label inside of any method calls in these blocks.
    private void labelBlockRecur(Block block, LogParser logParser) {
        var preds = block.getPreds();

        for (var unit : block) {
            if (callSiteToCFG.containsKey(unit)) {
                var cfg = callSiteToCFG.get(unit);
                var heads = cfg.cfg.getHeads();
                if (heads.size() == 1) {
                    cfg.labels.put(heads.get(0), Label.Must);
                }
                var tails = cfg.cfg.getTails();
                for (var tail : tails) {
                    labelBlockRecur(tail, logParser);
                }
            }
        }

        for (var pred : preds) {
            // If a printed log statement is here, label must
            if (blockContainsPrintedLog(pred, logParser)) {
                labels.put(pred, Label.Must);
            } else if (blockContainsNotPrintedLog(pred, logParser)) {
                labels.put(pred, Label.MustNot);
            }
            var predLabel = labels.get(pred);
            if (predLabel == null) {
                // If one of its children is must, pred is must
                if (anySuccOfBlockEquals(pred, Label.Must)) {
                    var mustSucc = pred.getSuccs()
                        .stream()
                        .filter(s -> labels.containsKey(s) && labels.get(s).equals(Label.Must))
                        .iterator()
                        .next();

                    if (mustSucc.getPreds().size() == 1) {
                        labels.put(pred, Label.Must);
                        labelSuccsAs(pred, Label.MustNot);
                    } else {
                        labels.put(pred, Label.May);
                        labelSuccsAs(pred, Label.May);
                    }
                } else if (allSuccOfBlockEquals(pred, Label.May)) {
                    labels.put(pred, Label.May);
//                    var currLabel = labels.get(pred);
//                    if (currLabel == Label.MustNot) {
//                        labelSuccsAs(pred, Label.MustNot);
//                    } else {
//
//                    }

                } else if (allSuccOfBlockEquals(pred, Label.MustNot)) {
                    labels.put(pred, Label.MustNot);
                }
            } else if (predLabel == Label.May) {

            }
            labelBlockRecur(pred, logParser);
        }
        // Recurse to the next method up in the stack trace
        if (preds.size() < 1 && pred.isPresent() && callSite.isPresent()) {
            pred.get().beginLabelingAt(callSite.get(), logParser);
        }
    }

    private boolean anySuccOfBlockEquals(Block block, Label label) {
        if (block.getSuccs() == null) return false;
        return block.getSuccs()
            .stream()
            .anyMatch(succ -> labels.containsKey(succ) && labels.get(succ).equals(label));
    }

    private boolean allSuccOfBlockEquals(Block block, Label label) {
        return block.getSuccs()
            .stream()
            .allMatch(succ -> labels.containsKey(succ) && labels.get(succ).equals(label));
    }

    private List<LogType> getLogsInBlock(Block block) {
        List<Unit> units = new ArrayList<>();
        block.iterator().forEachRemaining(units::add);
        var filePath = block.getBody().getMethod().getDeclaringClass().getFilePath();
        var signature = block.getBody().getMethod().getSignature();
        return ProjectParser.findLogs(units, filePath, signature);
    }

    private boolean blockContainsPrintedLog(Block block, LogParser logParser) {
        return getLogsInBlock(block)
            .stream()
            .anyMatch(logParser::wasLogExecuted);
    }

    private boolean blockContainsNotPrintedLog(Block block, LogParser logParser) {
        return getLogsInBlock(block)
            .stream()
            .anyMatch(log -> !logParser.wasLogExecuted(log));
    }

    // I think this is fine since the call site will always be must, no need for continuing to next call I think
    private void labelSuccsAs(Block block, Label newLabel) {
        for (var succ : block.getSuccs()) {
            var label = labels.get(block);
            if (label != Label.Must) {
                labels.put(block, newLabel);
                labelSuccsAs(succ, newLabel);
            }
        }
    }

    // TODO convert to Expr
    public void collectPaths(
        Block block,
        ArrayList<ArrayList<PathCondition>> paths,
        ArrayList<PathCondition> path,
        Unit excludeCallsite
    ) {
        var conds = 0;
        for (var unit : block) {
            if (callSiteToCFG.containsKey(unit) && unit != excludeCallsite) {
                var cfg = callSiteToCFG.get(unit);
                var tails = cfg.cfg.getTails();
                for (var tail : tails) {
                    collectPaths(tail, paths, path, null);
                }
            }
            if (unit instanceof JIfStmt) {
                var ifStmt = (JIfStmt)unit;
                var cond = (ConditionExpr)ifStmt.getCondition();

                var succs = block.getSuccs();
                if (succs.size() == 2) {
                    // The reason for flipping for Must labels is that Soot seems to invert
                    // conditions. This means we need to re-invert the must and may's, and
                    // not the must not labels.
                    var succ = succs.get(0);
                    var label = labels.get(succ);
                    if (label == Label.Must || label == Label.May) {
                        cond = negateCondition(cond);
                    }
                }

                path.add(new PathCondition(method, cond));
                conds++;
            }/* else if (unit instanceof JAssignStmt) {
                var assign = (JAssignStmt)unit;
                var varFinder = new VarFinder();
                varFinder.findVars(assign.getLeftOp())
                    .stream()
                    .filter(name -> !name.startsWith("$"))
                    .findFirst()
                    .ifPresent(name -> {
                        var left = assign.getLeftOp();
                        var right = assign.getRightOp();
                        var cond = new PathCondition(method, new JEqExpr(left, right));
                        path.add(cond);
                    });
            }*/
        }

        // Collect paths in preceding blocks in the method CFG
        var preds = block.getPreds();
        for (var pred : preds) {
            if (labels.containsKey(pred) && labels.get(pred) != Label.MustNot) {
                collectPaths(pred, paths, path, excludeCallsite);
            }
        }

        // Recurse to the next method up in the stack trace
        if (preds.isEmpty() && !block.getSuccs().isEmpty()) {
            if (pred.isPresent() && callSite.isPresent()) {
                var blk = pred.get().findBlockContainingUnit(callSite.get());
                blk.ifPresent(b -> pred.get().collectPaths(b, paths, path, callSite.get()));
            } else if (!path.isEmpty() && !paths.contains(path)) {
                // Add the path, since there was no predecessor method CFG
                paths.add(new ArrayList<>(path));
            }
        }

        // Remove the paths added in this recursive call, since they may not be in
        // the caller's next path that is found.
        for (int i = 0; i < conds; ++i) {
            if (!path.isEmpty()) {
                path.remove(path.size()-1);
            }
        }
    }

    private ConditionExpr negateCondition(ConditionExpr condition) {
        var op1 = condition.getOp1();
        var op2 = condition.getOp2();
        if (condition instanceof JEqExpr) {
            return new JNeExpr(op1, op2);
        } else if (condition instanceof JNeExpr) {
            return new JEqExpr(op1, op2);
        } else if (condition instanceof JGeExpr) {
            return new JLtExpr(op1, op2);
        } else if (condition instanceof JLeExpr) {
            return new JGtExpr(op1, op2);
        } else if (condition instanceof JGtExpr) {
            return new JLeExpr(op1, op2);
        } else if (condition instanceof JLtExpr) {
            return new JGeExpr(op1, op2);
        }

        /*
          else if (condition instanceof JAndExpr) {
            return new JOrExpr(new NegExpr(op1), new JNegExpr(op2));
        } else if (condition instanceof JOrExpr) {

        } else if (condition instanceof JNegExpr) {

        } else {

        }
         */

        return condition;
    }

    public List<VarDependency> collectVarDeps() {
        var deps = new ArrayList<VarDependency>();

        for (var unit : cfg.getBody().getUnits()) {
            if (unit instanceof AbstractDefinitionStmt) {
                VarDependencyMapper.map(method, (AbstractDefinitionStmt)unit).ifPresent(deps::add);
            }
        }

        return deps
            .stream()
            .filter(v -> v.getDeps() == null || v.getDeps().isEmpty())
            .filter(v -> !v.getVar().startsWith("$"))
            .collect(Collectors.toList());
    }

    public void requestIDsForCFG(){
        callSiteToCFG.forEach(
                (unit, cfg) -> {
                    //recurse to populate lists in lower CFGs
                    cfg.requestIDsForCFG();
                }
        );

        //analyze this CFG
        DirectedGraph<Unit> graph =
                new CompleteUnitGraph(this.getMethod().getActiveBody());

        FlowSet<Local> reads = new ArraySparseSet<>();

        //add all writes and reads from every unit
        //in the method
        for(Unit node : graph){
            for (ValueBox use: node.getUseBoxes()) {
                if (use.getValue() instanceof Local) {
                    reads.add((Local) use.getValue());
                }
            }

            for (ValueBox def: node.getUseAndDefBoxes()) {
                if (def.getValue() instanceof Local) {
                    writes.add((Local) def.getValue());
                }
            }

        }

        //reads - writes = request identifiers
        reads.difference(writes, reqIDs);

        //remove lower CFG writes from this CFGs, reqIDs
        callSiteToCFG.forEach(
                (unit, cfg) -> {
                    //remove writes from this reqIDs
                    this.reqIDs.difference(cfg.writes, this.reqIDs);
                }
        );
    }
}

// I discovered the CallGraph class exists but only after I connected the CFGs. This maybe could have
// been used for parent stuff but may be useful for telling whether a Unit is an outgoing edge or not.
//        var callsites = Scene.v().getCallGraph().edgesInto(method);
//        var edge = callsites.next(); // iterator over all callers
//        edge.getSrc().method(); // SootMethod
//        edge.srcUnit(); // Unit
