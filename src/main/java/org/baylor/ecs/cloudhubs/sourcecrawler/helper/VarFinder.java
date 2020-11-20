package org.baylor.ecs.cloudhubs.sourcecrawler.helper;

import soot.Value;
import soot.jimple.internal.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class VarFinder {
    public List<String> findVars(AbstractInvokeExpr invokeExpr) {
        return invokeExpr.getArgs()
            .stream()
            .flatMap(arg -> findVars(arg).stream())
            .collect(Collectors.toList());
    }

    public List<String> findVars(JimpleLocal jimpleLocal) {
        var list = new ArrayList<String>();
        list.add(jimpleLocal.getName());
        return list;
    }

    public List<String> findVars(AbstractBinopExpr addExpr) {
        var left = findVars(addExpr.getOp1());
        var right = findVars(addExpr.getOp2());
        if (left != null && right != null) {
            left.addAll(right);
            return left;
        }
        return right;
    }

    public List<String> findVars(Value value) {
        if (value instanceof AbstractBinopExpr) {
            return findVars((AbstractBinopExpr)value);
        } else if (value instanceof JimpleLocal) {
            return findVars((JimpleLocal)value);
        } else if (value instanceof AbstractInvokeExpr) {
            return findVars((AbstractInvokeExpr)value);
        }
        return null;
    }
}
