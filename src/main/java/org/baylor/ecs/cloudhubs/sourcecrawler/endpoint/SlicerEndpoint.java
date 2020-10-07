package org.baylor.ecs.cloudhubs.sourcecrawler.endpoint;

import lombok.NonNull;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.baylor.ecs.cloudhubs.sourcecrawler.cfg.CFG;
import org.baylor.ecs.cloudhubs.sourcecrawler.helper.ProjectParser;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Log4j2
@RestController
public class SlicerEndpoint {
    @Value
    static class SliceRequest {
        @NonNull
        String projectRoot;

        // Needed to fix 400 errors because of JSON deserialization
        SliceRequest() {
            projectRoot = "";
        }
    }

    @PostMapping("/slicer")
    public String slicer(@RequestBody SliceRequest s) {
        ProjectParser parser = new ProjectParser(s.projectRoot);
        List<CFG> cfgs = new ArrayList<>();
        parser.getMethods().forEach(m -> {
            try {
                cfgs.add(new CFG(m));
            } catch (RuntimeException e) {
                log.log(Level.WARN, "Method had no body: " + m.toString() + " -> "+ e.getMessage());
            }
        });
        return ""; // TODO return actual response
    }
}
