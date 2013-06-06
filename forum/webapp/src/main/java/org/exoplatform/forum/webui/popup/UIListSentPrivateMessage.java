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

import org.exoplatform.forum.service.ForumPrivateMessage;
import org.exoplatform.forum.service.Utils;
import org.exoplatform.forum.webui.UIForumPageIterator;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

@ComponentConfig(
    template = "app:/templates/forum/webui/popup/UIListSentPrivateMessage.gtmpl",
    events = {
      @EventConfig(listeners = UIListSentPrivateMessage.ViewMessageActionListener.class),
      @EventConfig(listeners = UIListSentPrivateMessage.DeleteMessageActionListener.class,confirm="UIPrivateMessageForm.confirm.Delete-message"),
      @EventConfig(listeners = UIListSentPrivateMessage.ForwardMessageActionListener.class)
    }
)
public class UIListSentPrivateMessage extends UIListPrivateMessage {

  public UIListSentPrivateMessage() throws Exception {
    super();
    getChild(UIForumPageIterator.class).setId("PageListSentMessage");
    getChild(UIViewPrivateMessage.class).setId("UIViewPrivateMessageInSend");
    setMessageType(Utils.SEND_MESSAGE);
  }

  static public class ForwardMessageActionListener extends EventListener<UIListSentPrivateMessage> {
    public void execute(Event<UIListSentPrivateMessage> event) throws Exception {
      UIListSentPrivateMessage uicontainer = event.getSource();
      String objctId = event.getRequestContext().getRequestParameter(OBJECTID);
      ForumPrivateMessage privateMessage = uicontainer.getPrivateMessage(objctId);
      UIPrivateMessageForm privateMessageForm = uicontainer.getParent();
      privateMessageForm.setUpdate(privateMessage, false);
      event.getRequestContext().addUIComponentToUpdateByAjax(privateMessageForm);
    }
  }
}
