package prologanalyzer.evaluationrules;

import java.util.*;

import prologanalyzer.*;
import prologanalyzer.structure.*;
import prologparser.*;

public class ParallelRule implements SymbolicEvaluationRule {

    public static Set<Integer> computeActiveCuts(final List<GoalElement> state) {
        final Set<Integer> result = new LinkedHashSet<Integer>();
        for (int i = 0; i < state.size(); i++) {
            final GoalElement element = state.get(i);
            if (!element.isScopeMarker()) {
                result.addAll(Terms.computeActiveCuts(element.getTerm()));
                if (element.hasClause() && element.getClause().body().isPresent()) {
                    final PrologTerm body = element.getClause().body().get();
                    if (Terms.containsCut(body)) {
                        result.add(element.getScope());
                    }
                }
            }
        }
        return result;
    }

    private final int splitIndex;

    public ParallelRule(final int splitIndex) {
        this.splitIndex = splitIndex;
    }

    @Override
    public List<SymbolicEvaluationResult> apply(final AbstractState state, final GlobalState global) {
        final List<GoalElement> firstGoals = state.goal().stream().limit(this.splitIndex).toList();
        final List<GoalElement> lastGoals = state.goal().stream().skip(this.splitIndex).toList();
        if (firstGoals.isEmpty() || lastGoals.isEmpty()) {
            return List.of();
        }
        final Set<Integer> activeCuts = ParallelRule.computeActiveCuts(firstGoals);
        if (
            lastGoals
            .stream()
            .skip(1)
            .limit(Math.max(0, lastGoals.size() - 2))
            .anyMatch(element -> element.isScopeMarker() && activeCuts.contains(element.getScope()))
        ) {
            return List.of();
        }
        return List.of(
            new SymbolicEvaluationResult(new AbstractState(firstGoals, state.knowledge()), new PrologSubstitution()),
            new SymbolicEvaluationResult(new AbstractState(lastGoals, state.knowledge()), new PrologSubstitution())
        );
    }

    @Override
    public String getDOTColor() {
        return "violet";
    }

    @Override
    public String toJSON() {
        return String.format("PARALLEL(%d)", this.splitIndex);
    }

    @Override
    public String toString() {
        return "PARALLEL";
    }

}
