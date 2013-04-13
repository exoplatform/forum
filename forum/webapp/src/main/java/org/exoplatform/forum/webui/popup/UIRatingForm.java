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
import java.util.List;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.forum.ForumUtils;
import org.exoplatform.forum.common.UserHelper;
import org.exoplatform.forum.common.webui.BaseEventListener;
import org.exoplatform.forum.service.ForumService;
import org.exoplatform.forum.service.Topic;
import org.exoplatform.forum.service.Utils;
import org.exoplatform.forum.webui.BaseForumForm;
import org.exoplatform.forum.webui.UIForumPortlet;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;

@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "app:/templates/forum/webui/popup/UIRatingForm.gtmpl",
    events = {
      @EventConfig(listeners = UIRatingForm.VoteTopicActionListener.class), 
      @EventConfig(listeners = UIRatingForm.CloseActionListener.class, phase = Phase.DECODE)
    }
)
public class UIRatingForm extends BaseForumForm implements UIPopupComponent {
  private Topic  topic;
  
  private boolean isAlreadyVoted = false;
  
  private double userRateValue = 0.0;

  public UIRatingForm() throws Exception {
  }

  public void updateRating(Topic topic, boolean isAlreadyVoted, double userRateValue) {
    this.topic = topic;
    this.isAlreadyVoted = isAlreadyVoted;
    this.userRateValue = userRateValue;
  }
  
  public boolean isAlreadyVoted() {
    return isAlreadyVoted;
  }
  
  public double getUserRateValue() {
    return userRateValue;
  }
  
  public String[] getStarNumber() throws Exception {
    return ForumUtils.getStarNumber(userRateValue);
  }

  public void activate() {
  }

  public void deActivate() {
  }

  static public class VoteTopicActionListener extends BaseEventListener<UIRatingForm> {
    public void onEvent(Event<UIRatingForm> event, UIRatingForm uiForm, final String vote) throws Exception {
      Topic topic = uiForm.topic;
      String userName = UserHelper.getCurrentUser();
      String[] Vote = topic.getUserVoteRating();
      int k = Vote.length;
      Double voteRating = topic.getVoteRating();
      voteRating = (voteRating * k + Integer.parseInt(vote)) / (k + 1);
      String[] temp = new String[k + 1];
      System.arraycopy(Vote, 0, temp, 0, k);
      temp[k] = userName+":"+vote;
      topic.setVoteRating(voteRating);
      topic.setUserVoteRating(temp);
      ForumService forumService = (ForumService) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(ForumService.class);
      UIForumPortlet forumPortlet = uiForm.getAncestorOfType(UIForumPortlet.class);
      List<Topic> topics = new ArrayList<Topic>();
      topics.add(topic);
      forumService.modifyTopic(topics, Utils.VOTE_RATING);
      forumPortlet.cancelAction();
    }
  }

  static public class CloseActionListener extends EventListener<UIRatingForm> {
    public void execute(Event<UIRatingForm> event) throws Exception {
      UIForumPortlet forumPortlet = event.getSource().getAncestorOfType(UIForumPortlet.class);
      forumPortlet.cancelAction();
    }
  }
}
