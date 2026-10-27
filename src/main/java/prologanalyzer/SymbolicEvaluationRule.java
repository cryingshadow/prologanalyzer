package prologanalyzer;

import java.util.*;
import java.util.function.*;

public interface SymbolicEvaluationRule extends BiFunction<AbstractState, FreshNameGenerator, List<AbstractState>> {

}
