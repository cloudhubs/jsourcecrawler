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
                if (sig.contains("void log(org.apache.logging.log4j")) {
                    var regex = findLogString(units, expr.getArg(1));
                    var logType = new LogType(
                        body.getMethod().getDeclaringClass().getFilePath(),
                        unit.getJavaSourceStartLineNumber(),
                        regex
                    );
                    logs.add(logType);
                }
            }
        }

        return logs;
    }

    private static String findLogString(List<Unit> units, Value vb) {
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

        return "";
    }

}
