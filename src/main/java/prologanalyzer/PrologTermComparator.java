package prologanalyzer;

import java.util.*;

import prologparser.*;

public class PrologTermComparator implements Comparator<PrologTerm> {

    public static final PrologTermComparator INSTANCE = new PrologTermComparator();

    private PrologTermComparator() {

    }

    @Override
    public int compare(final PrologTerm term1, final PrologTerm term2) {
        return term1.toString().compareTo(term2.toString());
    }

}
