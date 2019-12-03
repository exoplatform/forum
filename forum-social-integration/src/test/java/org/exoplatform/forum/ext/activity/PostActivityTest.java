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

import org.exoplatform.forum.service.Post;
import org.exoplatform.forum.service.Topic;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import static org.exoplatform.forum.ext.activity.ForumActivityBuilder.POST_LINK_KEY;

/**
 * Created by The eXo Platform SAS
 * Author : thanh_vucong
 *          thanh_vucong@exoplatform.com
 * Jan 17, 2013  
 */
@FixMethodOrder(MethodSorters.JVM)
public class PostActivityTest extends AbstractActivityTypeTest {

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
    Post post = createdPost(topic);
    
    //
    ForumActivityContext ctx = ForumActivityContext.makeContextForAddPost(post);
    ExoSocialActivity comment = ForumActivityBuilder.createActivityComment(post, ctx);
    assertPostTitle(comment, postContent);
    assertEquals(comment.getTemplateParams().get(POST_LINK_KEY), post.getLink());
  }

  public void testTriggerAddPostTwice() throws Exception {
    Topic topic = createdTopic("demo");
    ForumActivityContext ctx = ForumActivityContext.makeContextForAddTopic(topic);
    ExoSocialActivity topicActivity = ForumActivityBuilder.createActivity(topic, ctx);
    Post post = createdPost(topic);

    //
    ctx = ForumActivityContext.makeContextForAddPost(post);
    PostActivityTask task = PostActivityTask.ADD_POST;

    ExoSocialActivity comment = ForumActivityBuilder.createActivityComment(post, ctx);
    ctx.setTopic(topic);
    topicActivity = task.processActivity(ctx, topicActivity);
    comment = task.processComment(ctx, comment);
    comment = task.processComment(ctx, comment);

    assertNumberOfReplies(topicActivity, 1);
  }

  public void testAddTopicWithJob() throws Exception {
    Topic topic = createdTopic("demo");
    ForumActivityContext ctx = ForumActivityContext.makeContextForAddTopic(topic);
    ExoSocialActivity topicActivity = ForumActivityBuilder.createActivity(topic, ctx);
    Post post = createdPost(topic);
    
    //
    ctx = ForumActivityContext.makeContextForAddPost(post);
    PostActivityTask task = PostActivityTask.ADD_POST;
    ExoSocialActivity comment = ForumActivityBuilder.createActivityComment(post, ctx);
    ctx.setTopic(topic);
    topicActivity = task.processActivity(ctx, topicActivity);
    comment = task.processComment(ctx, comment);
    assertPostTitle(comment, postContent);
    assertNumberOfReplies(topicActivity, 1);
    assertEquals(comment.getTemplateParams().get(POST_LINK_KEY), post.getLink());
  }
  
  public void testUpdateTopic() throws Exception {
    Topic topic = createdTopic("demo");
    Post post = createdPost(topic);
    
    //
    ForumActivityContext ctx = ForumActivityContext.makeContextForAddPost(post);
    ExoSocialActivity comment = ForumActivityBuilder.createActivityComment(post, ctx);
    assertPostTitle(comment, postContent);
    assertEquals(comment.getTemplateParams().get(POST_LINK_KEY), post.getLink());
  }
  
  public void testUpdateTopicWithJob() throws Exception {
    Topic topic = createdTopic("demo");
    ForumActivityContext ctx = ForumActivityContext.makeContextForAddTopic(topic);
    Post post = createdPost(topic);
    updatePostContent(post, "edited post content");
    
    //
    ctx = ForumActivityContext.makeContextForUpdatePost(post);
    PostActivityTask task = PostActivityTask.UPDATE_POST;
    ExoSocialActivity comment = ForumActivityBuilder.createActivityComment(post, ctx);
    ctx.setTopic(topic);
    
    //Comment associated with this post exist --> update
    comment = task.processComment(ctx, comment);
    assertPostTitle(comment, "Edited his reply to: edited post content");
    assertEquals(comment.getTemplateParams().get(POST_LINK_KEY), post.getLink());

    //Comment associated with this post has been deleted --> add new
    comment = task.processComment(ctx, null);
    assertPostTitle(comment, "Edited his reply to: edited post content");
    assertEquals(comment.getTemplateParams().get(POST_LINK_KEY), post.getLink());

    //Case of post with multi-lines, 3 firsts lines will be used
    updatePostContent(post, "edited post content1\n2\n3\n4\n5");
    
    //
    ctx = ForumActivityContext.makeContextForUpdatePost(post);
    task = PostActivityTask.UPDATE_POST;
    comment = ForumActivityBuilder.createActivityComment(post, ctx);
    ctx.setTopic(topic);
    
    //Comment associated with this post exist --> update
    comment = task.processComment(ctx, comment);
    assertPostTitle(comment, "Edited his reply to: edited post content1<br/>2<br/>3...");
    assertEquals(comment.getTemplateParams().get(POST_LINK_KEY), post.getLink());
    
    //Comment associated with this post has been deleted --> add new
    comment = task.processComment(ctx, null);
    assertPostTitle(comment, "Edited his reply to: edited post content1<br/>2<br/>3...");
    assertEquals(comment.getTemplateParams().get(POST_LINK_KEY), post.getLink());
  }

  public void testCreateTopicWithBBCode() {
    Topic topic = createdTopic("demo");
    topic.setDescription("Topic content with link bb code [url]http://google.com[/url]");

    ForumActivityContext ctx = ForumActivityContext.makeContextForAddTopic(topic);
    ExoSocialActivity activity = ForumActivityBuilder.createActivity(topic, ctx);

    assertPostTitle(activity, topicTitle);
    assertTopicContent(activity, "Topic content with link bb code <a target=\"_blank\" href=\"http://google.com\">http://google.com</a>");
  }

  public void testCreatePostWithBBCode() {
    Topic topic = createdTopic("demo");
    Post post = createdPost(topic);
    updatePostContent(post, "post content with link bb code [url]http://google.com[/url]");

    //
    ForumActivityContext ctx = ForumActivityContext.makeContextForAddPost(post);
    ExoSocialActivity comment = ForumActivityBuilder.createActivityComment(post, ctx);
    assertPostTitle(comment, "post content with link bb code <a target=\"_blank\" href=\"http://google.com\">http://google.com</a>");
    assertTopicContent(comment, "post content with link bb code <a target=\"_blank\" href=\"http://google.com\">http://google.com</a>");
    assertEquals(comment.getTemplateParams().get(POST_LINK_KEY), post.getLink());
  }

  public void testCreatePostWithCutBBCode() {
    Topic topic = createdTopic("demo");
    Post post = createdPost(topic);
    updatePostContent(post, "[I]post content with link bb code[/I] [u]1\n2\n3\n4\n5[/u]");

    //
    ForumActivityContext ctx = ForumActivityContext.makeContextForAddPost(post);
    ExoSocialActivity comment = ForumActivityBuilder.createActivityComment(post, ctx);
    assertPostTitle(comment, "<i>post content with link bb code</i> 1<br/>2<br/>3...");
    assertTopicContent(comment, "<i>post content with link bb code</i> <u>1\n2\n3\n4\n5</u>");
    assertEquals(comment.getTemplateParams().get(POST_LINK_KEY), post.getLink());
  }
}
