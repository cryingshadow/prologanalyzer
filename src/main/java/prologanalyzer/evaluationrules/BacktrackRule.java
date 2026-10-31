package prologanalyzer.evaluationrules;

import java.util.*;

import prologanalyzer.*;
import prologanalyzer.structure.*;
import prologparser.*;

public class BacktrackRule implements SymbolicEvaluationRule {

    @Override
    public List<AbstractState> apply(final AbstractState state, final GlobalState global) {
        final GoalElement firstGoal = state.goal().getFirst();
        if (!firstGoal.hasClause()) {
            return List.of();
        }
        final List<GoalElement> newGoal = new ArrayList<GoalElement>(state.goal());
        newGoal.remove(0);
        final PrologTerm firstConjunct = Terms.getFirstConjunct(firstGoal.getTerm());
        final Optional<PrologSubstitution> optionalMGU =
            MGUComputer.computeMGUWithOnlyFreshVariables(firstConjunct, firstGoal.getClause().head(), global.fridge());
        if (optionalMGU.isPresent() && state.knowledge().isCompatibleWithNonUnification(optionalMGU.get())) {
            return List.of();
        }
        return List.of(new AbstractState(newGoal, state.knowledge()));
    }

    @Override
    public String toString() {
        return "BACKTRACK";
    }

}
