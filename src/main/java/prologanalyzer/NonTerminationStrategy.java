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

    private static boolean isDroppable(final AbstractState currentState) {
        return
            currentState.goal().size() > 1
            && currentState.goal().getFirst().hasTerm()
            && !currentState.goal().getFirst().hasClause();
    }

    private static boolean isFocussable(final AbstractState state) {
        return
            state.goal().size() == 1
            && state.goal().getFirst().hasTerm()
            && !state.goal().getFirst().hasClause()
            && Terms.isConjunction(state.goal().getFirst().getTerm());
    }

    private static boolean isSingleConjunct(final AbstractState state) {
        return
            state.goal().size() == 1
            && state.goal().getFirst().hasTerm()
            && !state.goal().getFirst().hasClause()
            && !Terms.isConjunction(state.goal().getFirst().getTerm());
    }

    private boolean done;

    private final long start;

    private final long timeout;

    public NonTerminationStrategy(final long timeout) {
        this.timeout = timeout;
        this.start = System.currentTimeMillis();
        this.done = false;
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
        if (this.done) {
            return;
        }
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
                    final InstanceRule rule = new InstanceRule(currentState, false);
                    expansion = rule.apply(otherState, global);
                    if (!expansion.isEmpty()) {
                        graph.addEdge(
                            currentVertex,
                            Optional.of(new SymbolicEvaluationEdgeLabel(rule, expansion.getFirst().substitution())),
                            vertex
                        );
                        this.done = true;
                        return;
                    }
                }
            }
        }
        if (NonTerminationStrategy.isDroppable(currentState)) {
            final PrologTerm firstConjunct = Terms.getFirstConjunct(currentState.goal().getFirst().getTerm());
            for (final Vertex<AbstractState> vertex : graph.getVertices()) {
                if (vertex == currentVertex) {
                    continue;
                }
                final AbstractState otherState = vertex.label().get();
                if (otherState.goal().isEmpty()) {
                    continue;
                }
                final GoalElement firstGoal = otherState.goal().getFirst();
                if (
                    firstGoal.hasClause()
                    || firstGoal.isScopeMarker()
                    || !Terms.haveSameFunctionSymbol(Terms.getFirstConjunct(firstGoal.getTerm()), firstConjunct)
                ) {
                    continue;
                }
                final DropRule rule = new DropRule(1);
                expansion = rule.apply(currentState, global);
                this.evaluate(currentVertex, expansion, rule, graph, global);
                return;
            }
        }
        if (NonTerminationStrategy.isFocussable(currentState)) {
            final PrologTerm firstConjunct = Terms.getFirstConjunct(currentState.goal().getFirst().getTerm());
            for (final Vertex<AbstractState> vertex : graph.getVertices()) {
                if (vertex == currentVertex) {
                    continue;
                }
                final AbstractState otherState = vertex.label().get();
                if (otherState.goal().isEmpty()) {
                    continue;
                }
                final GoalElement firstGoal = otherState.goal().getFirst();
                if (
                    firstGoal.hasClause()
                    || firstGoal.isScopeMarker()
                    || !Terms.haveSameFunctionSymbol(Terms.getFirstConjunct(firstGoal.getTerm()), firstConjunct)
                ) {
                    continue;
                }
                expansion = FocusRule.INSTANCE.apply(currentState, global);
                this.evaluate(currentVertex, expansion, FocusRule.INSTANCE, graph, global);
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

}
