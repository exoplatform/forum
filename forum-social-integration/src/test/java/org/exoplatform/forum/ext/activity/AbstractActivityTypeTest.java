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

import java.util.Date;
import java.util.Map;
import java.util.Random;

import org.exoplatform.commons.testing.BaseExoTestCase;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.forum.service.Category;
import org.exoplatform.forum.service.Forum;
import org.exoplatform.forum.service.Post;
import org.exoplatform.forum.service.Topic;
import org.exoplatform.services.idgenerator.IDGeneratorService;
import org.exoplatform.services.idgenerator.impl.IDGeneratorServiceImpl;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;

/**
 * Created by The eXo Platform SAS
 * Author : thanh_vucong
 *          thanh_vucong@exoplatform.com
 * Jan 14, 2013  
 */
@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.ROOT, path = "conf/configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.forum.social.test.configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.forum.social.test.dependencies-configuration.xml")
})
public abstract class AbstractActivityTypeTest extends BaseExoTestCase {

  private IdGenerator ig;
  @Override
  protected void setUp() throws Exception {
    IDGeneratorService is = new IDGeneratorServiceImpl();
    ig = new IdGenerator(is);
    super.setUp();
  }
  
  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    ig = null;
  }
  
  protected final String topicTitle = "topic title";
  protected final String topicContent = "topic content";
  
  protected final String postTitle = "post title";
  protected final String postContent = "post content";
  
  protected Topic closeTopic() {
    Topic topic = createdTopic("demo");
    topic.setEditedIsClosed(true);
    topic.setIsActiveByForum(false);
    return topic;
  }
  
  protected Topic openTopic() {
    Topic topic = createdTopic("demo");
    topic.setIsClosed(true);
    topic.setEditedIsClosed(false);
    return topic;
  }
  
  protected Topic lockTopic() {
    Topic topic = createdTopic("demo");
    topic.setEditedIsLock(true);
    return topic;
  }
  
  protected Topic unlockTopic() {
    Topic topic = createdTopic("demo");
    topic.setIsLock(true);
    topic.setEditedIsLock(false);
    return topic;
  }
  
  protected Topic censoringTopic() {
    Topic topic = createdTopic("demo");
    topic.setEditedIsWaiting(true);
    return topic;
  }
  
  protected Topic uncensoringTopic() {
    Topic topic = createdTopic("demo");
    topic.setIsWaiting(true);
    topic.setEditedIsWaiting(false);
    return topic;
  }
  
  protected Topic activeTopic() {
    Topic topic = createdTopic("demo");
    topic.setEditedIsActive(true);
    return topic;
  }
  
  protected Topic updateTopicName(Topic topic) {
    topic.setEditedTopicName("new topic name");
    return topic;
  }
  
  protected Topic updateTopicRate(Double rate) {
    Topic topic = createdTopic("demo");
    topic.setEditedVoteRating(rate);
    return topic;
  }
  
  protected Topic updateTopicDescription(Topic topic) {
    topic.setEditedDescription("new topic description.");
    return topic;
  }
  
  public void assertNumberOfReplies(ExoSocialActivity activity, long expectedNumber) {
   Map<String, String> templateParams = activity.getTemplateParams();
    
   String got = templateParams.get(ForumActivityBuilder.TOPIC_POST_COUNT_KEY);
   assertEquals("" + expectedNumber, got);
  }
  
  public void assertVoteRate(ExoSocialActivity activity, double expectedNumber) {
    Map<String, String> templateParams = activity.getTemplateParams();
     
    String got = templateParams.get(ForumActivityBuilder.TOPIC_VOTE_RATE_KEY);
    assertEquals("" + expectedNumber, got);
   }
  
  public void assertTopicTitle(ExoSocialActivity activity, String expectedTitle) {
    assertEquals(expectedTitle, activity.getTitle());
  }
  
  public void assertPostTitle(ExoSocialActivity comment, String expectedTitle) {
    assertEquals(expectedTitle, comment.getTitle());
  }
  
  public void assertTopicContent(ExoSocialActivity activity, String expectedContent) {
    assertEquals(expectedContent, activity.getBody());
  }
  
  protected Topic updateTopicTitle(Topic topic, String newTitle) {
    topic.setEditedTopicName(newTitle);
    return topic;
  }
  
  protected Topic updateTopicContent(Topic topic, String newContent) {
    topic.setEditedDescription(newContent);
    return topic;
  }
  
  protected Post updatePostContent(Post post, String newContent) {
    post.setMessage(newContent);
    return post;
  }
  
  protected Topic createdTopic(String owner) {
    Topic topicNew = new Topic();

    topicNew.setOwner(owner);
    topicNew.setTopicName(topicTitle);
    topicNew.setCreatedDate(new Date());
    topicNew.setModifiedBy("demo");
    topicNew.setModifiedDate(new Date());
    topicNew.setLastPostBy("demo");
    topicNew.setLastPostDate(new Date());
    topicNew.setDescription(topicContent);
    topicNew.setPostCount(0);
    topicNew.setViewCount(0);
    topicNew.setIsNotifyWhenAddPost("");
    topicNew.setIsModeratePost(false);
    topicNew.setIsClosed(false);
    topicNew.setIsLock(false);
    topicNew.setIsWaiting(false);
    topicNew.setIsActive(true);
    topicNew.setIcon("classNameIcon");
    topicNew.setIsApproved(true);
    topicNew.setCanView(new String[] {});
    topicNew.setCanPost(new String[] {});
    topicNew.setPath("forumCategory123/forum123/" + topicNew.getId());
    
    assertEquals("forumCategory123", topicNew.getCategoryId());
    assertEquals("forum123", topicNew.getForumId());
    return topicNew;
  }
  
  public Post createdPost(Topic topic) {
    Post post = new Post();
    post.setOwner("demo");
    post.setCreatedDate(new Date());
    post.setModifiedBy("demo");
    post.setModifiedDate(new Date());
    post.setName(postTitle);
    post.setMessage(postContent);
    post.setRemoteAddr("192.168.1.11");
    post.setIcon("classNameIcon");
    post.setIsApproved(true);
    post.setIsActiveByTopic(true);
    post.setIsHidden(false);
    post.setIsWaiting(false);
    post.setPath("forumCategory123/forum123/topic123/" + post.getId());
    post.setLink("http://localhost:8080/portal/intranet/forum/topic123/" + post.getId());
    topic.setPostCount(topic.getPostCount() + 1);
    return post;
  }
  

  protected Forum createdForum() {
    Forum forum = new Forum();
    forum.setOwner("root");
    forum.setForumName("TestForum");
    forum.setForumOrder(1);
    forum.setCreatedDate(new Date());
    forum.setModifiedBy("root");
    forum.setModifiedDate(new Date());
    forum.setLastTopicPath("");
    forum.setDescription("description");
    forum.setPostCount(0);
    forum.setTopicCount(0);

    forum.setNotifyWhenAddTopic(new String[] {});
    forum.setNotifyWhenAddPost(new String[] {});
    forum.setIsModeratePost(false);
    forum.setIsModerateTopic(false);
    forum.setIsClosed(false);
    forum.setIsLock(false);

    forum.setViewer(new String[] {});
    forum.setCreateTopicRole(new String[] {});
    forum.setModerators(new String[] {});
    return forum;
  }

  protected Category createCategory(String id) {
    Category cat = new Category(id);
    cat.setOwner("root");
    cat.setCategoryName("testCategory");
    cat.setCategoryOrder(1);
    cat.setCreatedDate(new Date());
    cat.setDescription("desciption");
    cat.setModifiedBy("root");
    cat.setModifiedDate(new Date());
    return cat;
  }

  protected String getId(String type) {
    try {
      return type + IdGenerator.generate();
    } catch (Exception e) {
      return type + String.valueOf(new Random().nextLong());
    }
  }
}
