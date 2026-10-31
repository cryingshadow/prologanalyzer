package prologanalyzer;

import java.util.*;

import prologanalyzer.structure.*;
import prologparser.*;

public abstract class QueryParser {

    public static AbstractState parseQuery(final GlobalState global) {
        for (final String comment : global.program().comments()) {
            if (comment.startsWith("%query: ")) {
                return QueryParser.parseQuery(comment.substring(8), global.fridge(), global.scoper());
            }
        }
        return null;
    }

    private static AbstractState parseQuery(
        final String query,
        final FreshNameGenerator fridge,
        final ScopeGenerator scopeGenerator
    ) {
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
                knowledge = knowledge.addGround(variable);
                break;
            case "a":
            case "o":
            case "f":
                break;
            default:
                throw new IllegalArgumentException("Could not parse query!");
            }
        }
        final int scope = scopeGenerator.getFreshScope();
        return new AbstractState(
            List.of(
                new TermGoalElement(
                    Terms.transformWithScope(new PrologTerm(query.substring(0, indexOpen), arguments), scope)
                ),
                new ScopeMarker(scope)
            ),
            knowledge
        );
    }

}
