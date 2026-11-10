package prologanalyzer.evaluationrules;

import java.util.*;

import prologanalyzer.structure.*;

public interface SymbolicEvaluationRule {

    List<SymbolicEvaluationResult> apply(AbstractState state, GlobalState global);

    String getDOTColor();

}
