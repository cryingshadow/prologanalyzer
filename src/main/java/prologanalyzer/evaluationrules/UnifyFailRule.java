package prologanalyzer.evaluationrules;

import java.util.*;

import prologanalyzer.*;
import prologanalyzer.structure.*;
import prologparser.*;

public class UnifyFailRule implements SymbolicEvaluationRule {

    public static final UnifyFailRule INSTANCE = new UnifyFailRule();

    private UnifyFailRule() {}

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
        final List<GoalElement> newGoal = new ArrayList<GoalElement>(state.goal);
        newGoal.remove(0);
        final Optional<PrologSubstitution> optionalMGU =
            MGUComputer.computeMGUWithOnlyFreshVariables(
                firstConjunct.arguments.getFirst(),
                firstConjunct.arguments.getLast(),
                global.fridge()
            );
        if (optionalMGU.isPresent() && state.knowledge.isCompatibleWithNonUnification(optionalMGU.get())) {
            return List.of();
        }
        return List.of(
            new SymbolicEvaluationResult(
                new AbstractState(newGoal, state.knowledge).restrictKnowledgeToExistingVariables(),
                new PrologSubstitution()
            )
        );
    }

    @Override
    public String getDOTColor() {
        return "lightgreen";
    }

    @Override
    public String toString() {
        return "UNIFY_FAIL";
    }

}
