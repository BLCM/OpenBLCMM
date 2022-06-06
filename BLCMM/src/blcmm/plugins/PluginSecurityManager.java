/*
 * Testing 123
 * abc
 */package blcmm.plugins;

import general.utilities.GlobalLogger;
import java.io.File;
import java.security.Permission;

/**
 *
 * @author LightChaosman
 *
 */
public class PluginSecurityManager extends SecurityManager {

    private static final ClassLoader//
            BOOTSTRAP_CL = null,
            EXT_CL,
            SYSTEM_CL = ClassLoader.getSystemClassLoader();

    static {
        ClassLoader cl = new Object() {
        }.getClass().getEnclosingClass().getClassLoader();
        ClassLoader prev = null;
        while (cl != null) {
            prev = cl;
            cl = cl.getParent();
        }
        EXT_CL = prev;
    }

    boolean recursed = false;
    private Class[] classContext;

    public PluginSecurityManager() {

    }

    @Override
    public void checkPermission(Permission prmsn) {
        if (recursed) {
            return;
        }
        recursed = true;
        if (isSafeCallStack()) {
            recursed = false;
            return;//If the entire call-stack is from the system class loader, we're good
        }
        recursed = false;
        String name = prmsn.getName();
        if (prmsn.getClass().equals(java.util.PropertyPermission.class) && prmsn.getActions().equals("read")) {
            return;//allow reading of system properties
        } else if (prmsn.getClass().equals(java.io.FilePermission.class)) {
            if (prmsn.getActions().equals("read")) {
                return;//allow reading of any file
            }
            if (prmsn.getActions().equals("write")) {
                String file = name;//new File(name).getAbsolutePath();
                File f = new File(System.getProperty("user.dir"));
                if (file.startsWith(f.getAbsolutePath())) {
                    return;//Only allow writing to the BLCMM folder
                }
            }
        } else if (prmsn.getClass().equals(java.lang.RuntimePermission.class)) {
            switch (name) {
                case "createClassLoader":
                    return; //We seem to need this to generate GUIs?
                case "accessDeclaredMembers":
                    return; //We seem to need this to generate GUIs?
                case "accessClassInPackage.sun.security.internal.spec":
                    return; //Needed to generate RSA key for HTTPS access to download from github
                case "accessClassInPackage.sun.security.rsa":
                    return; //Needed to generate RSA key for HTTPS access to download from github
                case "accessClassInPackage.sun.security.internal.interfaces":
                    return; //Needed to generate RSA key for HTTPS access to download from github
                case "modifyThreadGroup":
                    return; //needed for ImageIcons
                default:
                    break;
            }
        } else if (prmsn.getClass().equals(java.lang.reflect.ReflectPermission.class) && name.equals("suppressAccessChecks")) {
            return;//We seem to need this to generate GUIs?
        } else if (prmsn.getClass().equals(java.awt.AWTPermission.class)) {
            switch (name) {
                case "accessEventQueue":
                    return; //Allow to create new dialogs
                case "showWindowWithoutWarningBanner":
                    return; //We don't want a warning triangle
                case "replaceKeyboardFocusManager":
                    return; //We do this in the allow-resize function call
                case "listenToAllAWTEvents":
                    return; //Disposing newly created dialogs in the plugin does this
                case "accessClipboard":
                    return; //We want to be able to copy-paste from and to the plugin window
                case "watchMousePointer":
                    return; //Needed for sliders
                default:
                    break;
            }
        } else if (prmsn.getClass().equals(java.security.SecurityPermission.class)) {
            if (name.equals("putProviderProperty.SunJCE")) {
                return;//Needed to generate RSA key for HTTPS access to download from github
            }
        }

        GlobalLogger.log("Denying a security call:");
        GlobalLogger.log(prmsn.toString());
        GlobalLogger.log(prmsn.getActions());
        GlobalLogger.log(prmsn.getName());
        GlobalLogger.log(prmsn.getClass().toString());
        logCallStack();
        GlobalLogger.markAsPermanentLog();
        //
        super.checkPermission(prmsn);
    }

    private boolean isSafeCallStack() {
        classContext = getClassContext();
        int firstUnsafe = -1;
        for (int i = 0; i < classContext.length; i++) {
            ClassLoader classLoader = classContext[i].getClassLoader();
            if (!(classLoader == BOOTSTRAP_CL || classLoader == SYSTEM_CL || classLoader == EXT_CL)) {
                //GlobalLogger.log("Unsafe callstack due to: " + classContext[i] + " with classloader " + classLoader);
                firstUnsafe = i;
                break;
            }
        }
        if (firstUnsafe != -1) {
            int firstNotMe = 0;
            while (classContext[firstNotMe].isAssignableFrom(PluginSecurityManager.class)) {
                firstNotMe++;
            }
            for (int i = firstNotMe; i < firstUnsafe; i++) {
                if (classContext[i].getClassLoader() == SYSTEM_CL) {//TODO this also contains JRE elements, I think
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    private void logCallStack() {
        GlobalLogger.log("logging class stack + class loaders. The system classloader is: " + ClassLoader.getSystemClassLoader());
        for (Class classContext1 : classContext) {
            ClassLoader classLoader = classContext1.getClassLoader();
            if (classLoader != null) {
                GlobalLogger.log("class: " + classContext1 + " - CL: " + classLoader);
            }
        }
    }
}