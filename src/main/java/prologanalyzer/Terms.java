package prologanalyzer;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import prologparser.*;

public abstract class Terms {

    public static Optional<PrologSubstitution> computeMGU(
        final PrologTerm term1,
        final PrologTerm term2,
        final FreshNameGenerator fridge,
        final KnowledgeBase knowledge
    ) {
        // TODO Auto-generated method stub
        return null;
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

    public static boolean isCut(final PrologTerm term) {
        return Terms.getArity(term) == 0 && "!".equals(term.name);
    }

    public static boolean isEmptyList(final PrologTerm term) {
        return Terms.getArity(term) == 0 && "[]".equals(term.name);
    }

    public static boolean isNonAbstractVariable(final PrologTerm term) {
        return Terms.isVariable(term) && !Terms.isAbstractVariable(term);
    }

    public static boolean isVariable(final PrologTerm term) {
        return term instanceof PrologVariable;
    }

    public static PrologTerm removeFirstConjunct(final PrologTerm term) {
        // TODO Auto-generated method stub
        return null;
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

}
