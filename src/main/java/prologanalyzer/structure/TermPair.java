package prologanalyzer.structure;

import prologanalyzer.*;
import prologparser.*;

public record TermPair(PrologTerm term1, PrologTerm term2) implements Comparable<TermPair> {

    @Override
    public int compareTo(final TermPair o) {
        final int result = PrologTermComparator.INSTANCE.compare(this.term1(), o.term1());
        if (result != 0) {
            return result;
        }
        return PrologTermComparator.INSTANCE.compare(this.term2(), o.term2());
    }

}
