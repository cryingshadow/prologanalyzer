package prologanalyzer.evaluationrules;

import java.util.*;

import prologanalyzer.*;
import prologanalyzer.structure.*;
import prologparser.*;

public class OnlyUnifyRule implements SymbolicEvaluationRule {

    public static final OnlyUnifyRule NON_TERMINATION_ONLY_UNIFY = new OnlyUnifyRule(false);

    public static final OnlyUnifyRule TERMINATION_ONLY_UNIFY = new OnlyUnifyRule(true);

    private final boolean termination;

    private OnlyUnifyRule(final boolean termination) {
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
        if (mgu.hasPotentialUnificationConflict() || !state.knowledge.isCompatibleWithNonUnification(mgu)) {
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
        final List<GoalElement> newGoal = new ArrayList<GoalElement>();
        newGoal.add(new TermGoalElement(mguWithAdditionalRenamings.apply(tailOfConjunction)));
        newGoal.addAll(state.goal.stream().skip(1).map(goal -> goal.apply(mguOnGround)).toList());
        return List.of(
            new SymbolicEvaluationResult(
                new AbstractState(newGoal, state.knowledge.apply(mgu)).restrictKnowledgeToExistingVariables(),
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
        return String.format("ONLY_UNIFY(%s)", this.termination);
    }

    @Override
    public String toString() {
        return "ONLY_UNIFY";
    }

}
