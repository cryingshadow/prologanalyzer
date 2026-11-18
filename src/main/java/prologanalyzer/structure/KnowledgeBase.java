package prologanalyzer.structure;

import java.util.*;
import java.util.stream.*;

import prologanalyzer.*;
import prologparser.*;

public class KnowledgeBase {

    public static boolean matchesKnowledge(
        final PrologSubstitution matcher,
        final AbstractState parent,
        final AbstractState child,
        final Set<PrologVariable> nonAbstractVariablesInMatcher,
        final Set<PrologAbstractVariable> abstractVariablesInMatcher
    ) {
        outerLoop: for (final TermPair pairFromChild : child.knowledge.nonUnifiable) {
            final PrologTerm matchedInChild1 = matcher.apply(pairFromChild.term1());
            final PrologTerm matchedInChild2 = matcher.apply(pairFromChild.term2());
            for (final TermPair nonUnifyingInParent : parent.knowledge.nonUnifiable) {
                if (
                    (
                        nonUnifyingInParent.term1().equals(matchedInChild1)
                        && nonUnifyingInParent.term2().equals(matchedInChild2)
                    ) || (
                        nonUnifyingInParent.term1().equals(matchedInChild2)
                        && nonUnifyingInParent.term2().equals(matchedInChild1)
                    )
                ) {
                    continue outerLoop;
                }
            }
            for (final TermPair nonUnifyingInParent : parent.knowledge.nonUnifiable) {
                if (
                    KnowledgeBase.isNonAbstractAndUnusedMatching(
                        matchedInChild1,
                        matchedInChild2,
                        nonUnifyingInParent,
                        nonAbstractVariablesInMatcher,
                        abstractVariablesInMatcher,
                        matcher
                    )
                ) {
                    continue outerLoop;
                }
            }
            return false;
        }
        final Set<PrologVariable> matchedFreeVariables = new LinkedHashSet<PrologVariable>();
        for (final PrologVariable variable : child.knowledge.free) {
            PrologVariable variableToAdd = null;
            if (matcher.containsKey(variable)) {
                variableToAdd = (PrologVariable)matcher.get(variable);
                if (!parent.knowledge.isFree(variableToAdd)) {
                    return false;
                }
            } else {
                if (!parent.knowledge.isFree(variable)) {
                    if (!nonAbstractVariablesInMatcher.contains(variable)) {
                        final Set<PrologVariable> free = new LinkedHashSet<PrologVariable>(parent.knowledge.free);
                        free.removeAll(nonAbstractVariablesInMatcher);
                        if (!free.isEmpty()) {
                            matcher.put(variable, free.iterator().next());
                            variableToAdd = (PrologVariable)matcher.get(variable);
                        } else {
                            return false;
                        }
                    } else {
                        return false;
                    }
                } else {
                    variableToAdd = variable;
                }
            }
            nonAbstractVariablesInMatcher.add(variable);
            nonAbstractVariablesInMatcher.add(variableToAdd);
            matchedFreeVariables.add(variableToAdd);
        }
        for (final Map.Entry<PrologVariable, PrologTerm> entry : matcher.entrySet()) {
            if (Terms.isAbstractVariable(entry.getKey())) {
                for (final PrologVariable variable : Terms.createSetOfAllNonAbstractVariables(entry.getValue())) {
                    if (matchedFreeVariables.contains(variable)) {
                        return false;
                    }
                }
            }
        }
        for (final PrologAbstractVariable variable : child.knowledge.ground) {
            if (matcher.containsKey(variable)) {
                if (!parent.knowledge.isGround(matcher.get(variable))) {
                    return false;
                }
            } else if (!parent.knowledge.isGround(variable)) {
                if (
                    child.containsVariableInGoal(variable)
                    || parent.containsVariable(variable)
                    || abstractVariablesInMatcher.contains(variable)
                ) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isNonAbstractAndUnusedMatching(
        final PrologSubstitution matcher1,
        final PrologSubstitution matcher2,
        final Set<PrologVariable> nonAbstractVariablesInMatcher,
        final Set<PrologAbstractVariable> abstractVariablesInMatcher,
        final PrologSubstitution matcher
    ) {
        final PrologSubstitution combinedMatcher =
            PrologSubstitution.combineToConsistentMatching(matcher1, matcher2);
        final Set<PrologVariable> nextNonAbstractVariablesInMatcher = new LinkedHashSet<PrologVariable>();
        final Set<PrologAbstractVariable> nextAbstractVariablesInMatcher = new LinkedHashSet<PrologAbstractVariable>();
        boolean ok = true;
        if (combinedMatcher != null) {
            for (final Map.Entry<PrologVariable, PrologTerm> entry : combinedMatcher.entrySet()) {
                final PrologVariable key = entry.getKey();
                final PrologVariable value = (PrologVariable)entry.getValue();
                if (
                    nonAbstractVariablesInMatcher.contains(key)
                    || nonAbstractVariablesInMatcher.contains(value)
                    || nextNonAbstractVariablesInMatcher.contains(key)
                    || nextNonAbstractVariablesInMatcher.contains(value)
                    || abstractVariablesInMatcher.contains(key)
                    || abstractVariablesInMatcher.contains(value)
                    || nextAbstractVariablesInMatcher.contains(key)
                    || nextAbstractVariablesInMatcher.contains(value)
                ) {
                    ok = false;
                    break;
                } else {
                    if (Terms.isAbstractVariable(key)) {
                        nextAbstractVariablesInMatcher.add((PrologAbstractVariable)key);
                        nextAbstractVariablesInMatcher.add((PrologAbstractVariable)value);
                    } else {
                        nextNonAbstractVariablesInMatcher.add(key);
                        nextNonAbstractVariablesInMatcher.add(value);
                    }
                }
            }
            if (ok) {
                for (final Map.Entry<PrologVariable, PrologTerm> entry : combinedMatcher.entrySet()) {
                    matcher.put(entry.getKey(), entry.getValue());
                }
                nonAbstractVariablesInMatcher.addAll(nextNonAbstractVariablesInMatcher);
                abstractVariablesInMatcher.addAll(nextAbstractVariablesInMatcher);
                return true;
            }
        }
        return false;
    }

    private static boolean isNonAbstractAndUnusedMatching(
        final PrologTerm matchedInChild1,
        final PrologTerm matchedInChild2,
        final TermPair nonUnifyingInParent,
        final Set<PrologVariable> nonAbstractVariablesInMatcher,
        final Set<PrologAbstractVariable> abstractVariablesInMatcher,
        final PrologSubstitution matcher
    ) {
        return
            KnowledgeBase.isNonAbstractAndUnusedMatching(
                Terms.calculateMatcherWithAbstractVariables(matchedInChild1, nonUnifyingInParent.term1()),
                Terms.calculateMatcherWithAbstractVariables(matchedInChild2, nonUnifyingInParent.term2()),
                nonAbstractVariablesInMatcher,
                abstractVariablesInMatcher,
                matcher
            ) || KnowledgeBase.isNonAbstractAndUnusedMatching(
                Terms.calculateMatcherWithAbstractVariables(matchedInChild1, nonUnifyingInParent.term1()),
                Terms.calculateMatcherWithAbstractVariables(matchedInChild2, nonUnifyingInParent.term2()),
                nonAbstractVariablesInMatcher,
                abstractVariablesInMatcher,
                matcher
            );
    }

    private static PrologTerm replaceByFreshFreeVariable(
        final PrologTerm term,
        final int atNesting,
        final Set<PrologVariable> free,
        final FreshNameGenerator fridge
    ) {
        if (atNesting <= 0) {
            final PrologVariable variable =
                new PrologVariable(fridge.getFreshName(Terms.DEFAULT_NON_ABSTRACT_VARIABLE_NAME));
            free.add(variable);
            return variable;
        }
        if (term.arguments.isEmpty()) {
            return term;
        }
        return new PrologTerm(
            term.name,
            term
            .arguments
            .stream()
            .map(argument -> KnowledgeBase.replaceByFreshFreeVariable(argument, atNesting - 1, free, fridge))
            .toList());
    }

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
        final Set<PrologVariable> newFree = new TreeSet<PrologVariable>(PrologTermComparator.INSTANCE);
        newFree.addAll(this.free);
        newFree.addAll(variables);
        return new KnowledgeBase(this.ground, this.nonUnifiable, newFree);
    }

    public KnowledgeBase addGround(final Collection<PrologAbstractVariable> variables) {
        final Set<PrologAbstractVariable> newGround =
            new TreeSet<PrologAbstractVariable>(PrologTermComparator.INSTANCE);
        newGround.addAll(this.ground);
        newGround.addAll(variables);
        return new KnowledgeBase(newGround, this.nonUnifiable, this.free);
    }

    public KnowledgeBase addGround(final PrologAbstractVariable variable) {
        final Set<PrologAbstractVariable> newGround =
            new TreeSet<PrologAbstractVariable>(PrologTermComparator.INSTANCE);
        newGround.addAll(this.ground);
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

    public KnowledgeBase applyToNonUnification(final PrologSubstitution substitution) {
        return new KnowledgeBase(
            this.ground,
            this
            .nonUnifiable
            .stream()
            .map(pair -> new TermPair(substitution.apply(pair.term1()), substitution.apply(pair.term2())))
            .collect(Collectors.toSet()),
            this.free
        );
    }

    public KnowledgeBase cleanAndRestrictToVariables(final Set<PrologVariable> variables) {
        final Set<TermPair> nonUnifiable = new LinkedHashSet<TermPair>();
        outer: for (final TermPair pair : this.nonUnifiable) {
            final Optional<PrologSubstitution> optionalMGU = MGUComputer.computeMGU(pair.term1(), pair.term2());
            if (optionalMGU.isEmpty()) {
                continue;
            }
            final Set<PrologVariable> variablesInPair =
                new LinkedHashSet<PrologVariable>(Terms.createSetOfAllVariables(pair.term1()));
            variablesInPair.addAll(Terms.createSetOfAllVariables(pair.term2()));
            variablesInPair.retainAll(variables);
            if (variablesInPair.isEmpty()) {
                continue;
            }
            final PrologSubstitution mgu = optionalMGU.get();
            if (mgu.keySet().stream().allMatch(variable -> !variables.contains(variable))) {
                continue;
            }
            for (final TermPair in : nonUnifiable) {
                final Optional<PrologSubstitution> secondMGU =
                    MGUComputer.computeMGU(mgu.apply(in.term1()), mgu.apply(in.term2()));
                if (secondMGU.isPresent() && secondMGU.get().keySet().stream().allMatch(Terms::isNonAbstractVariable)) {
                    continue outer;
                }
            }
            nonUnifiable.add(pair);
        }
        final Set<PrologVariable> toRetain = new LinkedHashSet<PrologVariable>(variables);
        for (final TermPair pair : nonUnifiable) {
            toRetain.addAll(Terms.createSetOfAllVariables(pair.term1()));
            toRetain.addAll(Terms.createSetOfAllVariables(pair.term2()));
        }
        final Set<PrologAbstractVariable> ground = new TreeSet<PrologAbstractVariable>(this.ground);
        final Set<PrologVariable> free = new TreeSet<PrologVariable>(this.free);
        ground.retainAll(toRetain);
        free.retainAll(toRetain);
        return new KnowledgeBase(ground, nonUnifiable, free);
    }

    public KnowledgeBase concretize(final int maxNesting, final FreshNameGenerator fridge) {
        final Set<TermPair> nextNonUnifiable = new LinkedHashSet<TermPair>();
        final Set<PrologVariable> nextFree = new TreeSet<PrologVariable>(this.free);
        for (final TermPair pair : this.nonUnifiable) {
            if (Terms.maxNesting(pair.term1()) <= maxNesting && Terms.maxNesting(pair.term2()) <= maxNesting) {
                nextNonUnifiable.add(pair);
            } else {
                nextNonUnifiable.add(
                    new TermPair(
                        KnowledgeBase.replaceByFreshFreeVariable(pair.term1(), 2, nextFree, fridge),
                        KnowledgeBase.replaceByFreshFreeVariable(pair.term2(), 2, nextFree, fridge)
                    )
                );
            }
        }
        return new KnowledgeBase(this.ground, nextNonUnifiable, nextFree);
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof final KnowledgeBase other) {
            return
                this.ground.equals(other.ground)
                && this.nonUnifiable.equals(other.nonUnifiable)
                && this.free.equals(other.free);
        }
        return false;
    }

    public Set<PrologVariable> freeVariables(final PrologTerm term) {
        final Set<PrologVariable> result = Terms.createSetOfAllNonAbstractVariables(term);
        result.retainAll(this.free);
        return result;
    }

    public Set<PrologAbstractVariable> getAllAbstractVariables() {
        final Set<PrologAbstractVariable> result = new LinkedHashSet<PrologAbstractVariable>(this.ground);
        for (final TermPair pair : this.nonUnifiable) {
            result.addAll(Terms.createSetOfAllAbstractVariables(pair.term1()));
            result.addAll(Terms.createSetOfAllAbstractVariables(pair.term2()));
        }
        return result;
    }

    public Set<PrologVariable> getAllNonAbstractVariables() {
        final Set<PrologVariable> result = new LinkedHashSet<PrologVariable>(this.free);
        for (final TermPair pair : this.nonUnifiable) {
            result.addAll(Terms.createSetOfAllNonAbstractVariables(pair.term1()));
            result.addAll(Terms.createSetOfAllNonAbstractVariables(pair.term2()));
        }
        return result;
    }

    public Set<PrologVariable> getAllVariables() {
        final Set<PrologVariable> res = new TreeSet<PrologVariable>(PrologTermComparator.INSTANCE);
        res.addAll(this.free);
        res.addAll(this.ground);
        for (final TermPair pair : this.nonUnifiable) {
            res.addAll(Terms.createSetOfAllVariables(pair.term1()));
            res.addAll(Terms.createSetOfAllVariables(pair.term2()));
        }
        return res;
    }

    @Override
    public int hashCode() {
        return 2 * this.ground.hashCode() + 3 * this.nonUnifiable.hashCode() + 5 * this.free.hashCode();
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

    public boolean isFree(final PrologVariable variable) {
        return this.free.contains(variable);
    }

    public boolean isFree(final Set<PrologVariable> variables) {
        return this.free.containsAll(variables);
    }

    public boolean isGround(final PrologTerm term) {
        return this.ground.containsAll(Terms.createSetOfAllVariables(term)) && !Terms.isCyclic(term);
    }

    public boolean isGround(final Set<PrologAbstractVariable> variables) {
        return this.ground.containsAll(variables);
    }

    public int maxNesting() {
        int result = 0;
        for (final TermPair pair : this.nonUnifiable) {
            result = Math.max(result, Terms.maxNesting(pair.term1()));
            result = Math.max(result, Terms.maxNesting(pair.term2()));
        }
        return result;
    }

    public KnowledgeBase removeFree(final Set<PrologVariable> variables) {
        final Set<PrologVariable> newFree = new TreeSet<PrologVariable>(PrologTermComparator.INSTANCE);
        newFree.addAll(this.free);
        newFree.removeAll(variables);
        return new KnowledgeBase(this.ground, this.nonUnifiable, newFree);
    }

    public boolean respectsFree(final PrologSubstitution matcher, final KnowledgeBase parentKnowledge) {
        for (final PrologVariable variable : this.free) {
            final PrologTerm matched = matcher.apply(variable);
            if (!Terms.isNonAbstractVariable(matched) || !parentKnowledge.isFree((PrologVariable)matched)) {
                return false;
            }
        }
        for (final Map.Entry<PrologVariable, PrologTerm> entry : matcher.restrictToAbstractVariables().entrySet()) {
            if (
                Terms
                .createSetOfAllNonAbstractVariables(entry.getValue())
                .stream()
                .anyMatch(parentKnowledge.free::contains)
            ) {
                return false;
            }
        }
        return true;
    }

    public boolean respectsNonUnification(final PrologSubstitution matcher, final KnowledgeBase parentKnowledge) {
        outer: for (final TermPair childPair : this.nonUnifiable) {
            final TermPair matched = new TermPair(matcher.apply(childPair.term1()), matcher.apply(childPair.term2()));
            final FreshNameGenerator fridge =
                new FreshNameGenerator(
                    Stream.concat(
                        Terms.createSetOfAllVariables(matched.term1()).stream(),
                        Terms.createSetOfAllVariables(matched.term2()).stream()
                    ).map(PrologVariable::name)
                    .collect(Collectors.toSet())
                );
            final Optional<PrologSubstitution> optionalMGU =
                MGUComputer.computeMGUWithOnlyFreshVariables(matched.term1(), matched.term2(), fridge);
            if (optionalMGU.isEmpty()) {
                continue;
            }
            final PrologSubstitution mgu = optionalMGU.get();
            for (final TermPair parentPair : parentKnowledge.nonUnifiable) {
                final Optional<PrologSubstitution> secondMGU =
                    MGUComputer.computeMGU(mgu.apply(parentPair.term1()), mgu.apply(parentPair.term2()));
                if (secondMGU.isEmpty()) {
                    continue;
                }
                if (secondMGU.get().keySet().stream().allMatch(Terms::isNonAbstractVariable)) {
                    continue outer;
                }
            }
            return false;
        }
        return true;
    }

    public PrologSubstitution restrictToFree(final PrologSubstitution substitution) {
        return substitution.restrict(this.free);
    }

    public PrologSubstitution restrictToGround(final PrologSubstitution substitution) {
        return substitution.restrict(this.ground);
    }

    public Set<PrologAbstractVariable> restrictToGround(final Set<PrologVariable> variables) {
        final Set<PrologAbstractVariable> result = new TreeSet<PrologAbstractVariable>();
        for (final PrologVariable variable : variables) {
            if (this.ground.contains(variable)) {
                result.add((PrologAbstractVariable)variable);
            }
        }
        return result;
    }

    public Set<PrologVariable> restrictToNonFree(final Set<PrologVariable> variables) {
        final Set<PrologVariable> result = new LinkedHashSet<PrologVariable>(variables);
        result.removeAll(this.free);
        return result;
    }

    public Set<PrologVariable> restrictToNonGround(final Set<? extends PrologVariable> variables) {
        final Set<PrologVariable> result = new TreeSet<PrologVariable>(variables);
        result.removeAll(this.ground);
        return result;
    }

    @Override
    public String toString() {
        return String.format(
            "({%s}, {%s}, {%s})",
            this.ground.stream().map(PrologAbstractVariable::toString).collect(Collectors.joining(", ")),
            this.free.stream().map(PrologVariable::toString).collect(Collectors.joining(", ")),
            this
            .nonUnifiable
            .stream()
            .map(pair -> String.format("%s !~ %s", pair.term1(), pair.term2()))
            .collect(Collectors.joining(", "))
        );
    }

    private Set<PrologVariable> computeFreeAfterUnification(final PrologSubstitution mgu, final PrologClause clause) {
        final Set<PrologVariable> result = new TreeSet<PrologVariable>(PrologTermComparator.INSTANCE);
        result.addAll(this.free);
        final PrologSubstitution mguOnFree = this.restrictToFree(mgu);
        final Set<PrologVariable> newFree = new LinkedHashSet<PrologVariable>(this.free);
        newFree.addAll(Terms.createSetOfAllVariables(clause.head()));
        final PrologSubstitution mguOnNonFreeNonAbstract =
            mgu.restrictToNonAbstractVariables().restrictToComplement(newFree);
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
            .collect(Collectors.toCollection(() -> new TreeSet<PrologAbstractVariable>(PrologTermComparator.INSTANCE)));
    }

}
