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

import org.apache.commons.lang.StringUtils;

import org.exoplatform.forum.service.ForumService;
import org.exoplatform.forum.service.Topic;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.ActivityManager;

import java.util.Map;

/**
 * Created by The eXo Platform SAS
 * Author : thanh_vucong
 *          thanh_vucong@exoplatform.com
 * Jan 9, 2013  
 */
public abstract class PostActivityTask implements ActivityTask<ForumActivityContext> {

  protected static final Log   LOG = ExoLogger.getLogger(PostActivityTask.class);
  
  @Override
  public void start(ForumActivityContext ctx) { }
  
  @Override
  public void end(ForumActivityContext ctx) { }
  
  protected abstract ExoSocialActivity processTitle(ExoSocialActivity activity);
  protected abstract ExoSocialActivity processActivity(ForumActivityContext ctx, ExoSocialActivity activity);
  
  protected ExoSocialActivity processComment(ForumActivityContext ctx, ExoSocialActivity comment) {
    return processTitle(comment); 
  }
  
  public static PostActivityTask ADD_POST = new PostActivityTask() {

    @Override
    public ExoSocialActivity processTitle(ExoSocialActivity activity) {
      return ForumActivityType.ADD_POST.getActivity(activity, activity.getTitle());
    }
    
    @Override
    protected ExoSocialActivity processComment(ForumActivityContext ctx, ExoSocialActivity comment) {
      //censoring status or not approved yet, hidden post's comment in stream
      if (ctx.getPost().getIsWaiting() || !ctx.getPost().getIsApproved()) {
        comment.isHidden(true);
      }

      String parentPostId = ctx.getPost().getQuotedPostId();
      if(StringUtils.isNotBlank(parentPostId)) {
        ForumService forumService = ForumActivityUtils.getForumService();
        String parentCommentId = forumService.getCommentIdForOwnerId(parentPostId);
        if (parentCommentId != null) {
          ExoSocialActivity parentCommentActivity =
                                                  ForumActivityUtils.getActivityManager()
                                                                    .getActivity(parentCommentId);
          if (parentCommentActivity != null) {
            if (parentCommentActivity.getParentCommentId() != null) {
              parentCommentId = parentCommentActivity.getParentCommentId();
            }
            comment.setParentCommentId(parentCommentId);
          }
        }
      }

      return processTitle(comment);
    }
    
    @Override
    protected ExoSocialActivity processActivity(ForumActivityContext ctx, ExoSocialActivity topicActivity) {
      Map<String, String> templateParams = topicActivity.getTemplateParams();
      
      templateParams.put(ForumActivityBuilder.TOPIC_POST_COUNT_KEY, "" + ctx.getTopic().getPostCount());
      return topicActivity;
    };

    @Override
    public ExoSocialActivity execute(ForumActivityContext ctx) {
      try {
        String postActivityId = ForumActivityUtils.getForumService().getActivityIdForOwnerPath(ctx.getPost().getPath());
        if (StringUtils.isNotBlank(postActivityId)) {
          ExoSocialActivity commentOfPost = ForumActivityUtils.getCommentOfPost(ctx.getPost().getPath());
          if (commentOfPost == null) {
            return null;
          } else {
            return commentOfPost;
          }
        }

        Topic topic = ForumActivityUtils.getTopic(ctx);
        ctx.setTopic(topic);
        
        //FORUM_33 case: update topic activity's number of reply 
        ExoSocialActivity topicActivity = ForumActivityUtils.getActivityOfTopic(ctx);
        //
        if(topicActivity == null) {
          return null;
        }
        Map<String, String> templateParams = topicActivity.getTemplateParams();
        templateParams.put(ForumActivityBuilder.TOPIC_POST_COUNT_KEY, "" + topic.getPostCount());
        
        ActivityManager am = ForumActivityUtils.getActivityManager();
        
        //
        if (am.getActivity(topicActivity.getId()) == null) {
          return null;
        }
        
        am.updateActivity(topicActivity);
        
        //add new comment with title: first 3 lines
        ExoSocialActivity newComment = ForumActivityBuilder.createActivityComment(ctx.getPost(), ctx);
        newComment = processComment(ctx, newComment);
        
        //
        Identity poster = ForumActivityUtils.getIdentity(ctx.getPost().getOwner());
        newComment.setUserId(poster.getId());
        
        //
        am.saveComment(topicActivity, newComment);
        
        
        return newComment;
      } catch (Exception e) {
        LOG.warn("Can not record comment when add post : " + ctx.getPost().getId());
        LOG.debug(e.getMessage(), e);
      }
      return null;
    }
    
  };
  
  public static PostActivityTask UPDATE_POST = new PostActivityTask() {
    
    @Override
    protected ExoSocialActivity processTitle(ExoSocialActivity activity) {
      //where $value is first 3 lines of the reply
      return ForumActivityType.UPDATE_POST.getActivity(activity, activity.getTitle());
    }
    
    @Override
    protected ExoSocialActivity processComment(ForumActivityContext ctx, ExoSocialActivity comment) {
      ExoSocialActivity newComment = ForumActivityBuilder.createActivityComment(ctx.getPost(), ctx);
      if (comment != null) {
        comment.setTitle(newComment.getTitle());
        comment.setTitleId(newComment.getTitleId());
        comment.setTemplateParams(newComment.getTemplateParams());
        comment = processTitle(comment);
      } else {
        comment = newComment;
        comment = processTitle(comment);
      }
      return comment;
    };
    
    @Override
    protected ExoSocialActivity processActivity(ForumActivityContext ctx, ExoSocialActivity topicActivity) {
      return topicActivity;
    };
    
    @Override
    public ExoSocialActivity execute(ForumActivityContext ctx) {
      try {
        Topic topic = ForumActivityUtils.getTopic(ctx);
        ctx.setTopic(topic);
        
        //FORUM_34 case: update activity's title
        //update comment for updated post 
        ExoSocialActivity topicActivity = ForumActivityUtils.getActivityOfTopic(ctx);
        
        //
        ActivityManager am = ForumActivityUtils.getActivityManager();
        
        //Get comment corresponding to this post, null if don't exist
        ExoSocialActivity comment = ForumActivityUtils.getCommentOfPost(ctx.getPost().getPath());
        
        boolean isCommentExist = false;
        if (comment != null)
          isCommentExist = true;
        
        comment = processComment(ctx, comment);
        
        if (isCommentExist) {
          am.updateActivity(comment);
        } else {
          Identity poster = ForumActivityUtils.getIdentity(ctx.getPost().getOwner());
          comment.setUserId(poster.getId());
          am.saveComment(topicActivity, comment);
        }
        
        return comment;
      } catch (Exception e) {
        LOG.warn("Can not record Comment when updates post: " + ctx.getPost().getId());
        LOG.debug(e.getMessage(), e);
      }
      
      return null;
    }
    
  };
  
  public static PostActivityTask HIDE_POST = new PostActivityTask() {
    
    @Override
    protected ExoSocialActivity processTitle(ExoSocialActivity activity) {
      return activity;
    }
    
    @Override
    protected ExoSocialActivity processActivity(ForumActivityContext ctx, ExoSocialActivity topicActivity) {
      return topicActivity;
    };
    
    @Override
    public ExoSocialActivity execute(ForumActivityContext ctx) {
      try {
        ActivityManager am = ForumActivityUtils.getActivityManager();
        
        //FORUM_33 case: update topic activity's number of reply 
        ForumActivityUtils.updateTopicPostCount(ctx, false);
        
        String postActivityId = ForumActivityUtils.getForumService().getActivityIdForOwnerPath(ctx.getPost().getPath());
        ExoSocialActivity postActivity = null;
        if (postActivityId != null) {
          postActivity = am.getActivity(postActivityId);
          if (postActivity != null) {
            postActivity.isHidden(true);
            am.updateActivity(postActivity);
          }
        }
        return postActivity;
      } catch (Exception e) {
        LOG.warn("Can not hide comment when hide post: " + ctx.getPost().getId());
        LOG.debug(e.getMessage(), e);
      }
      return null;
    }
  };
  
  public static PostActivityTask UNHIDE_POST = new PostActivityTask() {
    
    @Override
    protected ExoSocialActivity processTitle(ExoSocialActivity activity) {
      return activity;
    }
    
    @Override
    protected ExoSocialActivity processActivity(ForumActivityContext ctx, ExoSocialActivity topicActivity) {
      return topicActivity;
    };
    
    @Override
    public ExoSocialActivity execute(ForumActivityContext ctx) {
      try {
        ActivityManager am = ForumActivityUtils.getActivityManager();
        
        //FORUM_33 case: update topic activity's number of reply 
        ForumActivityUtils.updateTopicPostCount(ctx, true);
        
        String postActivityId = ForumActivityUtils.getForumService().getActivityIdForOwnerPath(ctx.getPost().getPath());
        ExoSocialActivity postActivity = null;
        if (postActivityId != null) {
          postActivity = am.getActivity(postActivityId);
          if (postActivity != null) {
            postActivity.isHidden(false);
            am.updateActivity(postActivity);
          }
        }
        return postActivity;
      } catch (Exception e) {
        LOG.warn("Can not unhide comment when unhide post: " + ctx.getPost().getId());
        LOG.debug(e.getMessage(), e);
      }
      
      return null;
    }
    
  };
}
