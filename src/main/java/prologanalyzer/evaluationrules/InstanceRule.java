package prologanalyzer.evaluationrules;

import java.util.*;

import prologanalyzer.*;
import prologanalyzer.structure.*;
import prologparser.*;

public class InstanceRule implements SymbolicEvaluationRule {

    public static PrologSubstitution calculateInstanceMatcher(
        final AbstractState child,
        final AbstractState parent,
        final boolean noGroundLoss
    ) {
        if (child.goal().size() != parent.goal().size()) {
            return null;
        }
        final PrologSubstitution res = new PrologSubstitution();
        final Set<PrologVariable> used = new LinkedHashSet<PrologVariable>();
        final Set<PrologVariable> nonAbstractVariablesInMatcher = new LinkedHashSet<PrologVariable>();
        for (int i = 0; i < child.goal().size(); i++) {
            if (
                !InstanceRule.calculateInstanceMatcher(
                    child.goal().get(i),
                    parent.goal().get(i),
                    child.knowledge(),
                    parent.knowledge(),
                    used,
                    nonAbstractVariablesInMatcher,
                    res
                )
            ) {
                return null;
            }
        }
        if (InstanceRule.checkInstanceMatcher(child, parent, res, used, nonAbstractVariablesInMatcher, noGroundLoss)) {
            return res;
        }
        return null;
    }

    public static int calculateMaximalStateLengthForInstance(
        final AbstractState child,
        final AbstractState parent,
        final boolean noGroundLoss
    ) {
        for (int i = child.goal().size(); i > 0; i--) {
            final List<GoalElement> goal = new ArrayList<GoalElement>();
            for (int j = 0; j < i; j++) {
                goal.add(child.goal().get(j));
            }
            if (
                InstanceRule.calculateInstanceMatcher(
                    new AbstractState(goal, child.knowledge()),
                    parent,
                    noGroundLoss
                ) != null
            ) {
                return i;
            }
        }
        return 0;
    }

    public static boolean checkInstanceMatcher(
        final AbstractState child,
        final AbstractState parent,
        final PrologSubstitution matcher,
        final boolean noGroundLoss
    ) {
        final Set<PrologVariable> nonAbstractVariablesInMatcher = new LinkedHashSet<PrologVariable>();
        final Set<PrologVariable> used = new LinkedHashSet<PrologVariable>();
        for (final PrologVariable variable : matcher.keySet()) {
            if (Terms.isNonAbstractVariable(variable)) {
                used.add(variable);
            }
        }
        for (final GoalElement element : parent.goal()) {
            if (!element.isScopeMarker()) {
                nonAbstractVariablesInMatcher.addAll(Terms.createSetOfAllNonAbstractVariables(element.getTerm()));
            }
        }
        return InstanceRule.checkInstanceMatcher(
            child,
            parent,
            matcher,
            used,
            nonAbstractVariablesInMatcher,
            noGroundLoss
        );
    }

    private static boolean calculateInstanceMatcher(
        final GoalElement childElement,
        final GoalElement parentElement,
        final KnowledgeBase childKnowledge,
        final KnowledgeBase parentKnowledge,
        final Set<PrologVariable> used,
        final Set<PrologVariable> nonAbstractVariablesInMatcher,
        final PrologSubstitution matcher
    ) {
        if (childElement.isScopeMarker()) {
            if (parentElement.isScopeMarker()) {
                return childElement.getScope() == parentElement.getScope();
            } else {
                return false;
            }
        } else if (childElement.hasClause()) {
            if (!parentElement.isScopeMarker() && parentElement.hasClause()) {
                if (childElement.getClause() == parentElement.getClause()) {
                    nonAbstractVariablesInMatcher.addAll(
                        Terms.createSetOfAllNonAbstractVariables(parentElement.getTerm())
                    );
                    return InstanceRule.calculateInstanceMatcher(
                        childElement.getTerm(),
                        parentElement.getTerm(),
                        childKnowledge,
                        parentKnowledge,
                        used,
                        matcher
                    );
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else if (!parentElement.isScopeMarker() && !parentElement.hasClause()) {
            nonAbstractVariablesInMatcher.addAll(Terms.createSetOfAllNonAbstractVariables(parentElement.getTerm()));
            return InstanceRule.calculateInstanceMatcher(
                childElement.getTerm(),
                parentElement.getTerm(),
                childKnowledge,
                parentKnowledge,
                used,
                matcher
            );
        } else {
            return false;
        }
    }

    private static boolean calculateInstanceMatcher(
        final PrologTerm childTerm,
        final PrologTerm parentTerm,
        final KnowledgeBase childKnowledge,
        final KnowledgeBase parentKnowledge,
        final Set<PrologVariable> used,
        final PrologSubstitution matcher
    ) {
        if (Terms.isVariable(parentTerm)) {
            if (matcher.containsKey(parentTerm)) {
                return childTerm.equals(matcher.get(parentTerm));
            } else if (parentTerm.equals(childTerm)) {
                return !parentKnowledge.isGround(parentTerm) || childKnowledge.isGround(childTerm);
            } else if (Terms.isAbstractVariable(parentTerm)) {
                if (parentKnowledge.isGround(parentTerm) && !childKnowledge.isGround(childTerm)) {
                    return false;
                }
                matcher.put((PrologVariable)parentTerm, childTerm);
                return true;
            } else if (Terms.isNonAbstractVariable(childTerm) && !used.contains(childTerm)) {
                used.add((PrologVariable)childTerm);
                matcher.put((PrologVariable)parentTerm, childTerm);
                return true;
            } else {
                return false;
            }
        } else if (Terms.isCut(parentTerm)) {
            if (Terms.isCut(childTerm)) {
                if (parentTerm instanceof LabeledCut) {
                    if (childTerm instanceof LabeledCut) {
                        return ((LabeledCut)childTerm).scope == ((LabeledCut)parentTerm).scope;
                    } else {
                        return false;
                    }
                } else {
                    return !(childTerm instanceof LabeledCut);
                }
            } else {
                return false;
            }
        } else if (!Terms.isVariable(childTerm) && Terms.haveSameFunctionSymbol(parentTerm, childTerm)) {
            for (int i = 0; i < Terms.getArity(parentTerm); i++) {
                if (
                    !InstanceRule.calculateInstanceMatcher(
                        childTerm.arguments.get(i),
                        parentTerm.arguments.get(i),
                        childKnowledge,
                        parentKnowledge,
                        used,
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

    private static boolean checkInstanceMatcher(
        final AbstractState child,
        final AbstractState parent,
        final PrologSubstitution matcher,
        final Set<PrologVariable> used,
        final Set<PrologVariable> nonAbstractVariablesInMatcher,
        final boolean noGroundLoss
    ) {
        final KnowledgeBase childKnowledge = child.knowledge();
        final KnowledgeBase parentKnowledge = parent.knowledge();
        final Set<PrologAbstractVariable> abstractVariablesInMatcher = new LinkedHashSet<PrologAbstractVariable>();
        for (final PrologVariable variable : child.createSetOfAllVariablesInGoal()) {
            if (Terms.isAbstractVariable(variable)) {
                abstractVariablesInMatcher.add((PrologAbstractVariable)variable);
            }
        }
        for (final PrologVariable variable : parent.createSetOfAllVariablesInGoal()) {
            if (Terms.isAbstractVariable(variable)) {
                abstractVariablesInMatcher.add((PrologAbstractVariable)variable);
            }
        }
        used.retainAll(nonAbstractVariablesInMatcher);
        for (final PrologVariable variable : used) {
            if (!matcher.containsKey(variable)) {
                return false;
            }
        }
        if (
            !KnowledgeBase.matchesKnowledge(
                matcher,
                child,
                parent,
                nonAbstractVariablesInMatcher,
                abstractVariablesInMatcher
            )
        ) {
            return false;
        }
        if (noGroundLoss) {
            for (final Map.Entry<PrologVariable, PrologTerm> entry : matcher.entrySet()) {
                if (childKnowledge.isGround(entry.getValue()) && !parentKnowledge.isGround(entry.getKey())) {
                    if (parent.containsVariable(entry.getKey())) {
                        return false;
                    }
                }
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
    public List<AbstractState> apply(final AbstractState state, final GlobalState global) {
        final PrologSubstitution matcher = InstanceRule.calculateInstanceMatcher(state, this.target, this.noGroundLoss);
        if (matcher == null) {
            return List.of();
        }
        return List.of(this.target);
    }

    @Override
    public String toString() {
        return "INSTANCE";
    }
}
