package prologanalyzer.structure;

import java.util.*;

import prologparser.*;

public class LabeledCut extends PrologTerm {

    public final int scope;

    public LabeledCut(final int scope) {
        super("!", List.of());
        this.scope = scope;
    }

    @Override
    public String toString() {
        return "!_" + this.scope;
    }

}
