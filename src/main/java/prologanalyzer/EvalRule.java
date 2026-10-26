package prologanalyzer;

import java.util.*;

import prologparser.*;

public class EvalRule implements SymbolicEvaluationRule {

    @Override
    public List<AbstractState> apply(final AbstractState state) {
        // TODO Auto-generated method stub
        final GoalElement firstGoal = state.goal().getFirst();
        if (!firstGoal.hasClause()) {
            throw new IllegalArgumentException("First goal element must be a term labeled with a clause!");
        }
        final PrologTerm firstConjunct = Terms.getFirstConjunct(firstGoal.getTerm());

        return null;
    }

}
