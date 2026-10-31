package prologanalyzer.structure;

import java.util.*;

public record AbstractState(List<GoalElement> goal, KnowledgeBase knowledge) {

}
