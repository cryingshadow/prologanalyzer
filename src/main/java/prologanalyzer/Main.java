package prologanalyzer;

import java.io.*;

import com.google.gson.*;
import com.google.gson.stream.*;

import prologanalyzer.structure.*;
import prologparser.*;

public class Main {

    public static final Gson GSON = new Gson();

    public static void main(final String[] args) throws IOException {
        final EvaluationResult result =
            new TerminationStrategy(7, 0).evaluate(new PrologParser(new FileReader(args[0])).parse());
//        try (final JsonWriter writer = Main.GSON.newJsonWriter(new FileWriter(args[1]))) {
//            Main.GSON.toJson(result.graph().toJSON(null, null), JSONGraph.class, writer);
//        }
        if (args.length > 2) {
            DOTWriter.writeDOTFile(result.graph(), result.rootId(), true, new File(args[2]));
        }
    }

}
