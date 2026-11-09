package prologanalyzer.structure;

import prologanalyzer.evaluationrules.*;

public record SymbolicEvaluationExpansion(
    AbstractState state,
    SymbolicEvaluationRule rule,
    PrologSubstitution substitution
) {

}
