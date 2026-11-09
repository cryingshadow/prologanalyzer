package prologanalyzer.evaluationrules;

import java.util.*;

import prologanalyzer.*;
import prologanalyzer.structure.*;
import prologparser.*;

public class CutRule implements SymbolicEvaluationRule {

    public static final CutRule INSTANCE = new CutRule();

    private CutRule() {}

    @Override
    public List<SymbolicEvaluationResult> apply(final AbstractState state, final GlobalState global) {
        final GoalElement firstGoal = state.goal().getFirst();
        if (!firstGoal.hasTerm()) {
            return List.of();
        }
        final PrologTerm firstConjunct = Terms.getFirstConjunct(firstGoal.getTerm());
        if (!Terms.isCut(firstConjunct)) {
            return List.of();
        }
        if (!(firstConjunct instanceof LabeledCut)) {
            throw new IllegalStateException("A cut should never be evaluated without a label!");
        }
        final int scope = ((LabeledCut)firstConjunct).scope;
        final List<GoalElement> newGoal = new ArrayList<GoalElement>();
        newGoal.add(new TermGoalElement(Terms.removeFirstConjunct(firstGoal.getTerm())));
        boolean found = false;
        for (final GoalElement element : state.goal()) {
            if (found) {
                newGoal.add(element);
            } else if (element.isScopeMarker() && element.getScope() == scope) {
                found = true;
                newGoal.add(element);
            }
        }
        if (!found) {
            return List.of();
        }
        return List.of(
            new SymbolicEvaluationResult(new AbstractState(newGoal, state.knowledge()), new PrologSubstitution())
        );
    }

    @Override
    public String toString() {
        return "CUT";
    }

}
