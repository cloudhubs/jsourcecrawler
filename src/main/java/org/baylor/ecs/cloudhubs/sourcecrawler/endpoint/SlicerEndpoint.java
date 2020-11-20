package org.baylor.ecs.cloudhubs.sourcecrawler.endpoint;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.baylor.ecs.cloudhubs.sourcecrawler.cfg.CFG;
import org.baylor.ecs.cloudhubs.sourcecrawler.helper.ProjectParser;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@RestController
public class SlicerEndpoint {
    @Value
    static class SliceRequest {
        @NonNull
        String projectRoot;

        @NonNull
        String stackTrace;

        // Needed to fix 400 errors because of JSON deserialization
        SliceRequest() {
            projectRoot = "";
            stackTrace = "";
        }
    }

    @PostMapping("/slicer")
    public String slicer(@RequestBody SliceRequest s) {
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
            return "";
        }

        var entryMethod = stack.get(stack.size()-1);
        var cfg = cfgs.stream().filter(c -> c.getMethod() == entryMethod.getMethod()).collect(Collectors.toList());
        if (cfg.size() < 1) {
            log.log(Level.WARN, "couldn't find entry method");
            return "";
        }
        var entry = cfg.get(0);

        entry.connectCFGs(cfgs);

        var unitAndCFG = entry.findThrowUnitAndCFG(stack);

        entry.getMethod().getActiveBody().getUnits().stream().iterator().forEachRemaining(u -> {
            log.log(Level.INFO, "line: " + u.getJavaSourceStartLineNumber() + " col:" + u.getJavaSourceStartColumnNumber());
        });

        return ""; // TODO return actual response
    }
}
