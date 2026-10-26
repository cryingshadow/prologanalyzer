package prologanalyzer;

import java.math.*;
import java.util.*;

public class FreshNameGenerator extends LinkedHashMap<String, BigInteger> {

    private static final long serialVersionUID = 1L;

    public String getFreshName(final String name) {
        if (!this.containsKey(name)) {
            this.put(name, BigInteger.ONE);
        }
        final BigInteger number = this.get(name);
        final String result = name + number.toString();
        this.put(name, number.add(BigInteger.ONE));
        return result;
    }

}
