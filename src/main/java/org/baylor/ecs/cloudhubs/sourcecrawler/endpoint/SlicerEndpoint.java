package org.baylor.ecs.cloudhubs.sourcecrawler.endpoint;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Solver;
import lombok.*;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.baylor.ecs.cloudhubs.sourcecrawler.cfg.CFG;
import org.baylor.ecs.cloudhubs.sourcecrawler.helper.LogParser;
import org.baylor.ecs.cloudhubs.sourcecrawler.helper.ProjectParser;
import org.baylor.ecs.cloudhubs.sourcecrawler.helper.ValueAssignment;
import org.baylor.ecs.cloudhubs.sourcecrawler.helper.Z3Converter;
import org.baylor.ecs.cloudhubs.sourcecrawler.model.PathCondition;
import org.springframework.web.bind.annotation.*;
import soot.jimple.ConditionExpr;
import soot.toDex.ConstantVisitor;
import soot.toDex.ExprVisitor;
import soot.toDex.RegisterAllocator;
import soot.toDex.StmtVisitor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.microsoft.z3.Status.SATISFIABLE;

@Log4j2
@RestController
public class SlicerEndpoint {
    @Value
    static class SliceRequest {
        @NonNull
        String projectRoot;

        @NonNull
        String stackTrace;

        @NonNull
        String log;

        // Needed to fix 400 errors because of JSON deserialization
        SliceRequest() {
            projectRoot = "";
            stackTrace = "";
            log = "";
        }
    }

    @Value
    static class Slice {
        List<String> path;
        List<ValueAssignment> assignments;
    }

    @Value
    static class SliceResponse {
        List<List<String>> path;
        List<List<ValueAssignment>> assignments;

        List<Slice> getResponse() {
            var iter = assignments.iterator();
            return path.stream()
                .map(path -> new Slice(path, iter.next()))
                .collect(Collectors.toList());
        }
    }

    @PostMapping("/slicer")
    public List<Slice> slicer(@RequestBody SliceRequest s) {
        ProjectParser parser = new ProjectParser(s.projectRoot);
        List<CFG> cfgs = new ArrayList<>();
        log.log(Level.WARN, "sample log: " + s.projectRoot + " -> " + cfgs);
        log.log(Level.INFO, "stackTrace:\n" + s.stackTrace);
        parser.getSootMethods().forEach(m -> {
            try {
                cfgs.add(new CFG(m));
            } catch (RuntimeException e) {
                log.log(Level.WARN, "Method had no body: " + m.toString() + " -> "+ e.getMessage());
            }
        });
        var logs = parser.findLogs();
        log.log(Level.INFO, "found logs: " + logs);

        var stack = parser.methodsInStackTrace(s.stackTrace);
        if (stack.size() < 1) {
            log.log(Level.WARN, "no methods in stack trace");
            return null;
        }

        var entryMethod = stack.get(stack.size()-1);
        var cfg = cfgs.stream().filter(c -> c.getMethod() == entryMethod.getMethod()).collect(Collectors.toList());
        if (cfg.size() < 1) {
            log.log(Level.WARN, "couldn't find entry method");
            return null;
        }
        var entry = cfg.get(0);

        entry.connectCFGs(cfgs);

        var logParser = new LogParser(logs, s.log);
        var unitAndCFG = entry.findThrowUnitAndCFG(stack);
        var rootCause = unitAndCFG.getO1();
        var exceptionLoc = unitAndCFG.getO2();
        rootCause.beginLabelingAt(exceptionLoc, logParser);

        var paths = new ArrayList<ArrayList<PathCondition>>();
        var path = new ArrayList<PathCondition>();
        var exceptBlock = rootCause.findBlockContainingUnit(exceptionLoc);
        exceptBlock.ifPresent(except -> {
            rootCause.collectPaths(except, paths, path, null);
        });

        var varDeps = entry.collectVarDeps();
        var ctx = new Context();
        var z3Converter = new Z3Converter(ctx);
        var z3Paths = paths.stream()
            .map(z3Converter::convert)
            .collect(Collectors.toList());

        var pathAssignments = new ArrayList<List<ValueAssignment>>();

        for (var p : z3Paths) {
            var solver = ctx.mkSolver();
            for (var condition : p) {
                solver.add(ctx.mkEq((Expr)condition, ctx.mkTrue()));
            }

            if (solver.check() != SATISFIABLE) {
                log.debug("solver: " + solver.check());
                continue;
            }

            var model = solver.getModel();
            pathAssignments.add(Arrays.stream(model.getConstDecls())
                .map(f -> {
                    var value = model.getConstInterp(f);
                    return new ValueAssignment(f.getName().toString(), value.toString());
                })
                .collect(Collectors.toList()));
        }

        var pathsStr = paths
            .stream()
            .map(execPath -> execPath
                .stream()
                .map(PathCondition::getCondition)
                .map(Object::toString)
                .collect(Collectors.toList()))
            .collect(Collectors.toList());
        var resp = new SliceResponse(pathsStr, pathAssignments);
        ctx.close();
        return resp.getResponse();
    }
}
