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
    template = "app:/templates/forum/webui/popup/UIListInBoxPrivateMessage.gtmpl",
    events = {
      @EventConfig(listeners = UIListInBoxPrivateMessage.ViewMessageActionListener.class),
      @EventConfig(listeners = UIListInBoxPrivateMessage.DeleteMessageActionListener.class,confirm="UIPrivateMessageForm.confirm.Delete-message"),
      @EventConfig(listeners = UIListInBoxPrivateMessage.ReplyMessageActionListener.class)
    }
)
public class UIListInBoxPrivateMessage extends UIListPrivateMessage {

  public UIListInBoxPrivateMessage() throws Exception {
    super();
    getChild(UIForumPageIterator.class).setId("PageListInBoxMessage");
    getChild(UIViewPrivateMessage.class).setId("UIViewPrivateMessageInBox");
    setMessageType(Utils.RECEIVE_MESSAGE);
  }

  static public class ReplyMessageActionListener extends EventListener<UIListInBoxPrivateMessage> {
    public void execute(Event<UIListInBoxPrivateMessage> event) throws Exception {
      UIListInBoxPrivateMessage uicontainer = event.getSource();
      String objctId = event.getRequestContext().getRequestParameter(OBJECTID);
      ForumPrivateMessage privateMessage = uicontainer.getPrivateMessage(objctId);
      UIPrivateMessageForm privateMessageForm = uicontainer.getParent();
      privateMessageForm.setUpdate(privateMessage, true);
      event.getRequestContext().addUIComponentToUpdateByAjax(privateMessageForm);
    }
  }
}
