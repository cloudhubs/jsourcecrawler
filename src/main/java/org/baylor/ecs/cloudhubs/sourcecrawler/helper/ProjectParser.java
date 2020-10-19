package org.baylor.ecs.cloudhubs.sourcecrawler.helper;

import lombok.Getter;
import org.baylor.ecs.cloudhubs.sourcecrawler.model.LogType;
import soot.*;
import soot.jimple.StringConstant;
import soot.jimple.internal.ImmediateBox;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JDynamicInvokeExpr;
import soot.jimple.internal.JInvokeStmt;
import soot.options.Options;

import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class ProjectParser {
    @Getter
    SourceLocator srcLocator;
    @Getter
    List<SootClass> sootClasses;
    @Getter
    List<SootMethod> sootMethods;
    @Getter
    List<LogType> logs;
//        Scene.v().setSootClassPath(projectRoot);
//        Options.v().set_whole_program(true);
//        Options.v().set_prepend_classpath(true);
//        Options.v().set_soot_classpath(".:/usr/lib/jvm/java-14-openjdk/lib/jrt-fs.jar");
//        Scene.v().loadNecessaryClasses();

    public ProjectParser(String projectRoot) {
        super();
        soot.G.reset();
        srcLocator = SourceLocator.v();
        Options.v().set_verbose(true);
        String[] dirs = {projectRoot};
        Options.v().set_whole_program(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_process_dir(Arrays.stream(dirs).collect(Collectors.toList()));
        Options.v().setPhaseOption("jb", "use-original-names:true");
        Scene.v().loadNecessaryClasses();
        List<String> classes = srcLocator.getClassesUnder(projectRoot);
        List<ClassSource> classSources = new ArrayList<>();

        classes
            .parallelStream()
            .iterator()
            .forEachRemaining((c) -> classSources.add(srcLocator.getClassSource(c)));

        sootClasses = new ArrayList<>();
        Iterator<ClassSource> srcIter = classSources.iterator();
        for (String c : classes) {
            SootClass sc = new SootClass(c);
            var src = srcIter.next();
            src.resolve(sc);
            sootClasses.add(sc);
        }

        sootMethods = new ArrayList<>();
        sootClasses.forEach(sc -> sootMethods.addAll(sc.getMethods()));
        sootMethods.forEach(SootMethod::retrieveActiveBody);
    }

    public List<LogType> findLogs() {
        logs = sootMethods.stream()
            .map(e -> findLogs(e.getActiveBody()))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
        return logs;
    }

    private static List<LogType> findLogs(Body body) {
        var units = body.getUnits()
            .stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        List<LogType> logs = new ArrayList<>();

        for (var unit : units) {
            if (unit instanceof JInvokeStmt) {
                var invoke = (JInvokeStmt)unit;
                var expr = invoke.getInvokeExpr();
                var ref = expr.getMethodRef();
                var sig = ref.getSignature();
                var index = -1;
                if (sig.contains("void log(org.apache.logging.log4j")) {
                    index = 1;
                } else if (sig.contains("java.util.logging.Logger: void log(")) {
                    index = 1;
                } else if (sig.contains("java.util.logging.Logger")) {
                    index = 0;
                }
                // Method invalid, continue to next iteration
                if (index < 0) {
                    continue;
                }
                var regex = findLogString(units, expr.getArg(index));
                var logType = new LogType(
                    body.getMethod().getDeclaringClass().getFilePath(),
                    body.getMethod().getSignature(),
//                        unit.getJavaSourceStartLineNumber(),
                    regex
                );
                logs.add(logType);
            }
        }

        return logs;
    }

    private static String findLogString(List<Unit> units, Value vb) {
        // Handle java util info(), warn(), etc
        if (vb instanceof StringConstant) {
            var str = ((StringConstant)vb).value;
            return str.replaceAll(new String(new byte[] {1}), ".+");
        }

        // Handle java util and log4j log()
        var values = units.stream()
            .filter(u -> u instanceof JAssignStmt)
            .filter(u -> ((JAssignStmt)u).getLeftOp() == vb)
            .collect(Collectors.toList());

        var box = (JAssignStmt)values.get(values.size()-1);
        var right = box.getRightOp();
        if (right instanceof JDynamicInvokeExpr) {
            var inv = (JDynamicInvokeExpr) right;
            var arg = inv.getBootstrapArg(0);
            if (arg instanceof StringConstant) {
                var str = ((StringConstant)arg).value;
                return str.replaceAll(new String(new byte[] {1}), ".+");
            }
        }

        return ".+";
    }

    /**
     * Returns methods in the order they appear in the stack trace. Index 0 is where the exception was thrown,
     * and the last element is the entry point to the program.
     */
    public List<SootMethod> methodsInStackTrace(String stackTrace) {
        var lines = stackTrace.split("\n");
        var methodSignatures = Arrays.stream(lines)
            .filter(line -> line.matches("^\\s+at .+$"))
            .map(line -> line.split("^\\s+at ")[1])
            .map(method -> method.split("\\(")[0])
            .collect(Collectors.toList());

        var methods = new ArrayList<SootMethod>();

        for (var sig : methodSignatures) {
            var lastDot = sig.lastIndexOf('.');
            var clazz = sig.substring(0, lastDot);
            var call = sig.substring(lastDot + 1) + "(";
            for (var method : sootMethods) {
                var msig = method.getSignature();
                if (msig.contains(clazz) && msig.contains(call)) {
                    methods.add(method);
                    break;
                }
            }
        }

        return methods;
    }
}
