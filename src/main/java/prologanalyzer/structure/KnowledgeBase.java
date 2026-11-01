package prologanalyzer.structure;

import java.util.*;
import java.util.stream.*;

import prologanalyzer.*;
import prologparser.*;

public class KnowledgeBase {

    public static boolean matchesKnowledge(
        final PrologSubstitution matcher,
        final AbstractState child,
        final AbstractState parent,
        final Set<PrologVariable> nonAbstractVariablesInMatcher,
        final Set<PrologAbstractVariable> abstractVariablesInMatcher
    ) {
        outerLoop: for (final TermPair pair : parent.knowledge().nonUnifiable) {
            final PrologTerm matched1 = matcher.apply(pair.term1());
            final PrologTerm matched2 = matcher.apply(pair.term2());
            for (final TermPair nonUnifyingInChild : child.knowledge().nonUnifiable) {
                if (
                    (nonUnifyingInChild.term1().equals(matched1) && nonUnifyingInChild.term2().equals(matched2))
                    || (nonUnifyingInChild.term1().equals(matched2) && nonUnifyingInChild.term2().equals(matched1))
                ) {
                    continue outerLoop;
                }
            }
            for (final TermPair nonUnifyingInChild : child.knowledge().nonUnifiable) {
                if (
                    KnowledgeBase.isNonAbstractAndUnusedRenaming(
                        matched1,
                        matched2,
                        nonUnifyingInChild,
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
        for (final PrologVariable variable : parent.knowledge().free) {
            PrologVariable variableToAdd = null;
            if (matcher.containsKey(variable)) {
                variableToAdd = (PrologVariable)matcher.get(variable);
                if (!child.knowledge().isFree(variableToAdd)) {
                    return false;
                }
            } else {
                if (!child.knowledge().isFree(variable)) {
                    if (!nonAbstractVariablesInMatcher.contains(variable)) {
                        final Set<PrologVariable> free = new LinkedHashSet<PrologVariable>(child.knowledge().free);
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
        for (final PrologAbstractVariable variable : parent.knowledge().ground) {
            if (matcher.containsKey(variable)) {
                if (!child.knowledge().isGround(matcher.get(variable))) {
                    return false;
                }
            } else if (!child.knowledge().isGround(variable)) {
                if (
                    parent.containsVariableInGoal(variable)
                    || child.containsVariable(variable)
                    || abstractVariablesInMatcher.contains(variable)
                ) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isNonAbstractAndUnusedRenaming(
        final PrologSubstitution matcher1,
        final PrologSubstitution matcher2,
        final Set<PrologVariable> nonAbstractVariablesInMatcher,
        final Set<PrologAbstractVariable> abstractVariablesInMatcher,
        final PrologSubstitution matcher
    ) {
        final PrologSubstitution renaming = PrologSubstitution.combineToConsistentVariableRenaming(matcher1, matcher2);
        final Set<PrologVariable> nextNonAbstractVariablesInMatcher = new LinkedHashSet<PrologVariable>();
        final Set<PrologAbstractVariable> nextAbstractVariablesInMatcher = new LinkedHashSet<PrologAbstractVariable>();
        boolean ok = true;
        if (renaming != null) {
            for (final Map.Entry<PrologVariable, PrologTerm> entry : renaming.entrySet()) {
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
                for (final Map.Entry<PrologVariable, PrologTerm> entry : renaming.entrySet()) {
                    matcher.put(entry.getKey(), entry.getValue());
                }
                nonAbstractVariablesInMatcher.addAll(nextNonAbstractVariablesInMatcher);
                abstractVariablesInMatcher.addAll(nextAbstractVariablesInMatcher);
                return true;
            }
        }
        return false;
    }

    private static boolean isNonAbstractAndUnusedRenaming(
        final PrologTerm matched1,
        final PrologTerm matched2,
        final TermPair nonUnifyingInChild,
        final Set<PrologVariable> nonAbstractVariablesInMatcher,
        final Set<PrologAbstractVariable> abstractVariablesInMatcher,
        final PrologSubstitution matcher
    ) {
        return
            KnowledgeBase.isNonAbstractAndUnusedRenaming(
                Terms.calculateMatcherWithAbstractVariables(matched1, nonUnifyingInChild.term1()),
                Terms.calculateMatcherWithAbstractVariables(matched2, nonUnifyingInChild.term2()),
                nonAbstractVariablesInMatcher,
                abstractVariablesInMatcher,
                matcher
            )
            || KnowledgeBase.isNonAbstractAndUnusedRenaming(
                Terms.calculateMatcherWithAbstractVariables(matched1, nonUnifyingInChild.term1()),
                Terms.calculateMatcherWithAbstractVariables(matched2, nonUnifyingInChild.term2()),
                nonAbstractVariablesInMatcher,
                abstractVariablesInMatcher,
                matcher
            );
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

    public KnowledgeBase removeFree(final Set<PrologVariable> variables) {
        final Set<PrologVariable> newFree = new TreeSet<PrologVariable>(PrologTermComparator.INSTANCE);
        newFree.addAll(this.free);
        newFree.removeAll(variables);
        return new KnowledgeBase(this.ground, this.nonUnifiable, newFree);
    }

    public PrologSubstitution restrictToFree(final PrologSubstitution substitution) {
        return substitution.restrict(this.free);
    }

    public PrologSubstitution restrictToGround(final PrologSubstitution substitution) {
        return substitution.restrict(this.ground);
    }

    public Set<PrologVariable> restrictToNonFree(final Set<PrologVariable> variables) {
        final Set<PrologVariable> result = new LinkedHashSet<PrologVariable>(variables);
        result.removeAll(this.free);
        return result;
    }

    public Set<PrologAbstractVariable> restrictToNonGround(final Set<PrologAbstractVariable> variables) {
        final Set<PrologAbstractVariable> result = new LinkedHashSet<PrologAbstractVariable>(variables);
        result.removeAll(this.ground);
        return result;
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
