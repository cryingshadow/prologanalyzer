package prologanalyzer;

import java.util.*;
import java.util.stream.*;

import prologparser.*;

public class KnowledgeBase {

    private final Set<PrologVariable> free;

    private final Set<PrologAbstractVariable> ground;

    private final Set<TermPair> nonUnifiable;

    public KnowledgeBase() {
        this(Set.of(), Set.of(), Set.of());
    }

    public KnowledgeBase(
        final Set<PrologAbstractVariable> ground,
        final Set<TermPair> nonUnifiable,
        final Set<PrologVariable> free
    ) {
        this.ground = ground;
        this.nonUnifiable = nonUnifiable;
        this.free = free;
    }

    public KnowledgeBase addFree(final Collection<PrologVariable> variables) {
        final Set<PrologVariable> newFree = new TreeSet<PrologVariable>(this.free);
        newFree.addAll(variables);
        return new KnowledgeBase(this.ground, this.nonUnifiable, newFree);
    }

    public KnowledgeBase addGround(final Collection<PrologAbstractVariable> variables) {
        final Set<PrologAbstractVariable> newGround = new TreeSet<PrologAbstractVariable>(this.ground);
        newGround.addAll(variables);
        return new KnowledgeBase(newGround, this.nonUnifiable, this.free);
    }

    public KnowledgeBase addGround(final PrologAbstractVariable variable) {
        final Set<PrologAbstractVariable> newGround = new TreeSet<PrologAbstractVariable>(this.ground);
        newGround.add(variable);
        return new KnowledgeBase(newGround, this.nonUnifiable, this.free);
    }

    public KnowledgeBase addNonUnification(final PrologTerm term1, final PrologTerm term2) {
        final Set<TermPair> nonUnifiable = new TreeSet<TermPair>(this.nonUnifiable);
        nonUnifiable.add(new TermPair(term1, term2));
        return new KnowledgeBase(this.ground, nonUnifiable, this.free);
    }

    public PrologSubstitution addRenamings(
        final PrologSubstitution mgu,
        final PrologTerm term,
        final PrologClause clause
    ) {
        // TODO Auto-generated method stub
        return null;
    }

    public KnowledgeBase apply(final PrologSubstitution substitution, final PrologClause clause) {
        // TODO Auto-generated method stub
        return new KnowledgeBase(
            this
            .ground
            .stream()
            .flatMap(variable -> Terms.createSetOfAllAbstractVariables(substitution.apply(variable)).stream())
            .collect(Collectors.toSet()),
            null,
            null
        );
    }

    public boolean isCompatibleWithNonUnification(final PrologSubstitution substitution) {
        final PrologSubstitution restricted = this.restrictToGround(substitution);
        for (final TermPair pair : this.nonUnifiable) {
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
        result.keySet().retainAll(this.ground);
        return result;
    }

}
