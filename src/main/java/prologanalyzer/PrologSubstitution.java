package prologanalyzer;

import java.util.*;

import prologparser.*;

public class PrologSubstitution extends LinkedHashMap<PrologVariable, PrologTerm> {

    private static final long serialVersionUID = 1L;

    public static PrologSubstitution appendIfCommutativeNonAbstractRenaming(
        final PrologSubstitution substitution1,
        final PrologSubstitution substitution2
    ) {
        if (substitution1 != null && substitution2 != null) {
            final PrologSubstitution res = new PrologSubstitution();
            for (final Map.Entry<PrologVariable, PrologTerm> entry : substitution1.entrySet()) {
                if (Terms.isAbstractVariable(entry.getKey()) || !Terms.isNonAbstractVariable(entry.getValue())) {
                    return null;
                } else if (substitution2.containsKey(entry.getKey())) {
                    if (!entry.getValue().equals(substitution2.get(entry.getKey()))) {
                        return null;
                    }
                }
                res.put(entry.getKey(), entry.getValue());
            }
            for (final Map.Entry<PrologVariable, PrologTerm> entry : substitution2.entrySet()) {
                if (Terms.isAbstractVariable(entry.getKey()) || !Terms.isNonAbstractVariable(entry.getValue())) {
                    return null;
                } else if (substitution1.containsKey(entry.getKey())) {
                    if (!entry.getValue().equals(substitution1.get(entry.getKey()))) {
                        return null;
                    }
                }
                res.put(entry.getKey(), entry.getValue());
            }
            return res;
        } else {
            return null;
        }
    }

    public static PrologSubstitution appendIfCommutativeVariableRenaming(
        final PrologSubstitution substitution1,
        final PrologSubstitution substitution2
    ) {
        if (substitution1 != null && substitution2 != null) {
            final PrologSubstitution result1 =
                PrologSubstitution.appendIfConsistentVariableRenaming(substitution1, substitution2);
            if (result1 == null) {
                return null;
            }
            final PrologSubstitution result2 =
                PrologSubstitution.appendIfConsistentVariableRenaming(substitution2, substitution1);
            if (result2 != null && result1.equals(result2)) {
                return result1;
            }
        }
        return null;
    }

    private static PrologSubstitution appendIfConsistentVariableRenaming(
        final PrologSubstitution substitution1,
        final PrologSubstitution substitution2
    ) {
        final PrologSubstitution res = new PrologSubstitution();
        for (final Map.Entry<PrologVariable, PrologTerm> entry : substitution1.entrySet()) {
            final PrologVariable key = entry.getKey();
            final PrologTerm value = entry.getValue();
            if (Terms.isAbstractVariable(key) || !Terms.isNonAbstractVariable(value)) {
                if (Terms.isNonAbstractVariable(key) || !Terms.isAbstractVariable(value)) {
                    return null;
                } else if (substitution2.containsKey(key)) {
                    if (!value.equals(substitution2.get(key))) {
                        return null;
                    }
                }
            } else if (substitution2.containsKey(key)) {
                if (!value.equals(substitution2.get(key))) {
                    return null;
                }
            }
            res.put(entry.getKey(), entry.getValue());
        }
        return res;
    }

    public PrologSubstitution() {
        super();
    }

    public PrologSubstitution(final Map<? extends PrologVariable, ? extends PrologTerm> map) {
        super(map);
    }

    public PrologSubstitution append(final PrologSubstitution substitution) {
        final PrologSubstitution res = this.copy();
        for (final Map.Entry<PrologVariable, PrologTerm> entry : res.entrySet()) {
            entry.setValue(substitution.apply(entry.getValue()));
        }
        for (final Map.Entry<PrologVariable, PrologTerm> entry : substitution.entrySet()) {
            if (!res.containsKey(entry.getKey())) {
                res.put(entry.getKey(), entry.getValue());
            }
        }
        return res;
    }

    public PrologTerm apply(final PrologTerm term) {
        if (this.containsKey(term)) {
            return this.get(term);
        }
        final List<PrologTerm> newArguments = new ArrayList<PrologTerm>();
        for (final PrologTerm argument : term.arguments) {
            newArguments.add(this.apply(argument));
        }
        return new PrologTerm(term.name, newArguments);
    }

    public PrologSubstitution copy() {
        final PrologSubstitution res = new PrologSubstitution();
        for (final Map.Entry<PrologVariable, PrologTerm> entry : this.entrySet()) {
            res.put(entry.getKey(), entry.getValue());
        }
        return res;
    }

    public Set<PrologAbstractVariable> getAbstractVariablessInRange() {
        final Set<PrologAbstractVariable> res = new LinkedHashSet<PrologAbstractVariable>();
        for (final PrologTerm term : this.values()) {
            res.addAll(Terms.createSetOfAllAbstractVariables(term));
        }
        return res;
    }

    public Set<PrologVariable> getNonAbstractVariablesInRange() {
        final Set<PrologVariable> res = new LinkedHashSet<PrologVariable>();
        for (final PrologTerm term : this.values()) {
            res.addAll(Terms.createSetOfAllNonAbstractVariables(term));
        }
        return res;
    }

    public Set<PrologVariable> getVariablesInRange() {
        final Set<PrologVariable> res = new LinkedHashSet<PrologVariable>();
        for (final PrologTerm term : this.values()) {
            res.addAll(Terms.createSetOfAllVariables(term));
        }
        return res;
    }

    public boolean hasPotentialUnificationConflict() {
        final Set<PrologAbstractVariable> used = new LinkedHashSet<PrologAbstractVariable>();
        for (final Map.Entry<PrologVariable, PrologTerm> entry : this.entrySet()) {
            if (Terms.isAbstractVariable(entry.getKey())) {
                if (Terms.isAbstractVariable(entry.getValue())) {
                    if (used.contains(entry.getValue())) {
                        return true;
                    } else {
                        used.add((PrologAbstractVariable) entry.getValue());
                    }
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    public PrologSubstitution restrict(final Set<? extends PrologVariable> set) {
        final PrologSubstitution res = new PrologSubstitution();
        for (final Map.Entry<PrologVariable, PrologTerm> entry : this.entrySet()) {
            if (set.contains(entry.getKey())) {
                res.put(entry.getKey(), entry.getValue());
            }
        }
        return res;
    }

    public PrologSubstitution restrictToAbstractVariables() {
        final PrologSubstitution res = new PrologSubstitution();
        for (final Map.Entry<PrologVariable, PrologTerm> entry : this.entrySet()) {
            if (Terms.isAbstractVariable(entry.getKey())) {
                res.put(entry.getKey(), entry.getValue());
            }
        }
        return res;
    }

    public PrologSubstitution restrictToComplement(final Set<? extends PrologVariable> set) {
        final PrologSubstitution res = new PrologSubstitution();
        for (final Map.Entry<PrologVariable, PrologTerm> entry : this.entrySet()) {
            if (!set.contains(entry.getKey())) {
                res.put(entry.getKey(), entry.getValue());
            }
        }
        return res;
    }

    public PrologSubstitution restrictToNonAbstractVariables() {
        final PrologSubstitution res = new PrologSubstitution();
        for (final Map.Entry<PrologVariable, PrologTerm> entry : this.entrySet()) {
            if (Terms.isNonAbstractVariable(entry.getKey())) {
                res.put(entry.getKey(), entry.getValue());
            }
        }
        return res;
    }

    public String toLaTeX(final KnowledgeBase knowledge) {
        final StringBuilder res = new StringBuilder();
        res.append("\\{");
        boolean first = true;
        for (final Map.Entry<PrologVariable, PrologTerm> entry : this.entrySet()) {
            if (first) {
                first = false;
            } else {
                res.append(",");
            }
            res.append(Terms.toLaTeX(entry.getKey(), knowledge));
            res.append("/");
            res.append(Terms.toLaTeX(entry.getValue(), knowledge));
        }
        res.append("\\}");
        return res.toString();
    }

    @Override
    public String toString() {
        final StringBuilder res = new StringBuilder();
        boolean first = true;
        for (final Map.Entry<PrologVariable, PrologTerm> entry : this.entrySet()) {
            if (first) {
                first = false;
            } else {
                res.append(",\n");
            }
            res.append(entry.getKey());
            res.append(" -> ");
            res.append(entry.getValue());
        }
        return res.toString();
    }

}
