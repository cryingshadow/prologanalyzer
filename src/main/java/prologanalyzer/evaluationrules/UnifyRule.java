package prologanalyzer.evaluationrules;

import java.util.*;

import prologanalyzer.*;
import prologanalyzer.structure.*;
import prologparser.*;

public class UnifyRule implements SymbolicEvaluationRule {

    public static final UnifyRule NON_TERMINATION_UNIFY = new UnifyRule(false);

    public static final UnifyRule TERMINATION_UNIFY = new UnifyRule(true);

    private final boolean termination;

    private UnifyRule(final boolean termination) {
        this.termination = termination;
    }

    @Override
    public List<SymbolicEvaluationResult> apply(final AbstractState state, final GlobalState global) {
        if (state.goal.isEmpty()) {
            return List.of();
        }
        final GoalElement firstGoal = state.goal.getFirst();
        if (!firstGoal.hasTerm() || firstGoal.hasClause()) {
            return List.of();
        }
        final PrologTerm firstConjunct = Terms.getFirstConjunct(firstGoal.getTerm());
        if (!Terms.haveSameFunctionSymbol(PrologBuiltInPredicates.UNIFY, firstConjunct)) {
            return List.of();
        }
        final PrologTerm tailOfConjunction = Terms.removeFirstConjunct(firstGoal.getTerm());
        final Optional<PrologSubstitution> optionalMGU =
            MGUComputer.computeMGUWithOnlyFreshVariables(
                firstConjunct.arguments.getFirst(),
                firstConjunct.arguments.getLast(),
                global.fridge()
            );
        if (optionalMGU.isEmpty()) {
            return List.of();
        }
        final PrologSubstitution mgu = optionalMGU.get();
        if (!mgu.hasPotentialUnificationConflict() || !state.knowledge.isCompatibleWithNonUnification(mgu)) {
            return List.of();
        }
        final PrologSubstitution mguOnGround = state.knowledge.restrictToGround(mgu);
        final PrologSubstitution mguWithAdditionalRenamings =
            this.termination ?
                state.knowledge.addRenamings(
                    mgu,
                    firstConjunct,
                    tailOfConjunction,
                    firstGoal.getClause(),
                    global.fridge()
                ) :
                    mgu;
        final List<SymbolicEvaluationResult> result = new ArrayList<SymbolicEvaluationResult>();
        final List<GoalElement> newGoalEval = new ArrayList<GoalElement>();
        newGoalEval.add(new TermGoalElement(mguWithAdditionalRenamings.apply(tailOfConjunction)));
        newGoalEval.addAll(state.goal.stream().skip(1).map(goal -> goal.apply(mguOnGround)).toList());
        result.add(
            new SymbolicEvaluationResult(
                new AbstractState(newGoalEval, state.knowledge.apply(mgu)).restrictKnowledgeToExistingVariables(),
                mguWithAdditionalRenamings
            )
        );
        final List<GoalElement> newGoalBacktrack = new ArrayList<GoalElement>(state.goal);
        newGoalBacktrack.remove(0);
        result.add(
            new SymbolicEvaluationResult(
                new AbstractState(
                    newGoalBacktrack,
                    state
                    .knowledge
                    .addNonUnification(firstConjunct.arguments.getFirst(), firstConjunct.arguments.getLast())
                ).restrictKnowledgeToExistingVariables(),
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
        return String.format("UNIFY(%s)", this.termination);
    }

    @Override
    public String toString() {
        return "UNIFY";
    }

}
