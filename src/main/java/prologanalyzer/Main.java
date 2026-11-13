package prologanalyzer;

import java.io.*;
import java.util.*;

import com.google.gson.*;
import com.google.gson.stream.*;

import prologanalyzer.structure.*;
import prologparser.*;

public class Main {

    public static final Gson GSON = new Gson();

    public static void main(final String[] args) throws IOException {
        final boolean useTimeout = false;
        final long timeout = useTimeout ? 60000 : 0;
        final SymbolicEvaluationStrategy strategy =
            switch (args[0]) {
            case "t" -> new TerminationStrategy(7, timeout);
            case "n" -> new NonTerminationStrategy(timeout);
            default -> throw new IllegalArgumentException();
            };
        final EvaluationResult result = strategy.evaluate(new PrologParser(new FileReader(args[1])).parse());
            DOTWriter.writeDOTFile(result.graph(), result.rootId(), true, new File(args[2]));
        if (args.length > 3) {
            try (final JsonWriter writer = Main.GSON.newJsonWriter(new FileWriter(args[3]))) {
                Main.GSON.toJson(
                    result.graph().toJSON(
                        Optional::get,
                        null
                    ),
                    JSONGraph.class,
                    writer
                );
            }
        }
    }

}
