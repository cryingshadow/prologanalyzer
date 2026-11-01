package prologanalyzer;

import java.io.*;

import com.google.gson.*;
import com.google.gson.stream.*;

import prologanalyzer.structure.*;
import prologparser.*;

public class Main {

    public static final Gson GSON = new Gson();

    public static void main(final String[] args) throws IOException {
        try (final JsonWriter writer = Main.GSON.newJsonWriter(new FileWriter(args[1]))) {
            Main.GSON.toJson(
                new TerminationStrategy()
                .evaluate(new PrologParser(new FileReader(args[0])).parse())
                .toJSON(null, null),
                JSONGraph.class,
                writer
            );
        }
    }

}
