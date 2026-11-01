package prologanalyzer;

import java.math.*;
import java.util.*;

public class FreshNameGenerator extends LinkedHashMap<String, BigInteger> {

    private static final long serialVersionUID = 1L;

    private static String removeDigitSuffix(final String name) {
        return name.replaceAll("\\d*\\z", "");
    }

    public FreshNameGenerator() {
        super();
    }

    public FreshNameGenerator(final Set<String> existingNames) {
        for (final String name : existingNames) {
            final String nameWithoutSuffix = FreshNameGenerator.removeDigitSuffix(name);
            if (nameWithoutSuffix.length() != name.length()) {
                final BigInteger suffix = new BigInteger(name.substring(nameWithoutSuffix.length()));
                if (!this.containsKey(nameWithoutSuffix) || this.get(nameWithoutSuffix).compareTo(suffix) <= 0) {
                    this.put(nameWithoutSuffix, suffix.add(BigInteger.ONE));
                }
            } else if (!this.containsKey(name)) {
                this.put(name, BigInteger.ONE);
            }
        }
    }

    public String getFreshName(final String name) {
        final String nameWithoutSuffix = FreshNameGenerator.removeDigitSuffix(name);
        if (this.containsKey(nameWithoutSuffix)) {
            if (name.length() != nameWithoutSuffix.length()) {
                final BigInteger suffix = new BigInteger(name.substring(nameWithoutSuffix.length()));
                if (this.get(nameWithoutSuffix).compareTo(suffix) <= 0) {
                    this.put(nameWithoutSuffix, suffix.add(BigInteger.ONE));
                }
            }
        } else {
            if (name.length() != nameWithoutSuffix.length()) {
                final BigInteger suffix = new BigInteger(name.substring(nameWithoutSuffix.length()));
                this.put(nameWithoutSuffix, suffix.add(BigInteger.ONE));
            } else {
                this.put(nameWithoutSuffix, BigInteger.ONE);
            }
        }
        final BigInteger number = this.get(nameWithoutSuffix);
        final String result = nameWithoutSuffix + number.toString();
        this.put(nameWithoutSuffix, number.add(BigInteger.ONE));
        return result;
    }

}
