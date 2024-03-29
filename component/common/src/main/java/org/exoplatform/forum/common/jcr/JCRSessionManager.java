/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.forum.common.jcr;

import javax.jcr.Session;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;

public class JCRSessionManager implements SessionManager {

  /** . */
  private static final ThreadLocal<Session> currentSession = new ThreadLocal<Session>();

  String                                    workspaceName  = "knowledge";

  /**
   * Constructor
   * @param workspace
   */
  public JCRSessionManager(String workspace) {
    this.workspaceName = workspace;
  }

  public String getWorkspaceName() {
    return workspaceName;
  }

  public void setWorkspaceName(String workspaceName) {
    this.workspaceName = workspaceName;
  }

  /**
   * <p>Returns the session currently associated with the current thread of execution.<br>
   * The current session is set with {@link #openSession()} </p>
   *
   * @return the current session if exists, null otherwise
   */
  public Session getCurrentSession() {
    return currentSession.get();
  }

  public static Session currentSession() {
    return currentSession.get();
  }

  /*
   * (non-Javadoc)
   * @see org.exoplatform.forum.common.jcr.SessionManager#getSession(org.exoplatform.services.jcr.ext.common.SessionProvider)
   */
  public Session getSession(SessionProvider sessionProvider) {
    Session session = null;
    try {
      session = sessionProvider.getSession(workspaceName, SessionProviderService.getRepository());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return session;
  }

  /**
   * <p>Open and returns a session to the model. When the current thread is already associated with a previously
   * opened session the method will throw an {@link IllegalStateException}.</p>
   *
   * @return a session to the model.
   */
  public Session openSession() {
    Session session = currentSession.get();
    if (session == null) {
      session = createSession();
      currentSession.set(session);
    } else {
      //throw new IllegalStateException("A session is already opened.");
    }
    return session;
  }

  public Session openOrReuseSession() {
    Session session = currentSession.get();
    if (session == null) {
      session = createSession();
      currentSession.set(session);
    }
    return session;
  }

  /*
   * (non-Javadoc)
   * @see org.exoplatform.forum.common.jcr.SessionManager#createSession()
   */
  public Session createSession() {
    Session session = null;
    try {
      session = SessionProviderService.getRepository().getSystemSession(workspaceName);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return session;
  }

  /*
   * (non-Javadoc)
   * @see org.exoplatform.forum.common.jcr.SessionManager#closeSession()
   */
  public boolean closeSession() {
    return closeSession(false);
  }

  /*
   * (non-Javadoc)
   * @see org.exoplatform.forum.common.jcr.SessionManager#closeSession(boolean)
   */
  public boolean closeSession(boolean save) {
    Session session = currentSession.get();
    if (session == null) {
      // Should warn
      return false;
    } else {
      currentSession.set(null);
      try {
        if (save) {
          session.save();
        }
      } catch (Exception e) {
        return false;
      } finally {
        session.logout();
      }
      return true;
    }
  }

  /*
   * (non-Javadoc)
   * @see org.exoplatform.forum.common.jcr.SessionManager#executeAndSave(org.exoplatform.forum.common.jcr.JCRTask)
   */
  public <T> T executeAndSave(JCRTask<T> jcrTask) {
    try {
      openOrReuseSession();
      return jcrTask.execute(getCurrentSession());
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      closeSession(true);
    }
  }

  /*
   * (non-Javadoc)
   * @see org.exoplatform.forum.common.jcr.SessionManager#execute(org.exoplatform.forum.common.jcr.JCRTask)
   */
  public <T> T execute(JCRTask<T> jcrTask) {
    try {
      openOrReuseSession();
      return jcrTask.execute(getCurrentSession());
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      closeSession(true);
    }
  }

}
