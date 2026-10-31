package prologanalyzer.evaluationrules;

import java.util.*;

import prologanalyzer.structure.*;

@FunctionalInterface
public interface SymbolicEvaluationRule {

    List<AbstractState> apply(AbstractState state, GlobalState global);

}
