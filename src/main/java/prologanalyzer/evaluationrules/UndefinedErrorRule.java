package prologanalyzer.evaluationrules;

import java.util.*;

import prologanalyzer.*;
import prologanalyzer.structure.*;
import prologparser.*;

public class UndefinedErrorRule implements SymbolicEvaluationRule {

    public static final UndefinedErrorRule INSTANCE = new UndefinedErrorRule();

    private UndefinedErrorRule() {}

    @Override
    public List<SymbolicEvaluationResult> apply(final AbstractState state, final GlobalState global) {
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
        return List.of(
            new SymbolicEvaluationResult(new AbstractState(List.of(), state.knowledge()), new PrologSubstitution())
        );
    }

    @Override
    public String getDOTColor() {
        return "lightgreen";
    }

    @Override
    public String toString() {
        return "UNDEFINED_ERROR";
    }

}
