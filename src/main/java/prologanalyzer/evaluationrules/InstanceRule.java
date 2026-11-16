package prologanalyzer.evaluationrules;

import java.util.*;

import prologanalyzer.*;
import prologanalyzer.structure.*;
import prologparser.*;

public class InstanceRule implements SymbolicEvaluationRule {

    public static PrologSubstitution calculateInstanceMatcher(
        final AbstractState parent,
        final AbstractState child,
        final boolean noGroundLoss
    ) {
        if (parent.goal.size() != child.goal.size()) {
            return null;
        }
        final PrologSubstitution matcher = new PrologSubstitution();
        final Map<Integer, Integer> scopeMatcher = new LinkedHashMap<Integer, Integer>();
        for (int i = 0; i < parent.goal.size(); i++) {
            if (
                !InstanceRule.calculateInstanceMatcher(
                    parent.goal.get(i),
                    child.goal.get(i),
                    parent.knowledge,
                    child.knowledge,
                    matcher,
                    scopeMatcher
                )
            ) {
                return null;
            }
        }
        if (InstanceRule.checkInstanceMatcher(parent, child, matcher, scopeMatcher, noGroundLoss)) {
            return matcher;
        }
        return null;
    }

    public static int calculateMaximalStateLengthForInstance(
        final AbstractState parent,
        final AbstractState child,
        final boolean noGroundLoss
    ) {
        for (int i = parent.goal.size(); i > 0; i--) {
            final List<GoalElement> goal = new ArrayList<GoalElement>();
            for (int j = 0; j < i; j++) {
                goal.add(parent.goal.get(j));
            }
            if (
                InstanceRule.calculateInstanceMatcher(
                    new AbstractState(goal, parent.knowledge),
                    child,
                    noGroundLoss
                ) != null
            ) {
                return i;
            }
        }
        return 0;
    }

    public static boolean checkInstanceMatcher(
        final AbstractState parent,
        final AbstractState child,
        final PrologSubstitution matcher,
        final Map<Integer, Integer> scopeMatcher,
        final boolean noGroundLoss
    ) {
        final PrologSubstitution matcherOnNonAbstract = matcher.restrictToNonAbstractVariables();
        return
            InstanceRule.isRenaming(scopeMatcher)
            && InstanceRule.isRenaming(matcherOnNonAbstract)
            && InstanceRule.respectsGround(child.createSetOfAllGroundVariablesInGoal(), matcher, parent.knowledge)
            && child.knowledge.respectsFree(matcher, parent.knowledge)
            && child.knowledge.respectsNonUnification(matcher, parent.knowledge)
            && (!noGroundLoss || InstanceRule.noGroundLoss(matcher, parent.knowledge, child.knowledge));
    }

    private static boolean calculateInstanceMatcher(
        final GoalElement parentElement,
        final GoalElement childElement,
        final KnowledgeBase parentKnowledge,
        final KnowledgeBase childKnowledge,
        final PrologSubstitution matcher,
        final Map<Integer, Integer> scopeMatcher
    ) {
        if (parentElement.isScopeMarker()) {
            if (childElement.isScopeMarker()) {
                if (scopeMatcher.containsKey(childElement.getScope())) {
                    return parentElement.getScope() == scopeMatcher.get(childElement.getScope());
                }
                scopeMatcher.put(childElement.getScope(), parentElement.getScope());
                return true;
            } else {
                return false;
            }
        } else if (parentElement.hasClause()) {
            if (!childElement.isScopeMarker() && childElement.hasClause()) {
                if (parentElement.getClause() == childElement.getClause()) {
                    if (scopeMatcher.containsKey(childElement.getScope())) {
                        if (parentElement.getScope() != scopeMatcher.get(childElement.getScope())) {
                            return false;
                        }
                    } else {
                        scopeMatcher.put(childElement.getScope(), parentElement.getScope());
                    }
                    return InstanceRule.calculateInstanceMatcher(
                        parentElement.getTerm(),
                        childElement.getTerm(),
                        parentKnowledge,
                        childKnowledge,
                        matcher
                    );
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else if (!childElement.isScopeMarker() && !childElement.hasClause()) {
            return InstanceRule.calculateInstanceMatcher(
                parentElement.getTerm(),
                childElement.getTerm(),
                parentKnowledge,
                childKnowledge,
                matcher
            );
        } else {
            return false;
        }
    }

    private static boolean calculateInstanceMatcher(
        final PrologTerm parentTerm,
        final PrologTerm childTerm,
        final KnowledgeBase parentKnowledge,
        final KnowledgeBase childKnowledge,
        final PrologSubstitution matcher
    ) {
        if (Terms.isVariable(childTerm)) {
            if (matcher.containsKey(childTerm)) {
                return parentTerm.equals(matcher.get(childTerm));
            } else if (childTerm.equals(parentTerm)) {
                return !childKnowledge.isGround(childTerm) || parentKnowledge.isGround(parentTerm);
            } else if (Terms.isAbstractVariable(childTerm)) {
                if (childKnowledge.isGround(childTerm) && !parentKnowledge.isGround(parentTerm)) {
                    return false;
                }
                matcher.put((PrologVariable)childTerm, parentTerm);
                return true;
            } else if (Terms.isNonAbstractVariable(parentTerm)) {
                matcher.put((PrologVariable)childTerm, parentTerm);
                return true;
            } else {
                return false;
            }
        } else if (Terms.isCut(childTerm)) {
            if (Terms.isCut(parentTerm)) {
                if (childTerm instanceof LabeledCut) {
                    if (parentTerm instanceof LabeledCut) {
                        return ((LabeledCut)parentTerm).scope == ((LabeledCut)childTerm).scope;
                    } else {
                        return false;
                    }
                } else {
                    return !(parentTerm instanceof LabeledCut);
                }
            } else {
                return false;
            }
        } else if (!Terms.isVariable(parentTerm) && Terms.haveSameFunctionSymbol(childTerm, parentTerm)) {
            for (int i = 0; i < Terms.getArity(childTerm); i++) {
                if (
                    !InstanceRule.calculateInstanceMatcher(
                        parentTerm.arguments.get(i),
                        childTerm.arguments.get(i),
                        parentKnowledge,
                        childKnowledge,
                        matcher
                    )
                ) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    private static boolean isRenaming(final Map<Integer, Integer> scopeMatcher) {
        final Set<Integer> used = new LinkedHashSet<Integer>();
        for (final Map.Entry<Integer, Integer> entry : scopeMatcher.entrySet()) {
            if (used.contains(entry.getValue())) {
                return false;
            }
            used.add(entry.getValue());
        }
        return true;
    }

    private static boolean isRenaming(final PrologSubstitution matcher) {
        final Set<PrologVariable> used = new LinkedHashSet<PrologVariable>();
        for (final Map.Entry<PrologVariable, PrologTerm> entry : matcher.entrySet()) {
            if (!Terms.isNonAbstractVariable(entry.getValue()) || used.contains(entry.getValue())) {
                return false;
            }
            used.add((PrologVariable)entry.getValue());
        }
        return true;
    }

    private static boolean noGroundLoss(
        final PrologSubstitution matcher,
        final KnowledgeBase parentKnowledge,
        final KnowledgeBase childKnowledge
    ) {
        for (final Map.Entry<PrologVariable, PrologTerm> entry : matcher.entrySet()) {
            if (parentKnowledge.isGround(entry.getValue()) && !childKnowledge.isGround(entry.getKey())) {
                return false;
            }
        }
        return true;
    }

    private static boolean respectsGround(
        final Set<PrologAbstractVariable> ground,
        final PrologSubstitution matcher,
        final KnowledgeBase parentKnowledge
    ) {
        for (final PrologAbstractVariable variable : ground) {
            if (!parentKnowledge.isGround(matcher.apply(variable))) {
                return false;
            }
        }
        return true;
    }

    private final boolean noGroundLoss;

    private final AbstractState target;

    public InstanceRule(final AbstractState target, final boolean noGroundLoss) {
        this.target = target;
        this.noGroundLoss = noGroundLoss;
    }

    @Override
    public List<SymbolicEvaluationResult> apply(final AbstractState state, final GlobalState global) {
        final PrologSubstitution matcher = InstanceRule.calculateInstanceMatcher(state, this.target, this.noGroundLoss);
        if (matcher == null) {
            return List.of();
        }
        return List.of(new SymbolicEvaluationResult(this.target, matcher));
    }

    @Override
    public String getDOTColor() {
        return "lightgrey";
    }

    @Override
    public String toString() {
        return "INSTANCE";
    }
}
