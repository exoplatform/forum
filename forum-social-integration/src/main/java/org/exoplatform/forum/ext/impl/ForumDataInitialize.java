/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
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
package org.exoplatform.forum.ext.impl;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.forum.service.Category;
import org.exoplatform.forum.service.DataStorage;
import org.exoplatform.forum.service.Forum;
import org.exoplatform.forum.service.ForumService;
import org.exoplatform.forum.service.Utils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.social.core.space.SpaceListenerPlugin;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.impl.SpaceServiceImpl;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceLifeCycleEvent;
import org.jboss.util.Strings;

import static org.exoplatform.forum.service.Utils.CATEGORY_SPACE_ID_PREFIX;
import static org.exoplatform.forum.service.Utils.FORUM_SPACE_ID_PREFIX;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jul 7, 2010  
 */
public class ForumDataInitialize extends SpaceListenerPlugin {

  private static final Log LOG = ExoLogger.getLogger(ForumDataInitialize.class);
  
  private final InitParams params;

  private ForumService forumService;
  
  public ForumDataInitialize(InitParams params, ForumService forumService) {
    this.params = params;
    this.forumService = forumService;
  }
  
  @Override
  public void applicationActivated(SpaceLifeCycleEvent event) {

  }

  @Override
  public void applicationAdded(SpaceLifeCycleEvent event) {
    String portletName = "";
    
    if (params.getValueParam("portletName") != null)
      portletName = params.getValueParam("portletName").getValue();
    else if (LOG.isDebugEnabled()) {
      LOG.debug("Initparam is not configured for portletName property");
    }
   
    if (!portletName.equals(event.getSource())) {
      /*
       * this function is called only if Forum Portlet is added to Social Space. 
       * Hence, if the application added to space do not have the name as configured, we will be out now.
       */
      return;
    }
      DataStorage storage = CommonsUtils.getService(DataStorage.class);
    Space space = event.getSpace();
    String parentGrId = "";
    try {
        OrganizationService service = CommonsUtils.getService(OrganizationService.class);
      Group group = service.getGroupHandler().findGroupById(space.getGroupId());
      parentGrId = group.getParentId();

      String categorySpId = Utils.CATEGORY + parentGrId.replaceAll(CommonUtils.SLASH, CommonUtils.EMPTY_STR);
      Category category = storage.getCategory(categorySpId);
      if (category == null) {
        category = new Category(categorySpId);
        category.setCategoryName(SpaceUtils.SPACE_GROUP.replace(CommonUtils.SLASH, CommonUtils.EMPTY_STR));
        category.setOwner(space.getManagers()[0]);
        category.setCategoryOrder(100l);
        category.setUserPrivate(new String[]{""});
        category.setDescription("All forums from spaces");
        storage.saveCategory(category, true);
      }
      String forumId = FORUM_SPACE_ID_PREFIX + group.getGroupName();
        String groupId = space.getGroupId();
      if (storage.getForum(categorySpId, forumId) == null) {
          String[] roles = new String[] { groupId };

        String[] moderators = new String[] { new StringBuilder(SpaceServiceImpl.MANAGER).append(CommonUtils.COLON)
                                              .append(groupId).toString() };
        Forum forum = new Forum();
        forum.setOwner(space.getManagers()[0]);
        forum.setId(forumId);
        forum.setForumName(space.getDisplayName());
        forum.setDescription(space.getDescription() == null ? Strings.EMPTY : space.getDescription());
        forum.setModerators(moderators);
        forum.setCreateTopicRole(roles);
        forum.setPoster(roles);
        forum.setViewer(roles);
        storage.saveForum(categorySpId, forum, true);
        storage.saveUserPrivateOfCategory(categorySpId, groupId);
      }
    } catch (Exception e) {
      if(LOG.isDebugEnabled()) {
        LOG.debug("Failed to add forum space. " + e.getMessage());
      }
    }
  }

  @Override
  public void applicationDeactivated(SpaceLifeCycleEvent event) {
  }

  @Override
  public void applicationRemoved(SpaceLifeCycleEvent event) {

  }

  @Override
  public void grantedLead(SpaceLifeCycleEvent event) {

  }

  @Override
  public void joined(SpaceLifeCycleEvent event) {

  }

  @Override
  public void left(SpaceLifeCycleEvent event) {

  }

  @Override
  public void revokedLead(SpaceLifeCycleEvent event) {

  }

  @Override
  public void spaceCreated(SpaceLifeCycleEvent event) {

  }

  @Override
  public void spaceRemoved(SpaceLifeCycleEvent event) {

  }

  @Override
  public void spaceRenamed(SpaceLifeCycleEvent event) {
    Space space = event.getSpace();
    String groupName = space.getGroupId().replace(SpaceUtils.SPACE_GROUP+"/","");
    Forum forum = forumService.getForum(CATEGORY_SPACE_ID_PREFIX,FORUM_SPACE_ID_PREFIX + groupName);
    if(forum != null) {
      forum.setForumName(space.getDisplayName());
      try {
        forumService.saveForum(CATEGORY_SPACE_ID_PREFIX, forum, false);
      } catch (Exception e) {
        LOG.error("Can not rename forum of space {}", space, e);
      }
    }
  }

  @Override
  public void spaceDescriptionEdited(SpaceLifeCycleEvent event) {}

  @Override
  public void spaceAvatarEdited(SpaceLifeCycleEvent event) {}

  @Override
  public void spaceAccessEdited(SpaceLifeCycleEvent event) {
    
  }

  @Override
  public void addInvitedUser(SpaceLifeCycleEvent event) {
  }

  @Override
  public void addPendingUser(SpaceLifeCycleEvent event) {
  }

  @Override
  public void spaceBannerEdited(SpaceLifeCycleEvent event) {

  }

}
