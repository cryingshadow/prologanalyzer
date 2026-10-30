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
            MGUComputer.computeMGUWithOnlyFreshVariables(firstConjunct, firstGoal.getClause().head(), fridge);
        if (optionalMGU.isEmpty()) {
            return List.of();
        }
        final PrologSubstitution mgu = optionalMGU.get();
        if (mgu.hasPotentialUnificationConflict() || !state.knowledge().isCompatibleWithNonUnification(mgu)) {
            return List.of();
        }
        final PrologSubstitution mguOnGround = state.knowledge().restrictToGround(mgu);
        final PrologSubstitution mguWithAdditionalRenamings =
            state.knowledge().addRenamings(mgu, firstConjunct, firstGoal.getClause());
        final Optional<PrologTerm> body = firstGoal.getClause().body();
        final List<GoalElement> newGoal = new ArrayList<GoalElement>();
        if (body.isEmpty()) {
            newGoal.add(
                new TermGoalElement(mguWithAdditionalRenamings.apply(Terms.removeFirstConjunct(firstGoal.getTerm())))
            );
        } else {
            newGoal.add(
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
        newGoal.addAll(state.goal().stream().skip(1).map(goal -> goal.apply(mguOnGround)).toList());
        return List.of(new AbstractState(newGoal, state.knowledge().apply(mgu, firstGoal.getClause())));
    }

}
