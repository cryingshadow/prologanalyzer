package prologanalyzer;

import clit.*;

public enum Flag implements Parameter {

    DOT("d", "dot", "Output file in DOT format."),

    INPUT("i", "input", "Input Prolog file."),

    JSON("j", "json", "Output file in JSON format."),

    MODE("m", "mode", "Execution mode (NON_TERMINATION, TERMINATION)."),

    TIMEOUT("t", "timeout", "Timeout in ms.");

    private final String description;

    private final String longName;

    private final String shortName;

    private Flag(final String shortName, final String longName, final String description) {
        this.shortName = shortName;
        this.longName = longName;
        this.description = description;
    }

    @Override
    public String description() {
        return this.description;
    }

    @Override
    public String longName() {
        return this.longName;
    }

    @Override
    public String shortName() {
        return this.shortName;
    }

}
