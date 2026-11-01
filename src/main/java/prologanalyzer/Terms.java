package prologanalyzer;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import prologanalyzer.structure.*;
import prologparser.*;

public abstract class Terms {

    public static final String DEFAULT_ABSTRACT_VARIABLE_NAME = "T";

    public static final String DEFAULT_NON_ABSTRACT_VARIABLE_NAME = "X";

    public static PrologSubstitution calculateMatcher(final PrologTerm from, final PrologTerm to) {
        return Terms.calculateMatcher(from, to, false);
    }

    public static PrologSubstitution calculateMatcherWithAbstractVariables(final PrologTerm from, final PrologTerm to) {
        return Terms.calculateMatcher(from, to, true);
    }

    public static Set<Integer> computeActiveCuts(final PrologTerm term) {
        if (term instanceof final LabeledCut cut) {
            return Set.of(cut.scope);
        }
        return term
            .arguments
            .stream()
            .flatMap(argument -> Terms.computeActiveCuts(argument).stream())
            .collect(Collectors.toSet());
    }

    public static boolean containsCut(final PrologTerm term) {
        if (Terms.isCut(term)) {
            return true;
        } else if (Terms.isGoalJunctor(term)) {
            for (final PrologTerm argument : term.arguments) {
                if (Terms.containsCut(argument)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static List<PrologTerm> createConjunctionListOfPredications(final PrologTerm term) {
        return Terms.createConjunctionListOfPredications(term, new ArrayList<PrologTerm>());
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

    public static PrologClause fresh(final PrologClause clause, final FreshNameGenerator fridge) {
        final Set<PrologVariable> variables = Terms.createSetOfAllVariables(clause.head());
        if (clause.body().isPresent()) {
            variables.addAll(Terms.createSetOfAllVariables(clause.body().get()));
        }
        final PrologSubstitution renaming = new PrologSubstitution();
        for (final PrologVariable variable : variables) {
            renaming.put(variable, new PrologVariable(fridge.getFreshName(Terms.DEFAULT_NON_ABSTRACT_VARIABLE_NAME)));
        }
        return new PrologClause(renaming.apply(clause.head()), clause.body().map(renaming::apply));
    }

    public static Set<String> getAllVariableNames(final PrologProgram program) {
        final Set<String> result = new LinkedHashSet<String>();
        for (final PrologClause clause : program.clauses()) {
            for (final PrologVariable variable : Terms.createSetOfAllVariables(clause.head())) {
                result.add(variable.name);
            }
            if (clause.body().isPresent()) {
                for (final PrologVariable variable : Terms.createSetOfAllVariables(clause.body().get())) {
                    result.add(variable.name);
                }
            }
        }
        for (final PrologTerm directive : program.directives()) {
            for (final PrologVariable variable : Terms.createSetOfAllVariables(directive)) {
                result.add(variable.name);
            }
        }
        return result;
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

    public static FunctionSymbol getFunctionSymbol(final PrologTerm term) {
        return new FunctionSymbol(term.name, Terms.getArity(term));
    }

    public static boolean hasAbstractVariableOnPredicationPosition(final PrologTerm term) {
        if (Terms.isGoalJunctor(term)) {
            return term.arguments.stream().allMatch(Terms::hasAbstractVariableOnPredicationPosition);
        }
        if (Terms.isAbstractVariable(term)) {
            return true;
        }
        return false;
    }

    public static boolean haveSameFunctionSymbol(final FunctionSymbol symbol, final PrologTerm term) {
        return symbol.name().equals(term.name) && symbol.arity() == Terms.getArity(term);
    }

    public static boolean haveSameFunctionSymbol(final PrologTerm term1, final PrologTerm term2) {
        return Terms.haveSameFunctionSymbol(Terms.getFunctionSymbol(term1), term2);
    }

    public static boolean isAbstractVariable(final PrologTerm term) {
        return term instanceof PrologAbstractVariable;
    }

    public static boolean isConjunction(final PrologTerm term) {
        return ",".equals(term.name) && Terms.getArity(term) == 2;
    }

    public static boolean isConstant(final PrologTerm term) {
        return Terms.getArity(term) == 0 && !Terms.isVariable(term);
    }

    public static boolean isCut(final PrologTerm term) {
        return Terms.getArity(term) == 0 && "!".equals(term.name);
    }

    public static boolean isCyclic(final PrologTerm term) {
        if (term instanceof PrologCyclicTerm) {
            return true;
        }
        return term.arguments.stream().anyMatch(Terms::isCyclic);
    }

    public static boolean isEmptyList(final PrologTerm term) {
        return Terms.getArity(term) == 0 && "[]".equals(term.name);
    }

    public static boolean isGoalJunctor(final PrologTerm term) {
        switch (term.name) {
        case ",":
        case ";":
        case "->":
            return Terms.getArity(term) == 2;
        default:
            return false;
        }
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

    public static PrologTerm removeFirstConjunct(final PrologTerm term) {
        if (Terms.isConjunction(term)) {
            final PrologTerm first = term.arguments.getFirst();
            if (Terms.isConjunction(first)) {
                final List<PrologTerm> arguments = new ArrayList<PrologTerm>();
                arguments.add(Terms.removeFirstConjunct(first));
                arguments.add(term.arguments.getLast());
                return new PrologTerm(",", arguments);
            }
            return term.arguments.getLast();
        }
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
        if (Terms.isConjunction(term)) {
            final PrologTerm first = term.arguments.getFirst();
            if (Terms.isConjunction(first)) {
                final List<PrologTerm> arguments = new ArrayList<PrologTerm>();
                arguments.add(Terms.replaceFirstConjunct(first, newFirstConjunct));
                arguments.add(term.arguments.getLast());
                return new PrologTerm(",", arguments);
            }
            return new PrologTerm(",", List.of(newFirstConjunct, term.arguments.getLast()));
        }
        return newFirstConjunct;
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

    private static PrologSubstitution calculateMatcher(
        final PrologTerm from,
        final PrologTerm to,
        final boolean keepRangeAbstract
    ) {
        final PrologSubstitution matcher = new PrologSubstitution();
        if (from.equals(to)) {
            return matcher;
        } else if ((keepRangeAbstract && Terms.isVariable(from)) || Terms.isNonAbstractVariable(from)) {
            matcher.put((PrologVariable)from, to);
            return matcher;
        } else if (Terms.haveSameFunctionSymbol(from, to)) {
            for (int i = 0; i < Terms.getArity(from); i++) {
                final PrologSubstitution argumentMatcher =
                    Terms.calculateMatcher(from.arguments.get(i), to.arguments.get(i), keepRangeAbstract);
                if (argumentMatcher == null) {
                    return null;
                } else {
                    for (final Map.Entry<PrologVariable, PrologTerm> entry : argumentMatcher.entrySet()) {
                        if (matcher.containsKey(entry.getKey())) {
                            if (!matcher.get(entry.getKey()).equals(entry.getValue())) {
                                return null;
                            }
                        } else {
                            matcher.put(entry.getKey(), entry.getValue());
                        }
                    }
                }
            }
            return matcher;
        } else {
            return null;
        }
    }

    private static List<PrologTerm> createConjunctionListOfPredications(
        final PrologTerm term,
        final List<PrologTerm> predications
    ) {
        if (Terms.isConjunction(term)) {
            predications.addAll(Terms.createConjunctionListOfPredications(term.arguments.getFirst()));
            return Terms.createConjunctionListOfPredications(term.arguments.getLast(), predications);
        } else {
            predications.add(term);
            return predications;
        }
    }

}
