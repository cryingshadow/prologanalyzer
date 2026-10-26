package prologanalyzer;

import prologparser.*;

public interface GoalElement {

    PrologClause getClause();

    int getScope();

    PrologTerm getTerm();

    boolean hasClause();

    boolean hasTerm();

    boolean isScopeMarker();

}
