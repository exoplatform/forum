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
package org.exoplatform.forum.service.conf;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.forum.service.ForumService;
import org.exoplatform.forum.service.ForumServiceUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserEventListener;

public class ForumUserListener extends UserEventListener {

  private static Log  log = ExoLogger.getLogger(ForumUserListener.class);

  private ForumService getForumService() {
    return (ForumService) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(ForumService.class);
  }

  public void postSave(User user, boolean isNew) throws Exception {
    if (!isNew) {
      try {
        getForumService().updateUserProfile(user);
      } catch (Exception e) {
        log.warn("Error while updating forum profile: ", e);
      }
      ForumServiceUtils.clearCache();
    }
  }

  @Override
  public void postDelete(User user) throws Exception {
    try {
      getForumService().removeMember(user);
    } catch (Exception e) {
      log.warn("failed to remove member : ", e);
    }
    //
    ForumServiceUtils.clearCache();
  }

  @Override
  public void postSetEnabled(User user) {
    ForumService fservice = CommonsUtils.getService(ForumService.class);
    //
    fservice.processEnabledUser(user.getUserName(), user.getEmail(), user.isEnabled());
    
    if (!user.isEnabled()) {
      //
      try {
        fservice.userLogout(user.getUserName());
      } catch (Exception e) {
        log.warn(String.format("Removes online for user %s is unsuccessful.", user.getUserName()));
        log.debug(e.getMessage(), e);
      }
    }
    //
    ForumServiceUtils.clearCache();
  }

}
