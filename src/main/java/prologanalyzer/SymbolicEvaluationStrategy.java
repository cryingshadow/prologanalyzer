package prologanalyzer;

import prologanalyzer.structure.*;
import prologparser.*;

@FunctionalInterface
public interface SymbolicEvaluationStrategy {

    EvaluationResult evaluate(PrologProgram program);

}
