package prologanalyzer;

import prologparser.*;

public record TermGoalElement(PrologTerm term) implements GoalElement {

    @Override
    public boolean hasClause() {
        return false;
    }

    @Override
    public boolean hasTerm() {
        return true;
    }

    @Override
    public boolean isScopeMarker() {
        return false;
    }

    @Override
    public PrologClause getClause() {
        return null;
    }

    @Override
    public int getScope() {
        return -1;
    }

    @Override
    public PrologTerm getTerm() {
        return this.term();
    }

    @Override
    public TermGoalElement apply(final PrologSubstitution substitution) {
        return new TermGoalElement(substitution.apply(this.term()));
    }

}
