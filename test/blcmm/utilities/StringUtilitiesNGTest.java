/*
 * Copyright (C) 2023 Christopher J. Kucera
 *
 * OpenBLCMM is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */
package blcmm.utilities;

import static org.testng.Assert.assertEquals;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 *
 * @author apocalyptech
 */
public class StringUtilitiesNGTest {

    public StringUtilitiesNGTest() throws Exception {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @BeforeMethod
    public void setUpMethod() throws Exception {
    }

    @AfterMethod
    public void tearDownMethod() throws Exception {
    }

    /**
     * Data provider for our substringStartsWith() test.  As per TestNG
     * requirements, it's an array of arrays, though the internal array is
     * effectively a tuple.  The "tuple" elements should be:
     *
     *  1) A string label, just used during test reporting so it's obvious what
     *     data's being tested.
     *  2) The string to search in
     *  3) The starting index of the substring
     *  4) The string to search for
     *  5) The expected result of the substringStartsWith() call
     *
     * @return
     */
    @DataProvider
    public Object[][] getSubstringStartsWithData() {
        return new Object[][] {
            { "Substring at start",
                "foo bar baz",
                0,
                "foo",
                true
            },
            { "Substring in middle",
                "foo bar baz",
                4,
                "bar",
                true
            },
            { "Substring at end",
                "foo bar baz",
                8,
                "baz",
                true
            },
            { "Not found",
                "foo bar baz",
                0,
                "frotz",
                false
            },
            { "Index too high",
                "foo bar baz",
                9,
                "baz",
                false
            },
            { "Index very too high",
                "foo bar baz",
                999,
                "baz",
                false
            },
            /* Eh, was gonna have this return false, but I think I prefer it
             * throwing an exception.  Returning false could end up masking a
             * larger problem, etc.
            { "Negative index",
                "foo bar baz",
                -1,
                "foo",
                false
            },
            */
            { "Case sensitive",
                "foo bar baz",
                0,
                "FOO",
                false
            },
        };
    }

    /**
     * Test of StringUtilities.substringStartsWith method.
     *
     * @param label Test label, just for reporting purposes
     * @param initialString The string to search in
     * @param index The index to start looking
     * @param searchString The string to look for
     * @param expectedOutput Whether or not the substring was found
     */
    @Test(dataProvider = "getSubstringStartsWithData")
    public void testSubstringStartsWith(String label,
            String initialString,
            int index,
            String searchString,
            boolean expectedOutput) {
        boolean realOutput = StringUtilities.substringStartsWith(initialString, index, searchString);
        assertEquals(realOutput, expectedOutput);
    }


}
