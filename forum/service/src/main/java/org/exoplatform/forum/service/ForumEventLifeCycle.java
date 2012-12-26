/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
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
package org.exoplatform.forum.service;


/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jul 15, 2010  
 */
public interface ForumEventLifeCycle {
  /**
   * This will be call after save forum category
   * @param category
   */

  public void saveCategory(Category category);

  /**
   * This will be call after save forum
   * @param forum
   */
  public void saveForum(Forum forum);

  /**
   * This will be call after add topic
   * @param topic
   * @param forumId
   */
  public void addTopic(Topic topic, String categoryId, String forumId);

  /**
   * @deprecated use {@link #updatePost(Post post)}
   * Will remove in 4.0.1
   */
  @Deprecated
  public void updateTopic(Topic topic, String categoryId, String forumId);
  /**
   * This will be call after update topic
   * @param topic
   * @param forumId
   * @since 4.0
   */
  public void updateTopic(Topic topic);
  
  
  /**
   * This will be call after status of topic changed.
   *  
   * @param topic
   * @param type - type of status changed
   * It set by Utils.CLOSE, Utils.LOCK, Utils.APPROVE, Utils.STICKY, Utils.WAITING
   * Utils.ACTIVE, Utils.CHANGE_NAME, Utils.VOTE_RATING
   * @since 4.0
   */
  public void updateStatusTopic(Topic topic, int type);
  
  /**
   * This will be call after moved topic to other forum.
   *    + Add new comment of activity's topic
   *      width message has content Category Name > Forum Name 
   * 
   * @param topic
   * @param toCategoryName
   * @param toForumName
   * @since 4.0
   */
  public void moveTopic(Topic topic, String toCategoryName, String toForumName);

  /**
   * This will be call after merge two topics into new topic.
   *   + Activity is removed from the activity stream. 
   *   + Make new activity for 2 topics merged is created.
   * 
   * @param newTopic - the new topic merged.
   * @param removeActivityId1 - the activityId of source topic.
   * @param removeActivityId2 - the activityId of destination topic.
   * @since 4.0
   */
  public void mergeTopic(Topic newTopic, String removeActivityId1, String removeActivityId2);

  /**
   * This will be call after split one topic to two topics.
   *    + Activity is removed from the activity stream. 
   *    + Two new activities are created for two topics created with the splitting.
   * 
   * @param newTopic - the new topic make by split.
   * @param splitedTopic - the source topic that split. 
   * @param removeActivityId - the old activity's ID of source topic.
   * @since 4.0
   */
  public void splitTopic(Topic newTopic, Topic splitedTopic, String removeActivityId);
  
  /**
   * This will be call after save post
   * 
   * @param post
   * @param categoryId
   * @param forumId
   * @param topicId
   */
  public void addPost(Post post, String categoryId, String forumId, String topicId);

  /**
   * @deprecated use {@link #updatePost(Post post)}
   * Will remove in 4.0.1
   */
  @Deprecated
  public void updatePost(Post post, String categoryId, String forumId, String topicId);
  
  /**
   * This will be call after save post
   * @param post
   * @param forumId
   * @since 4.0
   */
  public void updatePost(Post post);
  
  /**
   * @param post
   * @param type - type of status changed
   * It set by Utils.APPROVE, Utils.WAITING, Utils.HIDDEN 
   * @since 4.0
   */
  public void updateStatusPost(Post post, int type);

  /**
   * This will be call after topics/posts removed.
   * 
   * @param activityId - the activity Id will remove.
   * @since 4.0
   */
  public void removeActiviry(String activityId);
}
