package prologanalyzer;

import java.util.*;

import prologparser.*;

public abstract class ProgramSlicer {

    public static List<PrologClause> slice(final PrologTerm term, final PrologProgram program) {
        final List<PrologClause> result = new ArrayList<PrologClause>();
        for (final PrologClause clause : program.clauses()) {
            if (Terms.haveSameFunctionSymbol(term, clause.head())) {
                result.add(clause);
            }
        }
        return result;
    }

}
