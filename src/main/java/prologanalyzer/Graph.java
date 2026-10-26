package prologanalyzer;

import java.math.*;
import java.util.*;
import java.util.Map.*;
import java.util.function.*;
import java.util.stream.*;

public class Graph<V, E> {

    public static<V, E> Graph<V, E> create(final AdjacencySets<V, E> adjacencySets) {
        final Graph<V, E> result = new Graph<V, E>();
        result.adjacencySets = new AdjacencySets<V, E>(adjacencySets);
        return result;
    }

    public static<V, E> Graph<V, E> create(final Map<Vertex<V>, ? extends Set<Edge<E, V>>> adjacencySets) {
        final Graph<V, E> result = new Graph<V, E>();
        result.adjacencySets = new AdjacencySets<V, E>(adjacencySets);
        return result;
    }

    private AdjacencySets<V, E> adjacencySets;

    private final Set<Vertex<V>> startVertices;

    public Graph() {
        this.adjacencySets = new AdjacencySets<V, E>();
        this.startVertices = new TreeSet<Vertex<V>>();
    }

    private Graph(final AdjacencySets<V, E> adjacencySets, final Set<Vertex<V>> startVertices) {
        this.adjacencySets = adjacencySets;
        this.startVertices = startVertices;
    }

    public void addEdge(final Vertex<V> from, final Optional<E> label, final Vertex<V> to) {
        if (from == null || to == null) {
            throw new NullPointerException();
        }
        this.adjacencySets.addEdge(from, label, to);
    }

    public void addVertex(final Vertex<V> vertex) {
        if (!this.adjacencySets.containsKey(vertex)) {
            this.adjacencySets.put(vertex, new TreeSet<Edge<E, V>>());
        }
    }

    public Graph<V, E> copy(final Function<E, E> labelCopy) {
        final Graph<V, E> result = this.nodeCopy();
        for (final Entry<Vertex<V>, Set<Edge<E, V>>> entry : this.adjacencySets.entrySet()) {
            for (final Edge<E, V> edge : entry.getValue()) {
                result.addEdge(
                    entry.getKey(),
                    edge.label().isEmpty() ? Optional.empty() : Optional.of(labelCopy.apply(edge.label().get())),
                    edge.to()
                );
            }
        }
        return result;
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof final Graph other) {
            return this.adjacencySets.equals(other.adjacencySets);
        }
        return false;
    }

    public Set<Edge<E, V>> getAdjacencySet(final Vertex<V> vertex) {
        final Set<Edge<E, V>> set = this.adjacencySets.get(vertex);
        if (set == null) {
            return null;
        }
        return new TreeSet<Edge<E, V>>(set);
    }

    public Set<Vertex<V>> getAdjacentVertices(final Vertex<V> vertex) {
        return this.getAdjacencySet(vertex).stream().map(edge -> edge.to()).collect(Collectors.toSet());
    }

    public Map<Vertex<V>, Set<Edge<E, V>>> getEdges() {
        final Map<Vertex<V>, Set<Edge<E, V>>> result = new TreeMap<Vertex<V>, Set<Edge<E, V>>>();
        for (final Map.Entry<Vertex<V>, Set<Edge<E, V>>> entry : this.adjacencySets.entrySet()) {
            result.put(entry.getKey(), new TreeSet<Edge<E, V>>(entry.getValue()));
        }
        return result;
    }

    public Set<Edge<E, V>> getEdges(final Vertex<V> from, final Vertex<V> to) {
        final Set<Edge<E, V>> res = new TreeSet<Edge<E, V>>();
        final Set<Edge<E, V>> set = this.adjacencySets.get(from);
        if (set != null) {
            for (final Edge<E, V> edge : set) {
                if (to.equals(edge.to())) {
                    res.add(edge);
                }
            }
        }
        return res;
    }

    public Vertex<V> getVertexWithUniqueLabel(final V label) {
        return this.adjacencySets
            .keySet()
            .stream()
            .filter(vertex -> vertex.label().isPresent() && vertex.label().get().equals(label))
            .findAny()
            .get();
    }

    public Set<Vertex<V>> getVertices() {
        return new TreeSet<Vertex<V>>(this.adjacencySets.keySet());
    }

    public Set<Vertex<V>> getVerticesWithLabel(final V label) {
        final Set<Vertex<V>> res = new TreeSet<Vertex<V>>();
        for (final Vertex<V> vertex : this.adjacencySets.keySet()) {
            if (vertex.label().isPresent() && label.equals(vertex.label().get())) {
                res.add(vertex);
            }
        }
        return res;
    }

    @Override
    public int hashCode() {
        return this.adjacencySets.hashCode() * 3;
    }

    public boolean isStartVertex(final Vertex<V> vertex) {
        return this.startVertices.contains(vertex);
    }

    public boolean logicallyEquals(final Graph<V,E> other) {
        return this.adjacencySets.logicallyEquals(other.adjacencySets);
    }

    public Graph<V, E> nodeCopy() {
        final AdjacencySets<V, E> emptySets = new AdjacencySets<V, E>();
        for (final Vertex<V> vertex : this.adjacencySets.keySet()) {
            emptySets.put(vertex, new TreeSet<Edge<E,V>>());
        }
        return new Graph<V, E>(emptySets, new TreeSet<Vertex<V>>(this.startVertices));
    }

    public void replaceEdgeLabel(final Vertex<V> from, final E label, final Vertex<V> to) {
        this.adjacencySets.put(
            from,
            this
            .adjacencySets
            .get(from)
            .stream()
            .map(edge -> edge.to().equals(to) ? new Edge<E, V>(Optional.of(label), to) : edge)
            .collect(Collectors.toCollection(TreeSet::new))
        );
    }

    public void setStartVertex(final Vertex<V> vertex) {
        this.startVertices.add(vertex);
    }

    public JSONGraph<V, E> toJSON(final V vertexNullLabel, final E edgeNullLabel) {
        final Map<BigInteger, V> nodes = new TreeMap<BigInteger, V>();
        final Set<JSONEdge<E>> edges = new TreeSet<JSONEdge<E>>();
        for (final Entry<Vertex<V>, Set<Edge<E, V>>> entry : this.adjacencySets.entrySet()) {
            final BigInteger from = entry.getKey().id();
            nodes.put(from, entry.getKey().label().orElse(vertexNullLabel));
            for (final Edge<E, V> edge : entry.getValue()) {
                edges.add(new JSONEdge<E>(from, edge.label().orElse(edgeNullLabel), edge.to().id()));
            }
        }
        return new JSONGraph<V, E>(nodes, edges);
    }

    @Override
    public String toString() {
        return this.adjacencySets.toString();
    }

    public Graph<V, E> transpose() {
        final AdjacencySets<V, E> transposed = new AdjacencySets<V, E>();
        for (final Map.Entry<Vertex<V>, Set<Edge<E, V>>> entry : this.adjacencySets.entrySet()) {
            for (final Edge<E, V> edge : entry.getValue()) {
                transposed.addEdge(edge.to(), edge.label(), entry.getKey());
            }
            if (entry.getValue().isEmpty() && !transposed.containsKey(entry.getKey())) {
                transposed.put(entry.getKey(), new TreeSet<Edge<E, V>>());
            }
        }
        return new Graph<V, E>(transposed, this.startVertices);
    }

}
