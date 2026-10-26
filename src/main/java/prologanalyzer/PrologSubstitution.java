package prologanalyzer;

import java.util.*;

import prologparser.*;

public class PrologSubstitution extends LinkedHashMap<PrologVariable, PrologTerm> {

    private static final long serialVersionUID = 1L;

    public static PrologSubstitution appendIfCommutativeNonAbstractRenaming(
        final PrologSubstitution sigma1,
        final PrologSubstitution sigma2
    ) {
        if (sigma1 != null && sigma2 != null) {
            final PrologSubstitution res = new PrologSubstitution();
            for (final Map.Entry<PrologVariable, PrologTerm> entry : sigma1.entrySet()) {
                if (Terms.isAbstractVariable(entry.getKey()) || !Terms.isNonAbstractVariable(entry.getValue())) {
                    return null;
                } else if (sigma2.containsKey(entry.getKey())) {
                    if (!entry.getValue().equals(sigma2.get(entry.getKey()))) {
                        return null;
                    }
                }
                res.put(entry.getKey(), entry.getValue());
            }
            for (final Map.Entry<PrologVariable, PrologTerm> entry : sigma2.entrySet()) {
                if (Terms.isAbstractVariable(entry.getKey()) || !Terms.isNonAbstractVariable(entry.getValue())) {
                    return null;
                } else if (sigma1.containsKey(entry.getKey())) {
                    if (!entry.getValue().equals(sigma1.get(entry.getKey()))) {
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
        final PrologSubstitution sigma1,
        final PrologSubstitution sigma2
    ) {
        if (sigma1 != null && sigma2 != null) {
            final PrologSubstitution res1 = PrologSubstitution.appendIfConsistentVariableRenaming(sigma1, sigma2);
            if (res1 == null) {
                return null;
            }
            final PrologSubstitution res2 = PrologSubstitution.appendIfConsistentVariableRenaming(sigma2, sigma1);
            if (res2 != null && res1.equals(res2)) {
                return res1;
            }
        }
        return null;
    }

    private static PrologSubstitution appendIfConsistentVariableRenaming(
        final PrologSubstitution sigma1,
        final PrologSubstitution sigma2
    ) {
        final PrologSubstitution res = new PrologSubstitution();
        for (final Map.Entry<PrologVariable, PrologTerm> entry : sigma1.entrySet()) {
            final PrologVariable key = entry.getKey();
            final PrologTerm value = entry.getValue();
            if (Terms.isAbstractVariable(key) || !Terms.isNonAbstractVariable(value)) {
                if (Terms.isNonAbstractVariable(key) || !Terms.isAbstractVariable(value)) {
                    return null;
                } else if (sigma2.containsKey(key)) {
                    if (!value.equals(sigma2.get(key))) {
                        return null;
                    }
                }
            } else if (sigma2.containsKey(key)) {
                if (!value.equals(sigma2.get(key))) {
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
