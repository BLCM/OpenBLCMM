/*
 * Copyright (C) 2022 Inteli9
 *
 * This code was made available under CC BY-SA 4.0 by the StackOverflow
 * Terms of Service.
 *
 * Original Post: https://stackoverflow.com/a/70621256/2013126
 * Author: https://stackoverflow.com/users/2807375/inteli9
 *
 */
package blcmm.gui.components;

import java.awt.Component;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;

/**
 * A JTabbedPane where the currently-active tab title is set to bold, for
 * enhanced readability.
 *
 * Lifted with very minimal changes from https://stackoverflow.com/a/70621256/2013126
 *
 * @author https://stackoverflow.com/users/2807375/inteli9
 */
public class BoldJTabbedPane extends JTabbedPane {

    @Override
    public void insertTab(String title, Icon icon, Component component, String tip, int index) {
        ((JComponent)component).putClientProperty("title", title);
        super.insertTab(title, icon, component, tip, index);
    }

    @Override
    public void setSelectedIndex(int index) {
        int currentIndex = getSelectedIndex();
        if (currentIndex >= 0) {
            JComponent previous = (JComponent) getComponentAt(currentIndex);
            String title = (String) previous.getClientProperty("title");
            setTitleAt(currentIndex, title);
        }
        super.setSelectedIndex(index);

        JComponent current = (JComponent)getSelectedComponent();
        String title = (String) current.getClientProperty("title");
        setTitleAt(index, "<html><b>" + title + "</b></html>");
    }

}
