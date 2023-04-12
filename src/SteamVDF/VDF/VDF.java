/*
 * Taken from StidOfficial's "SteamVDF" project and adapated a bit for our own
 * use.  https://github.com/StidOfficial/SteamVDF
 *
 * SteamVDF is distributed under the GPLv3:
 *
 * SteamVDF is free software: you can redistribute it and/or modify
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
 */

package SteamVDF.VDF;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class VDF extends VDFBaseElement {

	private static File VDFFile;

	public VDF() {
		super();
	}

	public VDF(String file) throws IOException {
		this(new File(file));
	}

	public VDF(File file) throws IOException {
		this(fileToLine(file));
	}

	public VDF(String[] VDFLine) {
		String ElementName = null;
		VDFElement Element = null;

		for (int i = 0; i < VDFLine.length; i++) {

                    String[] Line = VDFLine[i].trim().split("\t\t");
                    // Added 2023-02-13 for OpenBLCMM by Apocalyptech -- some older libraryfolder VDFs,
                    // at least, seem to use a different separator.  This should let the library
                    // support either.
                    if (Line.length == 1) {
                        Line = VDFLine[i].trim().split(" \t");
                    }

		    if (Line[0].startsWith("\"") && Line[0].endsWith("\"")) {
		    	if (Line.length == 2) {
		    		if (Element == null)
		    			this.addKey(removeQuote(Line[0]), removeQuote(Line[1]));
		    		else
		    			Element.addKey(removeQuote(Line[0]), removeQuote(Line[1]));
		    	} else if(Line.length == 1) {
		    		ElementName = removeQuote(Line[0]);
		    	}
		    } else if (Line[0].contains("{")) {
		    	if (Element == null) {
		    		Element = this.addParent(ElementName, null);
		    	} else {
		    		Element = Element.addParent(ElementName, Element);
		    	}
		    } else if (Line[0].contains("}")) {
		    	if (Element.getBase() == null)
		    		Element = null;
		    	else
		    		Element = Element.getBase();
		    }
		}
	}

	private static String[] fileToLine(File file) throws IOException {
		VDFFile = file;

		ArrayList<String> BufferLine = new ArrayList<>();
		BufferedReader BufferReader = new BufferedReader(new FileReader(file));
		String LineRead = null;
		while ((LineRead = BufferReader.readLine()) != null) {
			BufferLine.add(LineRead);
		}
		BufferReader.close();

		String[] VDFLine = new String[BufferLine.size()];

		return BufferLine.toArray(VDFLine);
	}

	private static String removeQuote(String str) {
		return str.replace("\"", "");
	}

	public File getFile() {
		return VDFFile;
	}

	public void Save() throws IOException {
		this.Save(VDFFile);
	}

	public void Save(String file) throws IOException {
		this.Save(new File(file));
	}

	public void Save(File file) throws IOException {
		BufferedWriter BufferWriter = new BufferedWriter(new FileWriter(file));
		BufferWriter.write(this.toString());
		BufferWriter.close();
	}
}