package org.baylor.ecs.cloudhubs.sourcecrawler.helper;

import org.baylor.ecs.cloudhubs.sourcecrawler.model.VarDependency;
import soot.SootMethod;
import soot.jimple.internal.AbstractDefinitionStmt;
import java.util.Optional;

public class VarDependencyMapper {
    private static final VarFinder finder = new VarFinder();

    public static Optional<VarDependency> map(SootMethod m, AbstractDefinitionStmt defStmt) {
        var left = defStmt.getLeftOp();
        var right = defStmt.getRightOp();
        var leftVars = finder.findVars(left);
        var rightVars = finder.findVars(right);
        if (leftVars != null && !leftVars.isEmpty()) {
            return Optional.of(new VarDependency(m, leftVars.get(0), rightVars));
        }
        return Optional.empty();
    }

}
