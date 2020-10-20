package org.baylor.ecs.cloudhubs.sourcecrawler.helper;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;
import soot.SootMethod;

@Value
@AllArgsConstructor
public class StackTraceMethod {
    @Getter
    SootMethod method;
    @Getter int line;
    @Getter String exception;
}
