package prologanalyzer.structure;

import prologanalyzer.evaluationrules.*;

public record SymbolicEvaluationEdgeLabel(SymbolicEvaluationRule rule, PrologSubstitution substitution) {

    @Override
    public String toString() {
        return String.format(
            "%s%s",
            this.rule().toString(),
            this.substitution().isEmpty() ? "" : " " + this.substitution().toString()
        );
    }

}
