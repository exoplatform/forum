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
package org.exoplatform.forum.service.impl;

import java.util.Map;

import org.exoplatform.forum.base.BaseForumServiceTestCase;
import org.exoplatform.forum.base.mock.MockOtherService;
import org.exoplatform.forum.service.MessageBuilder;
import org.exoplatform.forum.service.Post;
import org.exoplatform.forum.service.Topic;

public class TestForumEventListener extends BaseForumServiceTestCase {

  public TestForumEventListener() {
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
  }
  
  @Override
  public void tearDown() throws Exception {
    //
    super.tearDown();
  }
  
  public void testMovePost() throws Exception {
    //
    initDefaultData();
    // create posts
    String[] postPaths = new String[10];
    for (int i = 0; i < 10; i++) {
      Post p = createdPost();
      p.setName("Post " + i);
      forumService_.savePost(categoryId, forumId, topicId, p, true, new MessageBuilder());
      postPaths[i] = p.getPath();
    }
    // create new topic
    Topic topic = createdTopic(USER_JOHN);
    forumService_.saveTopic(categoryId, forumId, topic, true, false, new MessageBuilder());
    // move post to new topic
    forumService_.movePost(postPaths, topic.getPath(), false, "", "");
    // check value
    topic = forumService_.getTopic(categoryId, forumId, topic.getId(), "");
    assertEquals(10, topic.getPostCount());
    //
    Map<String, Integer> result = (Map<String, Integer>) getService(MockOtherService.class).getResultTest();
    assertEquals(10, result.get("newActivitySize").intValue());
    assertEquals(10, result.get("removeActivitySize").intValue());
  }

}
