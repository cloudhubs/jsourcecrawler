package org.baylor.ecs.cloudhubs.sourcecrawler.model;

import lombok.Getter;
import lombok.Value;
import soot.SootMethod;
import soot.jimple.Expr;

@Value
public class PathCondition {
    @Getter
    SootMethod method;
    @Getter
    Expr condition;
}
