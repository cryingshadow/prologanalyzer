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
            BuiltInFailRule.INSTANCE,
            UndefinedErrorRule.INSTANCE,
            VariableErrorRule.INSTANCE,
            UnifyFailRule.INSTANCE,
            OnlyUnifyRule.NON_TERMINATION_ONLY_UNIFY,
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

    private static boolean hasTwoTrailingCuts(final PrologTerm term) {
        if (!Terms.isConjunction(term)) {
            return false;
        }
        final List<PrologTerm> conjuncts = Terms.getConjuncts(term);
        return
            conjuncts.size() > 2
            && Terms.isCut(conjuncts.getLast())
            && Terms.isCut(conjuncts.get(conjuncts.size() - 2));
    }

    private static boolean isDroppable(final AbstractState currentState) {
        return
            currentState.goal.size() > 1
            && currentState.goal.getFirst().hasTerm()
            && !currentState.goal.getFirst().hasClause();
    }

    private static boolean isFocussable(final AbstractState state) {
        return
            state.goal.size() == 1
            && state.goal.getFirst().hasTerm()
            && !state.goal.getFirst().hasClause()
            && Terms.isConjunction(state.goal.getFirst().getTerm())
            && (
                Terms.computeActiveCuts(state.goal.getFirst().getTerm()).isEmpty()
                || NonTerminationStrategy.hasTwoTrailingCuts(state.goal.getFirst().getTerm())
            );
    }

    private static boolean isSingleGoal(final AbstractState state) {
        return
            state.goal.size() == 1
            && state.goal.getFirst().hasTerm()
            && !state.goal.getFirst().hasClause();
    }

    private final int maxEvaluationDepth;

    private final int maxNesting;

    private final long start;

    private final long timeout;

    public NonTerminationStrategy(final int maxNesting, final int maxEvaluationDepth, final long timeout) {
        this.maxNesting = maxNesting;
        this.maxEvaluationDepth = maxEvaluationDepth;
        this.timeout = timeout;
        this.start = System.currentTimeMillis();
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
        graph.setStartVertex(root);
        try {
            this.evaluate(root, graph, global, 0);
        } catch (final IllegalStateException e) {
            e.printStackTrace();
        }
        return graph;
    }

    private Optional<Vertex<AbstractState>> concretize(
        final Vertex<AbstractState> currentVertex,
        final AbstractState currentState,
        final Graph<AbstractState, SymbolicEvaluationEdgeLabel> graph,
        final GlobalState global
    ) {
        final Vertex<AbstractState> newVertex =
            new Vertex<AbstractState>(
                new AbstractState(
                    currentState.goal,
                    currentState.knowledge.concretize(this.maxNesting, global.fridge())
                ).restrictKnowledgeToExistingVariables()
            );
        final InstanceRule rule = new InstanceRule(currentState, false);
        final List<SymbolicEvaluationResult> expansion = rule.apply(newVertex.label().get(), global);
        if (expansion.isEmpty()) {
            return Optional.empty();
        }
        graph.addVertex(newVertex);
        graph.addEdge(
            currentVertex,
            Optional.of(new SymbolicEvaluationEdgeLabel(rule, expansion.getFirst().substitution())),
            newVertex
        );
        return Optional.of(newVertex);
    }

    private void evaluate(
        final Vertex<AbstractState> currentVertex,
        final AbstractState state,
        final SymbolicEvaluationRule rule,
        final PrologSubstitution substitution,
        final Graph<AbstractState, SymbolicEvaluationEdgeLabel> graph,
        final GlobalState global,
        final int evaluationDepth
    ) {
        final Vertex<AbstractState> newVertex = new Vertex<AbstractState>(state);
        graph.addVertex(newVertex);
        graph.addEdge(currentVertex, Optional.of(new SymbolicEvaluationEdgeLabel(rule, substitution)), newVertex);
        this.evaluate(newVertex, graph, global, evaluationDepth + 1);
    }

    private void evaluate(
        final Vertex<AbstractState> currentVertex,
        final Graph<AbstractState, SymbolicEvaluationEdgeLabel> graph,
        final GlobalState global,
        final int evaluationDepth
    ) {
        final AbstractState currentState = currentVertex.label().get();
        if (currentState.isEmpty()) {
            return;
        }
        if (this.timeout > 0 && System.currentTimeMillis() > this.start + this.timeout) {
            return;
        }
        if (evaluationDepth > this.maxEvaluationDepth) {
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
                global,
                evaluationDepth
            );
            return;
        }
        List<SymbolicEvaluationResult> expansion = UnifyRule.NON_TERMINATION_UNIFY.apply(currentState, global);
        if (!expansion.isEmpty()) {
            this.evaluate(currentVertex, expansion, UnifyRule.NON_TERMINATION_UNIFY, graph, global, evaluationDepth);
            return;
        }
        expansion = EvalRule.NON_TERMINATION_EVAL.apply(currentState, global);
        if (!expansion.isEmpty()) {
            this.evaluate(currentVertex, expansion, EvalRule.NON_TERMINATION_EVAL, graph, global, evaluationDepth);
            return;
        }
        if (NonTerminationStrategy.isSingleGoal(currentState)) {
            for (final Vertex<AbstractState> vertex : graph.getVertices()) {
                if (vertex == currentVertex) {
                    continue;
                }
                final AbstractState otherState = vertex.label().get();
                if (NonTerminationStrategy.isSingleGoal(otherState)) {
                    final InstanceRule rule = new InstanceRule(currentState, true);
                    expansion = rule.apply(otherState, global);
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
        if (this.nestingIsTooHighInKnowledge(currentState)) {
            final Optional<Vertex<AbstractState>> newVertex =
                this.concretize(currentVertex, currentState, graph, global);
            if (newVertex.isPresent()) {
                this.evaluate(newVertex.get(), graph, global, evaluationDepth + 1);
            }
            // otherwise give up
            return;
        }
        if (this.nestingIsTooHighInGoal(currentState)) {
            // give up
            return;
        }
        if (NonTerminationStrategy.isDroppable(currentState)) {
            final PrologTerm firstConjunct = Terms.getFirstConjunct(currentState.goal.getFirst().getTerm());
            for (final Vertex<AbstractState> vertex : graph.getVertices()) {
                if (vertex == currentVertex) {
                    continue;
                }
                final AbstractState otherState = vertex.label().get();
                if (otherState.goal.isEmpty()) {
                    continue;
                }
                final GoalElement firstGoal = otherState.goal.getFirst();
                if (
                    firstGoal.hasClause()
                    || firstGoal.isScopeMarker()
                    || !Terms.haveSameFunctionSymbol(Terms.getFirstConjunct(firstGoal.getTerm()), firstConjunct)
                ) {
                    continue;
                }

                final DropRule rule = new DropRule(1);
                expansion = rule.apply(currentState, global);
                this.evaluate(currentVertex, expansion, rule, graph, global, evaluationDepth);
                return;
            }
        }
        if (NonTerminationStrategy.isFocussable(currentState)) {
            final PrologTerm term = currentState.goal.getFirst().getTerm();
            if (NonTerminationStrategy.hasTwoTrailingCuts(term)) {
                final FocusRule rule = new FocusRule(Terms.getConjuncts(term).size() - 1);
                expansion = rule.apply(currentState, global);
                this.evaluate(currentVertex, expansion, rule, graph, global, evaluationDepth);
                return;
            } else {
                final PrologTerm firstConjunct = Terms.getFirstConjunct(term);
                for (final Vertex<AbstractState> vertex : graph.getVertices()) {
                    if (vertex == currentVertex) {
                        continue;
                    }
                    final AbstractState otherState = vertex.label().get();
                    if (otherState.goal.isEmpty()) {
                        continue;
                    }
                    final GoalElement firstGoal = otherState.goal.getFirst();
                    if (
                        firstGoal.hasClause()
                        || firstGoal.isScopeMarker()
                        || !Terms.haveSameFunctionSymbol(Terms.getFirstConjunct(firstGoal.getTerm()), firstConjunct)
                    ) {
                        continue;
                    }
                    final FocusRule rule = new FocusRule(1);
                    expansion = rule.apply(currentState, global);
                    this.evaluate(currentVertex, expansion, rule, graph, global, evaluationDepth);
                    return;
                }
            }
        }
        expansion = CallRule.INSTANCE.apply(currentState, global);
        if (!expansion.isEmpty()) {
            this.evaluate(currentVertex, expansion, CallRule.INSTANCE, graph, global, evaluationDepth);
            return;
        }
        expansion = CaseRule.INSTANCE.apply(currentState, global);
        if (!expansion.isEmpty()) {
            this.evaluate(currentVertex, expansion, CaseRule.INSTANCE, graph, global, evaluationDepth);
            return;
        }
        throw new IllegalStateException("Could not expand non-leaf state!");
    }

    private void evaluate(
        final Vertex<AbstractState> currentVertex,
        final List<SymbolicEvaluationResult> expansion,
        final SymbolicEvaluationRule rule,
        final Graph<AbstractState, SymbolicEvaluationEdgeLabel> graph,
        final GlobalState global,
        final int evaluationDepth
    ) {
        for (final SymbolicEvaluationResult nextExpansion : expansion) {
            this.evaluate(
                currentVertex,
                nextExpansion.state(),
                rule,
                nextExpansion.substitution(),
                graph,
                global,
                evaluationDepth + 1
            );
        }
    }

    private boolean nestingIsTooHighInGoal(final AbstractState currentState) {
        return
            currentState
            .goal
            .stream()
            .filter(element -> !element.isScopeMarker())
            .mapToInt(element -> Terms.maxNesting(element.getTerm()))
            .max()
            .orElse(0) > this.maxNesting;
    }

    private boolean nestingIsTooHighInKnowledge(final AbstractState state) {
        return state.knowledge.maxNesting() > this.maxNesting;
    }

}
