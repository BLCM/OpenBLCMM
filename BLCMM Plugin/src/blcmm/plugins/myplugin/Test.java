/*
 * Testing 123
 * abc
 */
package blcmm.plugins.myplugin;

import blcmm.plugins.BLCMMPlugin;

/**
 * A main class that simply lets a dialog pop up with the plugin that you made.
 *
 * Keep in mind that any and all that are distributed publicly are to be
 * distributed as a single jar file, meant to be run from inside BLCMM, and not
 * as a standalone application based on this test class.
 *
 * @author LightChaosman
 */
class Test {

    /**
     * @param args the command line arguments
     * @throws java.lang.Exception
     */
    public static void main(String[] args) throws Exception {
        BLCMMPlugin.test(new MyPlugin());
    }

}