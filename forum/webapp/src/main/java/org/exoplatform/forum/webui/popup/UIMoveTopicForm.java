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
package org.exoplatform.forum.webui.popup;

import java.util.List;

import org.exoplatform.forum.ForumUtils;
import org.exoplatform.forum.common.webui.WebUIUtils;
import org.exoplatform.forum.service.Topic;
import org.exoplatform.forum.webui.BaseDataForm;
import org.exoplatform.forum.webui.BaseForumEventListener;
import org.exoplatform.forum.webui.UIForumContainer;
import org.exoplatform.forum.webui.UIForumDescription;
import org.exoplatform.forum.webui.UIForumPortlet;
import org.exoplatform.forum.webui.UITopicContainer;
import org.exoplatform.forum.webui.UITopicDetail;
import org.exoplatform.forum.webui.UITopicDetailContainer;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;

@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "app:/templates/forum/webui/popup/UIMoveTopicForm.gtmpl",
    events = {
      @EventConfig(listeners = UIMoveTopicForm.SaveActionListener.class), 
      @EventConfig(listeners = UIMoveTopicForm.CancelActionListener.class,phase = Phase.DECODE)
    }
)
public class UIMoveTopicForm extends BaseDataForm implements UIPopupComponent {
  private List<Topic> topics;

  private boolean     isFormTopic = false;

  private boolean     isAdmin     = false;

  public boolean isAdmin() {
    return isAdmin;
  }

  public void setAdmin(boolean isAdmin) {
    this.isAdmin = isAdmin;
  }

  public UIMoveTopicForm() throws Exception {
  }

  public void activate() {
  }

  public void deActivate() {
  }

  public void updateTopic(String forumId, List<Topic> topics, boolean isFormTopic) throws Exception {
    this.forumId = forumId;
    this.topics = topics;
    this.isMoveTopic = true;
    this.pathTopic = topics.get(0).getPath();
    this.topics.get(0).setEditReason(getUserProfile().getUserId());
    this.isFormTopic = isFormTopic;
  }

  protected boolean getSelectCate(String cateId) throws Exception {
    if (this.topics.get(0).getPath().contains(cateId)) {
      return true;
    } else {
      return false;
    }
  }

  static public class SaveActionListener extends BaseForumEventListener<UIMoveTopicForm> {
    @Override
    public boolean isValid(UIMoveTopicForm component, String objectId) throws Exception {
      if (ForumUtils.isEmpty(objectId)){
        return false;
      }
      if(isExisting(objectId) == false) {
        return false;
      }
      return true;
    }

    @Override
    public void errorEvent() throws Exception {
      warning("UIMoveTopicForm.msg.parent-deleted", false);
      context.addUIComponentToUpdateByAjax(component);
    }

    @Override
    protected void onError(Throwable e) throws Exception {
      forumPortlet.cancelAction();
      notExist("UIForumPortlet.msg.topicEmpty");
      component.log.error("Failed to move post(s) to topic ", e);
    }

    public void onEvent(Event<UIMoveTopicForm> event, UIMoveTopicForm uiForm, final String forumPath) throws Exception {
      // set link
      String link = ForumUtils.createdForumLink(ForumUtils.TOPIC, "pathId", false);
      //
      forumService.moveTopic(uiForm.topics, forumPath, WebUIUtils.getLabel(null, "UINotificationForm.label.EmailToAuthorMoved"), link);
      forumPortlet.removeCacheUserProfile();
      forumPortlet.cancelAction();
      if (uiForm.isFormTopic) {
        UIForumContainer forumContainer = forumPortlet.findFirstComponentOfType(UIForumContainer.class);
        UITopicDetailContainer topicDetailContainer = forumContainer.getChild(UITopicDetailContainer.class);
        forumContainer.setIsRenderChild(false);
        String[] strs = forumPath.split(ForumUtils.SLASH);
        String catgoryId = strs[strs.length - 2], forumId = strs[strs.length - 1];
        UIForumDescription forumDescription = forumContainer.getChild(UIForumDescription.class);
        forumDescription.setForumId(catgoryId, forumId);
        UITopicDetail topicDetail = topicDetailContainer.getChild(UITopicDetail.class);
        topicDetail.setUpdateForum(uiForm.getForumService().getForum(catgoryId, forumId));
        topicDetail.setUpdateTopic(catgoryId, forumId, uiForm.topics.get(0).getId());
        context.addUIComponentToUpdateByAjax(forumPortlet);
      } else {
        UITopicContainer topicContainer = forumPortlet.findFirstComponentOfType(UITopicContainer.class);
        context.addUIComponentToUpdateByAjax(topicContainer);
      }
    }
  }

  static public class CancelActionListener extends EventListener<UIMoveTopicForm> {
    public void execute(Event<UIMoveTopicForm> event) throws Exception {
      UIMoveTopicForm uiForm = event.getSource();
      UIForumPortlet forumPortlet = uiForm.getAncestorOfType(UIForumPortlet.class);
      forumPortlet.cancelAction();
    }
  }
}
