package prologanalyzer;

import java.math.*;

public record JSONEdge<E>(BigInteger from, E label, BigInteger to) implements Comparable<JSONEdge<E>> {

    @Override
    public int compareTo(final JSONEdge<E> o) {
        int compare = this.from().compareTo(o.from());
        if (compare != 0) {
            return compare;
        }
        compare = this.to().compareTo(o.to());
        if (compare != 0) {
            return compare;
        }
        return Integer.compare(this.label().hashCode(), o.label().hashCode());
    }

}
