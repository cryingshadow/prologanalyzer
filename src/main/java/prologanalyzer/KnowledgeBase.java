package prologanalyzer;

import java.util.*;

import prologparser.*;

public record KnowledgeBase(
    Set<PrologAbstractVariable> ground,
    Set<NonUnifiablePair> nonUnifiable,
    Set<PrologVariable> free
) {

    public KnowledgeBase apply(final PrologSubstitution sigma) {
        // TODO Auto-generated method stub
        return null;
    }

    public KnowledgeBase addNonUnification(final PrologTerm term1, final PrologTerm term2) {
        // TODO Auto-generated method stub
        return null;
    }

}
