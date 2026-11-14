package prologanalyzer.structure;

import java.util.*;
import java.util.stream.*;

import prologanalyzer.*;
import prologparser.*;

public record AbstractState(List<GoalElement> goal, KnowledgeBase knowledge) {

    public Set<PrologVariable> createSetOfAllVariables() {
        final Set<PrologVariable> result = this.createSetOfAllVariablesInGoal();
        result.addAll(this.knowledge().getAllVariables());
        return result;
    }

    public Set<PrologVariable> createSetOfAllVariablesInGoal() {
        final Set<PrologVariable> variables = new LinkedHashSet<PrologVariable>();
        for (final GoalElement element : this.goal()) {
            final PrologTerm term = element.getTerm();
            if (term != null) {
                variables.addAll(Terms.createSetOfAllVariables(term));
            }
        }
        return variables;
    }

    public boolean containsVariable(final PrologVariable variable) {
        return this.containsVariableInGoal(variable) || this.knowledge().getAllVariables().contains(variable);
    }

    public boolean containsVariableInGoal(final PrologVariable variable) {
        for (final GoalElement element : this.goal()) {
            if (!element.isScopeMarker()) {
                if (Terms.createSetOfAllVariables(element.getTerm()).contains(variable)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isEmpty() {
        return this.goal().isEmpty();
    }

    @Override
    public String toString() {
        return String.format(
            "%s; %s",
            this
            .goal()
            .stream()
            .map(GoalElement::toString)
            .collect(Collectors.joining(" | ")),
            this.knowledge().toString()
        );
    }

    public JSONAbstractState toJSON() {
        return new JSONAbstractState(this.goal().stream().map(GoalElement::toJSON).toList(), this.knowledge());
    }

}
