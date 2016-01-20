/***************************************************************************
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
 ***************************************************************************/
package org.exoplatform.forum.webui;

import java.util.Arrays;

import org.exoplatform.forum.ForumUtils;
import org.exoplatform.forum.common.webui.WebUIUtils;
import org.exoplatform.forum.service.Forum;
import org.exoplatform.forum.service.ForumServiceUtils;
import org.exoplatform.forum.service.UserProfile;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIContainer;

@ComponentConfig(
    template = "app:/templates/forum/webui/UIForumInfos.gtmpl"
)
public class UIForumInfos extends UIContainer {

  private boolean     enableIPLogging = true;
  
  private Forum forum;

  public UIForumInfos() throws Exception {
    addChild(UIPostRules.class, null, null);
    addChild(UIForumModerator.class, null, null);
  }

  private String getRemoteIP() throws Exception {
    if (enableIPLogging) {
      return WebUIUtils.getRemoteIP();
    }
    return ForumUtils.EMPTY_STR;
  }

  public void setForum(Forum forum) throws Exception {
    this.forum = forum;
  }
  
  @Override
  public void processRender(WebuiRequestContext context) throws Exception {
    initContainer();
    //
    super.processRender(context);
  }
  
  private void initContainer() throws Exception {
    UIForumPortlet forumPortlet = getAncestorOfType(UIForumPortlet.class);
    enableIPLogging = forumPortlet.isEnableIPLogging();
    UserProfile userProfile = forumPortlet.getUserProfile();
    String[] moderators = ((forum != null) ? forum.getModerators() : null);
    UIPostRules postRules = getChild(UIPostRules.class);
    //
    if (forumPortlet.isShowRules()) {
      boolean isLock = true;
      if (forum != null) {
        isLock = forum.getIsClosed() || forum.getIsLock();
        if (!isLock && userProfile.getUserRole() != 0) {
          if (!ForumServiceUtils.hasPermission(moderators, userProfile.getUserId())) {
            isLock = forum.getBanIP().contains(getRemoteIP());
            if (!isLock) {
              isLock = !forumPortlet.checkForumHasAddTopic(forum.getCategoryId(), forum.getId());
            }
          }
        }
      }
      postRules.setLock(isLock);
      postRules.setUserProfile(userProfile);
    }
    postRules.setRendered(forumPortlet.isShowRules());
    //
    UIForumModerator forumModerator = getChild(UIForumModerator.class);
    if (forumPortlet.isShowModerators()) {
      forumModerator.setModeratorsForum(Arrays.asList(moderators));
      forumModerator.setUserRole(userProfile.getUserRole());
    }
    forumModerator.setRendered(forumPortlet.isShowModerators());
  }
}
