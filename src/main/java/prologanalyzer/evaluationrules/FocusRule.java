package prologanalyzer.evaluationrules;

import java.util.*;

import prologanalyzer.*;
import prologanalyzer.structure.*;
import prologparser.*;

public class FocusRule implements SymbolicEvaluationRule {

    public static final FocusRule INSTANCE = new FocusRule();

    private FocusRule() {}

    @Override
    public List<SymbolicEvaluationResult> apply(final AbstractState state, final GlobalState global) {
        if (state.goal().size() != 1 || !state.goal().getFirst().hasTerm() || state.goal().getFirst().hasClause()) {
            return List.of();
        }
        final PrologTerm term = state.goal().getFirst().getTerm();
        if (!Terms.isConjunction(term) || Terms.getArity(term) < 2) {
            return List.of();
        }
        final PrologTerm headOfConjunction = Terms.getFirstConjunct(term);
        if (Terms.isCut(headOfConjunction)) {
            return List.of();
        }
        return List.of(
            new SymbolicEvaluationResult(
                new AbstractState(List.of(new TermGoalElement(headOfConjunction)), state.knowledge()),
                new PrologSubstitution()
            )
        );
    }

    @Override
    public String getDOTColor() {
        return "violet";
    }

    @Override
    public String toString() {
        return "FOCUS";
    }
}
