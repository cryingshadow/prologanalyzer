package prologanalyzer.evaluationrules;

import java.util.*;

import prologanalyzer.*;
import prologanalyzer.structure.*;
import prologparser.*;

public class UndefinedErrorRule implements SymbolicEvaluationRule {

    @Override
    public List<AbstractState> apply(final AbstractState state, final GlobalState global) {
        final GoalElement firstGoal = state.goal().getFirst();
        if (!firstGoal.hasTerm()) {
            return List.of();
        }
        final PrologTerm firstConjunct = Terms.getFirstConjunct(firstGoal.getTerm());
        if (
            PrologBuiltInPredicates.INSTANCE.isBuiltInPredicate(firstConjunct)
            || !ProgramSlicer.slice(firstConjunct, global.program()).isEmpty()
        ) {
            return List.of();
        }
        return List.of(new AbstractState(List.of(), state.knowledge()));
    }

    @Override
    public String toString() {
        return "UNDEFINED_ERROR";
    }

}
