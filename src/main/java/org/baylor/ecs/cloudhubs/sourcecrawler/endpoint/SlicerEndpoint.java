package org.baylor.ecs.cloudhubs.sourcecrawler.endpoint;

import lombok.NonNull;
import lombok.Value;
import org.baylor.ecs.cloudhubs.sourcecrawler.helper.ProjectParser;
import org.springframework.web.bind.annotation.*;

@RestController
public class SlicerEndpoint {
    @Value
    static class SliceRequest {
        @NonNull
        String projectRoot;
    }

    @PostMapping("/slicer")
    public String slicer(@RequestBody SliceRequest s) {
        // TODO fix 400 bad response error.
        var parser = new ProjectParser(s.projectRoot);

        return ""; // TODO return actual response
    }
}
