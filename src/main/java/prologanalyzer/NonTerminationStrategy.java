package prologanalyzer;

import java.util.*;

import prologanalyzer.evaluationrules.*;
import prologanalyzer.structure.*;
import prologparser.*;

public class NonTerminationStrategy implements SymbolicEvaluationStrategy {

    private static final List<SymbolicEvaluationRule> SIMPLE_RULES =
        List.of(
            SuccessRule.INSTANCE,
            FailureRule.INSTANCE,
            CutRule.INSTANCE,
            CutAllRule.INSTANCE,
            UndefinedErrorRule.INSTANCE,
            VariableErrorRule.INSTANCE,
            BacktrackRule.INSTANCE,
            OnlyEvalRule.NON_TERMINATION_ONLY_EVAL
        );

    private static Optional<SymbolicEvaluationExpansion> applySimpleRules(
        final AbstractState currentState,
        final GlobalState global
    ) {
        for (final SymbolicEvaluationRule rule : NonTerminationStrategy.SIMPLE_RULES) {
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

    private static boolean isSingleConjunct(final AbstractState state) {
        return
            state.goal().size() == 1
            && state.goal().getFirst().hasTerm()
            && !state.goal().getFirst().hasClause()
            && !Terms.isConjunction(state.goal().getFirst().getTerm());
    }

    private static boolean isSplittable(final AbstractState state) {
        return
            state.goal().size() == 1
            && state.goal().getFirst().hasTerm()
            && !state.goal().getFirst().hasClause()
            && Terms.isConjunction(state.goal().getFirst().getTerm());
    }

    private final int maxNestingForGeneralization;

    private final long start;

    private final long timeout;

    public NonTerminationStrategy(final int maxNestingForGeneralization, final long timeout) {
        this.maxNestingForGeneralization = maxNestingForGeneralization;
        this.timeout = timeout;
        this.start = System.currentTimeMillis();
    }

    @Override
    public EvaluationResult evaluate(final PrologProgram program) {
        final GlobalState global =
            new GlobalState(program, new FreshNameGenerator(Terms.getAllVariableNames(program)), new ScopeGenerator());
        final AbstractState rootState = QueryParser.parseQuery(global);
        final Graph<AbstractState, SymbolicEvaluationEdgeLabel> graph =
            new Graph<AbstractState, SymbolicEvaluationEdgeLabel>();
        final Vertex<AbstractState> root = new Vertex<AbstractState>(rootState);
        graph.addVertex(root);
        try {
            this.evaluate(root, graph, global);
        } catch (final IllegalStateException e) {
            e.printStackTrace();
        }
        return new EvaluationResult(graph, root.id());
    }

    private void evaluate(
        final Vertex<AbstractState> currentVertex,
        final AbstractState state,
        final SymbolicEvaluationRule rule,
        final PrologSubstitution substitution,
        final Graph<AbstractState, SymbolicEvaluationEdgeLabel> graph,
        final GlobalState global
    ) {
        final Vertex<AbstractState> newVertex = new Vertex<AbstractState>(state);
        graph.addVertex(newVertex);
        graph.addEdge(currentVertex, Optional.of(new SymbolicEvaluationEdgeLabel(rule, substitution)), newVertex);
        this.evaluate(newVertex, graph, global);
    }

    private void evaluate(
        final Vertex<AbstractState> currentVertex,
        final Graph<AbstractState, SymbolicEvaluationEdgeLabel> graph,
        final GlobalState global
    ) {
        final AbstractState currentState = currentVertex.label().get();
        if (currentState.isEmpty()) {
            return;
        }
        if (this.timeout > 0 && System.currentTimeMillis() > this.start + this.timeout) {
            return;
        }
        final Optional<SymbolicEvaluationExpansion> optionalExpansion =
            NonTerminationStrategy.applySimpleRules(currentState, global);
        if (optionalExpansion.isPresent()) {
            final SymbolicEvaluationExpansion singleExpansion = optionalExpansion.get();
            this.evaluate(
                currentVertex,
                singleExpansion.state(),
                singleExpansion.rule(),
                singleExpansion.substitution(),
                graph,
                global
            );
            return;
        }
        List<SymbolicEvaluationResult> expansion = EvalRule.NON_TERMINATION_EVAL.apply(currentState, global);
        if (!expansion.isEmpty()) {
            this.evaluate(currentVertex, expansion, EvalRule.NON_TERMINATION_EVAL, graph, global);
            return;
        }
        if (
            currentState.goal().size() > 1
            && ParallelRule.computeActiveCuts(List.of(currentState.goal().getFirst())).isEmpty()
        ) {
            final ParallelRule rule = new ParallelRule(1);
            expansion = rule.apply(currentState, global);
            if (!expansion.isEmpty()) {
                this.evaluate(currentVertex, expansion, rule, graph, global);
                return;
            }
        }
        if (NonTerminationStrategy.isSingleConjunct(currentState)) {
            for (final Vertex<AbstractState> vertex : graph.getVertices()) {
                if (vertex == currentVertex) {
                    continue;
                }
                final AbstractState otherState = vertex.label().get();
                if (NonTerminationStrategy.isSingleConjunct(otherState)) {
                    final InstanceRule rule = new InstanceRule(otherState, true);
                    expansion = rule.apply(currentState, global);
                    if (!expansion.isEmpty()) {
                        graph.addEdge(
                            currentVertex,
                            Optional.of(new SymbolicEvaluationEdgeLabel(rule, expansion.getFirst().substitution())),
                            vertex
                        );
                        return;
                    }
                }
            }
            for (final Vertex<AbstractState> vertex : graph.getVertices()) {
                if (vertex == currentVertex) {
                    continue;
                }
                final AbstractState otherState = vertex.label().get();
                if (NonTerminationStrategy.isSingleConjunct(otherState)) {
                    final InstanceRule rule = new InstanceRule(otherState, false);
                    expansion = rule.apply(currentState, global);
                    if (!expansion.isEmpty()) {
                        graph.addEdge(
                            currentVertex,
                            Optional.of(new SymbolicEvaluationEdgeLabel(rule, expansion.getFirst().substitution())),
                            vertex
                        );
                        return;
                    }
                }
            }
        }
        if (this.nestingIsTooHigh(currentState)) {
            this.generalize(currentVertex, currentState, graph, global);
            return;
        }
        if (currentState.goal().size() > 1) {
            for (int i = 1; i < currentState.goal().size() - 1; i++) {
                final ParallelRule rule = new ParallelRule(i);
                expansion = rule.apply(currentState, global);
                if (!expansion.isEmpty()) {
                    this.evaluate(currentVertex, expansion, rule, graph, global);
                    return;
                }
            }
        }
        if (NonTerminationStrategy.isSplittable(currentState)) {
            expansion = SplitRule.INSTANCE.apply(currentState, global);
            if (!expansion.isEmpty()) {
                this.evaluate(currentVertex, expansion, SplitRule.INSTANCE, graph, global);
                return;
            }
        }
        expansion = CallRule.INSTANCE.apply(currentState, global);
        if (!expansion.isEmpty()) {
            this.evaluate(currentVertex, expansion, CallRule.INSTANCE, graph, global);
            return;
        }
        expansion = CaseRule.INSTANCE.apply(currentState, global);
        if (!expansion.isEmpty()) {
            this.evaluate(currentVertex, expansion, CaseRule.INSTANCE, graph, global);
            return;
        }
        throw new IllegalStateException("Could not expand non-leaf state!");
    }

    private void evaluate(
        final Vertex<AbstractState> currentVertex,
        final List<SymbolicEvaluationResult> expansion,
        final SymbolicEvaluationRule rule,
        final Graph<AbstractState, SymbolicEvaluationEdgeLabel> graph,
        final GlobalState global
    ) {
        for (final SymbolicEvaluationResult nextExpansion : expansion) {
            this.evaluate(
                currentVertex,
                nextExpansion.state(),
                rule,
                nextExpansion.substitution(),
                graph,
                global
            );
        }
    }

    private void generalize(
        final Vertex<AbstractState> currentVertex,
        final AbstractState currentState,
        final Graph<AbstractState, SymbolicEvaluationEdgeLabel> graph,
        final GlobalState global
    ) {
        final PrologTerm term = currentState.goal().getFirst().getTerm();
        final List<PrologTerm> generalizedArguments = new ArrayList<PrologTerm>();
        KnowledgeBase generalizedKnowledge = currentState.knowledge();
        for (final PrologTerm argument : term.arguments) {
            if (Terms.maxNesting(argument) > 0) {
                final PrologAbstractVariable variable =
                    new PrologAbstractVariable(global.fridge().getFreshName(Terms.DEFAULT_ABSTRACT_VARIABLE_NAME));
                generalizedArguments.add(variable);
                if (currentState.knowledge().isGround(argument)) {
                    generalizedKnowledge = generalizedKnowledge.addGround(variable);
                }
            } else {
                generalizedArguments.add(argument);
            }
        }
        final AbstractState generalizedState =
            new AbstractState(
                List.of(new TermGoalElement(new PrologTerm(term.name, generalizedArguments))),
                generalizedKnowledge
            );
        final InstanceRule rule = new InstanceRule(generalizedState, false);
        final Vertex<AbstractState> newVertex = new Vertex<AbstractState>(generalizedState);
        graph.addVertex(newVertex);
        graph.addEdge(
            currentVertex,
            Optional.of(
                new SymbolicEvaluationEdgeLabel(
                    rule,
                    rule.apply(currentState, global).getFirst().substitution()
                )
            ),
            newVertex
        );
        this.evaluate(newVertex, graph, global);
    }

    private boolean nestingIsTooHigh(final AbstractState state) {
        return
            NonTerminationStrategy.isSingleConjunct(state)
            && Terms.maxNesting(state.goal().getFirst().getTerm()) > this.maxNestingForGeneralization;
    }

}
