/*
 * Copyright (C) 2018-2020  LightChaosman
 * Copyright (C) 2018-2023 Christopher J. Kucera
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
package blcmm.gui.tree;

import blcmm.gui.theme.ThemeManager;
import blcmm.model.Category;
import blcmm.model.CompletePatch;
import blcmm.model.HotfixType;
import blcmm.model.HotfixWrapper;
import blcmm.model.ModelElement;
import blcmm.model.SetCMPCommand;
import blcmm.model.SetCommand;
import blcmm.utilities.Options;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.testng.Assert.*;
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
public class OverwriteCheckerNGTest {

    private SetCommand[] storedCommands;

    public OverwriteCheckerNGTest() throws Exception {
        Options.loadOptions(null);
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
     * Sets up a category with the specified commands setup in a freshly-created
     * master Category.  Hotfixes can be specified with a custom format string
     * like so:
     *
     *   patch set foo bar baz
     *   level none set foo bar baz
     *   ondemand none set foo bar baz
     *
     * This method will additionally populate our storedCommands array for
     * easy access during testing, and call reset() on OverwriteChecker, to
     * initialize its internal data structures.
     *
     * @param commands Commands to add to the root category
     * @return The created Category
     */
    private Category testCategory(String[] commands) {
        ArrayList<SetCommand> toStore = new ArrayList<>();
        Category cat = new Category("Root Cat");
        CompletePatch patch = new CompletePatch();
        patch.createNewProfile("default");
        patch.setRoot(cat);
        SetCommand cmd;
        for (String s : commands) {
            String[] parts = s.split(" ");
            HotfixType type = null;
            try {
                type = HotfixType.valueOf(parts[0].toUpperCase());
            } catch (IllegalArgumentException e) {
            }
            if (type != null) {
                String param = null;
                if (type == HotfixType.PATCH) {
                    s = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
                } else {
                    param = parts[1];
                    s = String.join(" ", Arrays.copyOfRange(parts, 2, parts.length));
                }
                HotfixWrapper wrapper = new HotfixWrapper("name", type, param, Arrays.asList(s));
                cmd = wrapper.get(0);
                patch.insertElementInto(wrapper, cat);
            } else {
                if (parts[0].equals("set_cmp")) {
                    cmd = new SetCMPCommand(s);
                } else {
                    cmd = new SetCommand(s);
                }
                patch.insertElementInto(cmd, cat);
            }
            toStore.add(cmd);
            patch.setSelected(cmd, true);
        }
        OverwriteChecker.reset(cat);
        storedCommands = toStore.toArray(new SetCommand[toStore.size()]);
        return cat;
    }

    /**
     * Test data class.  This base class is basically intended to be used
     * for any test data *after* the first element.  Our TestDataF class is
     * what resets the counter for us.  It's a bit silly, since you could
     * just use the boolean constructor, but I've found I just don't like
     * how that looks when defining all the tests.
     */
    public static class TestData {
        public static int counter=0;
        public int test;
        public int[] results;
        public TestData(int ... results) {
            this(false, results);
        }
        public TestData(boolean first, int ... results) {
            if (first) {
                counter = 0;
            }
            this.test = counter++;
            this.results = results;
        }
        public List<SetCommand> getResultSet(SetCommand[] storedCommands) {
            ArrayList<SetCommand> toReturn = new ArrayList<>();
            for (int result : results) {
                toReturn.add(storedCommands[result]);
            }
            return toReturn;
        }
    }

    /**
     * TestData intended to be used as the first in a series, so we know
     * to reset the counter.  This is all a bit silly, but in the end I don't
     * like how it looks when passing booleans in.
     */
    public static class TestDataF extends TestData {
        public TestDataF(int ... results) {
            super(true, results);
        }
    }

    /**
     * Data provider for our tests.  As per TestNG requirements, it's an array
     * of arrays, though the internal array is effectively a tuple.  The
     * "tuple" elements should be:
     *
     *  1) A string label, just used during test reporting so it's obvious what
     *     data's being tested.
     *  2) The commands to populate our test patch with.  Uses our custom little
     *     syntax for hotfixes; see testCategory's Javadocs for that info.
     *  3) Complete Overwriters test data
     *  4) Complete Overwrittens test data
     *  5) Partial Overwriters test data
     *  6) Partial Overwrittens test data
     *
     * @return
     */
    @DataProvider
    public Object[][] getOverwriteData() {
        return new Object[][] {
            { "Single Statement",
              new String[] {"set foo bar baz"},
              new TestData[] {
                  new TestDataF(),
              },
              new TestData[] {
                  new TestDataF(),
              },
              new TestData[] {
                  new TestDataF(),
              },
              new TestData[] {
                  new TestDataF(),
              },
            },
            { "No Overwrite",
              new String[] {"set foo bar baz", "set foo frotz baz"},
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
            },
            { "Similar Commands (no overwrite)",
              new String[] {"set foo bar baz", "set foo barbaz frotz"},
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
            },
            { "Simple",
              new String[] {"set foo bar baz", "set foo bar frotz"},
              new TestData[] {
                    new TestDataF(1),
                    new TestData(),
              },
              new TestData[] {
                    new TestDataF(),
                    new TestData(0),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
            },
            { "Substring before (should not overwrite)",
              new String[] {"set a b c", "set a bb c"},
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              }
            },
            { "Substring after (should not overwrite)",
              new String[] {"set a bb c", "set a b c"},
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              }
            },
            { "Simple set_cmp", // For now we're not trying to process set_cmp intelligently,
                                // so any set_cmp will trigger, even if maybe it wouldn't in-game.
              new String[] {"set foo bar baz", "level none set_cmp foo bar frotz nitfol"},
              new TestData[] {
                    new TestDataF(1),
                    new TestData(),
              },
              new TestData[] {
                    new TestDataF(),
                    new TestData(0),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
            },
            { "Double",
              new String[] {"set foo bar baz", "set foo bar frotz", "set foo bar nitfol"},
              new TestData[] {
                    new TestDataF(1, 2),
                    new TestData(2),
                    new TestData(),
              },
              new TestData[] {
                    new TestDataF(),
                    new TestData(0),
                    new TestData(0, 1),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
                  new TestData(),
              },
            },
            { "Simple Hotfix",
              new String[] {"set foo bar baz", "patch set foo bar frotz"},
              new TestData[] {
                  new TestDataF(1),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(0),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
            },
            { "Simple Patch",
              new String[] {"set foo bar baz", "patch set foo bar frotz"},
              new TestData[] {
                  new TestDataF(1),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(0),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
            },
            { "Simple Level (none)",
              new String[] {"set foo bar baz", "level none set foo bar frotz"},
              new TestData[] {
                  new TestDataF(1),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(0),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
            },
            { "Simple Level (exact)",
              new String[] {"set foo bar baz", "level mordor set foo bar frotz"},
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(1),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(0),
              },
            },
            { "Simple OnDemand (exact)",
              new String[] {"set foo bar baz", "ondemand maya set foo bar frotz"},
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(1),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(0),
              },
            },
            { "Two Hotfixes",
              new String[] {"patch set foo bar baz", "patch set foo bar baz"},
              new TestData[] {
                  new TestDataF(1),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(0),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
            },
            { "Hotfix Inbetween Set",
              new String[] {"set foo bar baz", "level none set foo bar nitfol", "set foo bar frotz"},
              new TestData[] {
                  new TestDataF(2, 1),
                  new TestData(),
                  new TestData(1),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(0, 2),
                  new TestData(0),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
            },
            { "Partial",
              new String[] {"set foo bar baz", "level none set foo bar.frotz baz"},
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(1),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(0),
              }
            },
            { "Partial Index",
              new String[] {"set foo bar baz", "level none set foo bar[0] baz"},
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(1),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(0),
              }
            },
            { "Nested Partials",
              new String[] {"level none set foo bar.baz frotz", "level none set foo bar.baz.nitfol filfre"},
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(1),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(0),
              }
            },
            { "Far Nested Partials",
              new String[] {"set foo bar frotz", "level none set foo bar.baz.nitfol filfre"},
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(1),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(0),
              }
            },
            { "Double Partials",
              new String[] {"set foo bar baz",
                  "level none set foo bar.frotz baz",
                  "level none set foo bar.nitfol baz"},
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(1, 2),
                  new TestData(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(0),
                  new TestData(0),
              }
            },
            { "Complex Partial",
              new String[] {"set foo bar[0] baz", "level none set foo bar[0].frotz baz"},
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(1),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(0),
              }
            },
            { "Different Levels",
              new String[] {"level mordor set foo bar baz", "level shire set foo bar baz"},
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              }
            },
            { "Different Levels (with initial set)",
              new String[] {"set foo bar baz", "level mordor set foo bar baz", "level shire set foo bar baz"},
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(1, 2),
                  new TestData(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(0),
                  new TestData(0),
              }
            },
            { "Different Levels (with initial hotfix)",
              new String[] {"level none set foo bar baz", "level mordor set foo bar baz", "level shire set foo bar baz"},
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(1, 2),
                  new TestData(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(0),
                  new TestData(0),
              }
            },
            { "Different OnDemand",
              new String[] {"ondemand maya set foo bar baz", "ondemand sal set foo bar baz"},
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              }
            },
            { "Different OnDemand (with initial set)",
              new String[] {"set foo bar baz", "ondemand maya set foo bar baz", "ondemand sal set foo bar baz"},
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(1, 2),
                  new TestData(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(0),
                  new TestData(0),
              }
            },
            { "Invalid Partial (multiple indicies)",
              new String[] {"set foo bar[0] baz", "set foo bar[0][0] baz"},
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(1),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(0),
              }
            },
            { "Full After Specific",
              new String[] {"level none set foo bar.baz frotz", "level none set foo bar frotz"},
              new TestData[] {
                  new TestDataF(1),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(0),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              }
            },
            { "Full After Invalid Specific",
              new String[] {"level none set foo bar[0]notadot frotz", "level none set foo bar frotz"},
              new TestData[] {
                  new TestDataF(1),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(0),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              }
            },
            { "Partial Overwrite in same level",
              new String[] {"level mordor set foo bar baz", "level mordor set foo bar.frotz filfre"},
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(1),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(0),
              }
            },
            { "Partial Index Overwrite in same level",
              new String[] {"level mordor set foo bar baz", "level mordor set foo bar[0] filfre"},
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(1),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(0),
              }
            },
            { "Partial Index Overwrite, different levels",
              new String[] {"level shire set foo bar baz", "level mordor set foo bar[0] filfre"},
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              }
            },
            { "Partial Index Overwrite, from non-level-specific",
              new String[] {"set foo bar baz", "level mordor set foo bar[0] filfre"},
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(1),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(0),
              }
            },
            { "TPS-specific add-to-array syntax (should be no overwrite)",
              new String[] {"level none set foo bar (baz)", "level none set foo bar +(baz)"},
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              }
            },
            { "Partial after TPS-specific add-to-array syntax (should be no overwrite?)",
              new String[] {"level none set foo bar +(baz)", "level none set foo bar[0] baz"},
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              }
            },
            { "Full after TPS-specific add-to-array syntax (expected test failure -- should full overwrite.  See bug #20)",
              new String[] {"level none set foo bar +(baz)", "level none set foo bar baz"},
              new TestData[] {
                  new TestDataF(1),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(0),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              },
              new TestData[] {
                  new TestDataF(),
                  new TestData(),
              }
            },
        };
    }

    /**
     * Test of getCompleteOverwriters method, of class OverwriteChecker.
     */
    @Test(dataProvider = "getOverwriteData")
    public void testGetCompleteOverwriters(String label, String[] commands,
            TestData[] completeOverwriterDataSet, TestData[] completeOverwrittenDataSet,
            TestData[] partialOverwriterDataSet, TestData[] partialOverwrittenDataSet) {
        //System.out.println("getCompleteOverwriters: " + label);
        testCategory(commands);
        for (TestData data : completeOverwriterDataSet) {
            List<SetCommand> expResult = data.getResultSet(storedCommands);
            List<SetCommand> result = OverwriteChecker.getCompleteOverwriters(storedCommands[data.test]);
            assertEquals(result, expResult);
        }
    }

    /**
     * Test of getCompleteOverwrittens method, of class OverwriteChecker.
     */
    @Test(dataProvider = "getOverwriteData")
    public void testGetCompleteOverwrittens(String label, String[] commands,
            TestData[] completeOverwriterDataSet, TestData[] completeOverwrittenDataSet,
            TestData[] partialOverwriterDataSet, TestData[] partialOverwrittenDataSet) {
        //System.out.println("getCompleteOverwrittens: " + label);
        testCategory(commands);
        for (TestData data : completeOverwrittenDataSet) {
            List<SetCommand> expResult = data.getResultSet(storedCommands);
            List<SetCommand> result = OverwriteChecker.getCompleteOverwrittens(storedCommands[data.test]);
            assertEquals(result, expResult);
        }
    }

    /**
     * Test of getPartialOverwriters method, of class OverwriteChecker.
     */
    @Test(dataProvider = "getOverwriteData")
    public void testGetPartialOverwriters(String label, String[] commands,
            TestData[] completeOverwriterDataSet, TestData[] completeOverwrittenDataSet,
            TestData[] partialOverwriterDataSet, TestData[] partialOverwrittenDataSet) {
        //System.out.println("getPartialOverwriters: " + label);
        testCategory(commands);
        for (TestData data : partialOverwriterDataSet) {
            List<SetCommand> expResult = data.getResultSet(storedCommands);
            List<SetCommand> result = OverwriteChecker.getPartialOverwriters(storedCommands[data.test]);
            assertEquals(result, expResult);
        }
    }

    /**
     * Test of getPartialOverwrittens method, of class OverwriteChecker.
     */
    @Test(dataProvider = "getOverwriteData")
    public void testGetPartialOverwrittens(String label, String[] commands,
            TestData[] completeOverwriterDataSet, TestData[] completeOverwrittenDataSet,
            TestData[] partialOverwriterDataSet, TestData[] partialOverwrittenDataSet) {
        //System.out.println("getPartialOverwrittens: " + label);
        testCategory(commands);
        for (TestData data : partialOverwrittenDataSet) {
            List<SetCommand> expResult = data.getResultSet(storedCommands);
            List<SetCommand> result = OverwriteChecker.getPartialOverwrittens(storedCommands[data.test]);
            assertEquals(result, expResult);
        }
    }

    /**
     * Test of getColor method, of class OverwriteChecker.
     */
    @Test(enabled=false)
    public void testGetColor() {
        System.out.println("getColor");
        ModelElement el = null;
        ThemeManager.ColorType expResult = null;
        ThemeManager.ColorType result = OverwriteChecker.getColor(el);
        assertEquals(result, expResult);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }


}
