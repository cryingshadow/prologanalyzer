package prologanalyzer.structure;

import java.util.*;
import java.util.stream.*;

import prologanalyzer.*;
import prologparser.*;

public class AbstractState {

    public final List<GoalElement> goal;

    public final KnowledgeBase knowledge;

    public AbstractState(final List<GoalElement> goal, final KnowledgeBase knowledge) {
        this.goal = goal;
        this.knowledge = knowledge;
    }

    public boolean containsVariable(final PrologVariable variable) {
        return this.containsVariableInGoal(variable) || this.knowledge.getAllVariables().contains(variable);
    }

    public boolean containsVariableInGoal(final PrologVariable variable) {
        for (final GoalElement element : this.goal) {
            if (!element.isScopeMarker()) {
                if (Terms.createSetOfAllVariables(element.getTerm()).contains(variable)) {
                    return true;
                }
            }
        }
        return false;
    }

    public Set<PrologVariable> createSetOfAllFutureGroundVariables(final PrologProgram program) {
        Set<PrologVariable> result = null;
        for (final GoalElement element : this.goal) {
            if (element.isScopeMarker()) {
                continue;
            }
            final Set<PrologVariable> futureGround =
                Terms.computeSetOfFutureGroundVariables(element.getTerm(), this.knowledge, program);
            if (result == null) {
                result = futureGround;
            } else {
                result.retainAll(futureGround);
            }
        }
        return result == null ? Set.of() : result;
    }

    public Set<PrologAbstractVariable> createSetOfAllGroundVariablesInGoal() {
        return this.knowledge.restrictToGround(this.createSetOfAllVariablesInGoal());
    }

    public Set<PrologVariable> createSetOfAllVariables() {
        final Set<PrologVariable> result = this.createSetOfAllVariablesInGoal();
        result.addAll(this.knowledge.getAllVariables());
        return result;
    }

    public Set<PrologVariable> createSetOfAllVariablesInGoal() {
        final Set<PrologVariable> variables = new LinkedHashSet<PrologVariable>();
        for (final GoalElement element : this.goal) {
            final PrologTerm term = element.getTerm();
            if (term != null) {
                variables.addAll(Terms.createSetOfAllVariables(term));
            }
        }
        return variables;
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof final AbstractState other) {
            return this.goal.equals(other.goal) && this.knowledge.equals(other.knowledge);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 2 * this.goal.hashCode() + 3 * this.knowledge.hashCode();
    }

    public boolean isEmpty() {
        return this.goal.isEmpty();
    }

    public boolean isSingleConjunct() {
        return
            this.goal.size() == 1
            && this.goal.getFirst().hasTerm()
            && !this.goal.getFirst().hasClause()
            && !Terms.isConjunction(this.goal.getFirst().getTerm());
    }

    public AbstractState restrictKnowledgeToExistingVariables() {
        return new AbstractState(
            this.goal,
            this.knowledge.cleanAndRestrictToVariables(this.createSetOfAllVariablesInGoal())
        );
    }

    public JSONAbstractState toJSON() {
        return new JSONAbstractState(this.goal.stream().map(GoalElement::toJSON).toList(), this.knowledge);
    }

    @Override
    public String toString() {
        return String.format(
            "%s; %s",
            this
            .goal
            .stream()
            .map(GoalElement::toString)
            .collect(Collectors.joining(" | ")),
            this.knowledge.toString()
        );
    }

}
