package prologanalyzer.evaluationrules;

import java.util.*;

import prologanalyzer.structure.*;

public class DropRule implements SymbolicEvaluationRule {

    private final int keep;

    public DropRule(final int keep) {
        this.keep = keep;
    }

    @Override
    public List<SymbolicEvaluationResult> apply(final AbstractState state, final GlobalState global) {
        final List<GoalElement> keptGoals = state.goal().stream().limit(this.keep).toList();
        final List<GoalElement> lastGoals = state.goal().stream().skip(this.keep).toList();
        if (keptGoals.isEmpty() || lastGoals.isEmpty()) {
            return List.of();
        }
        return List.of(
            new SymbolicEvaluationResult(new AbstractState(keptGoals, state.knowledge()), new PrologSubstitution())
        );
    }

    @Override
    public String getDOTColor() {
        return "violet";
    }

    @Override
    public String toJSON() {
        return String.format("DROP(%d)", this.keep);
    }

    @Override
    public String toString() {
        return "DROP";
    }

}
