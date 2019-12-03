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

import org.exoplatform.forum.service.Topic;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

/**
 * Created by The eXo Platform SAS
 * Author : thanh_vucong
 *          thanh_vucong@exoplatform.com
 * Jan 16, 2013  
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TopicActivityTest extends AbstractActivityTypeTest {
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }
  
  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testAddTopic() throws Exception {
    Topic topic = createdTopic("demo");
    ForumActivityContext ctx = ForumActivityContext.makeContextForAddTopic(topic);
    ExoSocialActivity a = ForumActivityBuilder.createActivity(topic, ctx);
    assertNumberOfReplies(a, 0);
    assertVoteRate(a, topic.getVoteRating());
    assertTopicTitle(a, topicTitle);
    assertTopicContent(a, topicContent);
  }
  
  public void testAddTopicWithJob() throws Exception {
    Topic topic = createdTopic("demo");
    ForumActivityContext ctx = ForumActivityContext.makeContextForAddTopic(topic);
    TopicActivityTask task = TopicActivityTask.ADD_TOPIC;
    ExoSocialActivity a = ForumActivityBuilder.createActivity(topic, ctx);
    a = task.processActivity(ctx, a);
    assertNumberOfReplies(a, 0);
    assertVoteRate(a, topic.getVoteRating());
    assertTopicTitle(a, topicTitle);
    assertTopicContent(a, topicContent);
  }
  
  public void testAddTopicWaitingWithJob() throws Exception {
    Topic topic = createdTopic("demo");
    topic.setIsWaiting(true);
    ForumActivityContext ctx = ForumActivityContext.makeContextForAddTopic(topic);
    TopicActivityTask task = TopicActivityTask.ADD_TOPIC;
    ExoSocialActivity a = ForumActivityBuilder.createActivity(topic, ctx);
    a = task.processActivity(ctx, a);
    assertEquals(true, a.isHidden());
  }
  
  public void testUpdateTopicPropertiesWithJob() throws Exception {
    Topic topic = createdTopic("demo");
    topic = updateTopicTitle(topic, "edited to new title for topic.");
    topic = updateTopicContent(topic, "edited to new content for topic.");
    assertEquals(2, topic.getChangeEvent().length);
    assertEquals(Topic.TOPIC_NAME, topic.getChangeEvent()[0].getPropertyName());
    assertEquals(Topic.TOPIC_CONTENT, topic.getChangeEvent()[1].getPropertyName());
    
    //
    ForumActivityContext ctx = ForumActivityContext.makeContextForUpdateTopic(topic);
    ctx.setPcs(topic.getPcs());
    
    //
    TopicActivityTask task = TopicActivityTask.UPDATE_TOPIC_PROPERTIES;
    ExoSocialActivity a = ForumActivityBuilder.createActivity(topic, ctx);
    
    //activity
    a = task.processActivity(ctx, a);
    assertNumberOfReplies(a, 0);
    assertVoteRate(a, topic.getVoteRating());
    assertEquals("edited to new title for topic.", a.getTitle());
    
    //comment
    ExoSocialActivity newComment = task.processComment(ctx);
    assertEquals("Title has been updated to: edited to new title for topic.\nContent has been edited.",
                 newComment.getTitle());
    
    task = TopicActivityTask.UPDATE_TOPIC_TITLE;
    //comment
    newComment = task.processComment(ctx);
    assertEquals("Title has been updated to: edited to new title for topic.", newComment.getTitle());
  }
  
  public void testUpdateTopicTitleWithSpecialCharacters() throws Exception {
    Topic topic = createdTopic("demo");
    topic = updateTopicTitle(topic, "&-*()");
    assertEquals(1, topic.getChangeEvent().length);
    assertEquals(Topic.TOPIC_NAME, topic.getChangeEvent()[0].getPropertyName());
    
    ForumActivityContext ctx = ForumActivityContext.makeContextForUpdateTopic(topic);
    ExoSocialActivity a = ForumActivityBuilder.createActivity(topic, ctx);
    assertTopicTitle(a, "&-*()");
    
    topic = updateTopicTitle(topic, "&-*() / --- == coucou #@");
    assertEquals(2, topic.getChangeEvent().length);
    assertEquals(Topic.TOPIC_NAME, topic.getChangeEvent()[0].getPropertyName());
    
    ctx = ForumActivityContext.makeContextForUpdateTopic(topic);
    a = ForumActivityBuilder.createActivity(topic, ctx);
    assertTopicTitle(a, "&-*() / --- == coucou #@");
    //
    topic = updateTopicTitle(topic, "&lt;script&gt;alert(1233)&lt;/script&gt;");
    ctx.setPcs(topic.getPcs());
    //
    TopicActivityTask task = TopicActivityTask.UPDATE_TOPIC_PROPERTIES;
    //comment when update only title and content
    ExoSocialActivity newComment = task.processComment(ctx);
    assertEquals("Title has been updated to: <script>alert(1233)</script>\n", newComment.getTitle());
    assertEquals("&lt;script&gt;alert(1233)&lt;/script&gt;", 
                 newComment.getTemplateParams().get("RESOURCE_BUNDLE_VALUES_PARAM"));
    //comment when update only title
    task = TopicActivityTask.UPDATE_TOPIC_TITLE;
    newComment = task.processComment(ctx);
    assertEquals("Title has been updated to: <script>alert(1233)</script>", newComment.getTitle());
    assertEquals("&lt;script&gt;alert(1233)&lt;/script&gt;", 
                 newComment.getTemplateParams().get("RESOURCE_BUNDLE_VALUES_PARAM"));
  }
  
  public void testUpdateTopicTitle() throws Exception {
    Topic topic = createdTopic("demo");
    topic = updateTopicTitle(topic, "edited to new title for topic.");
    assertEquals(1, topic.getChangeEvent().length);
    assertEquals(Topic.TOPIC_NAME, topic.getChangeEvent()[0].getPropertyName());
    
    ForumActivityContext ctx = ForumActivityContext.makeContextForUpdateTopic(topic);
    ExoSocialActivity a = ForumActivityBuilder.createActivity(topic, ctx);
    assertNumberOfReplies(a, 0);
    assertVoteRate(a, topic.getVoteRating());
    assertTopicTitle(a, "edited to new title for topic.");
    assertTopicContent(a, topicContent);
    
  }
  
  public void testUpdateTopicTitleWithJob() throws Exception {
    Topic topic = createdTopic("demo");
    topic = updateTopicTitle(topic, "edited to new title for topic.");
    ForumActivityContext ctx = ForumActivityContext.makeContextForUpdateTopic(topic);
    
    TopicActivityTask task = TopicActivityTask.UPDATE_TOPIC_TITLE;
    ExoSocialActivity a = ForumActivityBuilder.createActivity(topic, ctx);
    
    //activity
    a = task.processActivity(ctx, a);
    assertNumberOfReplies(a, 0);
    assertVoteRate(a, topic.getVoteRating());
    assertTopicTitle(a, "edited to new title for topic.");
    assertTopicContent(a, topicContent);
    
    //comment
    ExoSocialActivity newComment = task.processComment(ctx);
    assertTopicTitle(newComment, "Title has been updated to: edited to new title for topic.");
  }
  
  public void testUpdateTopicContent() throws Exception {
    Topic topic = createdTopic("demo");
    topic = updateTopicContent(topic, "edited to new content for topic.");
    assertEquals(1, topic.getChangeEvent().length);
    assertEquals(Topic.TOPIC_CONTENT, topic.getChangeEvent()[0].getPropertyName());
    
    ForumActivityContext ctx = ForumActivityContext.makeContextForUpdateTopic(topic);
    ExoSocialActivity a = ForumActivityBuilder.createActivity(topic, ctx);
    assertNumberOfReplies(a, 0);
    assertVoteRate(a, topic.getVoteRating());
    assertTopicTitle(a, topicTitle);
    assertTopicContent(a, "edited to new content for topic.");
    
  }
  
  public void testUpdateTopicContentWith4Lines() throws Exception {
    Topic topic = createdTopic("demo");
    topic = updateTopicContent(topic, "1\n2\n3\n4\n5");
    assertEquals(1, topic.getChangeEvent().length);
    assertEquals(Topic.TOPIC_CONTENT, topic.getChangeEvent()[0].getPropertyName());
    
    ForumActivityContext ctx = ForumActivityContext.makeContextForUpdateTopic(topic);
    ExoSocialActivity a = ForumActivityBuilder.createActivity(topic, ctx);
    assertTopicContent(a, "1<br/>2<br/>3<br/>4...");
    
    topic = updateTopicContent(topic, "1<br/>2<br/>3<br/>4<br/>5");
    ctx = ForumActivityContext.makeContextForUpdateTopic(topic);
    a = ForumActivityBuilder.createActivity(topic, ctx);
    assertTopicContent(a, "1<br/>2<br/>3<br/>4...");
    
    topic = updateTopicContent(topic, "<p>1</p><br/><p>2</p><br/><p>3</p><br/><p>4</p><br/><p>5</p>");
    ctx = ForumActivityContext.makeContextForUpdateTopic(topic);
    a = ForumActivityBuilder.createActivity(topic, ctx);
    assertTopicContent(a, "1<br/>2<br/>3<br/>4...");
  }
  
  public void testUpdateTopicContentWithJob() throws Exception {
    Topic topic = createdTopic("demo");
    topic = updateTopicContent(topic, "edited to new content for topic.");
    assertEquals(1, topic.getChangeEvent().length);
    ForumActivityContext ctx = ForumActivityContext.makeContextForUpdateTopic(topic);
    
    TopicActivityTask task = TopicActivityTask.UPDATE_TOPIC_CONTENT;
    ExoSocialActivity a = ForumActivityBuilder.createActivity(topic, ctx);
    
    //activity
    a = task.processActivity(ctx, a);
    assertNumberOfReplies(a, 0);
    assertVoteRate(a, topic.getVoteRating());
    assertTopicTitle(a, "topic title");
    assertTopicContent(a, "edited to new content for topic.");
    
    //comment
    ExoSocialActivity newComment = task.processComment(ctx);
    assertTopicTitle(newComment, "Content has been edited.");
  }
  
  public void testUpdateTopicRate() throws Exception {
    Topic topic = updateTopicRate(1.5);
    assertEquals(1, topic.getChangeEvent().length);
    assertEquals(Topic.TOPIC_RATING, topic.getChangeEvent()[0].getPropertyName());
    assertEquals(1.5, topic.getVoteRating());
  }
  
  public void testUpdateTopicRateWithJob() throws Exception {
    Topic topic = updateTopicRate(1.5);
    ForumActivityContext ctx = ForumActivityContext.makeContextForUpdateTopic(topic);
    assertEquals(1.5, topic.getVoteRating());
    
    //
    TopicActivityTask task = TopicActivityTask.UPDATE_TOPIC_RATE;
    ExoSocialActivity a = ForumActivityBuilder.createActivity(topic, ctx);
    
    //activity
    a = task.processActivity(ctx, a);
    assertTopicTitle(a, "topic title");
    
    //comment
    ExoSocialActivity newComment = task.processComment(ctx);
    assertTopicTitle(newComment, "Rated the topic: 1.5");
  }
  
  public void testCloseTopic() throws Exception {
    Topic topic = closeTopic();
    assertEquals(1, topic.getChangeEvent().length);
    assertEquals(Topic.TOPIC_STATE_CLOSED, topic.getChangeEvent()[0].getPropertyName());
    assertEquals(true, topic.getIsClosed());
  }
  
  public void testCloseTopicWithJob() throws Exception {
    Topic topic = closeTopic();
    assertEquals(1, topic.getChangeEvent().length);
    assertEquals(Topic.TOPIC_STATE_CLOSED, topic.getChangeEvent()[0].getPropertyName());
    
    ForumActivityContext ctx = ForumActivityContext.makeContextForUpdateTopic(topic);
    
    TopicActivityTask task = TopicActivityTask.CLOSE_TOPIC;
    ExoSocialActivity a = ForumActivityBuilder.createActivity(topic, ctx);
    
    //activity
    a = task.processActivity(ctx, a);
    assertEquals(true, a.isLocked());
    
    //comment
    ExoSocialActivity newComment = task.processComment(ctx);
    assertTopicTitle(newComment, "Topic has been closed.");
  }
  
  public void testLockTopic() throws Exception {
    Topic topic = lockTopic();
    assertEquals(1, topic.getChangeEvent().length);
    assertEquals(Topic.TOPIC_STATUS_LOCK, topic.getChangeEvent()[0].getPropertyName());
    assertEquals(true, topic.getIsLock());
  }
  
  public void testLockTopicWithJob() throws Exception {
    Topic topic = lockTopic();
    assertEquals(1, topic.getChangeEvent().length);
    assertEquals(Topic.TOPIC_STATUS_LOCK, topic.getChangeEvent()[0].getPropertyName());
    
    ForumActivityContext ctx = ForumActivityContext.makeContextForUpdateTopic(topic);
    
    TopicActivityTask task = TopicActivityTask.LOCK_TOPIC;
    ExoSocialActivity a = ForumActivityBuilder.createActivity(topic, ctx);
    
    //activity
    a = task.processActivity(ctx, a);
    assertEquals(true, a.isLocked());
    
    //comment
    ExoSocialActivity newComment = task.processComment(ctx);
    assertTopicTitle(newComment, "Topic has been locked.");
  }
  
  public void testUnlockTopic() throws Exception {
    Topic topic = unlockTopic();
    assertEquals(1, topic.getChangeEvent().length);
    assertEquals(Topic.TOPIC_STATUS_LOCK, topic.getChangeEvent()[0].getPropertyName());
    assertEquals(false, topic.getIsLock());
  }
  
  public void testUnlockTopicWithJob() throws Exception {
    Topic topic = unlockTopic();
    assertEquals(1, topic.getChangeEvent().length);
    assertEquals(Topic.TOPIC_STATUS_LOCK, topic.getChangeEvent()[0].getPropertyName());
    
    ForumActivityContext ctx = ForumActivityContext.makeContextForUpdateTopic(topic);
    
    TopicActivityTask task = TopicActivityTask.UNLOCK_TOPIC;
    ExoSocialActivity a = ForumActivityBuilder.createActivity(topic, ctx);
    
    //activity
    a = task.processActivity(ctx, a);
    assertEquals(false, a.isLocked());
    
    //comment
    ExoSocialActivity newComment = task.processComment(ctx);
    assertTopicTitle(newComment, "Topic has been unlocked.");
  }
  
  public void testCensoringTopic() throws Exception {
    Topic topic = censoringTopic();
    assertEquals(1, topic.getChangeEvent().length);
    assertEquals(Topic.TOPIC_STATUS_WAITING, topic.getChangeEvent()[0].getPropertyName());
    assertEquals(true, topic.getIsWaiting());
  }
  
  public void testCensoringTopicWithJob() throws Exception {
    Topic topic = censoringTopic();
    assertEquals(1, topic.getChangeEvent().length);
    assertEquals(Topic.TOPIC_STATUS_WAITING, topic.getChangeEvent()[0].getPropertyName());
    
    ForumActivityContext ctx = ForumActivityContext.makeContextForUpdateTopic(topic);
    
    TopicActivityTask task = TopicActivityTask.CENSORING_TOPIC;
    ExoSocialActivity a = ForumActivityBuilder.createActivity(topic, ctx);
    
    //activity
    a = task.processActivity(ctx, a);
    assertEquals(true, a.isHidden());
  }
  
  public void testUncensoringTopic() throws Exception {
    Topic topic = uncensoringTopic();
    assertEquals(1, topic.getChangeEvent().length);
    assertEquals(Topic.TOPIC_STATUS_WAITING, topic.getChangeEvent()[0].getPropertyName());
    assertEquals(false, topic.getIsWaiting());
  }
  
  public void testUncensoringTopicWithJob() throws Exception {
    Topic topic = censoringTopic();
    assertEquals(1, topic.getChangeEvent().length);
    assertEquals(Topic.TOPIC_STATUS_WAITING, topic.getChangeEvent()[0].getPropertyName());
    
    ForumActivityContext ctx = ForumActivityContext.makeContextForUpdateTopic(topic);
    
    TopicActivityTask task = TopicActivityTask.UNCENSORING_TOPIC;
    ExoSocialActivity a = ForumActivityBuilder.createActivity(topic, ctx);
    
    //activity
    a = task.processActivity(ctx, a);
    assertEquals(false, a.isHidden());
  }
  
  public void testLockTopicWhenLockForum() throws Exception {
    Topic topic = lockTopic();
    assertEquals(1, topic.getChangeEvent().length);
    assertEquals(Topic.TOPIC_STATUS_LOCK, topic.getChangeEvent()[0].getPropertyName());
    
    ForumActivityContext ctx = ForumActivityContext.makeContextForUpdateTopic(topic);
    
    TopicActivityTask task = TopicActivityTask.UPDATE_FORUM_TOPIC;
    ExoSocialActivity a = ForumActivityBuilder.createActivity(topic, ctx);
    
    //activity
    a = task.processActivity(ctx, a);
    assertEquals(true, a.isLocked());
    
    //Unlock forum
    topic = unlockTopic();
    assertEquals(Topic.TOPIC_STATUS_LOCK, topic.getChangeEvent()[0].getPropertyName());
    
    ctx = ForumActivityContext.makeContextForUpdateTopic(topic);
    
    task = TopicActivityTask.UPDATE_FORUM_TOPIC;
    a = ForumActivityBuilder.createActivity(topic, ctx);
    
    a = task.processActivity(ctx, a);
    assertEquals(false, a.isLocked());
  }
  
  public void testCloseTopicWhenCloseForum() throws Exception {
    Topic topic = closeTopic();
    assertEquals(1, topic.getChangeEvent().length);
    assertEquals(Topic.TOPIC_STATE_CLOSED, topic.getChangeEvent()[0].getPropertyName());
    
    ForumActivityContext ctx = ForumActivityContext.makeContextForUpdateTopic(topic);
    
    TopicActivityTask task = TopicActivityTask.UPDATE_FORUM_TOPIC;
    ExoSocialActivity a = ForumActivityBuilder.createActivity(topic, ctx);
    
    //activity
    a = task.processActivity(ctx, a);
    assertEquals(true, a.isLocked());
    
    //Open forum
    topic = openTopic();
    assertEquals(Topic.TOPIC_STATE_CLOSED, topic.getChangeEvent()[0].getPropertyName());
    
    ctx = ForumActivityContext.makeContextForUpdateTopic(topic);
    
    task = TopicActivityTask.UPDATE_FORUM_TOPIC;
    a = ForumActivityBuilder.createActivity(topic, ctx);
    
    a = task.processActivity(ctx, a);
    assertEquals(false, a.isLocked());
  }
  
}
