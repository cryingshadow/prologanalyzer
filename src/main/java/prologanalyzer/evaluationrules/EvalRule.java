package prologanalyzer.evaluationrules;

import java.util.*;

import prologanalyzer.*;
import prologanalyzer.structure.*;
import prologparser.*;

public class EvalRule implements SymbolicEvaluationRule {

    public static final EvalRule NON_TERMINATION_EVAL = new EvalRule(false);

    public static final EvalRule TERMINATION_EVAL = new EvalRule(true);

    private final boolean termination;

    private EvalRule(final boolean termination) {
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
        if (!mgu.hasPotentialUnificationConflict() || !state.knowledge().isCompatibleWithNonUnification(mgu)) {
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
        final List<SymbolicEvaluationResult> result = new ArrayList<SymbolicEvaluationResult>();
        final List<GoalElement> newGoalEval = new ArrayList<GoalElement>();
        if (body.isEmpty()) {
            newGoalEval.add(new TermGoalElement(mguWithAdditionalRenamings.apply(tailOfConjunction)));
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
        result.add(
            new SymbolicEvaluationResult(
                new AbstractState(newGoalEval, state.knowledge().apply(mgu, firstGoal.getClause())),
                mguWithAdditionalRenamings
            )
        );
        final List<GoalElement> newGoalBacktrack = new ArrayList<GoalElement>(state.goal());
        newGoalBacktrack.remove(0);
        result.add(
            new SymbolicEvaluationResult(
                new AbstractState(
                    newGoalBacktrack,
                    state
                    .knowledge()
                    .addNonUnification(firstConjunct, firstGoal.getClause().head())
                    .addFree(Terms.createSetOfAllNonAbstractVariables(firstGoal.getClause().head()))
                ),
                new PrologSubstitution()
            )
        );
        return result;
    }

    @Override
    public String getDOTColor() {
        return "lightblue";
    }

    @Override
    public String toJSON() {
        return String.format("EVAL(%s)", this.termination);
    }

    @Override
    public String toString() {
        return "EVAL";
    }

}
