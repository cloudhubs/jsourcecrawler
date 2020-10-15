package org.baylor.ecs.cloudhubs.sourcecrawler.model;

import lombok.Getter;
import lombok.Value;

@Value
public class LogType {
    @Getter String filePath;
    @Getter
    int lineNumber;
    @Getter String regex;
}
