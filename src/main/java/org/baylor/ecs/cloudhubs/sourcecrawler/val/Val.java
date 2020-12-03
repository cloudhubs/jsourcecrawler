package org.baylor.ecs.cloudhubs.sourcecrawler.val;

import lombok.AllArgsConstructor;
import soot.*;

@lombok.Value
@AllArgsConstructor
public class Val{
    Value value;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Val val = (Val) o;

        return value.toString().equals(val.value.toString());
    }

    @Override
    public int hashCode() {
        return value.equivHashCode();
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
