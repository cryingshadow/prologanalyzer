package prologanalyzer.structure;

import java.util.*;

public record Edge<E, V> (Optional<E> label, Vertex<V> to) implements Comparable<Edge<E, V>> {

    public boolean logicallyEquals(final Edge<E, V> other) {
        return this.label.equals(other.label) && this.to.logicallyEquals(other.to);
    }

    @Override
    public String toString() {
        return String.format(
            "-%s-> %s",
            this.label.isEmpty() ? "" : String.format("(%s)", this.label.get().toString()),
            this.to.toString()
        );
    }

    @Override
    public int compareTo(final Edge<E, V> o) {
        return this.toString().compareTo(o.toString());
    }

}
