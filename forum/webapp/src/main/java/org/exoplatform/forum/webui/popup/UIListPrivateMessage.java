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
package org.exoplatform.forum.webui.popup;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.forum.ForumUtils;
import org.exoplatform.forum.common.webui.WebUIUtils;
import org.exoplatform.forum.service.ForumPrivateMessage;
import org.exoplatform.forum.service.ForumService;
import org.exoplatform.forum.service.JCRPageList;
import org.exoplatform.forum.service.UserProfile;
import org.exoplatform.forum.service.Utils;
import org.exoplatform.forum.webui.UIForumActionBar;
import org.exoplatform.forum.webui.UIForumPageIterator;
import org.exoplatform.forum.webui.UIForumPortlet;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.organization.UserStatus;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.gatein.common.text.EntityEncoder;

@ComponentConfig(lifecycle = UIContainerLifecycle.class)
public class UIListPrivateMessage extends UIContainer {
  protected ForumService              forumService;
  
  protected UserHandler               userHandler;

  protected UserProfile               userProfile      = null;

  protected List<ForumPrivateMessage> privateMessages  = null;

  protected String                    userName         = ForumUtils.EMPTY_STR;

  protected boolean                  isRenderIterator = false;

  protected String                    messageType      = Utils.SEND_MESSAGE;

  protected String                    deletedUserFullName;
  
  public UIListPrivateMessage() throws Exception {
    forumService = (ForumService) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(ForumService.class);
    OrganizationService organizationService = (OrganizationService) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(OrganizationService.class);
    Locale locale = Util.getPortalRequestContext().getLocale();
    ResourceBundle rs = ResourceBundle.getBundle("locale/portlet/forum/ForumPortlet", locale);
    deletedUserFullName = EntityEncoder.FULL.encode(rs.getString("UIPrivateMessageForm.label.DeletedUser"));
    userHandler = organizationService.getUserHandler();
    addChild(UIForumPageIterator.class, null, null);
    addChild(UIViewPrivateMessage.class, null, null).setRendered(false);
  }

  public String getUserFullNameById(String userId) throws Exception {
    User user = userHandler.findUserByName(userId, UserStatus.ANY);
    String userFullName;
    if (user != null) {
      userFullName = user.getFullName();
    } else {
      userFullName = deletedUserFullName;
    }
    return userFullName;
  }
  
  public String getMessageType() {
    return messageType;
  }

  public void setMessageType(String messageType) {
    this.messageType = messageType;
  }

  protected UserProfile getUserProfile() throws Exception {
    if (userProfile == null) {
      userProfile = getAncestorOfType(UIForumPortlet.class).getUserProfile();
    }
    userName = userProfile.getUserId();
    return userProfile;
  }

  protected boolean isRenderIterator() {
    return isRenderIterator;
  }

  protected List<ForumPrivateMessage> getPrivateMessages() throws Exception {
    JCRPageList pageList = this.forumService.getPrivateMessage(userName, messageType);
    UIForumPageIterator forumPageIterator = getChild(UIForumPageIterator.class);
    forumPageIterator.initPage(pageList.getPageSize(), pageList.getCurrentPage(), 
                               pageList.getAvailable(), pageList.getAvailablePage());
    if (pageList != null) {
      pageList.setPageSize(10);
      int page = forumPageIterator.getPageSelected();
      privateMessages = pageList.getPage(page);
      if (pageList.getAvailable() > 10) {
        isRenderIterator = true;
      }
    }
    return privateMessages;
  }

  protected ForumPrivateMessage getPrivateMessage(String id) throws Exception {
    for (ForumPrivateMessage forumPrivateMessage : privateMessages) {
      if (forumPrivateMessage.getId().equals(id)) {
        return forumPrivateMessage;
      }
    }
    return null;
  }

  static public class ViewMessageActionListener extends EventListener<UIListPrivateMessage> {
    public void execute(Event<UIListPrivateMessage> event) throws Exception {
      UIListPrivateMessage uicontainer = event.getSource();
      String objctId = event.getRequestContext().getRequestParameter(OBJECTID);
      if (!ForumUtils.isEmpty(objctId)) {
        try {
          uicontainer.forumService.saveReadMessage(objctId, uicontainer.userName, uicontainer.messageType);
          ForumPrivateMessage privateMessage = uicontainer.getPrivateMessage(objctId);
          UIViewPrivateMessage viewPrivateMessage = uicontainer.getChild(UIViewPrivateMessage.class);
          viewPrivateMessage.setPrivateMessage(privateMessage);
          viewPrivateMessage.setRendered(true);
          WebUIUtils.addScripts("ForumUtils", "forumUtils", "forumUtils.initTooltip('" + uicontainer.getId() + "');");
          WebUIUtils.addScripts("UIForumPortlet", "forumPortlet", "forumPortlet.initConfirm('" + uicontainer.getId() + "');");
        } catch (Exception e) {
          event.getRequestContext().getUIApplication()
               .addMessage(new ApplicationMessage("UIListInBoxPrivateMessage.msg.fail-view", null, ApplicationMessage.WARNING));
        }
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uicontainer.getAncestorOfType(UIForumPortlet.class)
                                                                        .getChild(UIForumActionBar.class));
      event.getRequestContext().addUIComponentToUpdateByAjax(uicontainer);
    }
  }

  static public class DeleteMessageActionListener extends EventListener<UIListPrivateMessage> {
    public void execute(Event<UIListPrivateMessage> event) throws Exception {
      UIListPrivateMessage uicontainer = event.getSource();
      String objctId = event.getRequestContext().getRequestParameter(OBJECTID);
      if (!ForumUtils.isEmpty(objctId)) {
        uicontainer.forumService.removePrivateMessage(objctId, uicontainer.userName, uicontainer.messageType);
        uicontainer.getChild(UIViewPrivateMessage.class).reset();
        WebUIUtils.addScripts("ForumUtils", "forumUtils", "forumUtils.initTooltip('" + uicontainer.getId() + "');");
        WebUIUtils.addScripts("UIForumPortlet", "forumPortlet", "forumPortlet.initConfirm('" + uicontainer.getId() + "');");
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(uicontainer);
    }
  }
}
