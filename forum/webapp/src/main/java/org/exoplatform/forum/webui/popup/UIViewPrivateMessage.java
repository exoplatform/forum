/***************************************************************************
 * Copyright (C) 2003-2008 eXo Platform SAS.
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
package org.exoplatform.forum.webui.popup;

import org.exoplatform.forum.rendering.RenderHelper;
import org.exoplatform.forum.rendering.RenderingException;
import org.exoplatform.forum.service.ForumPrivateMessage;
import org.exoplatform.forum.service.Post;
import org.exoplatform.forum.service.UserProfile;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;

@ComponentConfig(
    template = "app:/templates/forum/webui/popup/UIViewPrivateMessage.gtmpl",
    events = {
      @EventConfig(listeners = UIViewPrivateMessage.CloseActionListener.class)
    }
)
public class UIViewPrivateMessage extends UIContainer {
  private ForumPrivateMessage privateMessage;

  private UserProfile         userProfile;

  RenderHelper                renderHelper = new RenderHelper();

  public UIViewPrivateMessage() {
  }

  public ForumPrivateMessage getPrivateMessage() {
    return privateMessage;
  }

  public void setPrivateMessage(ForumPrivateMessage privateMessage) {
    this.privateMessage = privateMessage;
  }

  public String renderMessage(String str) throws RenderingException {
    Post post = new Post();
    post.setMessage(str);
    return renderHelper.renderPost(post);
  }

  public UserProfile getUserProfile() {
    return userProfile;
  }

  public void setUserProfile(UserProfile userProfile) {
    this.userProfile = userProfile;
  }

  public void activate() {
  }

  public void deActivate() {
  }
  
  public void reset() {
    setRendered(false);
    privateMessage = null;
  }
  
  protected boolean isListSendPrivateMessage() {
    return (this.getParent() instanceof UIListSentPrivateMessage) ? true : false;
  }

  protected String eventParent(String action, String id) throws Exception {
    return getParent().event(action, id);
  }
  
  static public class CloseActionListener extends EventListener<UIViewPrivateMessage> {
    public void execute(Event<UIViewPrivateMessage> event) throws Exception {
      UIViewPrivateMessage uiForm = event.getSource();
      uiForm.reset();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getParent());
    }
  }
}
