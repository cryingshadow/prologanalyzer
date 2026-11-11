package prologanalyzer.evaluationrules;

import java.util.*;

import prologanalyzer.structure.*;

public class SuccessRule implements SymbolicEvaluationRule {

    public static final SuccessRule INSTANCE = new SuccessRule();

    private SuccessRule() {}

    @Override
    public List<SymbolicEvaluationResult> apply(final AbstractState state, final GlobalState global) {
        if (state.goal().isEmpty()) {
            return List.of();
        }
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
    public String getDOTColor() {
        return "lightgreen";
    }

    @Override
    public String toString() {
        return "SUCCESS";
    }

}
