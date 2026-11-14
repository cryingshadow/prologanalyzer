package prologanalyzer.structure;

import java.math.*;

public record EvaluationResult(
    Graph<AbstractState, SymbolicEvaluationEdgeLabel> graph,
    BigInteger rootId,
    String answer
) {

}
