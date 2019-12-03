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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.forum.ext.activity;

import java.util.Map;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.StringCommonUtils;
import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.forum.ext.impl.PollSpaceActivityPublisher;
import org.exoplatform.forum.service.Category;
import org.exoplatform.forum.service.Forum;
import org.exoplatform.forum.service.ForumService;
import org.exoplatform.forum.service.Post;
import org.exoplatform.forum.service.Topic;
import org.exoplatform.forum.service.Utils;
import org.exoplatform.forum.service.impl.model.PostFilter;
import org.exoplatform.poll.service.Poll;
import org.exoplatform.poll.service.PollService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

/**
 * Created by The eXo Platform SAS
 * Author : thanh_vucong
 *          thanh_vucong@exoplatform.com
 * Jan 10, 2013  
 */
public class ForumActivityUtils {

  private static final Log LOG = ExoLogger.getLogger(ForumActivityUtils.class);

  private static final int TYPE_PRIVATE = 2;
  /*
   * INTEG-391 : Do not keep the services on this class, make thread safety issue in multitenant environment
   */
  
  public static Identity getSpaceIdentity(String forumId) {
    Space space = getSpaceService().getSpaceByGroupId(getSpaceGroupId(forumId));
    Identity spaceIdentity = null;
    if (space != null) {
      spaceIdentity = getIdentityManager().getOrCreateIdentity(SpaceIdentityProvider.NAME, space.getPrettyName(), false);
    }
    return spaceIdentity;
  }
  
  public static String getSpaceGroupId(String forumId) {
    String groupId = forumId.replaceFirst(Utils.FORUM_SPACE_ID_PREFIX, "");
    String spaceGroupId = SpaceUtils.SPACE_GROUP + CommonUtils.SLASH + groupId;
    return spaceGroupId;
  }

  public static boolean hasSpace(String forumId) {
    return !Utils.isEmpty(forumId) && forumId.indexOf(Utils.FORUM_SPACE_ID_PREFIX) >= 0;
  }
  
  public static boolean isCategoryPublic(Category category) {
    // the category is public when it does not restrict viewers and private users.
    return category != null && Utils.isEmpty(category.getViewer()) && Utils.isEmpty(category.getUserPrivate());
  }
 
  public static boolean isForumPublic(Forum forum) {
    // the forum is public when it does not restrict viewers and is opening.
    return forum != null && !forum.getIsClosed() && Utils.isEmpty(forum.getViewer());
  }
  
  public static boolean isTopicPublic(Topic topic) {
    // the topic is public when it is active, not waiting, not closed yet and does not restrict users
    return topic != null && topic.getIsActive() && topic.getIsApproved() && !topic.getIsWaiting() && !topic.getIsClosed() && Utils.isEmpty(topic.getCanView());
  }
  
  public static boolean isPostPublic(Post post) {
    // the post is public when it is not private, not hidden by censored words, active by topic and not waiting for approval
    return post != null && post.getUserPrivate().length != TYPE_PRIVATE && !post.getIsWaiting() && !post.getIsHidden() && post.getIsActiveByTopic() && post.getIsApproved();
  }
  
  public static Topic getTopic(ForumActivityContext ctx) throws Exception {
    Post p = ctx.getPost();
    return getForumService().getTopic(p.getCategoryId(),
                                                         p.getForumId(),
                                                         p.getTopicId(),
                                                         "");
  }
  
  public static void takeActivityBack(Topic topic, ExoSocialActivity activity) {
    takeActivityBack(topic, activity.getId());
  }
  
  public static void takeActivityBack(Topic topic, String activityId) {
    getForumService().saveActivityIdForOwnerPath(topic.getPath(), activityId);
  }
  
  public static void takeCommentBack(Post post, ExoSocialActivity comment) {
    takeCommentBack(post, comment.getId());
  }
  
  public static void takeCommentBack(Post post, String commentId) {
    getForumService().saveActivityIdForOwnerPath(post.getPath(), commentId);
  }
  
  public static void updateTopicPostCount(ForumActivityContext ctx, boolean isAdded) throws Exception {
    Topic topic = getTopic(ctx);
    ctx.setTopic(topic);
    ExoSocialActivity topicActivity = getActivityOfTopic(ctx);
    Map<String, String> templateParams = topicActivity.getTemplateParams();
    templateParams.put(ForumActivityBuilder.TOPIC_POST_COUNT_KEY, "" + topic.getPostCount());
    getActivityManager().updateActivity(topicActivity);
  }
  
  public static void updateActivityByTopic(Topic topic, ExoSocialActivity activity) throws Exception {
    String topicPath = topic.getPath();
    PostFilter filter = new PostFilter(Utils.getCategoryId(topicPath), Utils.getForumId(topicPath), Utils.getTopicId(topicPath), "", "", "", "");
    ListAccess<Post> listPost = getForumService().getPosts(filter);
    
    //After split, if a topic has only one post (include the post by default of topic),
    //this topic is considered "has no reply" then no comment will be added to the activity 
    if (listPost.getSize() <= 1) {
      return;
    }
    
    Post[] posts = listPost.load(0, listPost.getSize());
    ActivityManager am = getActivityManager();
    int count = 0;
    
    //
    for (Post post : posts) {
      //The first post of destination topic will be ignored
      if (post.getId().equals(topic.getId().replace(Utils.TOPIC, Utils.POST))) {
        continue;
      }
      String commentId = getForumService().getActivityIdForOwnerPath(post.getPath());
      ExoSocialActivity comment = null;
      if (commentId != null) {
        comment = am.getActivity(commentId);
      }
      //when comment associated to post doesn't exist, we need to create new comment
      //Case of migrate data or for the first post of the source topic
      if (commentId == null || comment == null) {
        comment = ForumActivityBuilder.createActivityComment(post, ForumActivityContext.makeContextForAddPost(post));
        comment.setUserId(getIdentity(post.getOwner()).getId());
      }
      comment.setTitle(ForumActivityBuilder.processContent(post.getMessage(), 3));
      am.saveComment(activity, comment);
      takeCommentBack(post, comment);
      count++;
    }
    
    Map<String, String> templateParams = activity.getTemplateParams();
    templateParams.put(ForumActivityBuilder.TOPIC_POST_COUNT_KEY, "" + count);
    activity.setTemplateParams(templateParams);
    am.updateActivity(activity);
  }
  
  public static void saveTopicActivity(Identity poster, Identity streamOwner, ExoSocialActivity activity, Topic topic) throws Exception {
    //Save activity only in case the owner and poster of new topic exist
    if (poster != null && streamOwner != null) {
      activity.setUserId(poster.getId());
      getActivityManager().saveActivityNoReturn(streamOwner, activity);
      //update comment for activity's topic
      updateActivityByTopic(topic, activity);
    }
  }
  
  /**
   * Gets ActivityId from existing Topic in Context.
   * If is NULL, create new Activity for Topic.
   * @param ctx
   * @return
   */
  public static String getActivityId(ForumActivityContext ctx) {
    ForumService fs = getForumService();
    String activityId = fs.getActivityIdForOwnerPath(ctx.getTopic().getPath());
    
    //
    if (Utils.isEmpty(activityId)) {
      TopicActivityTask task = TopicActivityTask.ADD_TOPIC;
      ExoSocialActivity got = ActivityExecutor.execute(task, ctx);
      
      //
      takeActivityBack(ctx.getTopic(), got);
      activityId = got.getId();
    }
    
    return activityId;
  }
  
  /**
   * Gets ActivityId from existing Topic in Context.
   * If is NULL, create new Activity for Topic.
   * @param ctx
   * @return
   */
  public static ExoSocialActivity getActivityOfTopic(ForumActivityContext ctx) {
    ForumService fs = getForumService();
    String activityId = fs.getActivityIdForOwnerId(ctx.getTopic().getId());

    //
    if (Utils.isEmpty(activityId)) {
      return makeActivity(ctx);
    }

    //
    ExoSocialActivity got = getActivityManager().getActivity(activityId);

    //
    if (got == null) {
      got = makeActivity(ctx);
    }

    //title and body of activity may contain specials characters and when we get activity, the special character will be encoded
    //To avoid activity use these encodes when update, we set title and body = null
    got.setBody(null);
    got.setTitle(null);

    return got;
  }
  

  /**
   * Gets activity's comment from existing post in Context.
   * If is NULL, create new Activity for Topic.
   * 
   * @param postPath
   * @return
   */
  public static ExoSocialActivity getCommentOfPost(String postPath) {
    ForumService fs = getForumService();
    String activityId = fs.getActivityIdForOwnerPath(postPath);

    ActivityManager am = ForumActivityUtils.getActivityManager();

    //
    if (Utils.isEmpty(activityId)) {
      return null;
    }

    //
    ExoSocialActivity got = am.getActivity(activityId);

    return got;
  }
  
  private static ExoSocialActivity makeActivity(ForumActivityContext ctx) {
    TopicActivityTask task = TopicActivityTask.ADD_TOPIC;
    ExoSocialActivity got = ActivityExecutor.execute(task, ctx);
    
    //
    ForumActivityUtils.takeActivityBack(ctx.getTopic(), got);
    
    return got;
  }
  /**
   * Deletes Activities
   * 
   * @param activityIds
   */
  public static void removeActivities(String ... activityIds) {
    ActivityManager am = getActivityManager();
    for(String activityId : activityIds) {
      try {
        am.deleteActivity(activityId);
      } catch(Exception e) {
        LOG.error("Cannot delete activity " + activityId, e);
      }
    }
  }
  
  /**
   * Deletes comment
   * 
   * @param activityId
   * @param commentId
   */
  public static void removeComment(String activityId, String commentId) {
    ActivityManager am = getActivityManager();
    ExoSocialActivity activity = am.getActivity(activityId);
    if (activity == null)
      return;
    activity = ForumActivityBuilder.updateNumberOfReplies(activity, true);
    activity.setTitle(null);
    activity.setBody(null);
    am.updateActivity(activity);
    am.deleteComment(activityId, commentId);
  }

  public static void updateActivities(ExoSocialActivity activity) {
    ActivityManager am = getActivityManager();
    am.updateActivity(activity);
  }

  public static ForumService getForumService() {
    return CommonsUtils.getService(ForumService.class);
  }

  public static ActivityManager getActivityManager() {
    return CommonsUtils.getService(ActivityManager.class);
  }

  public static IdentityManager getIdentityManager() {
    return CommonsUtils.getService(IdentityManager.class);
  }

  public static SpaceService getSpaceService() {
    return CommonsUtils.getService(SpaceService.class);
  }

  public static Identity getIdentity(String remoteId) {
    return getIdentityManager().getOrCreateIdentity(OrganizationIdentityProvider.NAME, remoteId, false);
  }
  /**
   * Gets ActivityId of poll from existing Topic in Context.
   * @param ctx
   * @return
   */
  public static ExoSocialActivity getActivityOfPollTopic(ForumActivityContext ctx) {
    String path = ctx.getTopic().getPath().concat("/").concat(ctx.getTopic().getId().replace(Utils.TOPIC, Utils.POLL));
    String pollActivityId = getForumService().getActivityIdForOwnerPath(path);
    ExoSocialActivity got = getActivityManager().getActivity(pollActivityId);
    return got;
  }
  
  public static String getActivityParamValue(ExoSocialActivity activity, String key) {
    String value = null;
    Map<String, String> params = activity.getTemplateParams();
    if (params != null) {
      value = params.get(key);
    }
    return value != null ? value : "";
  }
  
  public static Topic getTopic(ExoSocialActivity activity) {
    String categoryId = getActivityParamValue(activity, ForumActivityBuilder.CATE_ID_KEY);
    String forumId = getActivityParamValue(activity, ForumActivityBuilder.FORUM_ID_KEY);
    String topicId = getActivityParamValue(activity, ForumActivityBuilder.TOPIC_ID_KEY);
    if(CommonUtils.isEmpty(topicId)) {
      return null;
    }
    try {
      return ForumActivityUtils.getForumService().getTopic(categoryId, forumId, topicId, "");
    } catch (Exception e) {
      return null;
    }
  }
  
  public static Post getPost(ExoSocialActivity activity) {
    String categoryId = getActivityParamValue(activity, ForumActivityBuilder.CATE_ID_KEY);
    String forumId = getActivityParamValue(activity, ForumActivityBuilder.FORUM_ID_KEY);
    String topicId = getActivityParamValue(activity, ForumActivityBuilder.TOPIC_ID_KEY);
    String postId = getActivityParamValue(activity, ForumActivityBuilder.POST_ID_KEY);
    if(CommonUtils.isEmpty(postId)) {
      return null;
    }
    try {
      return ForumActivityUtils.getForumService().getPost(categoryId, forumId, topicId, postId);
    } catch (Exception e) {
      return null;
    }
  }
  
  public static Poll getPoll(ExoSocialActivity activity) {
    String pollId = getActivityParamValue(activity, PollSpaceActivityPublisher.POLL_ID);
    if (pollId.indexOf(Utils.TOPIC) == 0) {
      pollId = pollId.replace(Utils.TOPIC, Utils.POLL);
    }
    try {
      return CommonsUtils.getService(PollService.class).getPoll(pollId);
    } catch (Exception e) {
      return null;
    }
  }
}
