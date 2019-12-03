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

import org.exoplatform.commons.utils.PropertyChangeSupport;
import org.exoplatform.forum.service.Post;
import org.exoplatform.forum.service.Topic;

/**
 * Created by The eXo Platform SAS
 * Author : thanh_vucong
 *          thanh_vucong@exoplatform.com
 * Jan 9, 2013  
 */
public class ForumActivityContext {

  private String categoryId;
  private String forumId;
  private String topicId;
  
  private Topic topic;
  private Post post;

  /**org.exoplatform.forum.service.Utils.CLOSE
  * org.exoplatform.forum.service.Utils.LOCK
  * org.exoplatform.forum.service.Utils.APPROVE
  * org.exoplatform.forum.service.Utils.STICKY
  * org.exoplatform.forum.service.Utils.WAITING
  * org.exoplatform.forum.service.Utils.ACTIVE
  * org.exoplatform.forum.service.Utils.CHANGE_NAME => change Topic title
  * org.exoplatform.forum.service.Utils.VOTE_RATING
  * org.exoplatform.forum.service.Utils.HIDDEN
  * */
  private int updateType;
  
  private String toCategoryName;
  private String toForumName;
  private String[] removeActivities;
  
  private Topic splitedTopic;
  
  private PropertyChangeSupport pcs;
  
  /** create ForumActivityContext for create new topic case*/
  public static ForumActivityContext makeContextForAddTopic(Topic topic) {
    ForumActivityContext ctx = new ForumActivityContext();
    ctx.topic = topic;
    ctx.categoryId = topic.getCategoryId();
    ctx.forumId = topic.getForumId();
    
    return ctx;
  }
  
  /** create ForumActivityContext for update content of topic case*/
  public static ForumActivityContext makeContextForUpdateTopic(Topic topic) {
    ForumActivityContext ctx = new ForumActivityContext();
    ctx.topic = topic;
    return ctx;
  }
  
  /** create ForumActivityContext for update type of topic case*/
  public static ForumActivityContext makeContextForUpdateStatusTopic(Topic topic, int updateType) {
    ForumActivityContext ctx = new ForumActivityContext();
    ctx.topic = topic;
    ctx.updateType = updateType;
    return ctx;
  }
  
  /** create ForumActivityContext for move topic case*/
  public static ForumActivityContext makeContextForMoveTopic(Topic topic, String toCategoryName, String toForumName) {
    ForumActivityContext ctx = new ForumActivityContext();
    ctx.topic = topic;
    ctx.toCategoryName = toCategoryName;
    ctx.toForumName = toForumName;
    return ctx;
  }
  
  /** create ForumActivityContext for merge topic case*/
  public static ForumActivityContext makeContextForMergeTopic(Topic newTopic, String removeActivityId1, String removeActivityId2) {
    ForumActivityContext ctx = new ForumActivityContext();
    ctx.topic = newTopic;
    ctx.removeActivities = new String[]{removeActivityId1, removeActivityId2};
    return ctx;
  }
  
  /** create ForumActivityContext for merge topic case*/
  public static ForumActivityContext makeContextForSplitTopic(Topic newTopic, Topic splitedTopic, String removeActivityId) {
    ForumActivityContext ctx = new ForumActivityContext();
    ctx.topic = newTopic;
    ctx.splitedTopic = splitedTopic;
    ctx.removeActivities = new String[]{removeActivityId};
    return ctx;
  }
  
  /** create ForumActivityContext for add post case*/
  public static ForumActivityContext makeContextForAddPost(Post post) {
    ForumActivityContext ctx = new ForumActivityContext();
    ctx.post = post;
    return ctx;
  }
  
  /** create ForumActivityContext for add post case*/
  public static ForumActivityContext makeContextForUpdatePost(Post post) {
    ForumActivityContext ctx = new ForumActivityContext();
    ctx.post = post;
    return ctx;
  }
  
  public void setPost(Post post) {
    this.post = post;
  }

  public void setTopic(Topic topic) {
    this.topic = topic;
  }

  /**
   * Gets categoryId
   * @return
   */
  public String getCategoryId() {
    return categoryId;
  }
  /**
   * Gets forumId
   * @return
   */
  public String getForumId() {
    return forumId;
  }
  /**
   * Ges topicId
   * @return
   */
  public String getTopicId() {
    return topicId;
  }

  /**
   * Gets Topic
   * @return
   */
  public Topic getTopic() {
    return topic;
  }

  /**
   * Gets Post
   * @return
   */
  public Post getPost() {
    return post;
  }

  /**
   * Gets Update Type for updating topic
   * @return
   */
  public int getUpdateType() {
    return updateType;
  }

  /**
   * get toCategoryName for Move Topic
   * @return
   */
  public String getToCategoryName() {
    return toCategoryName;
  }

  public String getToForumName() {
    return toForumName;
  }

  /**
   * Gets ActivityId array which will be removed
   * @return
   */
  public String[] getRemoveActivities() {
    return removeActivities;
  }

  /**
   * Gets splitedTopic for split topic
   * @return
   */
  public Topic getSplitedTopic() {
    return splitedTopic;
  }

  public PropertyChangeSupport getPcs() {
    return pcs;
  }

  public void setPcs(PropertyChangeSupport pcs) {
    this.pcs = pcs;
  }
  
}
