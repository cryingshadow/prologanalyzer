package prologanalyzer;

import java.io.*;
import java.math.*;
import java.util.*;
import java.util.Map.*;

import prologanalyzer.evaluationrules.*;
import prologanalyzer.structure.*;

public abstract class DOTWriter {

    public static void writeDOTFile(
        final Graph<AbstractState, SymbolicEvaluationEdgeLabel> graph,
        final BigInteger rootId,
        final boolean showNumbers,
        final File outputFile
    ) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write("digraph dp_graph {\nnode [outthreshold=100, inthreshold=100];\n");
            BigInteger maxNodeNr = BigInteger.ZERO;
            for (final Vertex<AbstractState> from : graph.getVertices()) {
                if (from.id().compareTo(maxNodeNr) > 0) {
                    maxNodeNr = from.id();
                }
                writer.write(from.id().toString());
                writer.write(" [");
                if (from.label().isPresent()) {
                    writer.write("label=\"");
                    if (showNumbers) {
                        writer.write(from.id().toString());
                        writer.write(": ");
                    }
                    writer.write(from.label().get().toString());
                    writer.write("\", ");
                }
                if (from.id().equals(rootId)) {
                    writer.write("fontsize=16, style=filled, fillcolor=lightyellow, color=red");
                } else {
                    writer.write("fontsize=16, style=filled, fillcolor=lightyellow");
                }
                writer.write("];\n");
            }
            for (
                final Entry<Vertex<AbstractState>, Set<Edge<SymbolicEvaluationEdgeLabel, AbstractState>>> edgeEntry :
                    graph.getEdges().entrySet()
            ) {
                for (final Edge<SymbolicEvaluationEdgeLabel, AbstractState> edge : edgeEntry.getValue()) {
                    maxNodeNr = maxNodeNr.add(BigInteger.ONE);
                    final SymbolicEvaluationEdgeLabel edgeLabel = edge.label().get();
                    final boolean isInstance = edgeLabel.rule() instanceof InstanceRule;
                    writer.write(maxNodeNr.toString());
                    writer.write(" [label=\"");
                    writer.write(edgeLabel.toString());
                    writer.write("\", ");
                    writer.write("fontsize=14, style=filled, fillcolor=");
                    writer.write(edgeLabel.rule().getDOTColor());
                    writer.write("];\n");
                    writer.write(edgeEntry.getKey().id().toString());
                    writer.write(" -> ");
                    writer.write(maxNodeNr.toString());
                    writer.write(" [arrowhead=none");
                    if (isInstance) {
                        writer.write(", style=dashed");
                    }
                    writer.write("];\n");
                    writer.write(maxNodeNr.toString());
                    writer.write(" -> ");
                    writer.write(edge.to().id().toString());
                    if (isInstance) {
                        writer.write("[style=dashed]");
                    }
                    writer.write(";\n\n");
                }
            }
            writer.write("}\n");
        }
    }

}
