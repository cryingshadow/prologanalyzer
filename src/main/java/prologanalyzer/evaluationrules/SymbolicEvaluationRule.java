package prologanalyzer.evaluationrules;

import java.util.*;
import java.util.function.*;

import prologanalyzer.*;
import prologanalyzer.structure.*;

public interface SymbolicEvaluationRule extends BiFunction<AbstractState, FreshNameGenerator, List<AbstractState>> {

}
