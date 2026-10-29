package prologanalyzer.evaluationrules;

import java.util.*;

import prologanalyzer.*;
import prologparser.*;

public class BacktrackRule implements SymbolicEvaluationRule {

    @Override
    public List<AbstractState> apply(final AbstractState state, final FreshNameGenerator fridge) {
        final GoalElement firstGoal = state.goal().getFirst();
        final List<GoalElement> newGoal = new ArrayList<GoalElement>(state.goal());
        newGoal.remove(0);
        if (firstGoal.isScopeMarker()) {
            return List.of(new AbstractState(newGoal, state.knowledge()));
        }
        if (!firstGoal.hasClause()) {
            return List.of();
        }
        final PrologTerm firstConjunct = Terms.getFirstConjunct(firstGoal.getTerm());
        final Optional<PrologSubstitution> optionalMGU =
            Terms.computeMGU(firstConjunct, firstGoal.getClause().head(), fridge, state.knowledge());
        if (optionalMGU.isPresent()) {
            return List.of();
        }
        return List.of(
            new AbstractState(newGoal, state.knowledge().addNonUnification(firstConjunct, firstGoal.getClause().head()))
        );
    }

}
