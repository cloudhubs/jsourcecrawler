package org.baylor.ecs.cloudhubs.sourcecrawler.helper;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class ValueAssignment {
    String name;
    String value;

    @Override
    public String toString() {
        return name + " = " + value;
    }
}
