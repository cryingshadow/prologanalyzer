package prologanalyzer.evaluationrules;

import java.util.*;

import prologanalyzer.structure.*;

public class FailureRule implements SymbolicEvaluationRule {

    @Override
    public List<AbstractState> apply(final AbstractState state, final GlobalState global) {
        final GoalElement firstGoal = state.goal().getFirst();
        if (!firstGoal.isScopeMarker()) {
            return List.of();
        }
        final List<GoalElement> newGoal = new ArrayList<GoalElement>(state.goal());
        newGoal.remove(0);
        return List.of(new AbstractState(newGoal, state.knowledge()));
    }

    @Override
    public String toString() {
        return "FAILURE";
    }

}
