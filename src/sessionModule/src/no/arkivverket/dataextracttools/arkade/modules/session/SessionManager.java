/*
 * The National Archives of Norway - 2014
 */
package no.arkivverket.dataextracttools.arkade.modules.session;

import java.util.UUID;

/**
 * Class for creating sessions. 
 * Each session gets a name which is unique within the application.
 * Each session also gets an id which is a type 4 UUID.
 * The part that is using SessionManager decides when a session can be created
 * by setting canCreateSession.
 * When a session is created, canCreateSession is set to false. 
 * The caller then has to explicitly set canCreateSession to true.
 * 
 * @version 0.11 2014-02-28
 * @author Riksarkivet
 */
public class SessionManager {

    private static SessionManager sessionManager;
    private boolean canCreateSession = true;
    private int sessionCounter = 0;

    private SessionManager() {
    }

    public static synchronized SessionManager getInstance() {
        if (sessionManager == null) {
            // The only instance
            sessionManager = new SessionManager();
        }
        return sessionManager;
    }
    
    /**
     * 
     * @return 
     */
    public boolean canCreateSession() {
        return canCreateSession;
    }
    
    /**
     * 
     * @param canCreateSession 
     */
    public void setCanCreateSession(boolean canCreateSession) {
        this.canCreateSession = canCreateSession;
    }
    
    /**
     * 
     * @param namePrefix the prefix for the session name
     * @return the created session object
     */
    public Session createSession(String namePrefix) {
        if (!canCreateSession) {
            return null;
        }
        sessionCounter++;
        String sessionId = UUID.randomUUID().toString();
        Session session = new Session(sessionId);
        String sessionName = "" + namePrefix + " #" + sessionCounter;
        session.setName(sessionName);
        canCreateSession = false;
        return session;
    }

}
