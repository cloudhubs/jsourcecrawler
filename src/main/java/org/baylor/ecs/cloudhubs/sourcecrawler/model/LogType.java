package org.baylor.ecs.cloudhubs.sourcecrawler.model;

import lombok.Getter;
import lombok.Value;

@Value
public class LogType {
    @Getter String filePath;
    @Getter String methodSignature;
    @Getter int line;
    @Getter String regex;
}
