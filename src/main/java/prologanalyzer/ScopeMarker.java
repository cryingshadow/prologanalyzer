package prologanalyzer;

import prologparser.*;

public record ScopeMarker(int scope) implements GoalElement {

    @Override
    public PrologClause getClause() {
        return null;
    }

    @Override
    public int getScope() {
        return this.scope();
    }

    @Override
    public PrologTerm getTerm() {
        return null;
    }

    @Override
    public boolean hasClause() {
        return false;
    }

    @Override
    public boolean hasTerm() {
        return false;
    }

    @Override
    public boolean isScopeMarker() {
        return true;
    }

    @Override
    public ScopeMarker apply(final PrologSubstitution substitution) {
        return this;
    }

}
