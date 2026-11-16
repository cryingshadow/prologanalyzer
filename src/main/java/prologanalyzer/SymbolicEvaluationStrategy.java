package prologanalyzer;

import prologanalyzer.structure.*;
import prologparser.*;

@FunctionalInterface
public interface SymbolicEvaluationStrategy {

    Graph<AbstractState, SymbolicEvaluationEdgeLabel> evaluate(PrologProgram program);

}
