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
 */
package blcmm.model;

import blcmm.utilities.Options;

/**
 *
 * @author LightChaosman
 */
public class SetCMPCommand extends HotfixCommand {

    private final String cmpValue;

    public SetCMPCommand(String object, String field, String cmpValue, String value) {
        this(new String[]{object, field, cmpValue, value});
    }

    public SetCMPCommand(String command) {
        this(split(command, 4));
        if (!(command.substring(0, 7).equalsIgnoreCase("set_cmp") && Character.isWhitespace(command.charAt(7)))) {
            throw new IllegalArgumentException(command);
        }
    }

    private SetCMPCommand(String[] args) {
        super(args[0], args[1], args[3], false);
        this.cmpValue = args[2];
        if (args.length != 4) {
            throw new IllegalArgumentException();
        }
        this.transientData = new TransientModelData(this);
    }

    @Override
    public String getCode() {
        return String.format("set_cmp %s %s %s %s", object, field, cmpValue, value);
    }

    public String getCmpValue() {
        return cmpValue;
    }

    @Override
    protected String getPrefix() {
        ModelElementContainer parent = getParent();
        if (parent instanceof HotfixWrapper) {
            return String.format("%s set_cmp", ((HotfixWrapper) parent).getPrefix());
        } else {
            return "set_cmp";
        }
    }

    @Override
    protected SetCMPCommand getBaseCopy() {
        return new SetCMPCommand(object, field, cmpValue, value);
    }

    @Override
    String[] getSplit() {
        String val;
        String cmpval;
        if (Options.INSTANCE.getTruncateCommands()
                && value.length() > Options.INSTANCE.getTruncateCommandLength()) {
            val = String.format("%s...",
                    value.substring(0, Options.INSTANCE.getTruncateCommandLength() - 3));
        } else {
            val = value;
        }
        if (Options.INSTANCE.getTruncateCommands()
                && cmpValue.length() > Options.INSTANCE.getTruncateCommandLength()) {
            cmpval = String.format("%s...",
                    cmpValue.substring(0, Options.INSTANCE.getTruncateCommandLength() - 3));
        } else {
            cmpval = cmpValue;
        }
        return new String[]{getPrefix(), object, field, cmpval, val};

    }

    /**
     * Returns the "raw" hotfix format actually used by Gearbox hotfixes.
     * @return The raw hotfix format
     */
    @Override
    public String toRawHotfixFormat() {
        return this.toRawHotfixFormat(this.cmpValue);
    }

}
