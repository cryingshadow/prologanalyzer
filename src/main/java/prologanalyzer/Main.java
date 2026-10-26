package prologanalyzer;

import java.io.*;

import com.google.gson.*;

import prologparser.*;

public class Main {

    public static final Gson GSON = new Gson();

    public static void main(final String[] args) throws IOException {
        IO.println(new PrologParser(new FileReader(args[0])).parse());
    }

}
