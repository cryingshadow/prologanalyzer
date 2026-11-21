package prologanalyzer.evaluationrules;

import java.util.*;

import prologanalyzer.*;
import prologanalyzer.structure.*;
import prologparser.*;

public class BuiltInFailRule implements SymbolicEvaluationRule {

    public static final BuiltInFailRule INSTANCE = new BuiltInFailRule();

    private BuiltInFailRule() {}

    @Override
    public List<SymbolicEvaluationResult> apply(final AbstractState state, final GlobalState global) {
        if (state.goal.isEmpty()) {
            return List.of();
        }
        final GoalElement firstGoal = state.goal.getFirst();
        if (!firstGoal.hasTerm() || firstGoal.hasClause()) {
            return List.of();
        }
        final PrologTerm term = firstGoal.getTerm();
        final PrologTerm firstConjunct = Terms.getFirstConjunct(term);
        if (!Terms.haveSameFunctionSymbol(PrologBuiltInPredicates.FAIL, firstConjunct)) {
            return List.of();
        }
        final List<GoalElement> newGoal = new ArrayList<GoalElement>(state.goal);
        newGoal.remove(0);
        return List.of(
            new SymbolicEvaluationResult(
                new AbstractState(newGoal, state.knowledge).restrictKnowledgeToExistingVariables(),
                new PrologSubstitution()
            )
        );
    }

    @Override
    public String getDOTColor() {
        return "lightgreen";
    }

    @Override
    public String toString() {
        return "BUILT_IN_FAIL";
    }

}
