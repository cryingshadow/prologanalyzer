package prologanalyzer;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.*;

import com.google.gson.*;
import com.google.gson.stream.*;

import clit.*;
import prologanalyzer.structure.*;
import prologparser.*;

public class Main {

    public static final Gson GSON = new Gson();

    public static void main(final String[] args)
    throws IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        final CLITamer<Flag> tamer = new CLITamer<Flag>(Flag.class, Set.of(Flag.MODE, Flag.INPUT, Flag.OUTPUT));
        if (args == null || args.length < 1) {
            System.out.println(tamer.getParameterDescriptions());
            System.out.println(Main.helpText());
            return;
        }
        final Parameters<Flag> options = tamer.parse(args);
        final long timeout = options.containsKey(Flag.TIMEOUT) ? options.getAsInt(Flag.TIMEOUT) * 90 / 100 : 0;
        final Mode mode = Mode.valueOf(options.get(Flag.MODE));
        final SymbolicEvaluationStrategy strategy =
            switch (mode) {
            case TERMINATION -> new TerminationStrategy(7, timeout);
            case NON_TERMINATION -> new NonTerminationStrategy(7, timeout);
            default -> throw new IllegalStateException("Mode must be provided!");
            };
        final PrologProgram program = new PrologParser(new FileReader(options.get(Flag.INPUT))).parse();
        final Graph<AbstractState, SymbolicEvaluationEdgeLabel> graph = strategy.evaluate(program);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(options.get(Flag.OUTPUT)))) {
            TRSTransformer.writeTRS(graph, mode, program, writer);
        }
        if (options.containsKey(Flag.DOT)) {
            DOTWriter.writeDOTFile(graph, true, new File(options.get(Flag.DOT)));
        }
        if (options.containsKey(Flag.JSON)) {
            try (final JsonWriter writer = Main.GSON.newJsonWriter(new FileWriter(options.get(Flag.JSON)))) {
                Main.GSON.toJson(
                    graph.toJSON(state -> state.get().toJSON(), edge -> edge.get().toJSON()),
                    JSONGraph.class,
                    writer
                );
            }
        }
    }

    private static String helpText() {
        return String.format(
            "Available modes:\n%s",
            Arrays.stream(Mode.values())
            .map(mode ->
                String.format(
                    "%s (%s)",
                    mode.name(),
                    mode.parameters.stream()
                    .map(flag -> "-" + flag.shortName())
                    .collect(Collectors.joining(" and "))
                )
            ).collect(Collectors.joining("\n"))
        );
    }

}
