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
package org.exoplatform.forum.service;

import java.util.Calendar;


import org.exoplatform.forum.base.BaseForumServiceTestCase;
import org.exoplatform.forum.mock.ResultTestForumListener;
import org.exoplatform.forum.mock.ResultTestForumListener.STATUS;
import org.junit.Test;

public class ForumListenerTestCase extends BaseForumServiceTestCase {

  private ResultTestForumListener resultListener;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    resultListener = getService(ResultTestForumListener.class);
    resultListener.reset();
  }

  @Override
  public void tearDown() throws Exception {
    super.setUp();
  }
  
  private void waitForThreadDone(long time) throws Exception {
    Thread.sleep(time);
  }

  @Test
  public void testCreateTopic() throws Exception {
    // create one category, forum and one topic
    initDefaultData();
    // create more 5 topic.
    for (int i = 0; i < 5; i++) {
      Topic topic = createdTopic("root");
      forumService_.saveTopic(categoryId, forumId, topic, true, false, new MessageBuilder());
    }
    //
    waitForThreadDone(200);
    assertEquals(STATUS.ADD_TOPIC, resultListener.getStatus());
    assertEquals(6, resultListener.getTopicCount());
    //
    Topic topic = forumService_.getTopic(categoryId, forumId, topicId, null);
    topic.setTopicName("Edit topic");
    topic.setModifiedBy(USER_JOHN);
    topic.setModifiedDate(Calendar.getInstance().getTime());
    forumService_.saveTopic(categoryId, forumId, topic, false, false, new MessageBuilder());
    //
    waitForThreadDone(100);
    assertEquals(STATUS.UPDATE_TOPIC, resultListener.getStatus());
    assertEquals(topic.getModifiedBy(), resultListener.getModifier());
    assertEquals(topic.getModifiedDate(), resultListener.getModifiedDate());
  }

  @Test
  public void testCreatePost() throws Exception {
    // create one category, forum and one topic
    initDefaultData();
    // create more 5 post.
    String postId = null;
    for (int i = 0; i < 5; i++) {
      Post post = createdPost();
      forumService_.savePost(categoryId, forumId, topicId, post, true, new MessageBuilder());
      postId = post.getId();
    }
    //
    waitForThreadDone(200);
    assertEquals(STATUS.ADD_POST, resultListener.getStatus());
    assertEquals(5, resultListener.getPostCount());
    //
    Post post = forumService_.getPost(categoryId, forumId, topicId, postId);
    post.setMessage("The post content");
    forumService_.savePost(categoryId, forumId, topicId, post, false, new MessageBuilder());
    //
    waitForThreadDone(100);
    assertEquals(STATUS.UPDATE_POST, resultListener.getStatus());
  }

  @Test
  public void testUpdateTopicAccess() throws Exception {
    // create one category, forum and one topic
    initDefaultData();
    // create a topic.
    Topic topic = createdTopic("root");
    forumService_.saveTopic(categoryId, forumId, topic, true, false, new MessageBuilder());
    //
    waitForThreadDone(200);
    assertEquals(STATUS.ADD_TOPIC, resultListener.getStatus());
    assertEquals(2, resultListener.getTopicCount());
    //
    topic = forumService_.getTopic(categoryId, forumId, topicId, null);
    topic.setTopicName("Edit topic");
    topic.setModifiedBy(USER_JOHN);
    topic.setModifiedDate(Calendar.getInstance().getTime());
    forumService_.updateTopicAccess(USER_JOHN,topic.getId());
    // check if the listener is called as  expected
    assertEquals(STATUS.OPEN_TOPIC, resultListener.getStatus());
  }

}
