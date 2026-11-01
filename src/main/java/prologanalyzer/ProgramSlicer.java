package prologanalyzer;

import java.util.*;

import prologanalyzer.structure.*;
import prologparser.*;

public abstract class ProgramSlicer {

    public static List<PrologClause> slice(final FunctionSymbol symbol, final PrologProgram program) {
        final List<PrologClause> result = new ArrayList<PrologClause>();
        for (final PrologClause clause : program.clauses()) {
            if (Terms.haveSameFunctionSymbol(symbol, clause.head())) {
                result.add(clause);
            }
        }
        return result;
    }

    public static List<PrologClause> slice(final PrologTerm term, final PrologProgram program) {
        return ProgramSlicer.slice(Terms.getFunctionSymbol(term), program);
    }

}
