package org.baylor.ecs.cloudhubs.sourcecrawler.model;

import lombok.Getter;
import lombok.Value;
import soot.SootMethod;

import java.util.List;

@Value
public class VarDependency {
    @Getter
    SootMethod method;
    @Getter
    String var;
    @Getter
    List<String> deps;
}
