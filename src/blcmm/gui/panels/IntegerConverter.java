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
package blcmm.gui.panels;

import blcmm.data.BehaviorProviderDefinition;
import blcmm.gui.FontInfo;
import blcmm.gui.components.EnhancedFormattedTextField;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.function.BiFunction;
import java.util.function.Function;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

/**
 * Integer converter, for use in the BPD Number Processing dialog.
 *
 * Note that we're passing in a Font to use as our base font -- I was unable
 * to find a reliable way of propagating a default font after the user has
 * changed the font size in the app, and eventually decided to just use a
 * sledgehammer instead.
 *
 * @author LightChaosman
 */
public class IntegerConverter extends javax.swing.JPanel {

    private FontInfo fontInfo;

    /**
     * Creates new form IntegerConverter
     *
     * @param fontInfo Font information for the dialog to use
     */
    public IntegerConverter(FontInfo fontInfo) {
        this.fontInfo = fontInfo;
        initComponents();
        Function<String, Object[]> forw1 = (String s) -> {
            int val = Integer.parseInt(s);
            return new Integer[]{
                BehaviorProviderDefinition.getIndexFromArrayIndexAndLength(val),
                BehaviorProviderDefinition.getLengthFromArrayIndexAndLength(val)
            };
        };
        BiFunction<String, String, Object> backw1 = (String s1, String s2)
                -> BehaviorProviderDefinition.getArrayIndexAndLength(Integer.parseInt(s1.trim()), Integer.parseInt(s2.trim()));

        Function<String, Object[]> forw2 = (String s) -> {
            int val = Integer.parseInt(s);
            return new Integer[]{
                BehaviorProviderDefinition.getLinkIdFromLinkIdAndLinkedBehavior(val),
                BehaviorProviderDefinition.getBehaviorFromLinkIdAndLinkedBehavior(val)
            };
        };
        BiFunction<String, String, Object> backw2 = (String s1, String s2)
                -> BehaviorProviderDefinition.getLinkIdAndLinkedBehavior(Integer.parseInt(s1.trim()), Integer.parseInt(s2.trim()));
        jPanel3.setLayout(new GridBagLayout());
        JPanel pan1 = new JPanel();
        JPanel pan2 = new JPanel();
        TitledBorder tb1 = new TitledBorder(new EtchedBorder(), "ArrayIndexAndLength", TitledBorder.LEFT, TitledBorder.CENTER);
        tb1.setTitleFont(fontInfo.getFont());
        pan1.setBorder(tb1);
        TitledBorder tb2 = new TitledBorder(new EtchedBorder(), "LinkIDAndLinkedBehavior", TitledBorder.LEFT, TitledBorder.CENTER);
        tb2.setTitleFont(fontInfo.getFont());
        pan2.setBorder(tb2);
        jPanel3.add(pan1, new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
        jPanel3.add(pan2, new GridBagConstraints(0, 1, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));

        initPanel(pan1,
                "ArrayIndexAndLength", Integer.MIN_VALUE, Integer.MAX_VALUE,
                "Index", 0, 0xFFFF,
                "Length", 0, 0xFFFF,
                forw1, backw1);
        initPanel(pan2,
                "LinkIdAndLinkedBehavior", Integer.MIN_VALUE, Integer.MAX_VALUE,
                "Link ID", Byte.MIN_VALUE, Byte.MAX_VALUE,
                "Linked Behavior Index", 0, 0xFFFF,
                forw2, backw2);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel3 = new javax.swing.JPanel();

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel jPanel3;
    // End of variables declaration//GEN-END:variables

    private void initPanel(JPanel panel, String label1, int min1, int max1, String label2, int min2, int max2, String label3, int min3, int max3,
            Function<String, Object[]> forward, BiFunction<String, String, Object> backward) {

        int inputHeight = (int)(this.fontInfo.getLineHeight()*1.5);

        panel.removeAll();
        panel.setLayout(new GridBagLayout());
        JLabel mainLabel = new JLabel(label1);
        mainLabel.setFont(this.fontInfo.getFont());
        EnhancedFormattedTextField mainText = new EnhancedFormattedTextField<>(
                this.fontInfo,
                EnhancedFormattedTextField.getIntegerValidator(min1, max1), s -> Integer.parseInt(s.trim()));
        mainText.setMaximumSize(new Dimension(150, 999));
        mainText.setMinimumSize(new Dimension(150, inputHeight));
        mainText.setPreferredSize(new Dimension(150, inputHeight));
        JPanel leftPanel_______ = BoxComponents(BoxLayout.PAGE_AXIS, mainLabel, mainText);

        JLabel topRightLabel = new JLabel(label2);
        topRightLabel.setFont(this.fontInfo.getFont());
        EnhancedFormattedTextField topRightText = new EnhancedFormattedTextField<>(
                this.fontInfo,
                EnhancedFormattedTextField.getIntegerValidator(min2, max2), s -> Integer.parseInt(s.trim()));
        topRightText.setMaximumSize(new Dimension(150, 999));
        topRightText.setMinimumSize(new Dimension(150, inputHeight));
        topRightText.setPreferredSize(new Dimension(150, inputHeight));
        JPanel topRightPanel___ = BoxComponents(BoxLayout.PAGE_AXIS, topRightLabel, topRightText);

        JLabel bottomRightLabel = new JLabel(label3);
        bottomRightLabel.setFont(this.fontInfo.getFont());
        EnhancedFormattedTextField bottomRightText = new EnhancedFormattedTextField<>(
                this.fontInfo,
                EnhancedFormattedTextField.getIntegerValidator(min3, max3), s -> Integer.parseInt(s.trim()));
        bottomRightText.setMaximumSize(new Dimension(150, 999));
        bottomRightText.setMinimumSize(new Dimension(150, inputHeight));
        bottomRightText.setPreferredSize(new Dimension(150, inputHeight));
        JPanel bottomRightPanel = BoxComponents(BoxLayout.PAGE_AXIS, bottomRightLabel, bottomRightText);
        mainText.setValue(0);
        topRightText.setValue(0);
        bottomRightText.setValue(0);

        panel.add(leftPanel_______, new GridBagConstraints(0, 1, 1, 2, 1, 1, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
        panel.add(topRightPanel___, new GridBagConstraints(2, 1, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
        panel.add(bottomRightPanel, new GridBagConstraints(2, 2, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));

        JButton forwards = new JButton("→");
        forwards.setFont(new java.awt.Font("Calibri", 1, this.fontInfo.getFont().getSize()));
        forwards.setToolTipText("Split");
        forwards.setBorder(new EmptyBorder(5, 8, 5, 8));

        JButton backwards = new JButton("←");
        backwards.setFont(new java.awt.Font("Calibri", 1, this.fontInfo.getFont().getSize()));
        backwards.setToolTipText("Join");
        backwards.setBorder(new EmptyBorder(5, 8, 5, 8));

        panel.add(forwards, new GridBagConstraints(1, 1, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(10, 5, 10, 5), 0, 0));
        panel.add(backwards, new GridBagConstraints(1, 2, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(10, 5, 10, 5), 0, 0));
        panel.add(Box.createHorizontalGlue(), new GridBagConstraints(0, 0, 3, 1, 1, 1000, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        panel.add(Box.createHorizontalGlue(), new GridBagConstraints(0, 3, 3, 1, 1, 1000, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

        ActionListener forw = e -> {
            Object[] apply = forward.apply(mainText.getValue().toString());
            topRightText.setValue(apply[0]);
            bottomRightText.setValue(apply[1]);
        };
        ActionListener backw = e
                -> mainText.setValue(backward.apply(topRightText.getValue().toString(), bottomRightText.getValue().toString()));

        mainText.addActionListener(forw);
        forwards.addActionListener(forw);

        topRightText.addActionListener(backw);
        bottomRightText.addActionListener(backw);
        backwards.addActionListener(backw);
    }

    private static JPanel BoxComponents(int axis, JComponent... comps) {
        JPanel panel = new JPanel();
        BoxLayout layout = new BoxLayout(panel, axis);
        panel.setLayout(layout);
        for (JComponent comp : comps) {
            if (panel.getComponentCount() > 0) {
                panel.add(Box.createRigidArea(new Dimension(5, 5)));
            }
            if (axis == BoxLayout.PAGE_AXIS || axis == BoxLayout.Y_AXIS) {
                comp.setAlignmentX(Component.LEFT_ALIGNMENT);
            } else {
                comp.setAlignmentY(Component.TOP_ALIGNMENT);

            }
            panel.add(comp);
        }
        return panel;
    }

}
