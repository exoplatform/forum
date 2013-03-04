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

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.forum.service.Category;
import org.exoplatform.forum.service.Forum;
import org.exoplatform.forum.service.ForumService;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIContainer;

/**
 * Created by The eXo Platform SARL
 * Author : Hung Nguyen
 *          hung.nguyen@exoplatform.com
 * Aus 01, 2007 2:48:18 PM 
 */

@ComponentConfig(
    template = "app:/templates/forum/webui/UIForumDescription.gtmpl"
)
public class UIForumDescription extends UIContainer {
  private ForumService forumService;

  private String  forumId;

  private String  categoryId;

  private Category   category   = null;

  private Forum   forum   = null;

  private boolean isForum = false;

  private boolean hasUpdate = false;

  public UIForumDescription() throws Exception {
    forumService = (ForumService) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(ForumService.class);
  }

  public void setCategory(Category category) {
    this.category = category;
    this.hasUpdate = false;
  }

  public void setForum(Forum forum) {
    this.forum = forum;
    this.hasUpdate = false;
  }

  
  public void setCategoryId(String categoryId) {
    this.hasUpdate = true;
    this.categoryId = categoryId;
  }

  public void setForumId(String categoryId, String forumId) {
    this.hasUpdate = true;
    this.forumId = forumId;
    this.categoryId = categoryId;
  }

  public String getName() {
    return (isForum && getForum() != null) ? forum.getForumName() :
              ((!isForum && getCategory() != null) ? category.getCategoryName() : null);
  }

  public String getDescription() {
    return (isForum && getForum() != null) ? forum.getDescription() :
      ((!isForum && getCategory() != null) ? category.getDescription() : null);
  }

  private Category getCategory() {
    if (this.category == null || hasUpdate) {
      return forumService.getCategory(categoryId);
    } else {
      return this.category;
    }
  }

  private Forum getForum() {
    if (forum == null || hasUpdate) {
      return forumService.getForum(categoryId, forumId);
    } else {
      return this.forum;
    }
  }
}
