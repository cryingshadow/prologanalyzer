package prologanalyzer;

import java.math.*;
import java.util.*;

public record JSONGraph<V, E>(Map<BigInteger, V> nodes, Set<JSONEdge<E>> edges) {

}
