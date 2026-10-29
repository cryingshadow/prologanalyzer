package prologanalyzer;

import java.util.*;
import java.util.stream.*;

import prologparser.*;

public record KnowledgeBase(
    Set<PrologAbstractVariable> ground,
    Set<TermPair> nonUnifiable,
    Set<PrologVariable> free
) {

    public KnowledgeBase() {
        this(Set.of(), Set.of(), Set.of());
    }

    public KnowledgeBase apply(final PrologSubstitution substitution) {
        // TODO Auto-generated method stub
        return new KnowledgeBase(
            this
            .ground()
            .stream()
            .flatMap(variable -> Terms.createSetOfAllAbstractVariables(substitution.apply(variable)).stream())
            .collect(Collectors.toSet()),
            null,
            null
        );
    }

    public KnowledgeBase addNonUnification(final PrologTerm term1, final PrologTerm term2) {
        final Set<TermPair> nonUnifiable = new TreeSet<TermPair>(this.nonUnifiable());
        nonUnifiable.add(new TermPair(term1, term2));
        return new KnowledgeBase(this.ground(), nonUnifiable, this.free());
    }

    public KnowledgeBase setGround(final PrologAbstractVariable variable) {
        final Set<PrologAbstractVariable> newGround = new TreeSet<PrologAbstractVariable>(this.ground());
        newGround.add(variable);
        return new KnowledgeBase(newGround, this.nonUnifiable(), this.free());
    }

}
