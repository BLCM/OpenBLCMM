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
package blcmm.model;

import blcmm.data.BehaviorProviderDefinition;
import blcmm.utilities.Options;
import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 * @author LightChaosman
 */
public class SetCommand extends EnableableModelElement {

    protected final String object, field, value;

    public SetCommand(String object, String field, String value) {
        this(new String[]{object, field, value}, true);
    }

    protected SetCommand(String object, String field, String value, boolean initTransient) {
        this(new String[]{object, field, value}, initTransient);
    }

    public SetCommand(String command) {
        this(split(command, 3), true);
        if (!(command.substring(0, 3).equalsIgnoreCase("set") && Character.isWhitespace(command.charAt(3)))) {
            throw new IllegalArgumentException(command);
        }
    }

    private SetCommand(String[] args, boolean initTransient) {
        this.object = args[0];
        this.field = args[1];
        this.value = args[2];
        if (initTransient) {
            this.transientData = new TransientModelData(this);
        }
        if (args.length != 3) {
            throw new IllegalArgumentException(Arrays.toString(args));
        }
        if (args[0].trim().isEmpty()) {
            throw new IllegalArgumentException(Arrays.toString(args));
        }
        if (args[1].trim().isEmpty()) {
            throw new IllegalArgumentException(Arrays.toString(args));
        }
    }

    /**
     * The object that this command modifies. Guaranteed to be non-null and
     * non-empty.
     *
     * @return The object that this command modifies
     */
    public String getObject() {
        return object;
    }

    /**
     * The field that this command modifies. Guaranteed to be non-null and
     * non-empty.
     *
     * @return The field that this command modifies
     */
    public String getField() {
        return field;
    }

    /**
     * The new value. Guaranteed to be non-null.
     *
     * @return The new value that this command assigns to the field of the
     * object.
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns the command in an executable representation
     *
     * @return an executable representation of the command
     */
    public String getCode() {
        return String.format("set %s %s %s", object, field, value);
    }

    @Override
    public String toString() {
        if (getParent() == null) {//This is called mid-way when dragging and dropping.
            //After a copy has been made, but before the parents have been set again.
            return getCode();
        }
        Category parentCategory = (Category) (getParent() instanceof Category ? getParent() : getParent().getParent());
        if (parentCategory == null) {
            // This can happen if you end up throwing some debugging statements
            // around a new Hotfix which has yet to be attached to the main
            // tree.
            return getCode();
        }
        StringBuilder sb = new StringBuilder();
        String[] split = getSplit();
        String[] specials = getSpecialToStringCases(split);
        for (int i = 0; i < split.length; i++) {
            sb.append(split[i]).append(specials[i]);
            for (int j = split[i].length() + specials[i].length(); j < parentCategory.getLongest()[i] + 1; j++) {
                sb.append(" ");
            }
        }

        //sb.append(this.getTransientData().summaryString());
        return sb.toString();
    }

    @Override
    protected String toXMLString() {
        return String.format("<code %s>%s</code>", getProfileString(), getCode());
    }

    public static boolean isValidCommand(String command) {
        try {
            validateCommand(command, false);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void validateCommand(String command, boolean allowEmptyCommand) {
        if (command == null) {
            throw new NullPointerException();
        }
        command = command.trim();
        String[] split = command.split("\\s+");
        if (split.length == 0) {
            throw new IllegalArgumentException("Empty string not valid");//This probably can't even happen, but just catch off an AIOOBE below
        }
        if (split[0].equalsIgnoreCase("set")) {
            if (split.length < (allowEmptyCommand ? 3 : 4)) {
                throw new IllegalArgumentException("A set command must have " + (allowEmptyCommand ? "2 arguments (object, field)" : "3 arguments (object, field, new value)"));
            }
        } else if (split[0].equalsIgnoreCase("set_cmp")) {
            if (split.length < 5) {
                throw new IllegalArgumentException("A set_cmp command must have 4 arguments (object, field, old value, new value)");
            }
        } else {
            throw new IllegalArgumentException("Command must start with 'set ' or 'set_cmp '");
        }
        if (split[1].isEmpty()) {
            throw new IllegalArgumentException("Must have an object");
        } else if (split[2].isEmpty()) {
            throw new IllegalArgumentException("Must have a field");
        }
    }

    @Override
    public SetCommand copy() {
        SetCommand copy = getBaseCopy();
        copy.copyProfilesAndSelectedFrom(this);
        copy.setParent(getParent());
        return copy;
    }

    protected SetCommand getBaseCopy() {
        return new SetCommand(object, field, value);
    }

    protected static String[] split(String command, int striveForLengthOf) {
        command = command.trim();
        ArrayList<String> res = new ArrayList<>();
        int lastspace = 0;
        while (!Character.isWhitespace(command.charAt(lastspace))) {
            lastspace++;
        }
        while (lastspace != command.length() - 1) {
            while (lastspace + 1 != command.length() - 1 && Character.isWhitespace(command.charAt(lastspace + 1))) {
                lastspace++;
            }
            int idx = lastspace + 1;
            char c;
            int depth = 0;
            boolean inquotes = false;
            boolean breakAfterThisChar = false;
            StringBuilder sb = new StringBuilder();
            while (true) {
                c = command.charAt(idx);
                switch (c) {
                    case '"':
                        inquotes = !inquotes;
                        break;
                    case '(':
                        depth += inquotes ? 0 : 1;
                        break;
                    case ')':
                        depth -= inquotes ? 0 : 1;
                        break;
                }
                if (Character.isWhitespace(c)) {
                    breakAfterThisChar = !inquotes && depth == 0;
                }
                if (idx == command.length() - 1) {
                    breakAfterThisChar = true;
                    sb.append(c);
                }
                if (breakAfterThisChar) {
                    lastspace = idx;
                    if (!sb.toString().trim().isEmpty()) {
                        res.add(sb.toString());
                    }
                    break;
                } else {
                    sb.append(c);
                    idx++;
                }
            }
        }
        if (res.size() > striveForLengthOf) {
            StringBuilder sb = new StringBuilder();
            for (int i = striveForLengthOf - 1; i < res.size(); i++) {
                sb.append(res.get(i));
                sb.append(i == res.size() - 1 ? "" : " ");
            }
            while (res.size() > striveForLengthOf - 1) {
                res.remove(res.size() - 1);
            }
            res.add(sb.toString());
        }
        while (res.size() < striveForLengthOf) {
            res.add("");
        }
        return res.toArray(new String[0]);
    }

    protected String getPrefix() {
        ModelElementContainer parent = getParent();
        if (parent instanceof HotfixWrapper) {
            return String.format("%s set", ((HotfixWrapper) parent).getPrefix());
        } else {
            return "set";
        }
    }

    String[] getSplit() {
        if (Options.INSTANCE.getTruncateCommands()
                && value.length() > Options.INSTANCE.getTruncateCommandLength()) {
            return new String[]{
                getPrefix(),
                object,
                field,
                String.format("%s...",
                value.substring(0, Options.INSTANCE.getTruncateCommandLength() - 3)),};
        } else {
            return new String[]{getPrefix(), object, field, value};
        }
    }

    private String[] getSpecialToStringCases(String[] split) {
        String[] res = new String[split.length];
        Arrays.fill(res, "");
        if (split[2].toLowerCase().endsWith("arrayindexandlength")) {
            for (int i = 3; i < split.length; i++) {//looping and other logic in case of set_cmp
                try {
                    int int32Value = Integer.parseInt(split[i]);
                    String toInsert = String.format(" {index = %s | length = %s}",
                            BehaviorProviderDefinition.getIndexFromArrayIndexAndLength(int32Value),
                            BehaviorProviderDefinition.getLengthFromArrayIndexAndLength(int32Value));
                    if (i < split.length - 1) {
                        toInsert += " ";
                    }
                    res[i] = toInsert;
                } catch (NumberFormatException e) {
                    //do nothing
                }
            }
        } else if (false) {
            //Other cases go here
        }
        return res;
    }
}
