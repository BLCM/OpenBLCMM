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
package blcmm.gui.components;

import blcmm.gui.FontInfo;
import blcmm.utilities.GlobalLogger;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;

/**
 * A component to render the tab labels in Object Explorer.  Consists of a
 * text label which just shows the tab number, plus a tab-close button.  Note
 * that tab 1 (index 0) is special and does not use this class -- its label
 * is just a JLabel, and doesn't have the close button.
 *
 * @author FromDarkHell
 * @author LightChaosman
 */
public class ButtonTabComponent extends JPanel {

    private final JTabbedPane pane;
    private final TabButton button;

    public ButtonTabComponent(final JTabbedPane pane, FontInfo fontInfo) {
        //unset default flow layout
        super(new FlowLayout(FlowLayout.LEFT, 0, 0));
        if (pane == null) {
            throw new NullPointerException("TabbedPane is null");
        }
        this.pane = pane;
        setOpaque(false);

        //make JLabel read titles from JTabbedPane
        JLabel label = new JLabel() {
            @Override
            public String getText() {
                int i = pane.indexOfTabComponent(ButtonTabComponent.this);
                if (i != -1) {
                    return pane.getTitleAt(i);
                }
                return null;
            }
        };
        label.setFont(fontInfo.getFont());

        add(label);
        //add more space between the label and the button
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 2));
        //tab button
        this.button = new TabButton(fontInfo);
        add(this.button);
        //add more space to the top of the component
        setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
    }

    /**
     * Update the close-tab buttons if needed, triggered by font-size changes.
     * The button will already share the global FontInfo object, so there's
     * no need to pass any new font information in here.
     */
    public void updateCurrentSizes() {
        this.button.updateCurrentSizes();
    }

    /**
     * Tab-close button which gets added on to tabs 2+ in OE.
     */
    class TabButton extends JButton implements ActionListener {

        private final FontInfo fontInfo;
        // Default size is based on our default 12-point font.  The actual
        // icon will get scaled based on our FontInfo object, which has the
        // actual font info in it.
        private final int defaultSize = 17;
        private final double marginPercent = 0.3;

        private int currentSize;
        private int currentMargin;

        public TabButton(FontInfo fontInfo) {
            this.fontInfo = fontInfo;
            this.updateCurrentSizes();
            setToolTipText("Close this tab");
            //Make the button look the same for all Laf's
            setUI(new BasicButtonUI());
            //Make it transparent
            setContentAreaFilled(false);
            //No need to be focusable
            setFocusable(false);
            setBorder(BorderFactory.createEtchedBorder());
            setBorderPainted(false);
            //Making nice rollover effect
            //we use the same listener for all buttons
            addMouseListener(buttonMouseListener);
            setRolloverEnabled(true);
            //Close the proper tab by clicking the button
            addActionListener(this);
        }

        /**
         * Updates our size parameters so that the "x" button can resize as
         * font sizes change.  This is also called from the constructor to set
         * the initial parameters.  The `invalidate()` call isn't necessary
         * during the constructor, but it doesn't *hurt*, so no reason to try
         * and be fancy with that.
         *
         * This button will already share the global FontInfo object, so there's
         * no need to pass any new font information in here.
         */
        private void updateCurrentSizes() {
            this.currentSize = (int)(this.defaultSize*this.fontInfo.getScaleHeight());
            this.currentMargin = (int)(this.defaultSize*this.marginPercent);
            this.setPreferredSize(new Dimension(this.currentSize, this.currentSize));
            this.invalidate();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int i = pane.indexOfTabComponent(ButtonTabComponent.this);
            if (i != -1) {
                if (pane.getSelectedIndex() == i) {
                    pane.setSelectedIndex(i - 1);
                }
                pane.remove(i);
                GlobalLogger.log("Object Explorer - removed tab " + i);
            }
        }

        //Don't want to update UI for this button
        @Override
        public void updateUI() {

        }

        //Paint the cross
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            //shift the image for pressed buttons
            if (getModel().isPressed()) {
                g2.translate(1, 1);
            }
            g2.setStroke(new BasicStroke(2));
            g2.setColor(Color.RED);
            if (getModel().isRollover()) {
                g2.setColor(Color.MAGENTA);
            }
            g2.drawLine(this.currentMargin, this.currentMargin,
                    getWidth() - this.currentMargin - 1, getHeight() - this.currentMargin - 1);
            g2.drawLine(getWidth() - this.currentMargin - 1, this.currentMargin,
                    this.currentMargin, getHeight() - this.currentMargin - 1);
            g2.dispose();
        }

        @Override
        public JToolTip createToolTip() {
            JToolTip tip = new JToolTip();
            tip.setComponent(this);
            tip.setFont(this.fontInfo.getFont());
            return tip;
        }

    }
    private final static MouseListener buttonMouseListener = new MouseAdapter() {
        @Override
        public void mouseEntered(MouseEvent e) {
            Component component = e.getComponent();
            if (component instanceof AbstractButton) {
                AbstractButton button = (AbstractButton) component;
                button.setBorderPainted(true);
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            Component component = e.getComponent();
            if (component instanceof AbstractButton) {
                AbstractButton button = (AbstractButton) component;
                button.setBorderPainted(false);
            }
        }
    };
}
