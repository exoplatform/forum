/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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
package org.exoplatform.forum.service.jcr.listener;

import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.forum.service.ForumService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class DeletedUserCalculateEventListener implements EventListener {
  private Log    LOG = ExoLogger.getLogger(DeletedUserCalculateEventListener.class);

  private String workspace_;

  private String repository_;

  public DeletedUserCalculateEventListener() throws Exception {
  }

  public String getSrcWorkspace() {
    return workspace_;
  }

  public String getRepository() {
    return repository_;
  }

  public void onEvent(EventIterator evIter) {
    try {
      ExoContainer container = ExoContainerContext.getCurrentContainer();
      ForumService forumService = (ForumService) container.getComponentInstanceOfType(ForumService.class);
      while (evIter.hasNext()) {
        Event ev = evIter.nextEvent();
        if (ev.getType() == Event.NODE_ADDED || ev.getType() == Event.NODE_REMOVED) {
          String userName = ev.getPath().substring(ev.getPath().lastIndexOf("/") + 1);
          forumService.calculateDeletedUser(userName);
          break;
        }
      }
    } catch (Exception e) {
      LOG.error("Add event for calculateDeletedUser is fall", e);
    }
  }
}
