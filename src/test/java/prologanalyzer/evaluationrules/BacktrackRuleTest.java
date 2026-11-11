package prologanalyzer.evaluationrules;

import java.util.*;
import java.util.Optional;

import org.testng.*;
import org.testng.annotations.*;

import prologanalyzer.*;
import prologanalyzer.structure.*;
import prologparser.*;

public class BacktrackRuleTest {

    @DataProvider
    public Object[][] backtrackData() {
        return new Object[][] {
            {
                new AbstractState(List.of(), new KnowledgeBase()),
                new GlobalState(
                    new PrologProgram(List.of(), List.of(), new OperatorSet(), List.of()),
                    new FreshNameGenerator(),
                    new ScopeGenerator()
                ),
                List.of()
            },
            {
                new AbstractState(
                    List.of(
                        new LabeledTermGoalElement(
                            new PrologTerm("p", List.of()),
                            new PrologClause(new PrologTerm("q", List.of()), Optional.empty()),
                            0
                        )
                    ),
                    new KnowledgeBase()
                ),
                new GlobalState(
                    new PrologProgram(
                        List.of(new PrologClause(new PrologTerm("q", List.of()), Optional.empty())),
                        List.of(),
                        new OperatorSet(),
                        List.of()
                    ),
                    new FreshNameGenerator(),
                    new ScopeGenerator()
                ),
                List.of(
                    new SymbolicEvaluationResult(
                        new AbstractState(List.of(), new KnowledgeBase()),
                        new PrologSubstitution()
                    )
                )
            },
            {
                new AbstractState(
                    List.of(
                        new LabeledTermGoalElement(
                            new PrologTerm("p", List.of(new PrologTerm("0", List.of()))),
                            new PrologClause(new PrologTerm("p", List.of(new PrologVariable("X"))), Optional.empty()),
                            0
                        )
                    ),
                    new KnowledgeBase()
                ),
                new GlobalState(
                    new PrologProgram(
                        List.of(
                            new PrologClause(new PrologTerm("p", List.of(new PrologVariable("X"))), Optional.empty())
                        ),
                        List.of(),
                        new OperatorSet(),
                        List.of()
                    ),
                    new FreshNameGenerator(),
                    new ScopeGenerator()
                ),
                List.of()
            }
        };
    }

    @Test(dataProvider="backtrackData")
    public void backtrackTest(
        final AbstractState state,
        final GlobalState global,
        final List<SymbolicEvaluationResult> expected
    ) {
        Assert.assertEquals(BacktrackRule.INSTANCE.apply(state, global), expected);
    }

}
