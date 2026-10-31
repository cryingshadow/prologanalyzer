package prologanalyzer.evaluationrules;

import java.util.*;

import prologanalyzer.*;
import prologanalyzer.structure.*;
import prologparser.*;

public class CaseRule implements SymbolicEvaluationRule {

    @Override
    public List<AbstractState> apply(final AbstractState state, final GlobalState global) {
        final GoalElement firstGoal = state.goal().getFirst();
        if (!firstGoal.hasTerm() || firstGoal.hasClause()) {
            return List.of();
        }
        final PrologTerm firstConjunct = Terms.getFirstConjunct(firstGoal.getTerm());
        if (PrologBuiltInPredicates.INSTANCE.isBuiltInPredicate(firstConjunct)) {
            return List.of();
        }
        final List<PrologClause> slice = ProgramSlicer.slice(firstConjunct, global.program());
        if (slice.isEmpty()) {
            return List.of();
        }
        final List<GoalElement> newGoal = new ArrayList<GoalElement>();
        final int scope = global.scoper().getFreshScope();
        for (final PrologClause clause : slice) {
            newGoal.add(new LabeledTermGoalElement(firstGoal.getTerm(), Terms.fresh(clause, global.fridge()), scope));
        }
        newGoal.add(new ScopeMarker(scope));
        newGoal.addAll(state.goal().stream().skip(1).toList());
        return List.of(new AbstractState(newGoal, state.knowledge()));
    }

    @Override
    public String toString() {
        return "CASE";
    }

}
