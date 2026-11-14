package prologanalyzer.evaluationrules;

import java.util.*;

import prologanalyzer.*;
import prologanalyzer.structure.*;
import prologparser.*;

public class OnlyEvalRule implements SymbolicEvaluationRule {

    public static final OnlyEvalRule NON_TERMINATION_ONLY_EVAL = new OnlyEvalRule(false);

    public static final OnlyEvalRule TERMINATION_ONLY_EVAL = new OnlyEvalRule(true);

    private final boolean termination;

    private OnlyEvalRule(final boolean termination) {
        this.termination = termination;
    }

    @Override
    public List<SymbolicEvaluationResult> apply(final AbstractState state, final GlobalState global) {
        if (state.goal().isEmpty()) {
            return List.of();
        }
        final GoalElement firstGoal = state.goal().getFirst();
        if (!firstGoal.hasClause()) {
            return List.of();
        }
        final PrologTerm firstConjunct = Terms.getFirstConjunct(firstGoal.getTerm());
        final PrologTerm tailOfConjunction = Terms.removeFirstConjunct(firstGoal.getTerm());
        final Optional<PrologSubstitution> optionalMGU =
            MGUComputer.computeMGUWithOnlyFreshVariables(firstConjunct, firstGoal.getClause().head(), global.fridge());
        if (optionalMGU.isEmpty()) {
            return List.of();
        }
        final PrologSubstitution mgu = optionalMGU.get();
        if (mgu.hasPotentialUnificationConflict() || !state.knowledge().isCompatibleWithNonUnification(mgu)) {
            return List.of();
        }
        final PrologSubstitution mguOnGround = state.knowledge().restrictToGround(mgu);
        final PrologSubstitution mguWithAdditionalRenamings =
            this.termination ?
                state.knowledge().addRenamings(
                    mgu,
                    firstConjunct,
                    tailOfConjunction,
                    firstGoal.getClause(),
                    global.fridge()
                ) :
                    mgu;
        final Optional<PrologTerm> body = firstGoal.getClause().body();
        final List<GoalElement> newGoal = new ArrayList<GoalElement>();
        if (body.isEmpty()) {
            newGoal.add(new TermGoalElement(mguWithAdditionalRenamings.apply(tailOfConjunction)));
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
        return List.of(
            new SymbolicEvaluationResult(
                new AbstractState(newGoal, state.knowledge().apply(mgu, firstGoal.getClause())),
                mguWithAdditionalRenamings
            )
        );
    }

    @Override
    public String getDOTColor() {
        return "lightblue";
    }

    @Override
    public String toJSON() {
        return String.format("ONLY_EVAL(%s)", this.termination);
    }

    @Override
    public String toString() {
        return "ONLY_EVAL";
    }

}
