/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
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

import javax.jcr.Node;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.forum.common.jcr.KSDataLocation;
import org.exoplatform.forum.service.ForumNodeTypes;
import org.exoplatform.forum.service.ForumServiceUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.MembershipEventListener;

public class ForumMembershipEventListener extends MembershipEventListener {
  
  private Log LOG = ExoLogger.getLogger(ForumMembershipEventListener.class);
  
  public static final String PLATFORM_ADMIN_GROUP = "/platform/administrators";

  @Override
  public void preSave(Membership m, boolean isNew) throws Exception {
    ForumServiceUtils.clearCache();
  }

  @Override
  public void preDelete(Membership m) throws Exception {
    ForumServiceUtils.clearCache();
  }
  
  @Override
  public void postSave(Membership m, boolean isNew) throws Exception {
    if (PLATFORM_ADMIN_GROUP.equals(m.getGroupId())) {
      try {
        KSDataLocation dataLocation = CommonsUtils.getService(KSDataLocation.class);
        Node rootNode = dataLocation.getSessionManager().openSession().getRootNode();
        if (rootNode.hasNode(dataLocation.getUserProfilesLocation() + "/" + m.getUserName())) {
          Node userProfileNode = rootNode.getNode(dataLocation.getUserProfilesLocation()).getNode(m.getUserName());
          userProfileNode.setProperty(ForumNodeTypes.EXO_USER_ROLE, 0);
          userProfileNode.save();
        }
      } catch (Exception e) {
        LOG.error("Failed to update user role : " + e.getMessage(), e);
      }
    }
  }
  
}
