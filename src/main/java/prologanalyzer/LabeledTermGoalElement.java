package prologanalyzer;

import prologparser.*;

public record LabeledTermGoalElement(PrologTerm term, PrologClause clause, int scope) implements GoalElement {

    @Override
    public PrologClause getClause() {
        return this.clause();
    }

    @Override
    public int getScope() {
        return this.scope();
    }

    @Override
    public PrologTerm getTerm() {
        return this.term();
    }

    @Override
    public boolean hasClause() {
        return true;
    }

    @Override
    public boolean hasTerm() {
        return true;
    }

    @Override
    public boolean isScopeMarker() {
        return false;
    }

}
