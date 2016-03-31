/*
 * The National Archives of Norway - 2014
 */
package no.arkivverket.dataextracttools.arkade.modules.init;

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;

/**
 *
 * @version 0.05 2014-02-28
 * @author Riksarkivet
 *
 */
@ActionID(
    category = "Help",
    id = "no.arkivverket.dataextracttools.arkade.modules.init.AboutAction")
@ActionRegistration(
displayName = "#LBL_About")
@ActionReferences({
    @ActionReference(path = "Menu/Help", position = 100)
})
public final class AboutAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        AboutPanel aboutPanel = new AboutPanel();

        String title =
                NbBundle.getMessage(AboutAction.class, "LBL_About");

        Object[] options = new Object[]{
            // Close
            NbBundle.getMessage(AboutAction.class, "CTL_Close")
        };

        DialogDescriptor descriptor = new DialogDescriptor(
                aboutPanel, 
                title, 
                true, // modal
                options, 
                options[0], // initial value
                DialogDescriptor.PLAIN_MESSAGE, 
                null, // HelpCtx helpCtx
                null // ActionListener
                );

        Dialog dialog = DialogDisplayer.getDefault().createDialog(descriptor);
        dialog.setResizable(false);
        dialog.setVisible(true);
    }
}
