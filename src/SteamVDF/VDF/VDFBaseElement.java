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

import java.util.HashMap;
import java.util.Map;

public class VDFBaseElement {

	private Map<String, VDFElement> MapParent = new HashMap<>();
	private Map<String, String> MapKey = new HashMap<>();

	public void addKey(String Name, boolean Value) {
		this.addKey(Name, (Value) ? "1" : "0");
	}

	public void addKey(String Name, int Value) {
		this.addKey(Name, String.valueOf(Value));
	}

	public void addKey(String Name, long Value) {
		this.addKey(Name, String.valueOf(Value));
	}

	public void addKey(String Name, String Value) {
		this.MapKey.put(Name, Value);
	}

	public VDFKey getKey(String Name) {
		return new VDFKey(this.MapKey.get(Name));
	}

        /**
         * Returns the number of keys in the element (ie: direct values).
         * Added 2023-02-13 for OpenBLCMM by apocalyptech
         * @return the number of keys available in the element
         */
        public int numKeys() {
                return this.MapKey.size();
        }

	public String[] getKeys() {
		String[] Keys = new String[MapKey.size()];

		int i = 0;
		for (String Key: this.MapKey.keySet()) {
			Keys[i] = Key;
			i++;
		}

		return Keys;
	}

	public VDFElement addParent(String Name) {
		return this.addParent(Name, null);
	}

	public VDFElement addParent(String Name, VDFElement Parent) {
		VDFElement Element = new VDFElement(Name, Parent);
		this.MapParent.put(Name, Element);

		return Element;
	}

	public VDFElement getParent(String Name) {
		return this.MapParent.get(Name);
	}

        /**
         * Returns the number of "parents" in the Element (ie: nested elements).
         * Added 2023-02-13 for OpenBLCMM purposes by apocalyptech.
         * @return the number of parent entries in the Element
         */
        public int numParents() {
                return this.MapParent.size();
        }

	public VDFElement[] getParents() {
		VDFElement[] Parents = new VDFElement[MapParent.size()];

		int i = 0;
		for (String Key: this.MapParent.keySet()) {
			Parents[i] = this.MapParent.get(Key);
			i++;
		}
		return Parents;
	}

	private static String generateTabs(int number) {
		String Tabs = "";
		for (int t = 0; t < number; t++) {
			Tabs = Tabs + "\t";
		}
		return Tabs;
	}

	private static String keysToString(VDFBaseElement Element, int tabNumber) {
		String OutputKeys = "";
		for (int i = 0; i < Element.getKeys().length; i++) {
			String Key = Element.getKeys()[i];
			OutputKeys = OutputKeys + generateTabs(tabNumber) + "\"" + Key + "\"\t\t\"" + Element.getKey(Key) + "\"\r\n";
		}
		return OutputKeys;
	}

	private static String parentsToString(VDFBaseElement Element, int tabNumber) {
		String OutputParents = "";
		for (int i = 0; i < Element.getParents().length; i++) {
			VDFElement Parent = Element.getParents()[i];
			OutputParents = OutputParents + generateTabs(tabNumber) + "\"" + Parent.getName() + "\"\r\n" + generateTabs(tabNumber) + "{" + generateTabs(tabNumber) + "\r\n" + Parent.toString(tabNumber+1) + generateTabs(tabNumber) + "}\r\n";
		}
		return OutputParents;
	}

	protected String toString(int tabNumber) {
		String OutputVDF = keysToString(this, tabNumber);
		OutputVDF = OutputVDF + parentsToString(this, tabNumber);
		return OutputVDF;
	}

	@Override
	public String toString() {
		return this.toString(0);
	}
}