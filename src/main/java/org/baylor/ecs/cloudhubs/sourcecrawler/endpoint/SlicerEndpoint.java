package org.baylor.ecs.cloudhubs.sourcecrawler.endpoint;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
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

        // Needed to fix 400 errors because of JSON deserialization
        SliceRequest() {
            projectRoot = "";
        }
    }

    @PostMapping("/slicer")
    public String slicer(@RequestBody SliceRequest s) {
        var parser = new ProjectParser(s.projectRoot);

        return ""; // TODO return actual response
    }
}
