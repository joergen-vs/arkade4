/*
 * The National Archives of Norway - 2014
 */
package no.arkivverket.dataextracttools.arkade.modules.session;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

@ActionID(
        category = "Edit",
        id = "no.arkivverket.dataextracttools.arkade.modules.session.NewSessionAction")
@ActionRegistration(
        iconBase = "no/arkivverket/dataextracttools/arkade/modules/session/resources/new.gif",
        displayName = "#CTL_NewSessionAction")
@ActionReferences({
    @ActionReference(path = "Menu/File", position = 1300),
    @ActionReference(path = "Toolbars/File", position = 300)
})
public final class NewSessionAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        if (SessionManager.getInstance().canCreateSession()) {
            SessionTopComponent tc = new SessionTopComponent();
            tc.init();
            tc.open();
            tc.requestActive();
        }
    }
}
