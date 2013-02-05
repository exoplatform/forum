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
package org.exoplatform.forum.create;

import org.exoplatform.forum.common.webui.UIForumFilter;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SAS
 * Author : Vu Duy Tu
 *          tuvd@exoplatform.com
 * Jan 2, 2013  
 */

@ComponentConfig(lifecycle = UIFormLifecycle.class, 
  template = "classpath:groovy/webui/forum/create/UICreate.gtmpl", 
  events = {
    @EventConfig(listeners = UICreateTopic.NextActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UICreateTopic.OnChangeLocalActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UICreateForm.CancelActionListener.class, phase = Phase.DECODE) 
  }
)
public class UICreateTopic extends UICreateForm {

  
  public UICreateTopic() throws Exception {

  }
  
  static public class NextActionListener extends EventListener<UICreateTopic> {

    public void execute(Event<UICreateTopic> event) throws Exception {
      UICreateTopic createTopic = event.getSource();
      nextAction(createTopic, ACTION_TYPE.CREATE_TOPIC, event.getRequestContext());
    }
  }
  
  static public class OnChangeLocalActionListener extends EventListener<UICreateTopic> {

    public void execute(Event<UICreateTopic> event) throws Exception {
      UICreateTopic createTopic = event.getSource();
      String location = createTopic.getUIFormScrollSelectBox(LOCALTION_SELEXT_BOX).getValue();
      createTopic.isStepOne = createTopic.currentIntranet.equals(location);
      if(createTopic.isStepOne) {
        nextAction(createTopic, ACTION_TYPE.CREATE_TOPIC, event.getRequestContext());
      } else {
        UIForumFilter forumFilter = createTopic.getUIForumFilter(FORUM_SELEXT_BOX);
        if(forumFilter != null){
          forumFilter.setRendered(false);
        }
        createTopic.hasNext = true;
        event.getRequestContext().addUIComponentToUpdateByAjax(createTopic);
      }
    }
  }

}
