/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
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
package org.exoplatform.forum.webui;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.forum.ForumUtils;
import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.forum.common.webui.BaseEventListener;
import org.exoplatform.forum.common.webui.BaseUIForm;
import org.exoplatform.forum.service.ForumService;
import org.exoplatform.forum.service.Utils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.event.Event;

public abstract class BaseForumEventListener<T extends BaseUIForm> extends BaseEventListener<T> {
  
  protected ForumService   forumService;

  protected UIForumPortlet forumPortlet;
  
  protected WebuiRequestContext context;
  
  public BaseForumEventListener() {
  }
  
  protected boolean isExisting(String objectId) {
    Object object = null;
    try {
      if (objectId.indexOf(CommonUtils.SLASH) > 0) {
        object = forumService.getObjectNameByPath(objectId);
      } else {
        String type = Utils.getObjectType(objectId);
        object = forumService.getObjectNameById(objectId, type);
      }
    } catch (Exception e) {
      return false;
    }
    return (object != null) ? true : false;
  }
  
  protected void topicNotExist() throws Exception {
    warning("UIForumPortlet.msg.topicEmpty", false);
    context.addUIComponentToUpdateByAjax(component);
  }

  protected void forumNotExist(String categoryId) throws Exception {
    warning("UITopicContainer.msg.forum-deleted", false);
    openCategory(categoryId);
  }

  protected void openCategory(String categoryId) throws Exception {
    UICategoryContainer categoryContainer = forumPortlet.getChild(UICategoryContainer.class);
    forumPortlet.updateIsRendered(ForumUtils.CATEGORIES);
    categoryContainer.updateIsRender(false);
    categoryContainer.getChild(UICategory.class).updateByBreadcumbs(categoryId);
    forumPortlet.getChild(UIBreadcumbs.class).setUpdataPath(categoryId);
    context.addUIComponentToUpdateByAjax(forumPortlet);
  }

  protected void forumNotExist() throws Exception {
    notExist("UITopicContainer.msg.forum-deleted");
  }

  protected void categoryNotExist() throws Exception {
    notExist("UIForumPortlet.msg.catagory-deleted");
  }

  private void notExist(String msg) throws Exception {
    warning(msg, false);
    forumPortlet.renderForumHome();
    context.addUIComponentToUpdateByAjax(forumPortlet);
  }
  

  public final void execute(Event<T> event) throws Exception {
    if (forumService == null) {
      forumService = CommonsUtils.getService(ForumService.class);
    }
    this.component = event.getSource();
    this.forumPortlet = this.component.getAncestorOfType(UIForumPortlet.class);
    this.context = event.getRequestContext();
    //
    String objectId = this.context.getRequestParameter(UIComponent.OBJECTID);
    if (isValid(component, objectId)) {
      try {
        onEvent(event, component, objectId);
      } catch (Exception e) {
        notExist("UIForumPortlet.msg.do-not-permission");
        ExoLogger.getLogger(component.getClass()).error("Failed to run action " + getClass().getName(), e);
      }
    } else {
      errorEvent();
    }
  }
  
  public abstract boolean isValid(T component, String objectId) throws Exception ;
  public abstract void errorEvent() throws Exception ;
}
