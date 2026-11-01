package prologanalyzer;

import prologanalyzer.structure.*;
import prologparser.*;

public class TerminationStrategy implements SymbolicEvaluationStrategy {

    @Override
    public Graph<AbstractState, SymbolicEvaluationEdgeLabel> evaluate(final PrologProgram program) {
        final GlobalState global =
            new GlobalState(program, new FreshNameGenerator(Terms.getAllVariableNames(program)), new ScopeGenerator());
        final AbstractState rootState = QueryParser.parseQuery(global);
        final Graph<AbstractState, SymbolicEvaluationEdgeLabel> graph =
            new Graph<AbstractState, SymbolicEvaluationEdgeLabel>();
        final Vertex<AbstractState> root = new Vertex<AbstractState>(rootState);
        graph.addVertex(root);
        this.evaluate(root, graph, global);
        return graph;
    }

    private void evaluate(
        final Vertex<AbstractState> currentVertex,
        final Graph<AbstractState, SymbolicEvaluationEdgeLabel> graph,
        final GlobalState global
    ) {
        // TODO Auto-generated method stub

    }

}
