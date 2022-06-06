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
 */
package blcmm.model;

import java.util.Collection;
import java.util.TreeMap;

/**
 *
 * @author LightChaosman
 */
public class HotfixWrapper extends ModelElementContainer<HotfixCommand> {

    private HotfixType type;
    private String name;
    private String parameter;

    /**
     * TreeMap to provide some cleaner-looking names to display in the tree view
     * for OnDemand hotfixes. TreeMap is used rather than HashMap so that the
     * keys can be case-insensitive.
     */
    private static final TreeMap<String, String> ON_DEMAND_MAPPING = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    static {
        ON_DEMAND_MAPPING.put("GD_Soldier_Streaming", "Axton");
        ON_DEMAND_MAPPING.put("GD_Tulip_Mechro_Streaming", "Gaige");
        ON_DEMAND_MAPPING.put("GD_Lilac_Psycho_Streaming", "Krieg");
        ON_DEMAND_MAPPING.put("GD_Siren_Streaming", "Maya");
        ON_DEMAND_MAPPING.put("GD_Mercenary_Streaming", "Salvador");
        ON_DEMAND_MAPPING.put("GD_Assassin_Streaming", "Zer0");
        ON_DEMAND_MAPPING.put("GD_Gladiator_Streaming", "Athena");
        ON_DEMAND_MAPPING.put("Crocus_Baroness_Streaming", "Aurelia");
        ON_DEMAND_MAPPING.put("GD_Prototype_Streaming", "Claptrap");
        ON_DEMAND_MAPPING.put("Quince_Doppel_Streaming", "Jack");
        ON_DEMAND_MAPPING.put("GD_Lawbringer_Streaming", "Nisha");
        ON_DEMAND_MAPPING.put("GD_Enforcer_Streaming", "Wilhelm");
    }

    public HotfixWrapper(String name, HotfixType type, String parameter) {
        this.name = name;
        this.type = type;
        this.parameter = parameter;
        this.transientData = new TransientModelData(this);
    }

    /**
     *
     * @param name the name of the hotfixes
     * @param type the type of the hotfixes
     * @param parameter the parameter of the hotfixes
     * @param commandsToHotfix A collection of set commands, as strings that
     * will be put as children of this wrapper.
     */
    public HotfixWrapper(String name, HotfixType type, String parameter, Collection<String> commandsToHotfix) {
        this(name, type, parameter);
        for (String s : commandsToHotfix) {
            HotfixCommand c = s.startsWith("set_cmp ") ? new SetCMPCommand(s) : new HotfixCommand(s);
            c.setParent(this);
            this.addElement(c);
        }
    }

    /**
     *
     * @param name the name of the hotfixes
     * @param type the type of the hotfixes
     * @param parameter the parameter of the hotfixes
     * @param commandsToHotfix A collection of hotfixes, as HotfixCommands that
     * will be put as children of this wrapper.
     * @param dummy A dummy bool to trick the pesky pesky compiler.
     */
    public HotfixWrapper(String name, HotfixType type, String parameter, Collection<HotfixCommand> commandsToHotfix, boolean dummy) {
        this(name, type, parameter);
        for (HotfixCommand c : commandsToHotfix) {
            c.setParent(this);
            this.addElement(c);
        }
    }

    @Override
    void updateLengths(SetCommand c) {
        if (getParent() != null) {
            getParent().updateLengths(c);
        }
    }

    @Override
    public HotfixWrapper copy() {
        HotfixWrapper copy = new HotfixWrapper(name, type, parameter);
        for (HotfixCommand c : getElements()) {
            HotfixCommand c2 = (HotfixCommand) c.copy();
            c2.setParent(copy);
            copy.addElement(c2);
        }
        return copy;
    }

    @Override
    void addElement(HotfixCommand c, int i) {
        super.addElement(c, i);
    }

    public String getParameter() {
        return parameter;
    }

    public String getName() {
        return name;
    }

    public HotfixType getType() {
        return type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    public void setType(HotfixType type) {
        this.type = type;
    }

    /**
     * Returns a textual prefix which can be used in the main tree, to readily
     * display what kind of hotfix the command is.
     *
     * @return A string prefix
     */
    public String getPrefix() {
        switch (type) {
            case PATCH:
                return "(hotfix)";
            case LEVEL:
                if (parameter.equalsIgnoreCase("None")) {
                    return "(in any level)";
                } else {
                    if (parameter.endsWith("_p") || parameter.endsWith("_P")) {
                        return String.format("(in %s)", parameter.substring(0, parameter.length() - 2));
                    } else {
                        return String.format("(in %s)", parameter);
                    }
                }
            default:
                if (parameter.equalsIgnoreCase("None")) {
                    return "(with any class)";
                } else {
                    if (HotfixWrapper.ON_DEMAND_MAPPING.containsKey(parameter)) {
                        return String.format("(with %s)", ON_DEMAND_MAPPING.get(parameter));
                    } else {
                        return String.format("(with %s)", parameter);
                    }
                }
        }
    }

    @Override
    String getXMLStringPrefix() {
        switch (type) {
            case PATCH:
                return String.format("<hotfix name=\"%s\">", name);
            case LEVEL:
                return String.format("<hotfix name=\"%s\" level=\"%s\">", name, parameter);
            default:
                return String.format("<hotfix name=\"%s\" package=\"%s\">", name, parameter);
        }
    }

    @Override
    String getXMLStringPostfix() {
        return "</hotfix>";
    }

    @Override
    public String toString() {
        return getXMLStringPrefix() + " " + transientData.summaryString();
    }

}
