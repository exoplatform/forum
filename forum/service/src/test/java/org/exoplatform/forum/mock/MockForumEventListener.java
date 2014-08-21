/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
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
package org.exoplatform.forum.mock;

import java.util.List;

import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.forum.mock.ResultTestForumListener.STATUS;
import org.exoplatform.forum.service.Category;
import org.exoplatform.forum.service.Forum;
import org.exoplatform.forum.service.ForumEventListener;
import org.exoplatform.forum.service.Post;
import org.exoplatform.forum.service.Topic;

public class MockForumEventListener extends ForumEventListener {

  ResultTestForumListener resultListener;

  public MockForumEventListener() {
    resultListener = CommonUtils.getComponent(ResultTestForumListener.class);
  }

  @Override
  public void addTopic(Topic topic) {
    resultListener.setStatus(STATUS.ADD_TOPIC);
  }

  @Override
  public void updateTopic(Topic topic) {
    resultListener.setStatus(STATUS.UPDATE_TOPIC);
  }

  @Override
  public void updateTopics(List<Topic> topics, boolean isLock) {
  }

  @Override
  public void moveTopic(Topic topic, String toCategoryName, String toForumName) {
  }

  /**
   * {@inheritDoc}
   */
  public void movePost(List<Post> posts, List<String> srcPostActivityIds, String desTopicPath) {
  }
  
  @Override
  public void mergeTopic(Topic newTopic, String removeActivityId1, String removeActivityId2) {
  }

  @Override
  public void splitTopic(Topic newTopic, Topic splitedTopic, String removeActivityId) {
  }

  @Override
  public void addPost(Post post) {
    resultListener.setStatus(STATUS.ADD_POST);
  }

  @Override
  public void updatePost(Post post) {
    resultListener.setStatus(STATUS.UPDATE_POST);
  }

  @Override
  public void updatePost(Post post, int type) {
  }

  @Override
  public void removeActivity(String activityId) {
  }

  @Override
  public void removeComment(String activityId, String commentId) {
  }

  @Override
  public void saveCategory(Category category) {
  }

  @Override
  public void saveForum(Forum forum) {
  }
}
