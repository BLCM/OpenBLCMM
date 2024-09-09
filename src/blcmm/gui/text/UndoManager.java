/*
 * Copyright (C) 2018-2020  LightChaosman
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
package blcmm.gui.text;

import javax.swing.event.UndoableEditEvent;
import javax.swing.undo.CompoundEdit;

/**
 * Third-gen UndoManager! Groups edits together by time in a CompoundEdit, which
 * will be used to perform the actual undo/redo operations. The time (in
 * milliseconds) between groupings can be altered via the `editGroupingMillis`
 * variable below.
 *
 * @author LightChaosman
 */
public class UndoManager extends javax.swing.undo.UndoManager {

    /**
     * If edits come in within this amount of time, they'll be grouped together.
     */
    final long editGroupingMillis = 200;

    /**
     * Our current CompoundEdit
     */
    private CompoundEdit compoundEdit;

    /**
     * Whether or not we're actually processing undo requests. This is to
     * support setting up the initial value of our text areas without polluting
     * the undo history.
     */
    private boolean processUndo;

    /**
     * The last edit time.
     */
    long lastEditTime;

    public UndoManager() {
        compoundEdit = new CompoundEdit();
        processUndo = false;
        resetLastEditTime();
    }

    /**
     * Tells us whether to register undoable edits or not.
     *
     * @param processUndo True to register undoable edits, false otherwise.
     */
    public void setProcessUndo(boolean processUndo) {
        this.processUndo = processUndo;
    }

    /**
     * Resets the last time that we saw an edit. This can be called to ensure
     * that the next edit triggers a new CompoundEdit regardless of time.
     */
    private void resetLastEditTime() {
        lastEditTime = System.currentTimeMillis() - (editGroupingMillis * 10);
    }

    /**
     * Discards all edits.
     */
    @Override
    public void discardAllEdits() {
        finalizeUndo();
        super.discardAllEdits();
        compoundEdit = new CompoundEdit();
    }

    /**
     * Called when we receive a new UndoableEditEvent. Will not do anything
     * unless `processUndo` is True.
     *
     * @param uee The event which can be undone
     */
    @Override
    public void undoableEditHappened(UndoableEditEvent uee) {

        // If we've not been told to start processing undos, ignore.
        if (!processUndo) {
            return;
        }

        // If this edit is coming in after a sufficient pause, we should close
        // out any existing compoundEdit and start a new one
        long curEditTime = System.currentTimeMillis();
        if (curEditTime > (lastEditTime + editGroupingMillis)) {
            this.finalizeUndo();
            compoundEdit = new CompoundEdit();
            this.addEdit(compoundEdit);
        }

        // Always add this edit to whatever our current compoundEdit is, and
        // reset our last edit time.
        compoundEdit.addEdit(uee.getEdit());
        lastEditTime = curEditTime;
    }

    /**
     * If we have an in-progress CompoundEdit, finalize it so that it can be
     * undone or whatever.
     */
    public void finalizeUndo() {
        resetLastEditTime();
        if (compoundEdit.isInProgress()) {
            compoundEdit.end();
        }
    }

}
