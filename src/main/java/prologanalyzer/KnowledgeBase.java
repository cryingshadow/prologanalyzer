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
        final PrologTerm headOfConjunction,
        final PrologTerm tailOfConjunction,
        final PrologClause clause,
        final FreshNameGenerator fridge
    ) {
        if (this.ground.containsAll(Terms.createSetOfAllAbstractVariables(headOfConjunction))) {
            if (this.free.containsAll(Terms.createSetOfAllNonAbstractVariables(headOfConjunction))) {
                return mgu;
            }
            final Set<PrologAbstractVariable> newGround = this.computeGroundAfterSubstitution(mgu);
            final Set<PrologVariable> toRename =
                new LinkedHashSet<PrologVariable>(Terms.createSetOfAllAbstractVariables(tailOfConjunction));
            if (clause.body().isPresent()) {
                toRename.addAll(Terms.createSetOfAllAbstractVariables(clause.body().get()));
            }
            toRename.removeAll(newGround);
            final PrologSubstitution renaming = new PrologSubstitution();
            for (final PrologVariable variable : toRename) {
                renaming.put(
                    variable,
                    new PrologAbstractVariable(fridge.getFreshName(Terms.DEFAULT_ABSTRACT_VARIABLE_NAME))
                );
            }
            return mgu.append(renaming);
        }
        final Set<PrologAbstractVariable> newGround = this.computeGroundAfterSubstitution(mgu);
        final Set<PrologVariable> newFree = this.computeFreeAfterUnification(mgu, clause);
        final Set<PrologVariable> toRename =
            new LinkedHashSet<PrologVariable>(Terms.createSetOfAllVariables(tailOfConjunction));
        if (clause.body().isPresent()) {
            toRename.addAll(Terms.createSetOfAllVariables(clause.body().get()));
        }
        toRename.removeAll(newGround);
        toRename.removeAll(newFree);
        final PrologSubstitution renaming = new PrologSubstitution();
        for (final PrologVariable variable : toRename) {
            renaming.put(
                variable,
                new PrologAbstractVariable(fridge.getFreshName(Terms.DEFAULT_ABSTRACT_VARIABLE_NAME))
            );
        }
        return mgu.append(renaming);
    }

    public KnowledgeBase apply(final PrologSubstitution mgu, final PrologClause clause) {
        final PrologSubstitution mguOnGround = this.restrictToGround(mgu);
        return new KnowledgeBase(
            this.computeGroundAfterSubstitution(mgu),
            this
            .nonUnifiable
            .stream()
            .map(pair -> new TermPair(mguOnGround.apply(pair.term1()), mguOnGround.apply(pair.term2())))
            .collect(Collectors.toCollection(TreeSet::new)),
            this.computeFreeAfterUnification(mgu, clause)
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

    public PrologSubstitution restrictToFree(final PrologSubstitution substitution) {
        return substitution.restrict(this.free);
    }

    public PrologSubstitution restrictToGround(final PrologSubstitution substitution) {
        return substitution.restrict(this.ground);
    }

    private Set<PrologVariable> computeFreeAfterUnification(final PrologSubstitution mgu, final PrologClause clause) {
        final Set<PrologVariable> result = new TreeSet<PrologVariable>(this.free);
        final PrologSubstitution mguOnFree = this.restrictToFree(mgu);
        final Set<PrologVariable> newFree = new LinkedHashSet<PrologVariable>(this.free);
        newFree.addAll(Terms.createSetOfAllVariables(clause.head()));
        final PrologSubstitution mguOnNonFreeNonAbstract = mgu.restrictToNonAbstractVariables().restrictToComplement(newFree);
        final Set<PrologVariable> substituteVariables =
            mguOnFree
            .values()
            .stream()
            .flatMap(term -> Terms.createSetOfAllNonAbstractVariables(term).stream())
            .collect(Collectors.toSet());
        substituteVariables.removeAll(
            mguOnNonFreeNonAbstract
            .values()
            .stream()
            .flatMap(term -> Terms.createSetOfAllNonAbstractVariables(term).stream())
            .collect(Collectors.toSet())
        );
        result.addAll(substituteVariables);
        final Set<PrologVariable> headVariables = Terms.createSetOfAllVariables(clause.head());
        if (clause.body().isPresent()) {
            headVariables.removeAll(Terms.createSetOfAllVariables(clause.body().get()));
        }
        result.addAll(headVariables);
        return result;
    }

    private Set<PrologAbstractVariable> computeGroundAfterSubstitution(final PrologSubstitution substitution) {
        return
            this
            .ground
            .stream()
            .flatMap(variable -> Terms.createSetOfAllAbstractVariables(substitution.apply(variable)).stream())
            .collect(Collectors.toCollection(TreeSet::new));
    }

}
