/*
 * Copyright (C) 2018-2020  LightChaosman
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
package blcmm.model;

/**
 * Instead of putting a boolean inside the setCommand class, we use a subclass
 * with no added functionality. This has the benefit of compile-time error
 * detection.
 *
 * Marking set commands as hotfix themselves, rather than inferring that from
 * their parent, makes the transient data management significantly easier, since
 * we don't need to update when switching parents, and then keep that in mind
 * when we add the current element to a new container, usually as the very next
 * model change. Now we can simply update everything when a child array changes,
 * at which point we already have a guaranteed correct parent.
 *
 * @author LightChaosman
 */
public class HotfixCommand extends SetCommand {

    public HotfixCommand(String object, String field, String value) {
        super(object, field, value);
    }

    public HotfixCommand(String command) {
        super(command);
    }

    protected HotfixCommand(String object, String field, String value, boolean initTransient) {
        super(object, field, value, initTransient);
    }

    public HotfixCommand(SetCommand command) {
        super(command.getCode());
    }

    @Override
    protected HotfixCommand getBaseCopy() {
        return new HotfixCommand(object, field, value);
    }

    @Override
    void setParent(ModelElementContainer newParent) {
        if (newParent != null && !(newParent instanceof HotfixWrapper)) {
            throw new IllegalArgumentException();
        }
        super.setParent(newParent);
    }

    @Override
    public HotfixWrapper getParent() {
        return (HotfixWrapper) super.getParent();
    }

    /**
     * Returns the "raw" hotfix format actually used by Gearbox hotfixes.
     * @return The raw hotfix format
     */
    public String toRawHotfixFormat() {
        return this.toRawHotfixFormat("");
    }

    /**
     * Returns the "raw" hotfix format actually used by Gearbox hotfixes,
     * with support for a previous value (used by our `set_cmp` syntax).
     *
     * @param previous The previous value, for set_cmp hotfixes
     * @return The raw hotfix format
     */
    public String toRawHotfixFormat(String previous) {
        HotfixWrapper wrapper = (HotfixWrapper)this.getParent();
        switch (wrapper.getType()) {
            case PATCH:
                return this.object
                    + "," + this.field
                    + "," + previous
                    + "," + this.value;
                
            default:
                return wrapper.getRawParameter()
                    + "," + this.object
                    + "," + this.field
                    + "," + previous
                    + "," + this.value;
        }
    }

}
