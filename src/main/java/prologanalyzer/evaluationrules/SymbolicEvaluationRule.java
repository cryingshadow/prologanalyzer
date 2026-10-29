package prologanalyzer.evaluationrules;

import java.util.*;
import java.util.function.*;

import prologanalyzer.*;

public interface SymbolicEvaluationRule extends BiFunction<AbstractState, FreshNameGenerator, List<AbstractState>> {

}
