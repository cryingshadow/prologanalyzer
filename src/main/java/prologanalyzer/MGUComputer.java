package prologanalyzer;

import java.util.*;

import prologparser.*;

public class MGUComputer {

    private static final boolean PREFER_NON_ABSTRACT_REPLACEMENT = true;

    private static final boolean THROW_EXCEPTION_ON_OCCURS_CHECK = true;

    private static final boolean USE_OCCURS_CHECK_BY_DEFAULT = false;

    public static Optional<PrologSubstitution> computeMGUWithOnlyFreshVariables(
        final PrologTerm term1,
        final PrologTerm term2,
        final boolean useOccursCheck,
        final FreshNameGenerator fridge
    ) {
        return MGUComputer.computeMGUwithOnlyFreshVariables(
            term1,
            term2,
            useOccursCheck,
            MGUComputer.PREFER_NON_ABSTRACT_REPLACEMENT,
            new LinkedHashSet<PrologVariable>(),
            fridge
        );
    }

    public static Optional<PrologSubstitution> computeMGUWithOnlyFreshVariables(
        final PrologTerm term1,
        final PrologTerm term2,
        final FreshNameGenerator fridge
    ) {
        return MGUComputer.computeMGUwithOnlyFreshVariables(
            term1,
            term2,
            MGUComputer.USE_OCCURS_CHECK_BY_DEFAULT,
            MGUComputer.PREFER_NON_ABSTRACT_REPLACEMENT,
            new LinkedHashSet<PrologVariable>(),
            fridge
        );
    }

    private static TermPair combineMGUforArguments(
        final PrologTerm initalTerm1,
        final PrologTerm initialTerm2,
        final int i,
        final PrologSubstitution mgu,
        final PrologSubstitution res
    ) {
        PrologTerm term1 = initalTerm1;
        PrologTerm term2 = initialTerm2;
        for (final Map.Entry<PrologVariable, PrologTerm> entry : mgu.entrySet()) {
            for (final Map.Entry<PrologVariable, PrologTerm> e : res.entrySet()) {
                res.put(e.getKey(), Terms.replaceAll(entry.getKey(), entry.getValue(), e.getValue()));
            }
            res.put(entry.getKey(), entry.getValue());
            for (int j = i + 1; j < Terms.getArity(term1); j++) {
                term1 =
                    Terms.replaceArgument(
                        j,
                        Terms.replaceAll(entry.getKey(), entry.getValue(), term1.arguments.get(j)),
                        term1
                    );
                term2 =
                    Terms.replaceArgument(
                        j,
                        Terms.replaceAll(entry.getKey(), entry.getValue(), term2.arguments.get(j)),
                        term2
                    );
            }
        }
        return new TermPair(term1, term2);
    }

    private static Optional<PrologSubstitution> computeMGUwithOnlyFreshVariables(
        final PrologTerm initalTerm1,
        final PrologTerm initialTerm2,
        final boolean occursCheck,
        final boolean nonAbstractReplacementPreferred,
        final Set<PrologVariable> freshVars,
        final FreshNameGenerator fridge
    ) {
        PrologTerm term1 = initalTerm1;
        PrologTerm term2 = initialTerm2;
        final PrologSubstitution res = new PrologSubstitution();
        if (Terms.isVariable(term1)) {
            if (Terms.isVariable(term2)) {
                if (term1.equals(term2)) {
                    return Optional.of(res);
                } else {
                    if (nonAbstractReplacementPreferred) {
                        if (
                            freshVars.contains(term1)
                            && (Terms.isAbstractVariable(term1) || Terms.isNonAbstractVariable(term2))
                        ) {
                            res.put((PrologVariable)term2, term1);
                        } else if (
                            freshVars.contains(term2)
                            && (Terms.isAbstractVariable(term2) || Terms.isNonAbstractVariable(term1))
                        ) {
                            res.put((PrologVariable)term1, term2);
                        } else if (Terms.isNonAbstractVariable(term1) && Terms.isNonAbstractVariable(term2)) {
                            final PrologVariable fresh =
                                new PrologVariable(fridge.getFreshName(Terms.DEFAULT_NON_ABSTRACT_VARIABLE_NAME));
                            res.put((PrologVariable)term1, fresh);
                            res.put((PrologVariable)term2, fresh);
                            freshVars.add(fresh);
                        } else {
                            final PrologAbstractVariable fresh =
                                new PrologAbstractVariable(fridge.getFreshName(Terms.DEFAULT_ABSTRACT_VARIABLE_NAME));
                            res.put((PrologVariable)term1, fresh);
                            res.put((PrologVariable)term2, fresh);
                            freshVars.add(fresh);
                        }
                        return Optional.of(res);
                    } else {
                        if (
                            freshVars.contains(term1)
                            && (Terms.isAbstractVariable(term2) || Terms.isNonAbstractVariable(term1))
                        ) {
                            res.put((PrologVariable)term2, term1);
                        } else if (
                            freshVars.contains(term2)
                            && (Terms.isAbstractVariable(term1) || Terms.isNonAbstractVariable(term2))
                        ) {
                            res.put((PrologVariable)term1, term2);
                        } else {
                            final PrologVariable fresh =
                                new PrologVariable(fridge.getFreshName(Terms.DEFAULT_NON_ABSTRACT_VARIABLE_NAME));
                            res.put((PrologVariable)term1, fresh);
                            res.put((PrologVariable)term2, fresh);
                            freshVars.add(fresh);
                        }
                        return Optional.of(res);
                    }
                }
            } else {
                for (final PrologVariable v : Terms.createSetOfAllVariables(term2)) {
                    if (freshVars.contains(v)) {
                        if (Terms.isAbstractVariable(term1) && Terms.isNonAbstractVariable(v)) {
                            final PrologVariable fresh =
                                new PrologAbstractVariable(fridge.getFreshName(Terms.DEFAULT_ABSTRACT_VARIABLE_NAME));
                            res.put(v, fresh);
                            freshVars.add(fresh);
                        }
                    } else {
                        PrologVariable fresh;
                        if (Terms.isAbstractVariable(term1) || Terms.isAbstractVariable(v)) {
                            fresh =
                                new PrologAbstractVariable(fridge.getFreshName(Terms.DEFAULT_ABSTRACT_VARIABLE_NAME));
                        } else {
                            fresh = new PrologVariable(fridge.getFreshName(Terms.DEFAULT_NON_ABSTRACT_VARIABLE_NAME));
                        }
                        res.put(v, fresh);
                        freshVars.add(fresh);
                    }
                }
                if (occursCheck) {
                    if (Terms.occurs(term1.name, Terms.getArity(term1), term2)) {
                        return Optional.empty();
                    } else {
                        res.put((PrologVariable)term1, res.apply(term2));
                        return Optional.of(res);
                    }
                } else {
                    if (Terms.occurs(term1.name, Terms.getArity(term1), term2)) {
                        if (MGUComputer.THROW_EXCEPTION_ON_OCCURS_CHECK) {
                            throw new IllegalArgumentException("Occurs check is relevant here!");
                        }
                        final PrologTerm cycle = res.apply(new PrologCyclicTerm(term2, (PrologVariable)term1));
                        res.put((PrologVariable)term1, cycle);
                        return Optional.of(res);
                    } else {
                        res.put((PrologVariable)term1, res.apply(term2));
                        return Optional.of(res);
                    }
                }
            }
        } else if (Terms.isVariable(term2)) {
            for (final PrologVariable v : Terms.createSetOfAllVariables(term1)) {
                if (freshVars.contains(v)) {
                    if (Terms.isAbstractVariable(term2) && Terms.isNonAbstractVariable(v)) {
                        final PrologVariable fresh =
                            new PrologAbstractVariable(fridge.getFreshName(Terms.DEFAULT_ABSTRACT_VARIABLE_NAME));
                        res.put(v, fresh);
                        freshVars.add(fresh);
                    }
                } else {
                    PrologVariable fresh;
                    if (Terms.isAbstractVariable(term2) || Terms.isAbstractVariable(v)) {
                        fresh = new PrologAbstractVariable(fridge.getFreshName(Terms.DEFAULT_ABSTRACT_VARIABLE_NAME));
                    } else {
                        fresh = new PrologVariable(fridge.getFreshName(Terms.DEFAULT_NON_ABSTRACT_VARIABLE_NAME));
                    }
                    res.put(v, fresh);
                    freshVars.add(fresh);
                }
            }
            if (occursCheck) {
                if (Terms.occurs(term2.name, Terms.getArity(term2), term1)) {
                    return Optional.empty();
                } else {
                    res.put((PrologVariable)term2, res.apply(term1));
                    return Optional.of(res);
                }
            } else {
                if (Terms.occurs(term2.name, Terms.getArity(term2), term1)) {
                    if (MGUComputer.THROW_EXCEPTION_ON_OCCURS_CHECK) {
                        throw new IllegalArgumentException("Occurs check is relevant here!");
                    }
                    final PrologTerm cycle = res.apply(new PrologCyclicTerm(term1, (PrologVariable)term2));
                    res.put((PrologVariable)term2, cycle);
                    return Optional.of(res);
                } else {
                    res.put((PrologVariable)term2, res.apply(term1));
                    return Optional.of(res);
                }
            }
        } else if (term1.name.equals(term2.name) && Terms.getArity(term1) == Terms.getArity(term2)) {
            for (int i = 0; i < Terms.getArity(term1); i++) {
                final Optional<PrologSubstitution> optionalMGU =
                    MGUComputer.computeMGUwithOnlyFreshVariables(
                        term1.arguments.get(i),
                        term2.arguments.get(i),
                        occursCheck,
                        nonAbstractReplacementPreferred,
                        freshVars,
                        fridge
                    );
                if (optionalMGU.isEmpty()) {
                    return optionalMGU;
                } else {
                    final TermPair pair = MGUComputer.combineMGUforArguments(term1, term2, i, optionalMGU.get(), res);
                    term1 = pair.term1();
                    term2 = pair.term2();
                }
            }
            return Optional.of(res);
        }
        return Optional.empty();
    }

}
