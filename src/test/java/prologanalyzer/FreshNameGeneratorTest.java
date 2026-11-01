package prologanalyzer;

import java.util.*;

import org.testng.*;
import org.testng.annotations.*;

public class FreshNameGeneratorTest {

    @DataProvider
    public Object[][] getFreshNameData() {
        return new Object[][] {
            {Set.of(), "X", "X1"},
            {Set.of(), "X2", "X3"},
            {Set.of(), "foo42", "foo43"},
            {Set.of(), "foo13bar7", "foo13bar8"},
            {Set.of("X23", "foo"), "X", "X24"},
            {Set.of("X23", "foo"), "foo", "foo1"},
            {Set.of("X23", "foo", "foo11", "foo1"), "foo", "foo12"},
            {Set.of("X23", "foo", "foo11", "foo1"), "foo11", "foo12"},
            {Set.of("X23", "foo", "foo11", "foo1"), "foo12", "foo13"},
            {Set.of("X23", "foo", "foo11", "foo1"), "foo7", "foo12"}
        };
    }

    @Test(dataProvider="getFreshNameData")
    public void getFreshNameTest(final Set<String> existing, final String name, final String expected) {
        Assert.assertEquals(new FreshNameGenerator(existing).getFreshName(name), expected);
    }

    @Test
    public void getMultipleFreshNamesTest() {
        final FreshNameGenerator fridge = new FreshNameGenerator();
        Assert.assertEquals(fridge.getFreshName("X"), "X1");
        Assert.assertEquals(fridge.getFreshName("X"), "X2");
        Assert.assertEquals(fridge.getFreshName("X1"), "X3");
        Assert.assertEquals(fridge.getFreshName("X5"), "X6");
        Assert.assertEquals(fridge.getFreshName("X5"), "X7");
        Assert.assertEquals(fridge.getFreshName("X7"), "X8");
    }

}
