package prologanalyzer;

import java.io.*;
import java.math.*;
import java.util.*;
import java.util.stream.*;

import prologanalyzer.evaluationrules.*;
import prologanalyzer.structure.*;
import prologparser.*;

public class TRSTransformer {

    private static final String CONNECTION_SYMBOL_PREFIX = "u";

    private static final String IN_SYMBOL_PREFIX = "fin";

    private static final String OUT_SYMBOL_PREFIX = "fout";

    public static void writeTRS(
        final Graph<AbstractState, SymbolicEvaluationEdgeLabel> graph,
        final Mode mode,
        final PrologProgram program,
        final BufferedWriter writer
    ) throws IOException {
        final VertexSets sets = TRSTransformer.computeVertexSets(graph);
        final List<TermPair> rules = new LinkedList<TermPair>();
        switch (mode) {
        case NON_TERMINATION:
            final List<SymbolicEvaluationPath> paths = TRSTransformer.calculateAllEvaluationPaths(graph, sets, false);
            for (final SymbolicEvaluationPath path : paths) {
                TRSTransformer.transformPathToRulesForNonTermination(path, rules, graph, sets, program);
            }
            break;
        case TERMINATION:
            //TODO
            break;
        default:
            throw new IllegalStateException("Unknown mode detected!");
        }
        writer.write("(format TRS)\n");
        final Set<FunctionSymbol> symbols =
            rules
            .stream()
            .flatMap(pair ->
                Stream.concat(
                    Terms.createSetOfAllFunctionSymbols(pair.term1()).stream(),
                    Terms.createSetOfAllFunctionSymbols(pair.term2()).stream()
                )
            ).collect(Collectors.toSet());
        for (final FunctionSymbol symbol : symbols) {
            writer.write(String.format("(fun %s %d)\n", TRSTransformer.toARI(symbol.name()), symbol.arity()));
        }
        for (final TermPair rule : rules) {
            writer.write(
                String.format("(rule %s %s)\n", TRSTransformer.toARI(rule.term1()), TRSTransformer.toARI(rule.term2()))
            );
        }
    }

    private static List<SymbolicEvaluationPath> calculateAllEvaluationPaths(
        final Graph<AbstractState, SymbolicEvaluationEdgeLabel> graph,
        final VertexSets sets,
        final boolean termination
    ) {
        final List<SymbolicEvaluationPath> result = new LinkedList<SymbolicEvaluationPath>();
        if (termination) {
            for (final Vertex<AbstractState> root : graph.getStartVertices()) {
                result.addAll(
                    TRSTransformer.calculateAllEvaluationPathsFromVertex(
                        graph,
                        root,
                        root,
                        new PrologSubstitution(),
                        0,
                        sets,
                        termination
                    )
                );
            }
            for (final Vertex<AbstractState> vertex : sets.splits()) {
                final Vertex<AbstractState> firstSplit = TRSTransformer.getFirstSplitChild(graph, vertex);
                final Vertex<AbstractState> lastSplit = TRSTransformer.getLastSplitChild(graph, vertex);
                result.addAll(
                    TRSTransformer.calculateAllEvaluationPathsFromVertex(
                        graph,
                        firstSplit,
                        firstSplit,
                        new PrologSubstitution(),
                        0,
                        sets,
                        termination
                    )
                );
                result.addAll(
                    TRSTransformer.calculateAllEvaluationPathsFromVertex(
                        graph,
                        lastSplit,
                        lastSplit,
                        new PrologSubstitution(),
                        0,
                        sets,
                        termination
                    )
                );
            }
        }
        for (final Vertex<AbstractState> vertex : sets.instanceChildren()) {
            if (termination || !sets.instanceParents().contains(vertex)) {
                result.addAll(
                    TRSTransformer.calculateAllEvaluationPathsFromVertex(
                        graph,
                        vertex,
                        vertex,
                        new PrologSubstitution(),
                        0,
                        sets,
                        termination
                    )
                );
            }
        }
        return result;
    }

    private static List<SymbolicEvaluationPath> calculateAllEvaluationPathsFromVertex(
        final Graph<AbstractState, SymbolicEvaluationEdgeLabel> graph,
        final Vertex<AbstractState> start,
        final Vertex<AbstractState> vertex,
        final PrologSubstitution substitution,
        final int length,
        final VertexSets sets,
        final boolean termination
    ) {
        final List<SymbolicEvaluationPath> result = new LinkedList<SymbolicEvaluationPath>();
        if (termination && length > 0 && sets.successes().contains(vertex)) {
            result.add(new SymbolicEvaluationPath(start, substitution, vertex));
        }
        if (
            length > 0
            && (
                (
                    sets.instanceChildren().contains(vertex)
                    && (termination || !sets.instanceParents().contains(vertex))
                ) || (termination && sets.splits().contains(vertex))
            )
        ) {
            result.add(new SymbolicEvaluationPath(start, substitution, vertex));
        } else if (termination && length > 0 && sets.instanceParents().contains(vertex)) {
            result.add(new SymbolicEvaluationPath(start, substitution, vertex));
        } else if (length == 0 || termination || !sets.instanceChildren().contains(vertex)) {
            for (final Edge<SymbolicEvaluationEdgeLabel, AbstractState> edge : graph.getAdjacencySet(vertex)) {
                final PrologSubstitution nextSubstitution = substitution.append(edge.label().get().substitution());
                result.addAll(
                    TRSTransformer.calculateAllEvaluationPathsFromVertex(
                        graph,
                        start,
                        edge.to(),
                        nextSubstitution,
                        length + 1,
                        sets,
                        termination
                    )
                );
            }
        }
        return result;
    }

    private static VertexSets computeVertexSets(final Graph<AbstractState, SymbolicEvaluationEdgeLabel> graph) {
        final Set<Vertex<AbstractState>> instanceChildren = new LinkedHashSet<Vertex<AbstractState>>();
        final Set<Vertex<AbstractState>> instanceParents = new LinkedHashSet<Vertex<AbstractState>>();
        final Set<Vertex<AbstractState>> splits = new LinkedHashSet<Vertex<AbstractState>>();
        final Set<Vertex<AbstractState>> parallels = new LinkedHashSet<Vertex<AbstractState>>();
        final Set<Vertex<AbstractState>> successes = new LinkedHashSet<Vertex<AbstractState>>();
        for (
            final Map.Entry<Vertex<AbstractState>, Set<Edge<SymbolicEvaluationEdgeLabel, AbstractState>>> edgeEntry :
                graph.getEdges().entrySet()
        ) {
            for (final Edge<SymbolicEvaluationEdgeLabel, AbstractState> edge : edgeEntry.getValue()) {
                final SymbolicEvaluationRule rule = edge.label().get().rule();
                if (rule instanceof InstanceRule) {
                    instanceParents.add(edgeEntry.getKey());
                    instanceChildren.add(edge.to());
                } else if (rule instanceof SuccessRule) {
                    successes.add(edgeEntry.getKey());
                } else if (rule instanceof ParallelRule) {
                    parallels.add(edgeEntry.getKey());
                } else if (rule instanceof SplitRule) {
                    splits.add(edgeEntry.getKey());
                }
            }
        }
        return new VertexSets(instanceChildren, instanceParents, splits, parallels, successes);
    }

    private static PrologTerm connectionTerm(
        final BigInteger id1,
        final BigInteger id2,
        final PrologTerm firstArgument,
        final List<PrologVariable> variables
    ) {
        final List<PrologTerm> arguments = new ArrayList<PrologTerm>();
        arguments.add(firstArgument);
        for (final PrologVariable variable : variables) {
            arguments.add(variable);
        }
        return new PrologTerm(
            String.format("%s%s/%s", TRSTransformer.CONNECTION_SYMBOL_PREFIX, id1.toString(), id2.toString()),
            arguments
        );
    }

    private static PrologTerm encodeInTerm(
        final Vertex<AbstractState> vertex,
        final Graph<AbstractState, SymbolicEvaluationEdgeLabel> graph,
        final VertexSets sets,
        final boolean termination
    ) {
        if (sets.instanceParents().contains(vertex)) {
            final Edge<SymbolicEvaluationEdgeLabel, AbstractState> edge =
                graph.getAdjacencySet(vertex).iterator().next();
            if (termination) {
                return edge.label().get().substitution().apply(
                    TRSTransformer.encodeInTerm(edge.to(), graph, sets, termination)
                );
            } else {
                return TRSTransformer.encodeInTerm(edge.to(), graph, sets, termination);
            }
        }
        final List<PrologTerm> arguments =
            new ArrayList<PrologTerm>(vertex.label().get().createSetOfAllGroundVariablesInGoal());
        return new PrologTerm(TRSTransformer.IN_SYMBOL_PREFIX + vertex.id().toString(), arguments);
    }

    private static PrologTerm encodeOutTerm(
        final Vertex<AbstractState> vertex,
        final Graph<AbstractState, SymbolicEvaluationEdgeLabel> graph,
        final VertexSets sets,
        final PrologProgram program,
        final boolean termination
    ) {
        if (sets.instanceParents().contains(vertex)) {
            final Edge<SymbolicEvaluationEdgeLabel, AbstractState> edge =
                graph.getAdjacencySet(vertex).iterator().next();
            if (termination) {
                return edge.label().get().substitution().apply(
                    TRSTransformer.encodeOutTerm(edge.to(), graph, sets, program, termination)
                );
            } else {
                return TRSTransformer.encodeOutTerm(edge.to(), graph, sets, program, termination);
            }
        }
        final List<PrologTerm> arguments =
            new ArrayList<PrologTerm>(vertex.label().get().createSetOfAllFutureGroundVariables(program));
        return new PrologTerm(TRSTransformer.OUT_SYMBOL_PREFIX + vertex.id().toString(), arguments);
    }

    private static Vertex<AbstractState> getFirstSplitChild(
        final Graph<AbstractState, SymbolicEvaluationEdgeLabel> graph,
        final Vertex<AbstractState> vertex
    ) {
        final List<Edge<SymbolicEvaluationEdgeLabel, AbstractState>> edges =
            new ArrayList<Edge<SymbolicEvaluationEdgeLabel, AbstractState>>(graph.getAdjacencySet(vertex));
        if (
            Terms.getFirstConjunct(vertex.label().get().goal.getFirst().getTerm())
            .equals(Terms.getFirstConjunct(edges.getFirst().to().label().get().goal.getFirst().getTerm()))
        ) {
            return edges.getFirst().to();
        }
        return edges.getLast().to();
    }

    private static PrologSubstitution getInstanceMatcher(
        final Vertex<AbstractState> end,
        final Graph<AbstractState, SymbolicEvaluationEdgeLabel> graph,
        final VertexSets sets
    ) {
        if (sets.instanceParents().contains(end)) {
            return graph.getAdjacencySet(end).iterator().next().label().get().substitution();
        }
        return new PrologSubstitution();
    }

    private static Vertex<AbstractState> getLastSplitChild(
        final Graph<AbstractState, SymbolicEvaluationEdgeLabel> graph,
        final Vertex<AbstractState> vertex
    ) {
        final List<Edge<SymbolicEvaluationEdgeLabel, AbstractState>> edges =
            new ArrayList<Edge<SymbolicEvaluationEdgeLabel, AbstractState>>(graph.getAdjacencySet(vertex));
        if (
            Terms.getFirstConjunct(vertex.label().get().goal.getFirst().getTerm())
            .equals(Terms.getFirstConjunct(edges.getFirst().to().label().get().goal.getFirst().getTerm()))
        ) {
            return edges.getLast().to();
        }
        return edges.getFirst().to();
    }

    private static String toARI(final PrologTerm term) {
        if (term.arguments.isEmpty()) {
            return TRSTransformer.toARI(term.name);
        }
        return String.format(
            "(%s %s)",
            TRSTransformer.toARI(term.name),
            term.arguments.stream().map(TRSTransformer::toARI).collect(Collectors.joining(" "))
        );
    }

    private static String toARI(final String name) {
        if (name.contains("|") || name.matches("(\\[|\\d|\\]).*")) {
            return String.format("|%s|", name.replaceAll("\\|", "/"));
        }
        return name;
    }

    private static void transformPathToRulesForNonTermination(
        final SymbolicEvaluationPath path,
        final List<TermPair> rules,
        final Graph<AbstractState, SymbolicEvaluationEdgeLabel> graph,
        final VertexSets sets,
        final PrologProgram program
    ) {
        final PrologTerm inTerm =
            path.substitution().apply(TRSTransformer.encodeInTerm(path.start(), graph, sets, false));
//        final List<PrologVariable> variables = new LinkedList<PrologVariable>(Terms.createSetOfAllVariables(inTerm));
        rules.add(
            new TermPair(
                inTerm,
                TRSTransformer.encodeInTerm(path.end(), graph, sets, false)
            )
        );
    }

}
