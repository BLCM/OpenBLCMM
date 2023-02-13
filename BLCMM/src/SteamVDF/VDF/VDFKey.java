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

public class VDFKey {
	
	private String Value;
	
	public VDFKey(String Value) {
		this.Value = Value;
	}
	
	@Override
	public String toString() {
		return this.Value;
	}
	
	public boolean toBoolean() { 
		return (this.Value == null || this.Value.equals("0")) ? false : true;
	}
	
	public int toInt() {
		return Integer.parseInt(this.Value);
	}
	
	public long toLong() {
		return Long.parseLong(this.Value);
	}
}