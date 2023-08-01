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
package blcmm.gui.tree;

import blcmm.gui.FontInfo;
import blcmm.gui.MainGUI;
import blcmm.gui.theme.Theme;
import blcmm.gui.theme.ThemeManager;
import blcmm.model.Category;
import blcmm.model.Comment;
import blcmm.model.ModelElement;
import blcmm.model.ModelElementContainer;
import blcmm.model.SetCommand;
import blcmm.model.TransientModelData;
import blcmm.model.properties.GlobalListOfProperties;
import blcmm.model.properties.PropertyChecker;
import blcmm.utilities.Options;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTree;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;

/**
 *
 * @author LightChaosman
 */
@SuppressWarnings("serial")
class CheckBoxTreeCellRenderer extends DefaultTreeCellRenderer {

    private final ImageIcon LOCK_ICON;
    private final ImageIcon SPEAKER_ICON;
    private final ImageIcon EXEC_ICON;
    private final HashMap<Icon, HashMap<Theme, ImageIcon>> iconMap = new HashMap();

    CheckBoxTreeCellRenderer(FontInfo fontInfo) {
        setFont(new Font(MainGUI.CODE_FONT_NAME, fontInfo.getFont().getSize(), Font.PLAIN));
        LOCK_ICON = new ImageIcon(getClass().getClassLoader().getResource("resources/padlock.png"));
        SPEAKER_ICON = new ImageIcon(getClass().getClassLoader().getResource("resources/speaker.png"));
        EXEC_ICON = new ImageIcon(getClass().getClassLoader().getResource("resources/exec.png"));
        iconMap.put(openIcon, new HashMap<>());
        iconMap.put(closedIcon, new HashMap<>());

    }
    private DefaultMutableTreeNode currentNode;

    @Override
    public Component getTreeCellRendererComponent(JTree tree2, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        CheckBoxTree tree = (CheckBoxTree) tree2;
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        TreePath tp = new TreePath(node.getPath());
        currentNode = (((DefaultMutableTreeNode) tp.getLastPathComponent()));
        CheckBoxTree.CheckedNode cn = tree.getCheckedNode(tp);
        Component c = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        if (currentNode.getUserObject() instanceof String) {
            return c;
        }
        if (cn == null) {
            cn = new CheckBoxTree.CheckedNode(true, false, true);
        }
        ModelElement modelElement = (ModelElement) currentNode.getUserObject();
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new GridBagLayout());
        GridBagConstraints constr = new GridBagConstraints();
        constr.gridx = constr.gridy = 0;
        constr.insets = new Insets(0, 0, 0, 0);
        constr.gridx = 1;
        constr.anchor = GridBagConstraints.EAST;
        Component UI = determineUIComponent(modelElement, cn);
        if (modelElement instanceof SetCommand) {
            JLabel l = new JLabel(leafIcon);
            UI.setPreferredSize(new Dimension(UI.getPreferredSize().width + l.getPreferredSize().width + 2, UI.getPreferredSize().height));
        }
        if (UI != null) {
            panel.add(UI, constr);
        }
        Color color = ColorGiver.getColor(modelElement);
        JLabel label = new JLabel(value.toString());
        setText(value.toString() + "abcd");
        if (color != null && modelElement instanceof Category) {
            setFont(getFont().deriveFont(Font.BOLD));
        } else if (modelElement.getTransientData().getOverwriteState() == TransientModelData.OverwriteState.Overwritten) {
            setFont(getFont().deriveFont(Font.ITALIC));
        } else {
            setFont(getFont().deriveFont(Font.PLAIN));
        }
        label.setFont(getFont());
        if (!tree.isEnabled()) {
            color = ThemeManager.getColor(ThemeManager.ColorType.UINimbusDisabledText);
        }
        if (color != null) {
            label.setForeground(color);
        }
        decideIcon(modelElement, label, expanded);
        constr.gridx = 2;
        constr.insets.left = 2;
        panel.add(label, constr);

        JPanel padding = new JPanel();
        padding.setOpaque(false);
        constr.gridx = 3;
        constr.weightx = 10000;
        panel.add(padding, constr);

        createTooltip(modelElement, panel);

        //Set the size of the UI element to be at least the width of the tree
        Dimension d = panel.getPreferredSize();
        int childIndent = ((BasicTreeUI) tree.getUI()).getLeftChildIndent() + ((BasicTreeUI) tree.getUI()).getRightChildIndent();
        int depth = tp.getPathCount();
        panel.setPreferredSize(new Dimension(Math.max(d.width, tree.getWidth() - childIndent * depth), d.height));

        return panel;
    }

    private Component determineUIComponent(ModelElement modelElement, CheckBoxTree.CheckedNode cn) {
        Component UI = null;
        if (modelElement.getParent() instanceof Category && ((Category) modelElement.getParent()).isMutuallyExclusive() && modelElement instanceof Category) {
            UI = new JRadioButton("", cn.isSelected);
        } else if (((modelElement instanceof Category) && ((Category) modelElement).getNumberOfCommandsDescendants() == 0)) {
            JLabel l = new JLabel(leafIcon);
            l.setPreferredSize(new TristateCheckBox().getPreferredSize());
            UI = l;
            JPanel p = new JPanel();
            p.setPreferredSize(new TristateCheckBox().getPreferredSize());
            p.setOpaque(false);
            UI = p;
        } else if (true
                && !(modelElement instanceof Comment)
                && true) {
            TristateCheckBox.State s;
            if (cn.isSelected && cn.hasChildren && !cn.allChildrenSelected) {
                s = TristateCheckBox.PARTIALLY_SELECTED;
            } else if (cn.isSelected) {
                s = TristateCheckBox.SELECTED;
            } else {
                s = TristateCheckBox.NOT_SELECTED;
            }
            UI = new TristateCheckBox("", s);
        }
        return UI;
    }

    private void createTooltip(ModelElement el, JPanel panel) {
        // Tooltip processing
        StringBuilder sb = new StringBuilder();
        TransientModelData transientData = el.getTransientData();
        if (el instanceof ModelElementContainer) {
            boolean hasErrors = false;
            ArrayList<String> infoMessages = new ArrayList<>();
            for (PropertyChecker property : transientData.getProperties()) {
                PropertyChecker.DescType descType = property.getPropertyDescriptionType();
                switch (descType) {
                    case ContentError:
                    case Warning:
                    case SyntaxError:
                        // Special case for empty-category check
                        if (Options.INSTANCE.isInDeveloperMode()) {
                            if (property instanceof GlobalListOfProperties.EmptyCategoryChecker) {
                                if (property.checkProperty(el)) {
                                    infoMessages.add(property.getPropertyDescription());
                                    break;
                                }
                            }
                        }
                        hasErrors = true;
                        break;
                    case Informational:
                        infoMessages.add(property.getPropertyDescription());
                        break;
                    case Invisible:
                        break;//handle this case explicitly, for completeness
                }
            }

            // Some special-case processing for overwrite status
            if (transientData.getOverwriteState() == TransientModelData.OverwriteState.Overwriter) {
                infoMessages.add("Contains statements which overwrite others");
            } else if (transientData.getOverwriteState() == TransientModelData.OverwriteState.Overwritten) {
                infoMessages.add("Contains statements which are overwritten later");
            }

            // Show a maximum of three status messages in the tooltip.  Error
            // notifications will count as two, since it's wordier.
            int max_messages = 3;

            // Do we have errors?
            if (hasErrors) {
                max_messages -= 2;
                sb.append("This category contains errors. ");
                sb.append("Click through to the actual statements for details. ");
            }

            // Now any informational messages
            for (String s : infoMessages) {
                sb.append(String.format("%s. ", s));
                max_messages--;
                if (max_messages <= 0) {
                    break;
                }
            }

        } else {
            for (PropertyChecker property : transientData.getProperties()) {
                if (Options.INSTANCE.isInDeveloperMode()
                        || property.getPropertyDescriptionType() == PropertyChecker.DescType.Informational) {
                    String s = property.getPropertyDescription();
                    if (s != null && !s.isEmpty()) {
                        sb.append(sb.length() > 0 ? ", " : "").append(s);
                    }
                }
            }

            // Some special-case processing for overwrite status
            switch (transientData.getOverwriteState()) {

                case Overwriter:
                    sb.append(String.format("%sThis statement overwrites an earlier statement",
                            sb.length() > 0 ? ", " : ""));
                    break;

                case PartialOverwriter:
                    sb.append(String.format("%sThis statement partially overwrites an earlier statement",
                            sb.length() > 0 ? ", " : ""));
                    break;

                case Overwritten:
                    sb.append(String.format("%sThis statement is overwritten by a later statement",
                            sb.length() > 0 ? ", " : ""));
                    break;

                case PartialOverwritten:
                    sb.append(String.format("%sThis statement is partially overwritten by a later statement",
                            sb.length() > 0 ? ", " : ""));
                    break;

            }

        }
        if (sb.length() > 0) {
            panel.setToolTipText("<html>" + sb.toString());
        }
    }

    private void decideIcon(ModelElement element, JLabel label, boolean expanded) {
        if (element instanceof Comment) {
            TransientModelData transientData = element.getTransientData();
            if (transientData.getNumberOfOccurences(GlobalListOfProperties.CommentChecker.Say.class) > 0) {
                label.setIcon(SPEAKER_ICON);
            } else if (transientData.getNumberOfOccurences(GlobalListOfProperties.CommentChecker.Exec.class) > 0) {
                label.setIcon(EXEC_ICON);
            } else {
                label.setIcon(getLeafIcon());
            }
        } else if (element instanceof Category) {
            if (((Category) element).size() > 0) {
                label.setIcon(expanded ? getOpenIcon() : getClosedIcon());
            } else {
                label.setIcon(getClosedIcon());
            }
        }
        label.setIcon(getCustomIcon(label.getIcon()));
        if (element.hasLockedAncestor()) {
            label.setIcon(LOCK_ICON);
        }
    }

    private Icon getCustomIcon(Icon ico) {
        HashMap<Theme, ImageIcon> get = iconMap.get(ico);
        if (get == null) {
            return ico;
        }
        Theme t = ThemeManager.getTheme();
        if (t == ThemeManager.getTheme("dark")) {
            iconMap.get(openIcon).put(t, applyMask(openIcon, Color.WHITE, 0.075f));
            iconMap.get(closedIcon).put(t, applyMask(closedIcon, Color.WHITE, 0.075f));
        }
        ImageIcon get1 = get.get(t);
        if (get1 == null) {
            return ico;
        }
        return get1;
    }

    private static ImageIcon applyMask(Icon ico, Color mask, float f) {
        int w = ico.getIconWidth(), h = ico.getIconHeight();
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(mask);
        g.fill(new Rectangle2D.Double(0, 0, w, h));

        // Only pull the alpha channel from the icon
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.DST_IN));
        ico.paintIcon(null, g, 0, 0);
        g.setColor(new Color(0, 0, 0, 0));
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1 - f));
        ico.paintIcon(null, g, 0, 0);
        return new ImageIcon(image);
    }
}
