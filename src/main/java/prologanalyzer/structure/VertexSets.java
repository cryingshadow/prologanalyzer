package prologanalyzer.structure;

import java.util.*;

public record VertexSets(
    Set<Vertex<AbstractState>> instanceChildren,
    Set<Vertex<AbstractState>> instanceParents,
    Set<Vertex<AbstractState>> splits,
    Set<Vertex<AbstractState>> parallels,
    Set<Vertex<AbstractState>> successes
) {

}
