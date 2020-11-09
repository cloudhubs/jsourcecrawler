package org.baylor.ecs.cloudhubs.sourcecrawler.helper;

import soot.Value;
import soot.jimple.internal.*;

import java.util.ArrayList;
import java.util.List;

public class VarFinder {
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
        }
        return null;
    }
}
