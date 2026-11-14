package prologanalyzer;

import java.util.*;

public enum Mode {

    NON_TERMINATION("Prove non-termination of input Prolog file.", Set.of(Flag.INPUT)),

    TERMINATION("Prove termination of input Prolog file.", Set.of(Flag.INPUT));

    public final String description;

    public final Set<Flag> parameters;

    private Mode(final String description, final Set<Flag> parameters) {
        this.description = description;
        this.parameters = parameters;
    }

}
