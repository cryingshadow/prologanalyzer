package prologanalyzer.structure;

public record SymbolicEvaluationPath(
    Vertex<AbstractState> start,
    PrologSubstitution substitution,
    Vertex<AbstractState> end
) {

}
