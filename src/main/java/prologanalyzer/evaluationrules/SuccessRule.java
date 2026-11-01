package prologanalyzer.evaluationrules;

import java.util.*;

import prologanalyzer.structure.*;

public class SuccessRule implements SymbolicEvaluationRule {

    @Override
    public List<SymbolicEvaluationResult> apply(final AbstractState state, final GlobalState global) {
        final GoalElement firstGoal = state.goal().getFirst();
        if (!firstGoal.hasTerm() || firstGoal.getTerm() != null) {
            return List.of();
        }
        final List<GoalElement> newGoal = new ArrayList<GoalElement>(state.goal());
        newGoal.remove(0);
        return List.of(
            new SymbolicEvaluationResult(new AbstractState(newGoal, state.knowledge()), new PrologSubstitution())
        );
    }

    @Override
    public String toString() {
        return "SUCCESS";
    }

}
