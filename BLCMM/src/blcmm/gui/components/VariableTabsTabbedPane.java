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
package blcmm.gui.components;

import blcmm.gui.ObjectExplorer;
import blcmm.gui.panels.ObjectExplorerPanel;
import general.utilities.GlobalLogger;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.TreeSet;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;

/**
 *
 * @author LightChaosman
 * @param <P> The class of the default new component.
 */
public abstract class VariableTabsTabbedPane<P extends JPanel> extends JTabbedPane {

    private final TreeSet<Integer> usedIndices = new TreeSet<>();
    private boolean init = false;
    private boolean dragging = false;
    private boolean finishingdrop = false;
    private Image tabImage = null;
    private Point currentMouseLocation = null;
    private int draggedTabIndex = 0;

    public VariableTabsTabbedPane() {
        super();
        super.add("", getDefaultNewComponent());
        JLabel label = new JLabel("Tab 1");
        usedIndices.add(1);
        int height = new ButtonTabComponent(VariableTabsTabbedPane.this).getPreferredSize().height;
        label.setPreferredSize(new Dimension(label.getPreferredSize().width, height));
        super.setTabComponentAt(0, label);
        super.add("+", new JPanel());
        installMouseListenerWrapper();
        installStateChangeListener();

        MouseAdapter adap = new MouseAdapter() {
            private int clicktabNumber;

            @Override
            public void mousePressed(MouseEvent e) {
                clicktabNumber = getUI().tabForCoordinate(VariableTabsTabbedPane.this, e.getX(), e.getY());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (!dragging) {
                    // Gets the tab index based on the mouse position
                    int tabNumber = getUI().tabForCoordinate(VariableTabsTabbedPane.this, e.getX(), e.getY());

                    if (tabNumber >= 1 && tabNumber < getTabCount() - 1 && clicktabNumber == tabNumber) {
                        draggedTabIndex = tabNumber;
                        Rectangle bounds = getUI().getTabBounds(VariableTabsTabbedPane.this, tabNumber);

                        // Paint the tabbed pane to a buffer
                        Image totalImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
                        Graphics totalGraphics = totalImage.getGraphics();
                        totalGraphics.setClip(bounds);
                        // Don't be double buffered when painting to a static image.
                        setDoubleBuffered(false);
                        paintComponent(totalGraphics);
                        //paintComponents(totalGraphics);//looks cleaner without this

                        // Paint just the dragged tab to the buffer
                        tabImage = new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_ARGB);
                        Graphics graphics = tabImage.getGraphics();
                        graphics.drawImage(totalImage, 0, 0, bounds.width, bounds.height, bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height, VariableTabsTabbedPane.this);

                        dragging = true;
                    }
                } else if (dragging) {
                    currentMouseLocation = e.getPoint();
                    // Need to repaint
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (dragging) {
                    int tabNumber = getUI().tabForCoordinate(VariableTabsTabbedPane.this, e.getX(), 10);
                    if (tabNumber >= 0) {
                        tabNumber = Math.min(Math.max(tabNumber, 1), getTabCount() - 2);
                        finishingdrop = true;
                        Component comp = getComponentAt(draggedTabIndex);
                        Component comp2 = getTabComponentAt(draggedTabIndex);
                        String title = getTitleAt(draggedTabIndex);
                        removeTabAt(draggedTabIndex);
                        insertTab(title, null, comp, null, tabNumber);
                        setTabComponentAt(tabNumber, comp2);
                        setSelectedIndex(tabNumber);
                        finishingdrop = false;
                    }
                }
                dragging = false;
                tabImage = null;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent me) {
                Rectangle bounds = getBounds();
                Rectangle bounds2 = getUI().getTabBounds(VariableTabsTabbedPane.this, 0);
                Rectangle bounds3 = new Rectangle(bounds.x, bounds2.y, bounds.width, bounds2.height);
                if (!bounds3.contains(me.getPoint())) {
                    dragging = false;
                    tabImage = null;
                    repaint();
                }
            }
        };

        addMouseMotionListener(adap);
        addMouseListener(adap);

        init = true;
    }

    protected abstract P getDefaultNewComponent();

    protected abstract Component getDefaultComponentToFocus(P comp);

    private void installMouseListenerWrapper() {
        MouseListener handler = findUIMouseListener();

        removeMouseListener(handler);
        addMouseListener(new MouseListenerWrapper(handler, this));
    }

    private MouseListener findUIMouseListener() {
        MouseListener[] listeners = getMouseListeners();
        return listeners[0];
    }

    private void installStateChangeListener() {
        this.addChangeListener((ChangeEvent ce) -> {
            if (getSelectedIndex() == getTabCount() - 1 && init && !finishingdrop) {
                int index2 = getTabCount();
                int newTabIndex = usedIndices.last() + 1;
                setTitleAt(index2 - 1, "Tab " + newTabIndex);
                P newComponent = getDefaultNewComponent();
                setComponentAt(index2 - 1, newComponent);
                Component toFocus = getDefaultComponentToFocus(newComponent);
                if (toFocus != null) {
                    EventQueue.invokeLater(toFocus::requestFocus);
                }
                ButtonTabComponent buttoncomp = new ButtonTabComponent(VariableTabsTabbedPane.this);
                setTabComponentAt(index2 - 1, buttoncomp);
                GlobalLogger.log("Object Explorer - added tab " + newTabIndex);
                usedIndices.add(newTabIndex);
                add("+", new JPanel());
            } else {
                GlobalLogger.log("Object Explorer - switched to tab " + getSelectedIndex());
                if (VariableTabsTabbedPane.this.getSelectedComponent() instanceof ObjectExplorerPanel) {
                    ObjectExplorerPanel selectedComponent = (ObjectExplorerPanel) VariableTabsTabbedPane.this.getSelectedComponent();
                    ObjectExplorer.INSTANCE.updateSearch(selectedComponent.getTextElement());
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Are we dragging?
        if (dragging && currentMouseLocation != null && tabImage != null) {
            // Draw the dragged tab
            g.drawImage(tabImage, currentMouseLocation.x, 3, this);
        }
    }

    @Override
    public void remove(int i) {
        Component tabComponent = getTabComponentAt(i);
        JLabel l = null;
        if (tabComponent instanceof JLabel) {
            l = (JLabel) tabComponent;
        } else if (tabComponent instanceof Container) {
            for (Component c : ((Container) tabComponent).getComponents()) {
                if (c instanceof JLabel) {
                    l = (JLabel) c;
                    break;
                }
            }
        }
        if (l != null) {
            String t = l.getText();
            int n = Integer.parseInt(t.substring(t.lastIndexOf(" ") + 1));
            usedIndices.remove(n);
        }
        super.remove(i);
    }

    private static class MouseListenerWrapper implements MouseListener {

        private final MouseListener delegate;
        private final JTabbedPane pane;

        public MouseListenerWrapper(MouseListener delegate, JTabbedPane pane) {
            this.delegate = delegate;
            this.pane = pane;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            delegate.mouseClicked(e);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (SwingUtilities.isRightMouseButton(e)) {
                return;
            } else if (SwingUtilities.isMiddleMouseButton(e)) {
                int idx = pane.indexAtLocation(e.getX(), e.getY());
                if (idx > 0 && idx < pane.getTabCount() - 1) {
                    if (idx == pane.getTabCount() - 2) {//tab next to the +
                        pane.setSelectedIndex(pane.getTabCount() - 3);
                    }
                    pane.remove(idx);
                }
            } else {
                delegate.mousePressed(e);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            delegate.mouseReleased(e);
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            delegate.mouseEntered(e);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            delegate.mouseExited(e);
        }

    }

}
