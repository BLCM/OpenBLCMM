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
 * Additional permission under GNU GPL version 3 section 7:
 *
 * If you modify this Program, or any covered work, by linking or
 * combining it with the original proprietary BLCMM Launcher, BLCMM
 * Lib Distributor, BLCMM Utilities, or BLCMM Data Interaction Library
 * Jarfiles (or modified versions of those libraries), containing parts
 * covered by the terms of their proprietary license, the licensors of
 * this Program grant you additional permission to convey the resulting
 * work.
 *
 */
package blcmm.utilities;

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
 * Tests for BLCMM's code-formatting functionality.  This is not exhaustive,
 * and could use expansion in a number of ways, but it should be better than
 * nothing, at least.
 *
 * @author apocalyptech
 */
public class CodeFormatterNGTest {

    public CodeFormatterNGTest() throws Exception {
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
     * Data provider for our formatCode() test.  As per TestNG requirements,
     * it's an array of arrays, though the internal array is effectively a
     * tuple.  The "tuple" elements should be:
     *
     *  1) A string label, just used during test reporting so it's obvious what
     *     data's being tested.
     *  2) The input string (ostensibly from the user)
     *  3) The formatted return string
     *
     * @return
     */
    @DataProvider
    public Object[][] getFormatCodeData() {
        return new Object[][] {
            { "Simple Set",
                "set foo bar baz",
                "set foo bar baz",
            },
            { "Reduce Spaces",
                "set  foo  bar  baz",
                "set foo bar baz",
            },
            { "Newlines after each component",
                "set\nfoo\nbar\nbaz\n",
                "set foo bar baz",
            },
            { "set_cmp",
                "set_cmp foo bar old baz",
                "set_cmp foo bar old baz",
            },
            { "Set with 'set' in the value",
                "set foo bar baz set more",
                "set foo bar baz set more"
            },
            { "Real example of Set with 'set' in the value",
                "set Behavior_ConsoleCommand'GD_Soldier_Skills.Guerrilla.Able:BehaviorProviderDefinition_0.Behavior_ConsoleCommand_0' Command"
                    + " set GD_Soldier_Skills.Guerrilla.Grenadier SkillDescription This Skill [skill]Has been upgraded[-skill].",
                "set Behavior_ConsoleCommand'GD_Soldier_Skills.Guerrilla.Able:BehaviorProviderDefinition_0.Behavior_ConsoleCommand_0' Command"
                    + " set GD_Soldier_Skills.Guerrilla.Grenadier SkillDescription This Skill [skill]Has been upgraded[-skill]."
            },
            { "Two statements",
                "set foo bar baz\nset one two three",
                "set foo bar baz\n\nset one two three",
            },
            { "One multiline statment",
                "set\nfoo\nbar\nbaz",
                "set foo bar baz",
            },
            { "One fancier multiline statement",
                "set foo bar (\nattr=foo,\nattr2=bar\n)\n\n",
                "set foo bar\n(\n    attr = foo,\n    attr2 = bar\n)",
            },
            { "Multi-nested statement",
                "set foo bar (attr=foo,attr2=(3,4),attr3=(one=two))",
                "set foo bar\n"
                + "(\n"
                + "    attr = foo,\n"
                + "    attr2 = (3,4),\n"
                + "    attr3 =\n"
                + "    (\n"
                + "        one = two\n"
                + "    )\n"
                + ")"
            },
            { "Two statements, one fancy",
                "set foo bar (attr=foo,attr2=bar)\nset foo bar baz\n",
                "set foo bar\n(\n    attr = foo,\n    attr2 = bar\n)\n\nset foo bar baz",
            },
            { "All currently-valid commands",
                "set foo bar baz\n"
                + "set_cmp foo bar baz frotz\n"
                + "exec patch.txt\n"
                + "say Some text which could crash the game\n",
                // We expect it to put some extra space between statements
                "set foo bar baz\n\n"
                + "set_cmp foo bar baz frotz\n\n"
                + "exec patch.txt\n\n"
                + "say Some text which could crash the game",
            },
            { "Random text (possibly command extensions, etc)",
                // Honestly I sort of wish this one behaved slightly differently; it's
                // fine in the edit window because splitIntoParts happens first, though
                // it *will* concat like this if you hit "auto format"
                "blarg bloog blurg\nblip blam blop\n",
                "blarg bloog blurg blip blam blop",
            },
            { "Comments with whitespace",
                // Will cut out excess whitespace.  Possibly not the best, but the moral
                // is "don't hit 'auto-format' on comments"
                "# some  whitespace  in  comments",
                "# some whitespace in comments",
            }
        };
    }

    /**
     * Test of CodeFormatter.formatCode method.
     *
     * @param label Test label, just for reporting purposes
     * @param input The original statement input
     * @param expectedOutput The formatted code we expect to see
     */
    @Test(dataProvider = "getFormatCodeData")
    public void testFormatCode(String label,
            String input,
            String expectedOutput) {
        String realOutput = CodeFormatter.formatCode(input);
        assertEquals(realOutput, expectedOutput);
    }

    /**
     * Data provider for our splitIntoParts() test.  As per TestNG requirements,
     * it's an array of arrays, though the internal array is effectively a
     * tuple.  The "tuple" elements should be:
     *
     *  1) A string label, just used during test reporting so it's obvious what
     *     data's being tested.
     *  2) The input string (ostensibly from the user)
     *  3) The list of strings returned by splitting
     *
     * @return
     */
    @DataProvider
    public Object[][] getSplitIntoPartsData() {
        return new Object[][] {
            { "Simple Set",
                "set foo bar baz",
                Arrays.asList(
                        "set foo bar baz"
                ),
            },
            { "Set with 'set' in the value",
               "set foo bar baz set more",
               Arrays.asList(
                       "set foo bar baz set more"
               ),
            },
            { "Two sets",
                "set foo bar baz\nset one two three\n",
                Arrays.asList(
                        "set foo bar baz",
                        "set one two three"
                ),
            },
            { "One fancier multiline statement",
                "set foo bar (\nattr=foo,\nattr2=bar\n)\n\n",
                Arrays.asList(
                        "set foo bar (attr=foo,attr2=bar)"
                ),
            },
            { "One fancier multiline statement with extra spaces",
                "set foo bar\n (\n   attr=foo,\n    attr2=bar\n)\n\n",
                Arrays.asList(
                        "set foo bar (attr=foo,attr2=bar)"
                ),
            },
            { "Two statements, one fancy",
                "set foo bar (attr=foo,attr2=bar)\nset foo bar baz\n",
                Arrays.asList(
                        "set foo bar (attr=foo,attr2=bar)",
                        "set foo bar baz"
                ),
            },
            { "Multi-nested statement",
                "set foo bar\n"
                + "(\n"
                + "    attr = foo,\n"
                + "    attr2 = (3,4),\n"
                + "    attr3 =\n"
                + "    (\n"
                + "        one = two\n"
                + "    )\n"
                + ")",
                Arrays.asList(
                        "set foo bar (attr=foo,attr2=(3,4),attr3=(one=two))"
                ),
            },
            { "More nested statements",
                "set foo bar\n"
                + "(\n"
                + "    (\n"
                + "        SlotName = \"WeaponAccuracyImpulse\",\n"
                + "        GradeIncrease = -10,\n"
                + "        bActivateSlot = True\n"
                + "    ),\n"
                + "    (\n"
                + "        SlotName = \"AccuracyMax\",\n"
                + "        GradeIncrease = -10,\n"
                + "        bActivateSlot = True\n"
                + "    )\n"
                + ")",
                Arrays.asList(
                        "set foo bar ((SlotName=\"WeaponAccuracyImpulse\",GradeIncrease=-10,bActivateSlot=True),(SlotName=\"AccuracyMax\",GradeIncrease=-10,bActivateSlot=True))"
                ),
            },
            { "All currently-valid commands",
                "set foo bar baz\n"
                + "set_cmp foo bar baz frotz\n"
                + "exec patch.txt\n"
                + "say Some text which could crash the game\n",
                Arrays.asList(
                        "set foo bar baz",
                        "set_cmp foo bar baz frotz",
                        "exec patch.txt",
                        "say Some text which could crash the game"
                ),
            },
            { "All currently-valid commands with extra newlines",
                "set foo bar baz\n\n"
                + "set_cmp foo bar baz frotz\n\n"
                + "exec patch.txt\n\n"
                + "say Some text which could crash the game\n\n",
                Arrays.asList(
                        "set foo bar baz",
                        "set_cmp foo bar baz frotz",
                        "exec patch.txt",
                        "say Some text which could crash the game"
                ),
            },
            { "Random text (possibly command extensions, etc)",
                "blarg bloog blurg\nblip blam blop\n",
                Arrays.asList(
                        "blarg bloog blurg",
                        "blip blam blop"
                ),
            },
            { "Random text with extra newline",
                "blarg bloog blurg\n\nblip blam blop\n",
                Arrays.asList(
                        "blarg bloog blurg",
                        "",
                        "blip blam blop"
                ),
            },
            { "Random text with a set in front",
                // Perhaps not ideal, but them's the breaks.
                "set foo bar baz\nblarg bloog blurg\nblip blam blop\n",
                Arrays.asList(
                        "set foo bar baz blarg bloog blurg blip blam blop"
                ),
            },
            { "Known Command Extensions example -- two set_early",
                "set_early foo bar (\n    baz\n)\nset_early one two three",
                Arrays.asList(
                        "set_early foo bar (baz)",
                        "set_early one two three"
                ),
            },
            { "Known Command Extensions example -- more complex set_early",
                "set_early foo bar (\n"
                + "  (\n"
                + "    baz = foo\n"
                + "  ),\n"
                + "  (\n"
                + "    baz = bar\n"
                + "  )\n"
                + ")",
                Arrays.asList(
                        "set_early foo bar ((baz=foo),(baz=bar))"
                ),
            },
            { "Comment with a set",
                "# This is a comment\nset foo bar baz",
                Arrays.asList(
                        "# This is a comment",
                        "set foo bar baz"
                ),
            },
            { "Comment with a set and then another comment",
                "# This is a comment\nset foo bar baz\n# One more comment",
                Arrays.asList(
                        "# This is a comment",
                        "set foo bar baz",
                        "# One more comment"
                ),
            },
            { "Comment with whitespace",
                "# comments  with  some  whitespace",
                Arrays.asList(
                        "# comments  with  some  whitespace"
                ),
            },
            { "Proper whitespace deletion",
                "set foo bar ( ( baz = frotz , nitfol = zemdor ) )",
                Arrays.asList(
                        "set foo bar ((baz=frotz,nitfol=zemdor))"
                ),
            },
            { "Cautious whitespace deletion",
                // Make sure we're not getting rid of whitespace we shouldn't.
                // This statement would be unlikely to be valid, but we still
                // shouldn't assume that two whitespace-separated strings are
                // supposed to concat
                "set foo bar ( ( baz = frotz more , nitfol = zemdor yep ) )",
                Arrays.asList(
                        "set foo bar ((baz=frotz more,nitfol=zemdor yep))"
                ),
            },
            { "Cautious whitespace deletion (real example)",
                // This one basically shouldn't get rid of *any* whitespace
                // This was submitted as issue #42
                "set foo SkillDescription You deal increased [skill]Gun Damage[-skill] (actually: [skill]Bullet Damage[-skill] and [skill]Gun Splash[-skill]) to any enemy with more than 50% health remaining.",
                Arrays.asList(
                        "set foo SkillDescription You deal increased [skill]Gun Damage[-skill] (actually: [skill]Bullet Damage[-skill] and [skill]Gun Splash[-skill]) to any enemy with more than 50% health remaining."
                ),
            },
            { "Cautious whitespace deletion (real example with legit trims)",
                // This one does have a couple things which should be trimmed
                "set foo SkillDescription You deal increased [skill]Gun Damage[-skill] ( actually: [skill]Bullet Damage[-skill] and [skill]Gun Splash[-skill] ) to any enemy with more than 50% health remaining.",
                Arrays.asList(
                        "set foo SkillDescription You deal increased [skill]Gun Damage[-skill] (actually: [skill]Bullet Damage[-skill] and [skill]Gun Splash[-skill]) to any enemy with more than 50% health remaining."
                ),
            },
            { "No whitespace stripping in quotes",
                "set foo bar \"( frotz = nitfol )\"",
                Arrays.asList(
                        "set foo bar \"( frotz = nitfol )\""
                ),
            },
            { "No whitespace stripping in quotes -- two values",
                "set foo bar ( frotz = \" one , two \" , nitfol = \" ( three )\" )",
                Arrays.asList(
                        "set foo bar (frotz=\" one , two \",nitfol=\" ( three )\")"
                ),
            },
            { "No whitespace stripping in quotes -- dangling quote",
                "set foo bar \"( frotz = nitfol )",
                Arrays.asList(
                        "set foo bar \"( frotz = nitfol )"
                ),
            },
            { "No whitespace stripping in quotes -- two values, dangling quote",
                "set foo bar ( frotz = \" one , two \" , nitfol = \" ( three )",
                Arrays.asList(
                        "set foo bar (frotz=\" one , two \",nitfol=\" ( three )"
                ),
            },
        };
    }

    /**
     * Test of CodeFormatter.splitIntoParts method.
     *
     * @param label Test label, just for reporting purposes
     * @param input The original statement input
     * @param expectedOutput The formatted code we expect to see
     */
    @Test(dataProvider = "getSplitIntoPartsData")
    public void testSplitIntoParts(String label,
            String input,
            List<String> expectedOutput) {
        List<String> realOutput = CodeFormatter.splitIntoDeformattedParts(input);
        //System.out.println(expectedOutput.toString());
        //System.out.println(realOutput.toString());
        assertEquals(realOutput, expectedOutput);
    }

}
