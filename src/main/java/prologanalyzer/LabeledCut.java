package prologanalyzer;

import java.util.*;

import prologparser.*;

public class LabeledCut extends PrologTerm {

    public final int scope;

    public LabeledCut(final int scope) {
        super("!", List.of());
        this.scope = scope;
    }

}
