/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.forum.ext.activity;

import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.processor.I18NActivityUtils;

/**
 * Created by The eXo Platform SAS Author : thanh_vucong
 * thanh_vucong@exoplatform.com Jan 9, 2013
 */
public enum ForumActivityType {

  ADD_TOPIC("forum.add-topic", "%s"),
  UPDATE_TOPIC_TITLE("forum.update-topic-title", "Title has been updated to: %s"),
  UPDATE_TOPIC_CONTENT("forum.update-topic-content", "Content has been edited."),
  UPDATE_TOPIC_RATE("forum.update-topic-rate", "Rated the topic: %s"),
  CLOSE_TOPIC("forum.closed-topic", "Topic has been closed."),
  OPEN_TOPIC("forum.opened-topic", "Topic has been opened."),
  LOCK_TOPIC("forum.locked-topic", "Topic has been locked."),
  UNLOCK_TOPIC("forum.unlocked-topic", "Topic has been unlocked."),
  APPROVED_TOPIC("forum.approved-topic", "Topic has been approved."),
  UNAPPROVED_TOPIC("forum.unapproved-topic", "Topic has been unapproved."),
  MERGE_TOPICS("forum.merge-topic", "%s"),
  SPLIT_TOPIC("forum.split-topic", "%s"),
  MOVE_TOPIC("forum.move-topic", "Topic have been moved to: %s>%s"),
  ADD_POST("forum.add-post", "%s"),
  UPDATE_POST("forum.update-post", "Edited his reply to: %s");

  private final String titleTemplate;
  private final String resourceBundleKey;

  private ForumActivityType(String resourceBundleKey, String titleTemplate) {
    this.titleTemplate = titleTemplate;
    this.resourceBundleKey = resourceBundleKey;
  }
  
  public String getTitle(ExoSocialActivity a, String value) {
    String got = titleTemplate;
    if (value != null) {
      got = String.format(titleTemplate, value);
    }
    I18NActivityUtils.addResourceKey(a, resourceBundleKey, value);
    
    return got;
  }
  
  public ExoSocialActivity getActivity(ExoSocialActivity a, String...values) {
    //
    a.setTitle(getTitle(a, values));
    return a;
  }
  
  public String getTitle(ExoSocialActivity a, String...values) {
    String got = titleTemplate;
    if (values != null && values.length > 0) {
      got = String.format(titleTemplate, values);
    }
    
    I18NActivityUtils.addResourceKey(a, resourceBundleKey, values);
    return got;
  }
  
  public String getTitleTemplate() {
    return titleTemplate;
  }
}
