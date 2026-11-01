package prologanalyzer.evaluationrules;

import java.util.*;

import prologanalyzer.*;
import prologanalyzer.structure.*;
import prologparser.*;

public class SplitRule implements SymbolicEvaluationRule {

    @Override
    public List<SymbolicEvaluationResult> apply(final AbstractState state, final GlobalState global) {
        if (state.goal().size() != 1 || !state.goal().getFirst().hasTerm() || state.goal().getFirst().hasClause()) {
            return List.of();
        }
        final PrologTerm term = state.goal().getFirst().getTerm();
        if (!Terms.isConjunction(term) || Terms.getArity(term) < 2) {
            return List.of();
        }
        final PrologTerm headOfConjunction = Terms.getFirstConjunct(term);
        if (Terms.isCut(headOfConjunction)) {
            return List.of();
        }
        final PrologTerm tailOfConjunction = Terms.removeFirstConjunct(term);
        final Set<PrologVariable> freeVariablesInTerm = state.knowledge().freeVariables(headOfConjunction);
        final Set<PrologVariable> toRename = new LinkedHashSet<PrologVariable>();
        if (state.knowledge().isGround(Terms.createSetOfAllAbstractVariables(term))) {
            if (state.knowledge().isFree(Terms.createSetOfAllNonAbstractVariables(term))) {
                toRename.addAll(freeVariablesInTerm);
            } else {
                toRename.addAll(Terms.createSetOfAllNonAbstractVariables(term));
                toRename.addAll(state.knowledge().restrictToNonGround(state.knowledge().getAllAbstractVariables()));
                toRename.addAll(
                    state.knowledge().restrictToNonGround(Terms.createSetOfAllAbstractVariables(tailOfConjunction))
                );
            }
        } else {
            toRename.addAll(freeVariablesInTerm);
            toRename.addAll(state.knowledge().restrictToNonGround(state.knowledge().getAllAbstractVariables()));
            toRename.addAll(
                state.knowledge().restrictToNonGround(Terms.createSetOfAllAbstractVariables(tailOfConjunction))
            );
            toRename.addAll(state.knowledge().restrictToNonFree(state.knowledge().getAllNonAbstractVariables()));
            toRename.addAll(
                state.knowledge().restrictToNonFree(Terms.createSetOfAllNonAbstractVariables(tailOfConjunction))
            );
        }
        final PrologSubstitution answerApproximation = new PrologSubstitution();
        for (final PrologVariable variable : toRename) {
            answerApproximation.put(
                variable,
                new PrologAbstractVariable(global.fridge().getFreshName(Terms.DEFAULT_ABSTRACT_VARIABLE_NAME))
            );
        }
        final Set<PrologAbstractVariable> newGround = new LinkedHashSet<PrologAbstractVariable>();
        return List.of(
            new SymbolicEvaluationResult(
                new AbstractState(List.of(new TermGoalElement(headOfConjunction)), state.knowledge()),
                new PrologSubstitution()
            ),
            new SymbolicEvaluationResult(
                new AbstractState(
                    List.of(new TermGoalElement(answerApproximation.apply(tailOfConjunction))),
                    state
                    .knowledge()
                    .addGround(newGround)
                    .removeFree(freeVariablesInTerm)
                    .applyToNonUnification(answerApproximation)
                ),
                answerApproximation
            )
        );
    }

    @Override
    public String toString() {
        return "SPLIT";
    }
}
