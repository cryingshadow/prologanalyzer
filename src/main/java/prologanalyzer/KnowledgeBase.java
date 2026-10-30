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

    public boolean isCompatibleWithNonUnification(final PrologSubstitution substitution) {
        final PrologSubstitution restricted = this.restrictToGround(substitution);
        for (final TermPair pair : this.nonUnifiable()) {
            final Optional<PrologSubstitution> optionalMGU =
                MGUComputer.computeMGUWithOnlyFreshVariables(
                    restricted.apply(pair.term1()),
                    restricted.apply(pair.term2()),
                    new FreshNameGenerator()
                );
            if (optionalMGU.isPresent()) {
                final PrologSubstitution mgu = optionalMGU.get();
                if (mgu.isIdempotent() && mgu.keySet().stream().allMatch(Terms::isNonAbstractVariable)) {
                    return false;
                }
            }
        }
        return true;
    }

    public PrologSubstitution restrictToGround(final PrologSubstitution substitution) {
        final PrologSubstitution result = new PrologSubstitution(substitution);
        result.keySet().retainAll(this.ground());
        return result;
    }

}
