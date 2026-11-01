package prologanalyzer.evaluationrules;

import java.util.*;

import prologanalyzer.structure.*;

@FunctionalInterface
public interface SymbolicEvaluationRule {

    List<SymbolicEvaluationResult> apply(AbstractState state, GlobalState global);

}
