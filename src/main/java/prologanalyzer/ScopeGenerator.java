package prologanalyzer;

public class ScopeGenerator {

    private int scope = 0;

    public int getFreshScope() {
        return this.scope++;
    }

}
