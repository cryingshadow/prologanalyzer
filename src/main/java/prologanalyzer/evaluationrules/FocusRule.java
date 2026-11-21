package prologanalyzer.evaluationrules;

import java.util.*;

import prologanalyzer.*;
import prologanalyzer.structure.*;
import prologparser.*;

public class FocusRule implements SymbolicEvaluationRule {

    private final int numberOfConjuncts;

    public FocusRule(final int numberOfConjuncts) {
        this.numberOfConjuncts = numberOfConjuncts;
    }

    @Override
    public List<SymbolicEvaluationResult> apply(final AbstractState state, final GlobalState global) {
        if (state.goal.size() != 1 || !state.goal.getFirst().hasTerm() || state.goal.getFirst().hasClause()) {
            return List.of();
        }
        final PrologTerm term = state.goal.getFirst().getTerm();
        if (!Terms.isConjunction(term)) {
            return List.of();
        }
        final List<PrologTerm> conjuncts = Terms.getConjuncts(term);
        if (conjuncts.size() <= this.numberOfConjuncts) {
            return List.of();
        }
        if (!Terms.computeActiveCuts(term).isEmpty()) {
            if (
                !Terms.isCut(conjuncts.get(this.numberOfConjuncts - 1))
                || !conjuncts.stream().skip(this.numberOfConjuncts).allMatch(Terms::isCut)
            ) {
                return List.of();
            }
        }
        final PrologTerm focus = Terms.createConjunction(conjuncts.stream().limit(this.numberOfConjuncts).toList());
        return List.of(
            new SymbolicEvaluationResult(
                new AbstractState(
                    List.of(new TermGoalElement(focus)),
                    state.knowledge
                ).restrictKnowledgeToExistingVariables(),
                new PrologSubstitution()
            )
        );
    }

    @Override
    public String getDOTColor() {
        return "violet";
    }

    @Override
    public String toString() {
        return "FOCUS";
    }
}
