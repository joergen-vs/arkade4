/*
 * The National Archives of Norway - 2014
 */
package no.arkivverket.dataextracttools.arkade.modules.init;

import javax.swing.JFrame;
import javax.swing.UIManager;
import no.arkivverket.dataextracttools.arkade.modules.session.SessionTopComponent;
import org.openide.modules.ModuleInstall;
import org.openide.util.NbBundle;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 * Manages a module's lifecycle.
 *
 * @version 0.06 2014-02-28
 * @author Riksarkivet
 */
public class Installer extends ModuleInstall {

    @Override
    public void restored() {

        // JFileChooser - i18n

        UIManager.put("FileChooser.cancelButtonText",
                NbBundle.getMessage(Installer.class, "LBL_Cancel"));

        UIManager.put("FileChooser.fileNameLabelText",
                NbBundle.getMessage(Installer.class,
                "FileChooser.fileNameLabelText") + ":");
        UIManager.put("FileChooser.filesOfTypeLabelText",
                NbBundle.getMessage(Installer.class,
                "FileChooser.filesOfTypeLabelText") + ":");

        UIManager.put("FileChooser.lookInLabelText",
                NbBundle.getMessage(Installer.class,
                "FileChooser.lookInLabelText"));
        UIManager.put("FileChooser.saveInLabelText",
                NbBundle.getMessage(Installer.class,
                "FileChooser.saveInLabelText"));
        UIManager.put("FileChooser.upFolderToolTipText",
                NbBundle.getMessage(Installer.class,
                "FileChooser.upFolderToolTipText"));
        UIManager.put("FileChooser.newFolderToolTipText",
                NbBundle.getMessage(Installer.class,
                "FileChooser.newFolderToolTipText"));
        UIManager.put("FileChooser.saveButtonText",
                NbBundle.getMessage(Installer.class,
                "FileChooser.saveButtonText"));
        UIManager.put("FileChooser.openButtonText",
                NbBundle.getMessage(Installer.class,
                "FileChooser.openButtonText"));

        /*
         *                     
         FileChooser.fileNameLabelText
         FileChooser.homeFolderToolTipText
         FileChooser.newFolderToolTipText
         FileChooser.listViewButtonToolTipTextlist
         FileChooser.detailsViewButtonToolTipText
         FileChooser.saveButtonText=Save
         FileChooser.openButtonText=Open
         FileChooser.cancelButtonText=Cancel
         FileChooser.updateButtonText=Update
         FileChooser.helpButtonText=Help
         FileChooser.saveButtonToolTipText=Save
         FileChooser.openButtonToolTipText=Open
         FileChooser.cancelButtonToolTipText=Cancel
         FileChooser.updateButtonToolTipText=Update
         FileChooser.helpButtonToolTipText=Help

         */

        WindowManager.getDefault().invokeWhenUIReady(new Runnable() {
            @Override
            public void run() {
                JFrame frame = (JFrame) WindowManager.getDefault().getMainWindow();
                frame.setTitle(NbBundle.getMessage(Installer.class, "ApplicationName"));
                System.setProperty("application.name", 
                        NbBundle.getMessage(Installer.class, "ApplicationName"));
                System.setProperty("application.version", 
                        NbBundle.getMessage(Installer.class, "ApplicationVersion"));

                WindowManager.getDefault().findTopComponent("output").putClientProperty("netbeans.winsys.tc.closing_disabled", Boolean.TRUE);

                // Makes sure the Output window always shows at application startup
                InputOutput io = IOProvider.getDefault().getIO("Dummy", false);
                io.select();
                io.closeInputOutput();

                // Always open with a new session
                SessionTopComponent tc = new SessionTopComponent();
                tc.init();
                tc.open();
                tc.requestActive();
            }
        });
    }
    
    /**
     * 
     * @return <code>true</code> if all sessions (SessionTopComponent)
     *         can be closed. 
     */
    @Override
    public boolean closing() {
        boolean canClose = true;

        for (TopComponent tc : TopComponent.getRegistry().getOpened()) {
            if (tc instanceof SessionTopComponent) {
                if (!((SessionTopComponent) tc).canClose()) {
                    // Close is cancelled
                    canClose = false;
                    break;
                }
            }
        }

        return canClose;
    }
}
