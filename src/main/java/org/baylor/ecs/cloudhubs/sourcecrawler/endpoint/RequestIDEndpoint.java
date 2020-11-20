package org.baylor.ecs.cloudhubs.sourcecrawler.endpoint;

import lombok.NonNull;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.baylor.ecs.cloudhubs.sourcecrawler.cfg.CFG;
import org.baylor.ecs.cloudhubs.sourcecrawler.helper.LiveVariableAnalysis;
import org.baylor.ecs.cloudhubs.sourcecrawler.helper.ProjectParser;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.toolkits.ide.icfg.BackwardsInterproceduralCFG;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import soot.toolkits.graph.CompleteUnitGraph;
import soot.toolkits.graph.DirectedGraph;

import java.util.*;
import java.util.stream.Collectors;

@Log4j2
@RestController
public class RequestIDEndpoint {
    @Value
    static class IDRequest {
        @NonNull
        String projectRoot;

        // Needed to fix 400 errors because of JSON deserialization
        IDRequest() {
            projectRoot = "";
        }
    }

    @PostMapping("/id")
    public String id(@RequestBody IDRequest s) {
        ProjectParser parser = new ProjectParser(s.projectRoot);
        List<CFG> cfgs = new ArrayList<>();
        log.log(Level.WARN, "sample log: " + s.projectRoot + " -> " + cfgs);

        parser.getSootMethods().forEach(m -> {
            try {
                cfgs.add(new CFG(m));
            } catch (RuntimeException e) {
                log.log(Level.WARN, "Method had no body: " + m.toString() + " -> "+ e.getMessage());
            }
        });

        cfgs.forEach(cfg -> {
            cfg.connectCFGs(cfgs);
            DirectedGraph<Unit> graph =
                    new CompleteUnitGraph(cfg.getMethod().getActiveBody());
            LiveVariableAnalysis analysis = new LiveVariableAnalysis(graph);

            for (Unit u : graph) {
                System.out.println(u.toString());
                System.out.println(analysis.getFlowBefore(u));
                System.out.println(analysis.getFlowAfter(u));
                System.out.println("---------------------------");
            }
        });



        return ""; // TODO return actual response
    }
}