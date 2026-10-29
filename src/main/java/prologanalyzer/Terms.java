package prologanalyzer;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import prologparser.*;

public abstract class Terms {

    public static final String DEFAULT_ABSTRACT_VARIABLE_NAME = "T";

    public static final String DEFAULT_NON_ABSTRACT_VARIABLE_NAME = "X";

    public static Optional<PrologSubstitution> computeMGUWithOnlyFreshVariables(
        final PrologTerm term1,
        final PrologTerm term2,
        final FreshNameGenerator fridge
    ) {
        return Terms.computeMGUwithOnlyFreshVariables(
            term1,
            term2,
            false,
            true,
            new LinkedHashSet<PrologVariable>(),
            fridge
        );
    }

    public static Set<PrologAbstractVariable> createSetOfAllAbstractVariables(final PrologTerm term) {
        return Terms.createSetOfAllOccurrences(t -> Terms.isAbstractVariable(t), term);
    }

    public static Set<PrologVariable> createSetOfAllNonAbstractVariables(final PrologTerm term) {
        return Terms.createSetOfAllOccurrences(t -> Terms.isNonAbstractVariable(t), term);
    }

    @SuppressWarnings("unchecked")
    public static <T> Set<T> createSetOfAllOccurrences(final Predicate<PrologTerm> identifier, final PrologTerm term) {
        if (identifier.test(term)) {
            return Set.of((T)term);
        }
        final Set<T> res = new LinkedHashSet<T>();
        for (final PrologTerm argument : term.arguments) {
            res.addAll(Terms.createSetOfAllOccurrences(identifier, argument));
        }
        return res;
    }

    public static Set<PrologVariable> createSetOfAllVariables(final PrologTerm term) {
        return Terms.createSetOfAllOccurrences(t -> Terms.isVariable(t), term);
    }

    public static int getArity(final PrologTerm term) {
        return term.arguments.size();
    }

    public static List<PrologTerm> getConjuncts(final PrologTerm term) {
        if (Terms.isConjunction(term)) {
            return term.arguments.stream().flatMap(a -> Terms.getConjuncts(a).stream()).toList();
        }
        return List.of(term);
    }

    public static PrologTerm getFirstConjunct(final PrologTerm term) {
        if (Terms.isConjunction(term)) {
            return Terms.getFirstConjunct(term.arguments.getFirst());
        }
        return term;
    }

    public static boolean isAbstractVariable(final PrologTerm term) {
        return term instanceof PrologAbstractVariable;
    }

    public static boolean isConjunction(final PrologTerm term) {
        return ",".equals(term.name);
    }

    public static boolean isConstant(final PrologTerm term) {
        return Terms.getArity(term) == 0 && !Terms.isVariable(term);
    }

    public static boolean isCut(final PrologTerm term) {
        return Terms.getArity(term) == 0 && "!".equals(term.name);
    }

    public static boolean isEmptyList(final PrologTerm term) {
        return Terms.getArity(term) == 0 && "[]".equals(term.name);
    }

    public static boolean isGoalJunctor(final PrologTerm term) {
        // TODO Auto-generated method stub
        return false;
    }

    public static boolean isNonAbstractVariable(final PrologTerm term) {
        return Terms.isVariable(term) && !Terms.isAbstractVariable(term);
    }

    public static boolean isVariable(final PrologTerm term) {
        return term instanceof PrologVariable;
    }

    public static boolean occurs(final String functionSymbol, final int arity, final PrologTerm inTerm) {
        if (inTerm.name.equals(functionSymbol) && inTerm.arguments.size() == arity) {
            return true;
        }
        for (final PrologTerm argument : inTerm.arguments) {
            if (Terms.occurs(functionSymbol, arity, argument)) {
                return true;
            }
        }
        return false;
    }

    public static AbstractState parseQuery(final PrologProgram program, final FreshNameGenerator fridge) {
        for (final String comment : program.comments()) {
            if (comment.startsWith("%query: ")) {
                return Terms.parseQuery(comment.substring(8), fridge);
            }
        }
        return null;
    }

    public static AbstractState parseQuery(final String query, final FreshNameGenerator fridge) {
        final int indexOpen = query.indexOf('(');
        if (indexOpen < 0) {
            return new AbstractState(
                List.of(new TermGoalElement(new PrologTerm(query, List.of()))),
                new KnowledgeBase()
            );
        }
        final String[] argumentStrings = query.substring(indexOpen + 1, query.indexOf(')')).split(",");
        KnowledgeBase knowledge = new KnowledgeBase();
        final List<PrologTerm> arguments = new ArrayList<PrologTerm>();
        for (final String argument : argumentStrings) {
            final PrologAbstractVariable variable =
                new PrologAbstractVariable(fridge.getFreshName(Terms.DEFAULT_ABSTRACT_VARIABLE_NAME));
            arguments.add(variable);
            switch (argument.strip().toLowerCase()) {
            case "g":
            case "i":
            case "b":
                knowledge = knowledge.setGround(variable);
                break;
            case "a":
            case "o":
            case "f":
                break;
            default:
                throw new IllegalArgumentException("Could not parse query!");
            }
        }
        return new AbstractState(
            List.of(new TermGoalElement(new PrologTerm(query.substring(0, indexOpen), arguments))),
            knowledge
        );
    }

    public static PrologTerm removeFirstConjunct(final PrologTerm term) {
        // TODO Auto-generated method stub
        return null;
    }

    public static PrologTerm replaceAll(
        final PrologTerm toReplace,
        final PrologTerm byReplacement,
        final PrologTerm inTerm
    ) {
        if (inTerm.equals(toReplace)) {
            return byReplacement;
        } else if (Terms.getArity(inTerm) == 0) {
            return inTerm;
        } else {
            final List<PrologTerm> arguments = new ArrayList<PrologTerm>();
            for (final PrologTerm argument : inTerm.arguments) {
                arguments.add(Terms.replaceAll(toReplace, byReplacement, argument));
            }
            return new PrologTerm(inTerm.name, arguments);
        }
    }

    public static PrologTerm replaceArgument(
        final int argumentIndex,
        final PrologTerm byReplacement,
        final PrologTerm inTerm
    ) {
        final List<PrologTerm> arguments = new ArrayList<PrologTerm>(inTerm.arguments);
        arguments.set(argumentIndex, byReplacement);
        return new PrologTerm(inTerm.name, arguments);
    }

    public static PrologTerm replaceFirstConjunct(final PrologTerm term, final PrologTerm newFirstConjunct) {
        // TODO Auto-generated method stub
        return null;
    }

    public static String toLaTeX(final PrologTerm term, final KnowledgeBase knowledge) {
        if (Terms.getArity(term) == 0) {
            if (Terms.isCut(term)) {
                return "\\textbf{!}";
            } else if (Terms.isEmptyList(term)) {
                return "\\textbf{[]}";
            } else {
                return String.format("\\code{%s}", term.name);
            }
        }
        final StringBuilder res = new StringBuilder();
        if (Terms.isConjunction(term)) {
            res.append(
                Terms
                .getConjuncts(term)
                .stream()
                .map(c -> Terms.toLaTeX(c, knowledge))
                .collect(Collectors.joining(", "))
            );
        } else {
            res.append(
                String.format(
                    "\\code{%s}(%s)",
                    term.name,
                    term.arguments.stream().map(a -> Terms.toLaTeX(a, knowledge)).collect(Collectors.joining(", "))
                )
            );
        }
        return res.toString();
    }

    public static PrologTerm transformWithScope(final PrologTerm term, final int scope) {
        if (term == null) {
            return null;
        }
        if (Terms.isGoalJunctor(term)) {
            return new PrologTerm(
                term.name,
                term.arguments.stream().map(argument -> Terms.transformWithScope(argument, scope)).toList()
            );
        }
        if (Terms.isNonAbstractVariable(term)) {
            return new PrologTerm("call", List.of(term));
        }
        if (Terms.isCut(term)) {
            return new LabeledCut(scope);
        }
        return term;
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
                    Terms.computeMGUwithOnlyFreshVariables(
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
                    final TermPair pair = Terms.combineMGUforArguments(term1, term2, i, optionalMGU.get(), res);
                    term1 = pair.term1();
                    term2 = pair.term2();
                }
            }
            return Optional.of(res);
        }
        return Optional.empty();
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

}
