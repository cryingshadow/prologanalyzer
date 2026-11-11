package prologanalyzer.structure;

import java.util.*;

import org.testng.*;
import org.testng.annotations.*;

import prologparser.*;

public class PrologSubstitutionTest {

    @DataProvider
    public Object[][] applicationData(){
        return new Object[][] {
            {new PrologSubstitution(), null, null},
            {new PrologSubstitution(), new LabeledCut(1), new LabeledCut(1)},
            {new PrologSubstitution(), new PrologVariable("X"), new PrologVariable("X")},
            {new PrologSubstitution(), new PrologAbstractVariable("T"), new PrologAbstractVariable("T")},
            {
                new PrologSubstitution(Map.of(new PrologVariable("X"), new PrologTerm("p", List.of()))),
                new PrologVariable("X"),
                new PrologTerm("p", List.of())
            }
        };
    }

    @Test(dataProvider="applicationData")
    public void applicationTest(
        final PrologSubstitution substitution,
        final PrologTerm term,
        final PrologTerm expected
    ) {
        Assert.assertEquals(substitution.apply(term), expected);
    }

}
