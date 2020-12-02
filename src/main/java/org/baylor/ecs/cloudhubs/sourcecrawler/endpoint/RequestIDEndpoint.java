package org.baylor.ecs.cloudhubs.sourcecrawler.endpoint;

import lombok.NonNull;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.baylor.ecs.cloudhubs.sourcecrawler.cfg.CFG;
import org.baylor.ecs.cloudhubs.sourcecrawler.helper.ProjectParser;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@Log4j2
@RestController
public class RequestIDEndpoint {
    @Value
    static class IDRequest {
        @NonNull
        String projectRoot;

        IDRequest() {
            projectRoot = "";
        }
    }

    @PostMapping("/id")
    public String id(@RequestBody IDRequest s) {
        ProjectParser parser = new ProjectParser(s.projectRoot);
        List<CFG> cfgs = new ArrayList<>();

        //make cfg for each method in project
        parser.getSootMethods().forEach(m -> {
            try {
                cfgs.add(new CFG(m));
            } catch (RuntimeException e) {
                log.log(Level.WARN, "Method had no body: " + m.toString() + " -> "+ e.getMessage());
            }
        });

        //expand all cfgs
        cfgs.forEach(cfg -> {
            cfg.connectCFGs(cfgs);
        });

        //remove cfgs that are called in others
        //to find the main cfg(s) for the project
        List<CFG> topLevelCFGs = new ArrayList<>(List.copyOf(cfgs));
        cfgs.forEach(cfg -> {
            cfgs.forEach(cfg2 -> {
                if(cfg.getCallSiteToCFG().containsValue(cfg2)){
                    topLevelCFGs.remove(cfg2);
                }
            });
        });

        //find the request IDs
        topLevelCFGs.forEach(cfg -> {
            cfg.requestIDsForCFG();
        });

        //log request IDs
        topLevelCFGs.forEach(cfg -> {
            log.info(cfg.getReqIDs());
        });

        return ""; // TODO return actual response
    }
}