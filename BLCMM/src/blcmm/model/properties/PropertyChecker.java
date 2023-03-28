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
package blcmm.model.properties;

import blcmm.model.ModelElement;

/**
 *
 * @author LightChaosman
 */
public abstract class PropertyChecker {

    private final boolean propagatingToAncestors;
    private final boolean dependantOnChildren;

    /**
     * For generating tooltips, we want to know what "type" of description is
     * being given, to know what do report on containers.
     */
    public enum DescType {
        SyntaxError,
        ContentError,
        Warning,
        Informational,
        Invisible
    }

    public PropertyChecker(boolean propagatingToAncestors, boolean dependantOnChildren) {
        this.propagatingToAncestors = propagatingToAncestors;
        this.dependantOnChildren = dependantOnChildren;
    }

    public abstract boolean checkProperty(ModelElement element);

    /**
     * Some checkers might have a better performance when given pre-computed
     * hints, like lower-case versions of the element. These can override this
     * method.
     *
     * @param element
     * @param hints
     * @return
     */
    public boolean checkProperty(ModelElement element, Hints hints) {
        return checkProperty(element);//Default implementation
    }

    public abstract DescType getPropertyDescriptionType();

    public abstract String getPropertyDescription();

    public boolean isDependantOnChildren() {
        return dependantOnChildren;
    }

    public boolean isPropagatingToAncestors() {
        return propagatingToAncestors;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    public static class Hints {

        String lowerCaseObject;
        String lowerCaseField;
        String lowerCaseValue;

        public Hints() {
        }

        public Hints(String lowerCaseObject, String lowerCaseField, String lowerCaseValue) {
            this.lowerCaseObject = lowerCaseObject;
            this.lowerCaseField = lowerCaseField;
            this.lowerCaseValue = lowerCaseValue;
        }

    }

}
