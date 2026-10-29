package prologanalyzer.evaluationrules;

import java.util.*;

import prologanalyzer.*;
import prologparser.*;

public class OnlyEvalRule implements SymbolicEvaluationRule {

    @Override
    public List<AbstractState> apply(final AbstractState state, final FreshNameGenerator fridge) {
        final GoalElement firstGoal = state.goal().getFirst();
        if (!firstGoal.hasClause()) {
            return List.of();
        }
        final PrologTerm firstConjunct = Terms.getFirstConjunct(firstGoal.getTerm());
        final Optional<PrologSubstitution> optionalMGU =
            Terms.computeMGU(firstConjunct, firstGoal.getClause().head(), fridge, state.knowledge());
        if (optionalMGU.isEmpty()) {
            return List.of();
        }
        final PrologSubstitution mgu = optionalMGU.get();
        if (mgu.hasPotentialUnificationConflict()) {
            return List.of();
        }
        final Optional<PrologTerm> body = firstGoal.getClause().body();
        if (body.isEmpty()) {
            final List<GoalElement> newGoal = new ArrayList<GoalElement>(state.goal());
            newGoal.set(0, new TermGoalElement(Terms.removeFirstConjunct(firstGoal.getTerm())));
            return List.of(new AbstractState(newGoal, state.knowledge().apply(mgu)));
        } else {
            final PrologTerm newFirstConjunct = mgu.apply(body.get());
            final List<GoalElement> newGoal = new ArrayList<GoalElement>(state.goal());
            newGoal.set(0, new TermGoalElement(Terms.replaceFirstConjunct(firstGoal.getTerm(), newFirstConjunct)));
            return List.of(new AbstractState(newGoal, state.knowledge().apply(mgu)));
        }
    }

}
