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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.exoplatform.forum.ForumUtils;
import org.exoplatform.forum.common.webui.WebUIUtils;
import org.exoplatform.forum.service.Post;
import org.exoplatform.forum.webui.BaseDataForm;
import org.exoplatform.forum.webui.BaseForumEventListener;
import org.exoplatform.forum.webui.UIForumContainer;
import org.exoplatform.forum.webui.UIForumDescription;
import org.exoplatform.forum.webui.UIForumPortlet;
import org.exoplatform.forum.webui.UITopicDetail;
import org.exoplatform.forum.webui.UITopicDetailContainer;
import org.exoplatform.forum.webui.UITopicPoll;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;

@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "app:/templates/forum/webui/popup/UIMovePostForm.gtmpl",
    events = {
      @EventConfig(listeners = UIMovePostForm.SaveActionListener.class), 
      @EventConfig(listeners = UIMovePostForm.CancelActionListener.class,phase = Phase.DECODE)
    }
)
public class UIMovePostForm extends BaseDataForm implements UIPopupComponent {
  private List<Post> posts;

  public UIMovePostForm() throws Exception {
  }

  public void activate() {
  }

  public void deActivate() {
  }

  public void updatePost(String topicId, List<Post> posts) {
    this.topicId = topicId;
    this.posts = posts;
    this.isMovePost = true;
    this.pathPost = posts.get(0).getPath();
  }

  protected boolean getSelectForum(String forumId) throws Exception {
    if (this.posts.get(0).getPath().contains(forumId)) {
      return true;
    } else {
      return false;
    }
  }
  
  static public class SaveActionListener extends BaseForumEventListener<UIMovePostForm> {
    @Override
    public boolean isValid(UIMovePostForm component, String objectId) throws Exception {
      if (ForumUtils.isEmpty(objectId)) {
        return false;
      }
      if (isExisting(objectId) == false) {
        return false;
      }
      return true;
    }

    @Override
    public void errorEvent() throws Exception {
      warning("UIForumPortlet.msg.topicEmpty", false);
      context.addUIComponentToUpdateByAjax(component);
    }

    @Override
    protected void onError(Throwable e) throws Exception {
      forumPortlet.cancelAction();
      notExist("UIMovePostForm.msg.parent-deleted");
      component.log.error("Failed to move topic(s) to forum ", e);
    }

    public void onEvent(Event<UIMovePostForm> event, UIMovePostForm uiForm, final String topicPath) throws Exception {
      String[] temp = topicPath.split(ForumUtils.SLASH);
      // set link
      String link = ForumUtils.createdForumLink(ForumUtils.TOPIC, "pathId", false);
      //
      Collections.sort(uiForm.posts, new ForumUtils.DatetimeComparatorDESC());
      List<String> postPaths = new ArrayList<String>(uiForm.posts.size());
      for (Post p : uiForm.posts) {
        postPaths.add(p.getPath());
      }
      uiForm.getForumService().movePost(postPaths.toArray(new String[postPaths.size()]), topicPath, false, WebUIUtils.getLabel(null, "UINotificationForm.label.EmailToAuthorMoved"), link);
      forumPortlet.cancelAction();
      UIForumContainer forumContainer = forumPortlet.findFirstComponentOfType(UIForumContainer.class);
      UITopicDetailContainer topicDetailContainer = forumContainer.getChild(UITopicDetailContainer.class);
      topicDetailContainer.getChild(UITopicDetail.class).setUpdateTopic(temp[temp.length - 3], temp[temp.length - 2], temp[temp.length - 1]);
      topicDetailContainer.getChild(UITopicPoll.class).updateFormPoll(temp[temp.length - 3], temp[temp.length - 2], temp[temp.length - 1]);
      UIForumDescription forumDescription = forumContainer.getChild(UIForumDescription.class);
      forumDescription.setForumId(temp[temp.length - 3], temp[temp.length - 2]);
      context.addUIComponentToUpdateByAjax(forumPortlet);
    }
  }

  static public class CancelActionListener extends EventListener<UIMovePostForm> {
    public void execute(Event<UIMovePostForm> event) throws Exception {
      UIMovePostForm uiForm = event.getSource();
      UIForumPortlet forumPortlet = uiForm.getAncestorOfType(UIForumPortlet.class);
      forumPortlet.cancelAction();
    }
  }
}
