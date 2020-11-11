package org.baylor.ecs.cloudhubs.sourcecrawler.helper;


import com.microsoft.z3.*;
import lombok.AllArgsConstructor;
import org.baylor.ecs.cloudhubs.sourcecrawler.model.PathCondition;
import soot.Value;
import soot.jimple.IntConstant;
import soot.jimple.internal.*;
import soot.jimple.parser.node.TBoolConstant;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class Z3Converter {
    Context ctx;

    public List<AST> convert(List<PathCondition> conditions) {
        var asts = new ArrayList<AST>();
        for (var condition : conditions) {
            var conv = convertValue(condition.getCondition());
            if (conv != null) {
                asts.add(conv);
            }
        }
        return asts;
    }

    public AST convertValue(JLtExpr expr) {
        var left = convertValue(expr.getOp1());
        var right = convertValue(expr.getOp2());
        if (left instanceof ArithExpr && right instanceof ArithExpr) {
            return ctx.mkLt((ArithExpr)left, (ArithExpr)right);
        }
        return null;
    }

    public AST convertValue(JGtExpr expr) {
        var left = convertValue(expr.getOp1());
        var right = convertValue(expr.getOp2());
        if (left instanceof ArithExpr && right instanceof ArithExpr) {
            return ctx.mkGt((ArithExpr)left, (ArithExpr)right);
        }
        return null;
    }

    public AST convertValue(JLeExpr expr) {
        var left = convertValue(expr.getOp1());
        var right = convertValue(expr.getOp2());
        if (left instanceof ArithExpr && right instanceof ArithExpr) {
            return ctx.mkLe((ArithExpr)left, (ArithExpr)right);
        }
        return null;
    }

    public AST convertValue(JGeExpr expr) {
        var left = convertValue(expr.getOp1());
        var right = convertValue(expr.getOp2());
        if (left instanceof ArithExpr && right instanceof ArithExpr) {
            return ctx.mkGe((ArithExpr)left, (ArithExpr)right);
        }
        return null;
    }

    public AST convertValue(JEqExpr expr) {
        var left = convertValue(expr.getOp1());
        var right = convertValue(expr.getOp2());
        return ctx.mkEq((Expr)left, (Expr)right);
    }

    public AST convertValue(JNeExpr expr) {
        var left = convertValue(expr.getOp1());
        var right = convertValue(expr.getOp2());
        return ctx.mkNot(ctx.mkEq((Expr)left, (Expr)right));
    }

    public AST convertValue(JMulExpr expr) {
        var left = convertValue(expr.getOp1());
        var right = convertValue(expr.getOp2());
        if (left instanceof IntExpr && right instanceof IntExpr) {
            return ctx.mkMul((IntExpr)left, (IntExpr)right);
        }
        return null;
    }

    public AST convertValue(JAddExpr expr) {
        var left = convertValue(expr.getOp1());
        var right = convertValue(expr.getOp2());
        if (left instanceof IntExpr && right instanceof IntExpr) {
            return ctx.mkAdd((IntExpr)left, (IntExpr)right);
        }
        return null;
    }

    public AST convertValue(JSubExpr expr) {
        var left = convertValue(expr.getOp1());
        var right = convertValue(expr.getOp2());
        if (left instanceof IntExpr && right instanceof IntExpr) {
            return ctx.mkSub((IntExpr)left, (IntExpr)right);
        }
        return null;
    }

    public AST convertValue(JDivExpr expr) {
        var left = convertValue(expr.getOp1());
        var right = convertValue(expr.getOp2());
        if (left instanceof IntExpr && right instanceof IntExpr) {
            return ctx.mkDiv((IntExpr)left, (IntExpr)right);
        }
        return null;
    }

    public AST convertValue(JRemExpr expr) {
        var left = convertValue(expr.getOp1());
        var right = convertValue(expr.getOp2());
        if (left instanceof IntExpr && right instanceof IntExpr) {
            return ctx.mkRem((IntExpr)left, (IntExpr)right);
        }
        return null;
    }

    public AST convertValue(JNegExpr expr) {
        var v = convertValue(expr.getOp());
        if (v instanceof BoolExpr) {
            return ctx.mkNot((BoolExpr)v);
        }
        return null;
    }

    public AST convertValue(JimpleLocal expr) {
        return ctx.mkConst(expr.getName(), sootTypeToZ3(expr));
    }

    public Sort sootTypeToZ3(JimpleLocal expr) {
        var str = expr.getType().toString();
        switch (str) {
            case "int":
            case "Integer":
                return ctx.mkIntSort();
            case "boolean":
            case "Boolean":
                return ctx.mkBoolSort();
            default: return null;
        }
    }

    public AST convertValue(IntConstant expr) {
        return ctx.mkInt(expr.value);
    }

    public AST convertValue(Value expr) {
        if (expr instanceof JimpleLocal) {
            return convertValue((JimpleLocal) expr);
        } else if (expr instanceof JNegExpr) {
            return convertValue((JNegExpr) expr);
        } else if (expr instanceof JEqExpr) {
            return convertValue((JEqExpr) expr);
        } else if (expr instanceof JNeExpr) {
            return convertValue((JNeExpr) expr);
        } else if (expr instanceof JRemExpr) {
            return convertValue((JRemExpr) expr);
        } else if (expr instanceof JDivExpr) {
            return convertValue((JDivExpr) expr);
        } else if (expr instanceof JSubExpr) {
            return convertValue((JSubExpr) expr);
        } else if (expr instanceof JAddExpr) {
            return convertValue((JAddExpr) expr);
        } else if (expr instanceof JMulExpr) {
            return convertValue((JMulExpr) expr);
        }else if (expr instanceof JGeExpr) {
            return convertValue((JGeExpr) expr);
        } else if (expr instanceof JGtExpr) {
            return convertValue((JGtExpr) expr);
        } else if (expr instanceof JLtExpr) {
            return convertValue((JLtExpr) expr);
        } else if (expr instanceof JLeExpr) {
            return convertValue((JLeExpr) expr);
        } else if (expr instanceof IntConstant) {
            return convertValue((IntConstant) expr);
        }
        return null;
    }
}
