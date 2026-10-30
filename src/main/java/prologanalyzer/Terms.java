package prologanalyzer;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import prologparser.*;

public abstract class Terms {

    public static final String DEFAULT_ABSTRACT_VARIABLE_NAME = "T";

    public static final String DEFAULT_NON_ABSTRACT_VARIABLE_NAME = "X";

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
        switch (term.name) {
        case ",":
            return true;
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
        if (Terms.isConjunction(term)) {
            final PrologTerm first = term.arguments.getFirst();
            if (Terms.isConjunction(first)) {
                final List<PrologTerm> arguments = new ArrayList<PrologTerm>();
                arguments.add(Terms.removeFirstConjunct(first));
                arguments.addAll(term.arguments.stream().skip(1).toList());
                return new PrologTerm(",", arguments);
            }
            final int arity = Terms.getArity(term);
            if (arity == 2) {
                return term.arguments.getLast();
            }
            if (arity == 1) {
                return null;
            }
            return new PrologTerm(",", term.arguments.stream().skip(1).toList());
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
                arguments.addAll(term.arguments.stream().skip(1).toList());
                return new PrologTerm(",", arguments);
            }
            final int arity = Terms.getArity(term);
            if (arity == 2) {
                return new PrologTerm(",", List.of(newFirstConjunct, term.arguments.getLast()));
            }
            if (arity == 1) {
                return newFirstConjunct;
            }
            final List<PrologTerm> arguments = new ArrayList<PrologTerm>(term.arguments);
            arguments.set(0, newFirstConjunct);
            return new PrologTerm(",", arguments);
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

}
