package prologanalyzer.structure;

import prologparser.*;

public record JSONGoalElement(PrologTerm term, JSONClause clause, Integer scope) {

}
