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
package org.exoplatform.forum.ext.forum;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.forum.service.Forum;
import org.exoplatform.forum.service.MessageBuilder;
import org.exoplatform.forum.service.Post;
import org.exoplatform.forum.service.Topic;
import org.exoplatform.forum.service.Utils;
import org.exoplatform.forum.service.impl.model.PostFilter;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;

public class ForumActivityTestCase extends BaseForumActivityTestCase {
  
  public void setUp() throws Exception {
    super.setUp();
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  public void testForumService() throws Exception {
    assertNotNull(getForumService());
  }

  public void testSplitTopic() throws Exception {
    Topic topic = forumService.getTopic(categoryId, forumId, topicId, "");
    assertNotNull(topic);
    String activityId = forumService.getActivityIdForOwnerPath(topic.getPath());
    ExoSocialActivity activity = getActivityManager().getActivity(activityId);
    assertNotNull(activity);
    assertEquals(0, getActivityManager().getCommentsWithListAccess(activity).getSize());

    Post post1 = createdPost("name1", "message1");
    Post post2 = createdPost("name2", "message2");
    Post post3 = createdPost("name3", "message3");
    Post post4 = createdPost("name4", "message4");
    forumService.savePost(categoryId, forumId, topicId, post1, true, new MessageBuilder());
    forumService.savePost(categoryId, forumId, topicId, post2, true, new MessageBuilder());
    forumService.savePost(categoryId, forumId, topicId, post3, true, new MessageBuilder());
    forumService.savePost(categoryId, forumId, topicId, post4, true, new MessageBuilder());
    
    activity = getActivityManager().getActivity(activityId);
    assertEquals(4, getActivityManager().getCommentsWithListAccess(activity).getSize());
    
    List<String> postPaths = new ArrayList<String>();
    postPaths.add(post1.getPath());
    postPaths.add(post2.getPath());
    postPaths.add(post3.getPath());
    postPaths.add(post4.getPath());
    Topic newTopic = createdTopic("root");
    newTopic.setId(post1.getId().replace("post", "topic"));
    newTopic.setOwner(post1.getOwner());
    newTopic.setPath(categoryId + "/" + forumId + "/" + post1.getId().replace("post", "topic"));
    newTopic.setTopicName("NewTopic");
    //split topic and move post1-post2 to new topic
    forumService.splitTopic(newTopic, post1, postPaths, "", "");
    
    assertEquals(1, forumService.getPosts(new PostFilter(topic.getPath())).getSize());
    assertEquals(4, forumService.getPosts(new PostFilter(newTopic.getPath())).getSize());
    
    //2 actitivies created after split topic
    String activityId1 = forumService.getActivityIdForOwnerPath(topic.getPath());
    ExoSocialActivity activity1 = getActivityManager().getActivity(activityId1);
    assertNotNull(activity1);
    ListAccess<ExoSocialActivity> list = getActivityManager().getCommentsWithListAccess(activity1);
    assertEquals(0, list.getSize());

    String activityId2 = forumService.getActivityIdForOwnerPath(newTopic.getPath());
    ExoSocialActivity activity2 = getActivityManager().getActivity(activityId2);
    assertNotNull(activity2);
    ListAccess<ExoSocialActivity> list2 = getActivityManager().getCommentsWithListAccess(activity2);
    //FIXME INTEG-476 - removing the old activity removes the comments of the new activities
    //assertEquals(3, list2.getSize());
    //assertEquals("message2", list2.load(0, 10)[0].getTitle());
  }

  //FIXME INTEG-476
  public void censoredTopic() throws Exception {
    Identity rootIdentity = getIdentityManager().getOrCreateIdentity(OrganizationIdentityProvider.NAME, "root", true);
    List<ExoSocialActivity> activities = getActivityManager().getActivityFeedWithListAccess(rootIdentity).loadAsList(0, 10);

    //By default, root has a topic then there is always an activity on root's stream
    assertEquals(1, activities.size());

    //root add a censored topic
    Topic topic1 = createdTopic("root");
    topic1.setTopicName("Topic1111");
    topic1.setIsWaiting(true);
    forumService.saveTopic(categoryId, forumId, topic1, true, false, new MessageBuilder());
    //the activity associated with topic1 must be hidden
    topic1 = forumService.getTopic(categoryId, forumId, topic1.getId(), "");
    String activityId1 = forumService.getActivityIdForOwnerPath(topic1.getPath());
    ExoSocialActivity activity1 = getActivityManager().getActivity(activityId1);
    assertNotNull(activity1);
    assertTrue(activity1.isHidden());
    activities = getActivityManager().getActivityFeedWithListAccess(rootIdentity).loadAsList(0, 10);
    assertEquals(1, activities.size());

    List<Topic> topics = new ArrayList<Topic>();
    //approve topic1
    topic1.setIsWaiting(false);
    topics.add(topic1);
    forumService.modifyTopic(topics, Utils.WAITING);
    activity1 = getActivityManager().getActivity(activityId1);
    assertNotNull(activity1);
    assertFalse(activity1.isHidden());
    activities = getActivityManager().getActivityFeedWithListAccess(rootIdentity).loadAsList(0, 10);
    assertEquals(2, activities.size());
  }

  //FIXME INTEG-476
  public void mergeTopics() throws Exception {
    Forum forum = forumService.getForum(categoryId, forumId);
    assertNotNull(forum);
    
    //create 2 topic
    Topic topic1 = createdTopic("root");
    topic1.setDescription("topic 1");
    Topic topic2 = createdTopic("root");
    topic2.setDescription("topic 2");
    forumService.saveTopic(categoryId, forumId, topic1, true, false, new MessageBuilder());
    forumService.saveTopic(categoryId, forumId, topic2, true, false, new MessageBuilder());
    
    //get all post of topic1, include the first post
    PostFilter filter = new PostFilter(categoryId, forumId, topic1.getId(), "", "", "", "");
    ListAccess<Post> listPost = forumService.getPosts(filter);
    assertEquals(1, listPost.getSize());
    //get all post of topic2, include the first post
    filter = new PostFilter(categoryId, forumId, topic2.getId(), "", "", "", "");
    listPost = forumService.getPosts(filter);
    assertEquals(1, listPost.getSize());
    
    Identity rootIdentity = getIdentityManager().getOrCreateIdentity(OrganizationIdentityProvider.NAME, "root", false);
    List<ExoSocialActivity> activities = getActivityManager().getActivitiesWithListAccess(rootIdentity).loadAsList(0, 10);
    //there are 3 activities of root, 1 for topic created by default + 1 for topic1 + 1 for topic2
    assertEquals(3, activities.size());
    
    String topicPath1 = categoryId + "/" + forumId + "/" + topic1.getId();
    String topicPath2 = "/exo:applications/ForumService/ForumData/CategoryHome/" + categoryId + "/" + forumId + "/" + topic2.getId();
    forumService.mergeTopic(topicPath1, topicPath2, "", "", "topicMerged");
    
    listPost = forumService.getPosts(filter);
    assertEquals(2, listPost.getSize());
    assertEquals("topic 1", (listPost.load(0, 10)[1]).getMessage());
    
    activities = getActivityManager().getActivitiesWithListAccess(rootIdentity).loadAsList(0, 10);
    assertEquals(2, activities.size());
    
    String activityId = forumService.getActivityIdForOwnerPath(topic2.getPath());
    ExoSocialActivity activity = getActivityManager().getActivity(activityId);
    assertNotNull(activity);
    assertEquals(1, getActivityManager().getCommentsWithListAccess(activity).getSize());
    assertEquals("topic 1", getActivityManager().getCommentsWithListAccess(activity).load(0, 10)[0].getTitle());
    
    //Create new topic with 2 posts
    Topic topic3 = createdTopic("root");
    topic3.setDescription("topic 3");
    forumService.saveTopic(categoryId, forumId, topic3, true, false, new MessageBuilder());
    Post post1 = createdPost("name1", "message1");
    Post post2 = createdPost("name2", "message2");
    forumService.savePost(categoryId, forumId, topic3.getId(), post1, true, new MessageBuilder());
    forumService.savePost(categoryId, forumId, topic3.getId(), post2, true, new MessageBuilder());
    
    //merge topic3 into topic2
    String topicPath3 = categoryId + "/" + forumId + "/" + topic3.getId();
    forumService.mergeTopic(topicPath3, topicPath2, "", "", "topicMerged");
    
    //topic will now have 5 posts
    filter = new PostFilter(categoryId, forumId, topic2.getId(), "", "", "", "");
    listPost = forumService.getPosts(filter);
    assertEquals(5, listPost.getSize());
    
    //check activity after merge
    activities = getActivityManager().getActivitiesWithListAccess(rootIdentity).loadAsList(0, 10);
    assertEquals(2, activities.size());
    
    activityId = forumService.getActivityIdForOwnerPath(topic2.getPath());
    activity = getActivityManager().getActivity(activityId);
    assertNotNull(activity);
    //FIXME INTEG-476 - removing the old activities removes the comments of the new activity
    //assertEquals(4, getActivityManager().getCommentsWithListAccess(activity).getSize());
    //assertEquals("topic 1", getActivityManager().getCommentsWithListAccess(activity).load(0, 10)[0].getTitle());
    //assertEquals("topic 3", getActivityManager().getCommentsWithListAccess(activity).load(0, 10)[1].getTitle());
    //assertEquals("message1", getActivityManager().getCommentsWithListAccess(activity).load(0, 10)[2].getTitle());
    //assertEquals("message2", getActivityManager().getCommentsWithListAccess(activity).load(0, 10)[3].getTitle());
  }

  public void testSplitTopicWithSpecialCharacter() throws Exception {
    Topic topic = forumService.getTopic(categoryId, forumId, topicId, "");
    //Create new topic with special characters
    topic.setTopicName("sujet avec des caractères spéciaux 1");
    topic.setDescription("Description dans le sujet avec des caractères spéciaux");
    forumService.saveTopic(categoryId, forumId, topic, false, false, new MessageBuilder());
    
    //Create some post with special characters
    Post post1 = createdPost("Re:sujet avec des caractères spéciaux 1", "Message en réponse avec des caractères spéciaux 1");
    Post post2 = createdPost("Re:sujet avec des caractères spéciaux 1", "Message en réponse avec des caractères spéciaux 2");
    Post post3 = createdPost("Re:sujet avec des caractères spéciaux 1", "Message en réponse avec des caractères spéciaux 3");
    Post post4 = createdPost("Re:sujet avec des caractères spéciaux 1", "Message en réponse avec des caractères spéciaux 4");
    forumService.savePost(categoryId, forumId, topicId, post1, true, new MessageBuilder());
    forumService.savePost(categoryId, forumId, topicId, post2, true, new MessageBuilder());
    forumService.savePost(categoryId, forumId, topicId, post3, true, new MessageBuilder());
    forumService.savePost(categoryId, forumId, topicId, post4, true, new MessageBuilder());
    List<String> postPaths = new ArrayList<String>();
    postPaths.add(post1.getPath());
    postPaths.add(post2.getPath());
    postPaths.add(post3.getPath());
    postPaths.add(post4.getPath());
    //Create new topic before split topic
    Topic newTopic = createdTopic("root");
    newTopic.setTopicName("sujet avec des caractères spéciaux 2");
    newTopic.setDescription("Description dans le sujet avec des caractères spéciaux 2");
    newTopic.setId(post1.getId().replace("post", "topic"));
    newTopic.setOwner(post1.getOwner());
    newTopic.setPath(categoryId + "/" + forumId + "/" + post1.getId().replace("post", "topic"));
    //Split topic
    forumService.splitTopic(newTopic, post1, postPaths, "", "");
    
    assertEquals(1, forumService.getPosts(new PostFilter(topic.getPath())).getSize());
    assertEquals(4, forumService.getPosts(new PostFilter(newTopic.getPath())).getSize());
    
    //2 actitivies created after split topic
    String activityId1 = forumService.getActivityIdForOwnerPath(topic.getPath());
    ExoSocialActivity activity1 = getActivityManager().getActivity(activityId1);
    assertNotNull(activity1);
    assertEquals("sujet avec des caractères spéciaux 1", activity1.getTitle());
    assertEquals("Description dans le sujet avec des caractères spéciaux", activity1.getBody());
    
    ListAccess<ExoSocialActivity> list1 = getActivityManager().getCommentsWithListAccess(activity1);
    assertEquals(0, list1.getSize());
    
    String activityId2 = forumService.getActivityIdForOwnerPath(newTopic.getPath());
    ExoSocialActivity activity2 = getActivityManager().getActivity(activityId2);
    assertNotNull(activity2);
    assertEquals("sujet avec des caractères spéciaux 2", activity2.getTitle());
    assertEquals("Description dans le sujet avec des caractères spéciaux 2", activity2.getBody());
    ListAccess<ExoSocialActivity> list2 = getActivityManager().getCommentsWithListAccess(activity2);
    //FIXME INTEG-476 - removing the old activity removes the comments of the new activities
    //assertEquals(3, list2.getSize());
    //assertEquals("Message en réponse avec des caractères spéciaux 2", list2.load(0, 10)[0].getBody());
    //assertEquals("Message en réponse avec des caractères spéciaux 3", list2.load(0, 10)[1].getBody());
    //assertEquals("Message en réponse avec des caractères spéciaux 4", list2.load(0, 10)[2].getBody());
  }
  
  public void testMovePostsWithSpecialCharacter() throws Exception {
    Topic topic1 = forumService.getTopic(categoryId, forumId, topicId, "");
    //Create new topic with special characters
    topic1.setTopicName("sujet avec des caractères spéciaux 1");
    topic1.setDescription("Description dans le sujet avec des caractères spéciaux");
    forumService.saveTopic(categoryId, forumId, topic1, false, false, new MessageBuilder());
    
    //Create some post with special characters
    Post post1 = createdPost("Re:sujet avec des caractères spéciaux 1", "Message en réponse avec des caractères spéciaux 1");
    Post post2 = createdPost("Re:sujet avec des caractères spéciaux 1", "Message en réponse avec des caractères spéciaux 2");
    forumService.savePost(categoryId, forumId, topicId, post1, true, new MessageBuilder());
    forumService.savePost(categoryId, forumId, topicId, post2, true, new MessageBuilder());
    List<String> postPaths = new ArrayList<String>();
    postPaths.add(post1.getPath());
    postPaths.add(post2.getPath());
    //Create new topic before move posts
    Topic topic2 = createdTopic("root");
    topic2.setTopicName("sujet avec des caractères spéciaux 2");
    topic2.setDescription("Description dans le sujet avec des caractères spéciaux 2");
    forumService.saveTopic(categoryId, forumId, topic2, true, false, new MessageBuilder());
    //Move posts
    forumService.movePost(postPaths.toArray(new String[postPaths.size()]), topic2.getPath(), false, "", "");
    
    assertEquals(1, forumService.getPosts(new PostFilter(topic1.getPath())).getSize());
    assertEquals(3, forumService.getPosts(new PostFilter(topic2.getPath())).getSize());
    
    //2 actitivies on AS
    String activityId1 = forumService.getActivityIdForOwnerPath(topic1.getPath());
    ExoSocialActivity activity1 = getActivityManager().getActivity(activityId1);
    assertNotNull(activity1);
    assertEquals("sujet avec des caractères spéciaux 1", activity1.getTitle());
    assertEquals("Description dans le sujet avec des caractères spéciaux", activity1.getBody());
    
    String activityId2 = forumService.getActivityIdForOwnerPath(topic2.getPath());
    ExoSocialActivity activity2 = getActivityManager().getActivity(activityId2);
    assertNotNull(activity2);
    assertEquals("sujet avec des caractères spéciaux 2", activity2.getTitle());
    assertEquals("Description dans le sujet avec des caractères spéciaux 2", activity2.getBody());
  }
  
  public void testMovePost() throws Exception {
    // create topic1
    Topic topic01 = createdTopic("root");
    topic01.setTopicName("topic01");
    topic01.setDescription("This is topic01");
    forumService.saveTopic(categoryId, forumId, topic01, true, false, new MessageBuilder());

    // create reply post on topic01
    Post post1 = createdPost("Re:topic01", "Reply01 on topic01.");
    Post post2 = createdPost("Re:topic01", "Reply02 on topic01.");
    forumService.savePost(categoryId, forumId, topicId, post1, true, new MessageBuilder());
    forumService.savePost(categoryId, forumId, topicId, post2, true, new MessageBuilder());
    List<String> postPaths = new ArrayList<String>();
    postPaths.add(post1.getPath());

    // check comments on topic01
    String commentId01 = forumService.getCommentIdForOwnerPath(post2.getPath());
    assertNotNull(commentId01);
    String commentId02 = forumService.getCommentIdForOwnerPath(post2.getPath());
    assertNotNull(commentId02);

    // create topic2
    Topic topic02 = createdTopic("root");
    topic02.setTopicName("topic02");
    topic02.setDescription("This is topic02");
    forumService.saveTopic(categoryId, forumId, topic02, true, false, new MessageBuilder());

    // move reply from topic01 to topic02
    forumService.movePost(postPaths.toArray(new String[postPaths.size()]), topic02.getPath(), false, "", "");

    // The case to check comment of moved reply not belong comment of topic01 is true but ignored
    // due to PathNotFoundException is logged out from forum service.
    // check comment on topic02
    commentId02 = forumService.getCommentIdForOwnerPath(topic02.getPath() + "/" + post1.getId());
    assertNotNull(commentId02);
  }
  
  public void testUpdateForum() throws Exception {
    Forum forum = forumService.getForum(categoryId, forumId);
    assertNotNull(forum);
    
    //create a topic
    Topic topic1 = createdTopic("root");
    topic1.setDescription("topic 1");
    forumService.saveTopic(categoryId, forumId, topic1, true, false, new MessageBuilder());
    
    String activityId1 = forumService.getActivityIdForOwnerPath(topic1.getPath());
    ExoSocialActivity activity1 = getActivityManager().getActivity(activityId1);
    assertFalse(activity1.isLocked());
    
    //close forum
    forum.setIsClosed(true);
    forumService.modifyForum(forum, Utils.CLOSE);
    activity1 = getActivityManager().getActivity(activityId1);
    assertTrue(activity1.isLocked());
    
    //open forum
    forum.setIsClosed(false);
    forumService.modifyForum(forum, Utils.CLOSE);
    activity1 = getActivityManager().getActivity(activityId1);
    assertFalse(activity1.isLocked());
    
    //lock forum
    forum.setIsLock(true);
    forumService.modifyForum(forum, Utils.LOCK);
    activity1 = getActivityManager().getActivity(activityId1);
    assertTrue(activity1.isLocked());
  }

  public void testModerateTopic() throws Exception {
    Forum forum = forumService.getForum(categoryId, forumId);
    assertNotNull(forum);
    
    //create a topic
    Topic topic1 = createdTopic("root");
    topic1.setDescription("topic 1");
    topic1.setIsModeratePost(true);
    forumService.saveTopic(categoryId, forumId, topic1, true, false, new MessageBuilder());
   
    String activityId1 = forumService.getActivityIdForOwnerPath(topic1.getPath());
    ExoSocialActivity activity1 = getActivityManager().getActivity(activityId1);
    assertNotNull(activity1);
    List<ExoSocialActivity> comments = getActivityManager().getCommentsWithListAccess(activity1).loadAsList(0, 10);
    assertEquals(0, comments.size());
    
    Post post1 = createdPost("Re:topic1", "Reply1 on topic1.");
    post1.setIsApproved(false);
    forumService.savePost(categoryId, forumId, topic1.getId(), post1, true, new MessageBuilder());
    
    activity1 = getActivityManager().getActivity(activityId1);
    comments = getActivityManager().getCommentsWithListAccess(activity1).loadAsList(0, 10);
    // FIXME INTEG-476 - getActivityManager().getCommentsWithListAccess() returns all the comments, no matter if there are hidden or not
    //assertEquals(0, comments.size());
    
    forumService.modifyPost(Arrays.asList(post1), Utils.APPROVE);
    activity1 = getActivityManager().getActivity(activityId1);
    comments = getActivityManager().getCommentsWithListAccess(activity1).loadAsList(0, 10);
    assertEquals(1, comments.size());
  }

  public void testQuotePost() throws Exception {
    Topic topic = forumService.getTopic(categoryId, forumId, topicId, "");

    //Create new topic with special characters
    topic.setTopicName("Topic name");
    topic.setDescription("Topic description");
    forumService.saveTopic(categoryId, forumId, topic, true, false, new MessageBuilder());

    //Create some post with special characters
    Post post1 = createdPost("Post comment 1", "Post comment content 1");
    forumService.savePost(categoryId, forumId, topicId, post1, true, new MessageBuilder());

    Post post2 = createdPost("Post reply to comment 1 - 1", "Post reply to comment content 1 - 1");
    post2.setQuotedPostId(post1.getId());
    forumService.savePost(categoryId, forumId, topicId, post2, true, new MessageBuilder());

    Post post3 = createdPost("Post reply to comment 1 - 2", "Post reply to comment content 1 - 2");
    post3.setQuotedPostId(post1.getId());
    forumService.savePost(categoryId, forumId, topicId, post3, true, new MessageBuilder());

    Post post4 = createdPost("Post comment 2", "Post comment content 2");
    forumService.savePost(categoryId, forumId, topicId, post4, true, new MessageBuilder());

    assertEquals(5, forumService.getPosts(new PostFilter(topic.getPath())).getSize());

    String activityId1 = forumService.getActivityIdForOwnerPath(topic.getPath());
    ExoSocialActivity activity1 = getActivityManager().getActivity(activityId1);
    assertNotNull(activity1);
    assertEquals(topic.getTopicName(), activity1.getTitle());
    assertEquals(topic.getDescription(), activity1.getBody());

    end();
    begin();

    ListAccess<ExoSocialActivity> list1 = getActivityManager().getCommentsWithListAccess(activity1, true);
    assertEquals(2, list1.getSize());
    ExoSocialActivity[] comments = list1.load(0, -1);

    assertEquals(4, comments.length);

    assertEquals(post1.getMessage(), comments[0].getTitle());
    assertEquals(post1.getMessage(), comments[0].getBody());
    assertNull(comments[0].getParentCommentId());

    assertEquals(post2.getMessage(), comments[1].getTitle());
    assertEquals(post2.getMessage(), comments[1].getBody());
    assertNotNull(comments[1].getParentCommentId());
    assertEquals(comments[0].getId(), comments[1].getParentCommentId());

    assertEquals(post3.getMessage(), comments[2].getTitle());
    assertEquals(post3.getMessage(), comments[2].getBody());
    assertNotNull(comments[2].getParentCommentId());
    assertEquals(comments[0].getId(), comments[2].getParentCommentId());

    assertEquals(post4.getMessage(), comments[3].getTitle());
    assertEquals(post4.getMessage(), comments[3].getBody());
    assertNull(comments[3].getParentCommentId());
  }

  private ActivityManager getActivityManager() {
    return (ActivityManager) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(ActivityManager.class);
  }
  
  private IdentityManager getIdentityManager() {
    return (IdentityManager) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(IdentityManager.class);
  }

  private ExoSocialActivity createComment(ExoSocialActivity existingActivity,
                                          String posterIdentity,
                                          String commentTitle,
                                          String commentMessage,
                                          String parentCommentId) {
    ExoSocialActivity comment = new ExoSocialActivityImpl();
    comment.setTitle(commentTitle);
    comment.setBody(commentMessage);
    comment.setParentCommentId(parentCommentId);
    comment.setUserId(posterIdentity);
    return comment;
  }
}
