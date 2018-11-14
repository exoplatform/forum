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
package org.exoplatform.forum.base.mock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.forum.service.Category;
import org.exoplatform.forum.service.Forum;
import org.exoplatform.forum.service.ForumEventListener;
import org.exoplatform.forum.service.ForumService;
import org.exoplatform.forum.service.Post;
import org.exoplatform.forum.service.Topic;

public class MockForumEventListener extends ForumEventListener {
  
  public MockForumEventListener() {
  }

  @Override
  public void saveCategory(Category category) {

  }

  @Override
  public void saveForum(Forum forum) {

  }

  @Override
  public void addTopic(Topic topic) {
    CommonUtils.getComponent(ForumService.class)
              .saveActivityIdForOwnerPath(topic.getPath(), "topicActivityId" + new Random().nextInt(1000));
  }

  @Override
  public void updateTopic(Topic topic) {

  }

  @Override
  public void updateTopics(List<Topic> topics, boolean isLock) {

  }

  @Override
  public void moveTopic(Topic topic, String toCategoryName, String toForumName) {

  }

  public void movePost(List<Post> posts, List<String> srcPostActivityIds, String desTopicPath) {
    Map<String, Integer> result = new HashMap<String, Integer>();
    result.put("newActivitySize", posts.size());
    result.put("removeActivitySize", srcPostActivityIds.size());
    CommonUtils.getComponent(MockOtherService.class).setResult(result);
  }

  @Override
  public void mergeTopic(Topic newTopic, String removeActivityId1, String removeActivityId2) {

  }

  @Override
  public void splitTopic(Topic newTopic, Topic splitedTopic, String removeActivityId) {

  }

  @Override
  public void addPost(Post post) {
    CommonUtils.getComponent(ForumService.class)
                .saveActivityIdForOwnerPath(post.getPath(), "postActivityId" + new Random().nextInt(1000));
  }

  @Override
  public void updatePost(Post post) {

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
  public void openTopic(String userId, String topicId) {

  }
}
