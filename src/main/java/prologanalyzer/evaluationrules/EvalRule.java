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
            MGUComputer.computeMGUWithOnlyFreshVariables(firstConjunct, firstGoal.getClause().head(), fridge);
        if (optionalMGU.isEmpty()) {
            return List.of();
        }
        final PrologSubstitution mgu = optionalMGU.get();
        if (!mgu.hasPotentialUnificationConflict() || !state.knowledge().isCompatibleWithNonUnification(mgu)) {
            return List.of();
        }
        final PrologSubstitution mguOnGround = state.knowledge().restrictToGround(mgu);
        final PrologSubstitution mguWithAdditionalRenamings =
            state.knowledge().addRenamings(mgu, firstConjunct, firstGoal.getClause());
        final Optional<PrologTerm> body = firstGoal.getClause().body();
        final List<AbstractState> result = new ArrayList<AbstractState>();
        final List<GoalElement> newGoalEval = new ArrayList<GoalElement>();
        if (body.isEmpty()) {
            newGoalEval.add(
                new TermGoalElement(mguWithAdditionalRenamings.apply(Terms.removeFirstConjunct(firstGoal.getTerm())))
            );
        } else {
            newGoalEval.add(
                new TermGoalElement(
                    mguWithAdditionalRenamings.apply(
                        Terms.replaceFirstConjunct(
                            firstGoal.getTerm(),
                            Terms.transformWithScope(body.get(), firstGoal.getScope())
                        )
                    )
                )
            );
        }
        newGoalEval.addAll(state.goal().stream().skip(1).map(goal -> goal.apply(mguOnGround)).toList());
        result.add(new AbstractState(newGoalEval, state.knowledge().apply(mgu, firstGoal.getClause())));
        final List<GoalElement> newGoalBacktrack = new ArrayList<GoalElement>(state.goal());
        newGoalBacktrack.remove(0);
        result.add(
            new AbstractState(
                newGoalBacktrack,
                state
                .knowledge()
                .addNonUnification(firstConjunct, firstGoal.getClause().head())
                .addFree(Terms.createSetOfAllNonAbstractVariables(firstGoal.getClause().head()))
            )
        );
        return result;
    }

}
