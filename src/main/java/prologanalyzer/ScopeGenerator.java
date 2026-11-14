package prologanalyzer;

public class ScopeGenerator {

    private int scope = 1;

    public int getFreshScope() {
        return this.scope++;
    }

}
