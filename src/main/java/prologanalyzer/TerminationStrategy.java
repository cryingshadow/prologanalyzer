package prologanalyzer;

import java.util.*;

import prologanalyzer.evaluationrules.*;
import prologanalyzer.structure.*;
import prologparser.*;

public class TerminationStrategy implements SymbolicEvaluationStrategy {

    private static final List<SymbolicEvaluationRule> SIMPLE_RULES =
        List.of(
            SuccessRule.INSTANCE,
            FailureRule.INSTANCE,
            CutRule.INSTANCE,
            CutAllRule.INSTANCE,
            UndefinedErrorRule.INSTANCE,
            VariableErrorRule.INSTANCE,
            CallRule.INSTANCE,
            BacktrackRule.INSTANCE,
            OnlyEvalRule.INSTANCE
        );

    private static Optional<SymbolicEvaluationExpansion> applySimpleRules(
        final AbstractState currentState,
        final GlobalState global
    ) {
        for (final SymbolicEvaluationRule rule : TerminationStrategy.SIMPLE_RULES) {
            final List<SymbolicEvaluationResult> expansion = rule.apply(currentState, global);
            if (!expansion.isEmpty()) {
                final SymbolicEvaluationResult singleExpansion = expansion.getFirst();
                return Optional.of(
                    new SymbolicEvaluationExpansion(singleExpansion.state(), rule, singleExpansion.substitution())
                );
            }
        }
        return Optional.empty();
    }

    private static void evaluate(
        final Vertex<AbstractState> currentVertex,
        final AbstractState state,
        final String rule,
        final PrologSubstitution substitution,
        final Graph<AbstractState, SymbolicEvaluationEdgeLabel> graph,
        final GlobalState global
    ) {
        final Vertex<AbstractState> newVertex = new Vertex<AbstractState>(state);
        graph.addVertex(newVertex);
        graph.addEdge(currentVertex, Optional.of(new SymbolicEvaluationEdgeLabel(rule, substitution)), newVertex);
        TerminationStrategy.evaluate(newVertex, graph, global);
    }

    private static void evaluate(
        final Vertex<AbstractState> currentVertex,
        final Graph<AbstractState, SymbolicEvaluationEdgeLabel> graph,
        final GlobalState global
    ) {
        final AbstractState currentState = currentVertex.label().get();
        if (currentState.isEmpty()) {
            return;
        }
        final Optional<SymbolicEvaluationExpansion> optionalExpansion =
            TerminationStrategy.applySimpleRules(currentState, global);
        if (optionalExpansion.isPresent()) {
            final SymbolicEvaluationExpansion singleExpansion = optionalExpansion.get();
            TerminationStrategy.evaluate(
                currentVertex,
                singleExpansion.state(),
                singleExpansion.rule().toString(),
                singleExpansion.substitution(),
                graph,
                global
            );
            return;
        }
        List<SymbolicEvaluationResult> expansion = EvalRule.INSTANCE.apply(currentState, global);
        if (!expansion.isEmpty()) {
            TerminationStrategy.evaluate(currentVertex, expansion, EvalRule.INSTANCE.toString(), graph, global);
            return;
        }
        if (
            currentState.goal().size() > 1
            && ParallelRule.computeActiveCuts(List.of(currentState.goal().getFirst())).isEmpty()
        ) {
            final ParallelRule rule = new ParallelRule(1);
            expansion = rule.apply(currentState, global);
            if (!expansion.isEmpty()) {
                TerminationStrategy.evaluate(currentVertex, expansion, rule.toString(), graph, global);
                return;
            }
        }
        if (TerminationStrategy.isSingleConjunct(currentState)) {
            for (final Vertex<AbstractState> vertex : graph.getVertices()) {
                if (vertex == currentVertex) {
                    continue;
                }
                final AbstractState otherState = vertex.label().get();
                if (TerminationStrategy.isSingleConjunct(otherState)) {
                    final InstanceRule rule = new InstanceRule(otherState, true);
                    expansion = rule.apply(currentState, global);
                    if (!expansion.isEmpty()) {
                        graph.addEdge(
                            currentVertex,
                            Optional.of(
                                new SymbolicEvaluationEdgeLabel(rule.toString(), expansion.getFirst().substitution())
                            ),
                            vertex
                        );
                    }
                }
            }
            for (final Vertex<AbstractState> vertex : graph.getVertices()) {
                if (vertex == currentVertex) {
                    continue;
                }
                final AbstractState otherState = vertex.label().get();
                if (TerminationStrategy.isSingleConjunct(otherState)) {
                    final InstanceRule rule = new InstanceRule(otherState, false);
                    expansion = rule.apply(currentState, global);
                    if (!expansion.isEmpty()) {
                        graph.addEdge(
                            currentVertex,
                            Optional.of(
                                new SymbolicEvaluationEdgeLabel(rule.toString(), expansion.getFirst().substitution())
                            ),
                            vertex
                        );
                    }
                }
            }
        }
        // if generalize somehow
        // TODO Auto-generated method stub
        expansion = CaseRule.INSTANCE.apply(currentState, global);
        if (!expansion.isEmpty()) {
            TerminationStrategy.evaluate(currentVertex, expansion, CaseRule.INSTANCE.toString(), graph, global);
            return;
        }
        throw new IllegalStateException("Could not expand non-leaf state!");
    }

    private static void evaluate(
        final Vertex<AbstractState> currentVertex,
        final List<SymbolicEvaluationResult> expansion,
        final String rule,
        final Graph<AbstractState, SymbolicEvaluationEdgeLabel> graph,
        final GlobalState global
    ) {
        for (final SymbolicEvaluationResult nextExpansion : expansion) {
            TerminationStrategy.evaluate(
                currentVertex,
                nextExpansion.state(),
                rule,
                nextExpansion.substitution(),
                graph,
                global
            );
        }
    }

    private static boolean isSingleConjunct(final AbstractState state) {
        return
            state.goal().size() == 1
            && state.goal().getFirst().hasTerm()
            && !state.goal().getFirst().hasClause()
            && !Terms.isConjunction(state.goal().getFirst().getTerm());
    }

    @Override
    public Graph<AbstractState, SymbolicEvaluationEdgeLabel> evaluate(final PrologProgram program) {
        final GlobalState global =
            new GlobalState(program, new FreshNameGenerator(Terms.getAllVariableNames(program)), new ScopeGenerator());
        final AbstractState rootState = QueryParser.parseQuery(global);
        final Graph<AbstractState, SymbolicEvaluationEdgeLabel> graph =
            new Graph<AbstractState, SymbolicEvaluationEdgeLabel>();
        final Vertex<AbstractState> root = new Vertex<AbstractState>(rootState);
        graph.addVertex(root);
        TerminationStrategy.evaluate(root, graph, global);
        return graph;
    }

}
