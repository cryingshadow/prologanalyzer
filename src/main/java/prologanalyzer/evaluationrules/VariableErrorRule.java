package prologanalyzer.evaluationrules;

import java.util.*;

import prologanalyzer.*;
import prologanalyzer.structure.*;
import prologparser.*;

public class VariableErrorRule implements SymbolicEvaluationRule {

    public static final VariableErrorRule INSTANCE = new VariableErrorRule();

    private VariableErrorRule() {}

    @Override
    public List<SymbolicEvaluationResult> apply(final AbstractState state, final GlobalState global) {
        final GoalElement firstGoal = state.goal().getFirst();
        if (!firstGoal.hasTerm()) {
            return List.of();
        }
        final PrologTerm firstConjunct = Terms.getFirstConjunct(firstGoal.getTerm());
        if (
            !"call".equals(firstConjunct.name)
            || Terms.getArity(firstConjunct) != 1
            || !Terms.isNonAbstractVariable(firstConjunct.arguments.getFirst())
        ) {
            return List.of();
        }
        return List.of(
            new SymbolicEvaluationResult(new AbstractState(List.of(), state.knowledge()), new PrologSubstitution())
        );
    }

    @Override
    public String toString() {
        return "VARIABLE_ERROR";
    }

}
