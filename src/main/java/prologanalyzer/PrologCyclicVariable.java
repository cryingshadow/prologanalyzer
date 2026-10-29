package prologanalyzer;

import java.util.*;

import prologparser.*;

public final class PrologCyclicVariable extends PrologTerm {

    public PrologCyclicVariable() {
        super("**", List.of());
    }

    @Override
    public boolean equals(final Object o) {
        return o instanceof PrologCyclicVariable;
    }

    @Override
    public int hashCode() {
        return 421984;
    }

}
