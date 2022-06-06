/*
 * Testing 123
 * abc
 */
package blcmm.plugins.myplugin;

import blcmm.plugins.BLCMMModelPlugin;
import blcmm.plugins.pseudo_model.PCategory;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

/**
 * The default implementation for a plugin. Replace all implementations shown in
 * this file by your own. The main plugin itself will be contained in the getGUI
 * method. It is advised to create a class that extends JPanel which contains
 * all the functional code.
 *
 * @author LightChaosman
 */
//public class MyPlugin extends BLCMMPlugin {//Use this for utility plugins, like calculators
public class MyPlugin extends BLCMMModelPlugin {//Use this if you want your plugin to export to the currently opened file in BLCMM
//public class MyPlugin extends BLCMMFilePlugin {//Use this if you want your plugin to export to a file

    public MyPlugin() {
        super(false, false);
        //These two booleans indicate if the plugin works for BL2 and TPS respectively
    }

    /*
     * Give your plugin a meaningful name!
     */
    @Override
    public String getName() {
        return "Your plugin name goes here";
    }

    /*
     * Make this method return the entire panel containing the GUI of your
     * plugin.
     */
    @Override
    public JPanel getGUI() {
        return new JPanel();
    }

    /*
     * If your plugin requires specific data classes, list them here.
     * The plugin won't be accessible to users without the packages containing the listed classes, and inform them about this.
     * This way you don't need to worry about people not having the required data, and crashes this may cause.
     */
    @Override
    public String[] getRequiredDataClasses() {
        return new String[]{};
    }

    /*This is the result of the plugin, and will be imported into BLCMM.
     */
    @Override
    public PCategory getOutputModel() {
        return new PCategory("thingy");
    }

    /*
     * Make this return a progress bar if your plugin takes a while to execute.
     * Consequently, make sure the progress bar is updated while getOutputModel is in progress.
     * BLCMM will take care of the multi-threading.
     */
    @Override
    public JProgressBar getProgressBar() {
        return null;
    }
}