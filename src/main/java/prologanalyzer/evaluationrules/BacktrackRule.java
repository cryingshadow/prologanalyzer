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
            MGUComputer.computeMGUWithOnlyFreshVariables(firstConjunct, firstGoal.getClause().head(), fridge);
        if (optionalMGU.isPresent() && state.knowledge().isCompatibleWithNonUnification(optionalMGU.get())) {
            return List.of();
        }
        return List.of(new AbstractState(newGoal, state.knowledge()));
    }

}
