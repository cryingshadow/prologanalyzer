package prologanalyzer;

import java.util.*;
import java.util.Optional;

import org.testng.*;
import org.testng.annotations.*;

import prologparser.*;

public class MGUComputerTest {

    @DataProvider
    public Object[][] computeMGUWithOnlyFreshVariablesData() {
        return new Object[][] {
            {
                new PrologTerm("t", List.of()),
                new PrologTerm("t", List.of()),
                false,
                Optional.of(new PrologSubstitution())
            },
            {
                new PrologTerm("t", List.of()),
                new PrologTerm("s", List.of()),
                false,
                Optional.empty()
            },
            {
                new PrologTerm("f", List.of(new PrologVariable("X"), new PrologVariable("Y"))),
                new PrologTerm("f", List.of(new PrologVariable("Z"), new PrologVariable("Z"))),
                false,
                Optional.of(
                    new PrologSubstitution(
                        Map.of(
                            new PrologVariable("X"), new PrologVariable("X1"),
                            new PrologVariable("Y"), new PrologVariable("X1"),
                            new PrologVariable("Z"), new PrologVariable("X1")
                        )
                    )
                )
            },
            {
                new PrologTerm("f", List.of(new PrologVariable("X"))),
                new PrologTerm("f", List.of(new PrologTerm("f", List.of(new PrologAbstractVariable("T"))))),
                false,
                Optional.of(
                    new PrologSubstitution(
                        Map.of(
                            new PrologVariable("X"), new PrologTerm("f", List.of(new PrologAbstractVariable("T1"))),
                            new PrologAbstractVariable("T"), new PrologAbstractVariable("T1")
                        )
                    )
                )
            },
            {
                new PrologTerm("f", List.of(new PrologVariable("X"))),
                new PrologTerm("f", List.of(new PrologTerm("f", List.of(new PrologVariable("X"))))),
                true,
                Optional.empty()
            }
        };
    }

    @Test(dataProvider="computeMGUWithOnlyFreshVariablesData")
    public void computeMGUWithOnlyFreshVariablesTest(
        final PrologTerm term1,
        final PrologTerm term2,
        final boolean useOccursCheck,
        final Optional<PrologSubstitution> expected
    ) {
        Assert.assertEquals(
            MGUComputer.computeMGUWithOnlyFreshVariables(term1, term2, useOccursCheck, new FreshNameGenerator()),
            expected
        );
    }

}
