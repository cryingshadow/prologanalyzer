package prologanalyzer;

import java.util.*;

import prologparser.*;

public class PrologCyclicTerm extends PrologTerm {

    public static final PrologCyclicVariable CYCLIC_VARIABLE = new PrologCyclicVariable();

    private final PrologTerm cycle;

    // TODO
    private boolean showCycle;

    public PrologCyclicTerm(final PrologTerm cycleParam, final PrologVariable cycleVar) {
        this(new PrologSubstitution(Map.of(cycleVar, PrologCyclicTerm.CYCLIC_VARIABLE)).apply(cycleParam));
    }

    private PrologCyclicTerm(final PrologTerm cycleParam) {
        super(cycleParam.name, cycleParam.arguments);
        this.cycle = cycleParam;
        this.showCycle = true;
    }

    //TODO equals, hashCode

    @Override
    public String toString() {
        if (this.showCycle) {
            this.showCycle = false;
            final String res = this.cycle.toString();
            this.showCycle = true;
            return res;
        } else {
            return "**";
        }
    }

}
