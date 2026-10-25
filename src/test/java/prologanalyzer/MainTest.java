package prologanalyzer;

import org.testng.*;
import org.testng.annotations.*;

public class MainTest {

    @DataProvider
    public Object[][] mainData() {
        return new Object[][] {
            {"foo", 3},
            {"", 0}
        };
    }

    @Test(dataProvider="mainData")
    public void mainTest(final String input, final int expected) {
        Assert.assertEquals(input.length(), expected);
    }

}
