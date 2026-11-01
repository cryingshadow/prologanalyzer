package prologanalyzer;

import java.util.*;

import prologanalyzer.structure.*;
import prologparser.*;

public abstract class GroundnessAnalysis {

    private static record GroundnessInformation(FunctionSymbol symbol, Set<Integer> groundPositions) {}

    public static Set<Integer> getGroundPositions(
        final FunctionSymbol symbol,
        final Set<Integer> groundPositions,
        final PrologProgram program
    ) {
        return GroundnessAnalysis.getGroundPositions(
            symbol,
            groundPositions,
            new LinkedHashMap<GroundnessInformation, Set<Integer>>(),
            program
        );
    }

    private static void calculateCandidatesFromFacts(
        final Set<PrologTerm> facts,
        final Set<Integer> groundPositions,
        final int arity,
        final Set<Integer> candidates
    ) {
        for (final PrologTerm fact : facts) {
            final Set<PrologVariable> groundVars = new LinkedHashSet<PrologVariable>();
            for (final Integer i : groundPositions) {
                if (i >= 0 && i < arity) {
                    groundVars.addAll(Terms.createSetOfAllNonAbstractVariables(fact.arguments.get(i)));
                }
            }
            for (int i = 0; i < arity; i++) {
                if (!groundPositions.contains(i)
                    && !groundVars.containsAll(Terms.createSetOfAllNonAbstractVariables(fact.arguments.get(i))))
                {
                    candidates.remove(i);
                }
            }
        }
    }

    private static Set<Integer> calculateResultFromRules(
        final Set<PrologClause> rules,
        final GroundnessInformation groundnessInformation,
        final Map<GroundnessInformation, Set<Integer>> candidates,
        final PrologProgram program
    ) {
        Set<Integer> toDelete = null;
        start: do {
            toDelete = new LinkedHashSet<Integer>();
            for (final PrologClause rule : rules) {
                final Set<PrologVariable> groundVars = new LinkedHashSet<PrologVariable>();
                final PrologTerm head = rule.head();
                for (final Integer groundPosition : groundnessInformation.groundPositions) {
                    groundVars.addAll(Terms.createSetOfAllNonAbstractVariables(head.arguments.get(groundPosition)));
                }
                for (final PrologTerm term : Terms.createConjunctionListOfPredications(rule.body().get())) {
                    final FunctionSymbol symbol = Terms.getFunctionSymbol(term);
                    final Set<Integer> groundPositions = new LinkedHashSet<Integer>();
                    for (int i = 0; i < symbol.arity(); i++) {
                        if (groundVars.containsAll(Terms.createSetOfAllNonAbstractVariables(term.arguments.get(i)))) {
                            groundPositions.add(i);
                        }
                    }
                    final GroundnessInformation nextGroundnessInformation =
                        new GroundnessInformation(symbol, groundPositions);
                    if (!candidates.containsKey(nextGroundnessInformation)) {
                        candidates.put(
                            nextGroundnessInformation,
                            GroundnessAnalysis.getGroundPositions(symbol, groundPositions, candidates, program)
                        );
                    }
                    for (final Integer nextGroundPosition : candidates.get(nextGroundnessInformation)) {
                        groundVars.addAll(
                            Terms.createSetOfAllNonAbstractVariables(term.arguments.get(nextGroundPosition))
                        );
                    }
                }
                for (int i = 0; i < groundnessInformation.symbol.arity(); i++) {
                    if (!groundVars.containsAll(Terms.createSetOfAllNonAbstractVariables(head.arguments.get(i)))) {
                        toDelete.add(i);
                    }
                }
                toDelete.retainAll(candidates.get(groundnessInformation));
                if (!toDelete.isEmpty()) {
                    candidates.get(groundnessInformation).removeAll(toDelete);
                    continue start;
                }
            }
        } while (!toDelete.isEmpty());
        return candidates.get(groundnessInformation);
    }

    private static Set<Integer> getGroundPositions(
        final FunctionSymbol symbol,
        final Set<Integer> groundPositions,
        final Map<GroundnessInformation, Set<Integer>> candidates,
        final PrologProgram program
    ) {
        final Set<PrologTerm> facts = new LinkedHashSet<PrologTerm>();
        final Set<PrologClause> rules = new LinkedHashSet<PrologClause>();
        for (final PrologClause clause : ProgramSlicer.slice(symbol, program)) {
            if (clause.body().isEmpty()) {
                facts.add(clause.head());
            } else {
                rules.add(clause);
            }
        }
        final Set<Integer> candidateSet = new LinkedHashSet<Integer>();
        for (int i = 0; i < symbol.arity(); i++) {
            candidateSet.add(i);
        }
        GroundnessAnalysis.calculateCandidatesFromFacts(facts, groundPositions, symbol.arity(), candidateSet);
        final GroundnessInformation groundnessInformation = new GroundnessInformation(symbol, groundPositions);
        candidates.put(groundnessInformation, candidateSet);
        return GroundnessAnalysis.calculateResultFromRules(rules, groundnessInformation, candidates, program);
    }

}
