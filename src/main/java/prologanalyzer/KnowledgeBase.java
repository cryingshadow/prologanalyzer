package prologanalyzer;

import java.util.*;

import prologparser.*;

public record KnowledgeBase(
    Set<PrologAbstractVariable> ground,
    Set<NonUnifiablePair> nonUnifiable,
    Set<PrologVariable> free
) {

}
