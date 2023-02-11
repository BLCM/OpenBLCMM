/*
 * Copyright (C) 2018-2020  LightChaosman
 *
 * BLCMM is free software: you can redistribute it and/or modify
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
 * combining it with BLCMM Launcher, BLCMM Lib Distributor, BLCMM
 * Resources, or BLCMM Utilities (or modified versions of those
 * libraries), containing parts covered by the terms of their
 * proprietary license, the licensors of this Program grant you
 * additional permission to convey the resulting work.
 *
 */
package blcmm.model;

/**
 *
 * @author LightChaosman
 */
public class Comment extends ModelElement {

    private final String comment;

    public Comment(String comment) {
        assert !SetCommand.isValidCommand(comment);
        this.comment = comment;
        this.transientData = new TransientModelData(this);
    }

    @Override
    public Category getParent() {
        return (Category) super.getParent();
    }

    @Override
    protected String toXMLString() {
        return String.format("<comment>%s</comment>", comment);
    }

    @Override
    public String toString() {
        return comment;
    }

    @Override
    public Comment copy() {
        Comment copy = new Comment(comment);
        return copy;
    }

    public String getComment() {
        return comment;
    }

}
