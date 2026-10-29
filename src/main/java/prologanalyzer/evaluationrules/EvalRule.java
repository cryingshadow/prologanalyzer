package prologanalyzer.evaluationrules;

import java.util.*;

import prologanalyzer.*;
import prologparser.*;

public class EvalRule implements SymbolicEvaluationRule {

    @Override
    public List<AbstractState> apply(final AbstractState state, final FreshNameGenerator fridge) {
        final GoalElement firstGoal = state.goal().getFirst();
        if (!firstGoal.hasClause()) {
            return List.of();
        }
        final PrologTerm firstConjunct = Terms.getFirstConjunct(firstGoal.getTerm());
        final Optional<PrologSubstitution> optionalMGU =
            Terms.computeMGUWithOnlyFreshVariables(firstConjunct, firstGoal.getClause().head(), fridge);
        //TODO check knowledge
        if (optionalMGU.isEmpty()) {
            return List.of();
        }
        final PrologSubstitution mgu = optionalMGU.get();
        final Optional<PrologTerm> body = firstGoal.getClause().body();
        final List<AbstractState> result = new ArrayList<AbstractState>();
        if (body.isEmpty()) {
            final List<GoalElement> newGoal = new ArrayList<GoalElement>(state.goal());
            newGoal.set(0, new TermGoalElement(Terms.removeFirstConjunct(firstGoal.getTerm())));
            result.add(new AbstractState(newGoal, state.knowledge().apply(mgu)));
        } else {
            final PrologTerm newFirstConjunct = mgu.apply(body.get());
            final List<GoalElement> newGoal = new ArrayList<GoalElement>(state.goal());
            newGoal.set(0, new TermGoalElement(Terms.replaceFirstConjunct(firstGoal.getTerm(), newFirstConjunct)));
            result.add(new AbstractState(newGoal, state.knowledge().apply(mgu)));
        }
        if (!mgu.hasPotentialUnificationConflict()) {
            return List.of();
        }
        final List<GoalElement> newGoal = new ArrayList<GoalElement>(state.goal());
        newGoal.remove(0);
        result.add(
            new AbstractState(newGoal, state.knowledge().addNonUnification(firstConjunct, firstGoal.getClause().head()))
        );
        return result;
    }

}
