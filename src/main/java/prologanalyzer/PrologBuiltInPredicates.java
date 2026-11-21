package prologanalyzer;

import java.util.*;

import prologanalyzer.structure.*;
import prologparser.*;

public class PrologBuiltInPredicates extends LinkedHashMap<String, Integer> {

    public static final FunctionSymbol FAIL = new FunctionSymbol("fail", 0);

    public static final PrologBuiltInPredicates INSTANCE = new PrologBuiltInPredicates();

    public static final FunctionSymbol UNIFY = new FunctionSymbol("=", 2);

    private static final long serialVersionUID = 1L;

    public static PrologProgram transform(final PrologProgram program) {
        if (program.clauses().stream().anyMatch(clause -> Terms.getFunctionSymbol(clause.head()).equals(PrologBuiltInPredicates.UNIFY))) {
            final FreshNameGenerator fridge =
                new FreshNameGenerator(PrologBuiltInPredicates.createSetOfAllNames(program));
            final String unifyName = fridge.getFreshName("unify");
            return new PrologProgram(
                program.clauses().stream().map(clause ->
                    new PrologClause(
                        PrologBuiltInPredicates.transformUnify(clause.head(), unifyName),
                        clause.body().map(body -> PrologBuiltInPredicates.transformUnify(body, unifyName))
                    )
                ).toList(),
                program.directives().stream().map(directive ->
                    PrologBuiltInPredicates.transformUnify(directive, unifyName)
                ).toList(),
                PrologBuiltInPredicates.transformUnify(program.operators(), unifyName),
                program.comments()
            );
        }
        return program;
    }

    private static void addAllNames(final PrologTerm term, final Set<String> names) {
        names.add(term.name);
        for (final PrologTerm argument : term.arguments) {
            PrologBuiltInPredicates.addAllNames(argument, names);
        }
    }

    private static Set<String> createSetOfAllNames(final PrologProgram program) {
        final Set<String> result = new LinkedHashSet<String>();
        for (final PrologClause clause : program.clauses()) {
            PrologBuiltInPredicates.addAllNames(clause.head(), result);
            if (clause.body().isPresent()) {
                PrologBuiltInPredicates.addAllNames(clause.body().get(), result);
            }
        }
        for (final PrologTerm directive : program.directives()) {
            PrologBuiltInPredicates.addAllNames(directive, result);
        }
        result.addAll(program.operators().keySet());
        return result;
    }

    private static OperatorSet transformUnify(final OperatorSet operators, final String unifyName) {
        if (operators.containsKey("=")) {
            final OperatorData data = operators.get("=");
            if (
                data.containsKey(OperatorFixity.XFX)
                || data.containsKey(OperatorFixity.YFX)
                || data.containsKey(OperatorFixity.XFY)
            ) {
                final OperatorSet result = new OperatorSet();
                result.putAll(operators);
                result.put(unifyName, data);
                return result;
            }
        }
        return operators;
    }

    private static PrologTerm transformUnify(final PrologTerm term, final String unifyName) {
        if (PrologBuiltInPredicates.UNIFY.equals(Terms.getFunctionSymbol(term))) {
            return new PrologTerm(
                unifyName,
                term.arguments.stream().map(argument ->
                    PrologBuiltInPredicates.transformUnify(argument, unifyName)
                ).toList()
            );
        }
        if (term.arguments.isEmpty()) {
            return term;
        }
        return new PrologTerm(
            term.name,
            term.arguments.stream().map(argument ->
                PrologBuiltInPredicates.transformUnify(argument, unifyName)
            ).toList()
        );
    }

    private PrologBuiltInPredicates() {
        super();
        this.put("abolish", 1);
        this.put("arg", 3);
        this.put("=:=", 2);
        this.put("=\\=", 2);
        this.put(">", 2);
        this.put(">=", 2);
        this.put("<", 2);
        this.put("=<", 2);
        this.put("asserta", 1);
        this.put("assertz", 1);
        this.put("at_end_of_stream", 0);
        this.put("at_end_of_stream", 1);
        this.put("atom", 1);
        this.put("atom_chars", 2);
        this.put("atom_codes", 2);
        this.put("atom_concat", 3);
        this.put("atom_length", 2);
        this.put("atomic", 1);
        this.put("bagof", 3);
        this.put("call", 1);
        this.put("catch", 3);
        this.put("char_code", 2);
        this.put("char_conversion", 2);
        this.put("clause", 2);
        this.put("close", 1);
        this.put("close", 2);
        this.put("compound", 1);
        this.put(",", 2);
        this.put("copy_term", 2);
        this.put("current_char_conversion", 2);
        this.put("current_input", 1);
        this.put("current_op", 3);
        this.put("current_output", 1);
        this.put("current_predicate", 1);
        this.put("current_prolog_flag", 2);
        this.put("!", 0);
        this.put(";", 2);
        this.put("fail", 0);
        this.put("findall", 3);
        this.put("float", 1);
        this.put("flush_output", 0);
        this.put("flush_output", 1);
        this.put("functor", 3);
        this.put("get_byte", 1);
        this.put("get_byte", 2);
        this.put("get_char", 1);
        this.put("get_char", 2);
        this.put("get_code", 1);
        this.put("get_code", 2);
        this.put("halt", 0);
        this.put("halt", 1);
        this.put("->", 2);
        this.put("integer", 1);
        this.put("is", 2);
        this.put("nl", 0);
        this.put("nl", 1);
        this.put("nonvar", 1);
        this.put("\\+", 1);
        this.put("number", 1);
        this.put("number_chars", 2);
        this.put("number_codes", 2);
        this.put("once", 1);
        this.put("op", 3);
        this.put("open", 3);
        this.put("open", 4);
        this.put("peek_byte", 1);
        this.put("peek_byte", 2);
        this.put("peek_char", 1);
        this.put("peek_char", 2);
        this.put("peek_code", 1);
        this.put("peek_code", 2);
        this.put("put_byte", 1);
        this.put("put_byte", 2);
        this.put("put_char", 1);
        this.put("put_char", 2);
        this.put("put_code", 1);
        this.put("put_code", 2);
        this.put("read", 1);
        this.put("read", 2);
        this.put("read_term", 2);
        this.put("read_term", 3);
        this.put("repeat", 0);
        this.put("retract", 1);
        this.put("set_input", 1);
        this.put("set_output", 1);
        this.put("set_prolog_flag", 2);
        this.put("set_stream_position", 2);
        this.put("setof", 3);
        this.put("stream_property", 2);
        this.put("sub_atom", 5);
        this.put("@>", 2);
        this.put("@>=", 2);
        this.put("==", 2);
        this.put("@<", 2);
        this.put("@=<", 2);
        this.put("\\==", 2);
        this.put("throw", 1);
        this.put("true", 0);
        this.put("\\=", 2);
        this.put("=", 2);
        this.put("unify_with_occurscheck", 2);
        this.put("=..", 2);
        this.put("var", 1);
        this.put("write", 1);
        this.put("write", 2);
        this.put("write_canonical", 1);
        this.put("write_canonical", 2);
        this.put("write_term", 2);
        this.put("write_term", 3);
        this.put("writeq", 1);
        this.put("writeq", 2);
    }

    public boolean isBuiltInPredicate(final PrologTerm term) {
        return this.containsKey(term.name) && Terms.getArity(term) == this.get(term.name);
    }

}
