package prologanalyzer;

import prologparser.*;

public interface GoalElement {

    GoalElement apply(PrologSubstitution substitution);

    PrologClause getClause();

    int getScope();

    PrologTerm getTerm();

    boolean hasClause();

    boolean hasTerm();

    boolean isScopeMarker();

}
