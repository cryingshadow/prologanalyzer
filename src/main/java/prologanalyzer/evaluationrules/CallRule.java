package prologanalyzer.evaluationrules;

import java.util.*;

import prologanalyzer.*;
import prologanalyzer.structure.*;
import prologparser.*;

public class CallRule implements SymbolicEvaluationRule {

    public static final CallRule INSTANCE = new CallRule();

    private CallRule() {}

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
            || Terms.isNonAbstractVariable(firstConjunct.arguments.getFirst())
        ) {
            return List.of();
        }
        final PrologTerm call = firstConjunct.arguments.getFirst();
        if (call instanceof PrologCyclicTerm) {
            throw new UnsupportedOperationException(
                "Handling of cyclic terms in meta programming has not been implemented yet!"
            );
        }
        if (Terms.hasAbstractVariableOnPredicationPosition(call)) {
            throw new UnsupportedOperationException("Calls of arbitrary terms cannot be analyzed!");
        }
        final List<GoalElement> newGoal = new ArrayList<GoalElement>();
        final int scope = global.scoper().getFreshScope();
        newGoal.add(
            new TermGoalElement(Terms.replaceFirstConjunct(firstGoal.getTerm(), Terms.transformWithScope(call, scope)))
        );
        newGoal.add(new ScopeMarker(scope));
        newGoal.addAll(state.goal().stream().skip(1).toList());
        return List.of(
            new SymbolicEvaluationResult(new AbstractState(newGoal, state.knowledge()), new PrologSubstitution())
        );
    }

    @Override
    public String getDOTColor() {
        return "lightcyan";
    }

    @Override
    public String toString() {
        return "CALL";
    }

}
