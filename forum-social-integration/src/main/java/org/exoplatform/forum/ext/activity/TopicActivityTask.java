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

import java.util.Map;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.PropertyChangeSupport;
import org.exoplatform.commons.utils.StringCommonUtils;
import org.exoplatform.forum.common.UserHelper;
import org.exoplatform.forum.service.Topic;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;

/**
 * Created by The eXo Platform SAS
 * Author : thanh_vucong
 *          thanh_vucong@exoplatform.com
 * Jan 9, 2013  
 */
public abstract class TopicActivityTask implements ActivityTask<ForumActivityContext> {
  protected static final Log   LOG = ExoLogger.getLogger(TopicActivityTask.class);
  
  public static final String SPACE_GROUP_ID  = "SpaceGroupId";

  /**
   * 
   * @param activity
   * @return
   */
  protected abstract ExoSocialActivity processTitle(ForumActivityContext ctx, ExoSocialActivity activity);
    
  protected abstract ExoSocialActivity processActivity(ForumActivityContext ctx, ExoSocialActivity activity);
  
  protected ExoSocialActivity processComment(ForumActivityContext ctx) {
    ExoSocialActivity activity = ForumActivityBuilder.createActivityComment(ctx.getTopic(), ctx);
    return processTitle(ctx, activity); 
  }

  public static TopicActivityTask ADD_TOPIC = new TopicActivityTask() {
    @Override
    protected ExoSocialActivity processTitle(ForumActivityContext ctx, ExoSocialActivity activity) {
      return ForumActivityType.ADD_TOPIC.getActivity(activity, activity.getTitle());
    }
    
    @Override
    protected ExoSocialActivity processActivity(ForumActivityContext ctx, ExoSocialActivity activity) {
      
      //censoring status, hidden topic's activity in stream
      if (ctx.getTopic().getIsWaiting()) {
        activity.isHidden(true);
      }
      
      //unapprove status, lock topic's activity in stream
      if (ctx.getTopic().getIsApproved() == false) {
        activity.isLocked(true);
      }
      return processTitle(ctx, activity); 
    }

    @Override
    public ExoSocialActivity execute(ForumActivityContext ctx) {
      try {

        ActivityManager am = ForumActivityUtils.getActivityManager();
        Identity streamOwner = getOwnerStream(ctx);
        
        ////FORUM_01 case: creates topic
        ExoSocialActivity newActivity = ForumActivityBuilder.createActivity(ctx.getTopic(), ctx);
        newActivity = processActivity(ctx, newActivity);
        
        //
        Identity poster = ForumActivityUtils.getIdentity(ctx.getTopic().getOwner());
        newActivity.setUserId(poster.getId());
        
        am.saveActivityNoReturn(streamOwner, newActivity);
        
        return newActivity;
      } catch (Exception e) {
        LOG.warn("Can not record Activity for when add topic's id: " + ctx.getTopic().getId());
        LOG.debug(e.getMessage(), e);
        return null;
      }
    }
    
  };
  
  public static TopicActivityTask UPDATE_TOPIC_PROPERTIES = new TopicActivityTask() {

    @Override
    protected ExoSocialActivity processTitle(ForumActivityContext ctx, ExoSocialActivity activity) {
      return activity;
    }
    
    protected ExoSocialActivity processComment(ForumActivityContext ctx) {
      ExoSocialActivity newComment = ForumActivityBuilder.createActivityComment(ctx.getTopic(), ctx);
      PropertyChangeSupport newPcs = ctx.getPcs();
      Topic topic = ctx.getTopic();
      StringBuilder sb = new StringBuilder();
      //
      if (newPcs.hasPropertyName(Topic.TOPIC_NAME)) {
        String topicName = topic.getTopicName();
        ForumActivityType.UPDATE_TOPIC_TITLE.getTitle(newComment, StringCommonUtils.decodeSpecialCharToHTMLnumberIgnore(topicName));
        //
        String title = ForumActivityType.UPDATE_TOPIC_TITLE.getTitleTemplate();
        title = String.format(title, StringCommonUtils.decodeSpecialCharToHTMLnumber(topicName));
        sb.append(title).append("\n");
      }

      if (newPcs.hasPropertyName(Topic.TOPIC_CONTENT)) {
        String description = topic.getDescription();
        sb.append(ForumActivityType.UPDATE_TOPIC_CONTENT.getTitle(newComment, description));
      }

      newComment.setTitle(sb.toString());
      return newComment;
    }
    
    @Override
    protected ExoSocialActivity processActivity(ForumActivityContext ctx, ExoSocialActivity activity) {
      activity.setTitle(StringCommonUtils.decodeSpecialCharToHTMLnumber(ctx.getTopic().getTopicName()));
      activity.setBody(ForumActivityBuilder.processContent(ctx.getTopic().getDescription(), 4));
      
      return activity;
    };

    @Override
    public ExoSocialActivity execute(ForumActivityContext ctx) {
      try {
        
        ActivityManager am = ForumActivityUtils.getActivityManager();
        ExoSocialActivity a = ForumActivityUtils.getActivityOfTopic(ctx);
        
        //
        a = processActivity(ctx, a);
        am.updateActivity(a);
        
        //FORUM_12 case: update topic's title
        ExoSocialActivity newComment = processComment(ctx);
        
        //
        Identity poster = ForumActivityUtils.getIdentity(ctx.getTopic().getModifiedBy());
        newComment.setUserId(poster.getId());
        
        //
        am.saveComment(a, newComment);
        
        return newComment;
      } catch (Exception e) {
        LOG.warn("Can not record Comment for when update topic: " + ctx.getTopic().getId());
        LOG.debug(e.getMessage(), e);
      }
      return null;
    }
    
  };
  
  public static TopicActivityTask UPDATE_TOPIC_TITLE = new TopicActivityTask() {

    @Override
    protected ExoSocialActivity processTitle(ForumActivityContext ctx, ExoSocialActivity activity) {
      // only apply for new comment
      String topicName = ctx.getTopic().getTopicName();
      String title = ForumActivityType.UPDATE_TOPIC_TITLE.getTitleTemplate();
      title = String.format(title, StringCommonUtils.decodeSpecialCharToHTMLnumber(topicName));
      activity.setTitle(title);
      //
      ForumActivityType.UPDATE_TOPIC_TITLE.getTitle(activity, StringCommonUtils.decodeSpecialCharToHTMLnumberIgnore(topicName));
      return activity;
    }
    
    @Override
    protected ExoSocialActivity processActivity(ForumActivityContext ctx, ExoSocialActivity activity) {
      activity.setTitle(StringCommonUtils.decodeSpecialCharToHTMLnumber(ctx.getTopic().getTopicName()));
      return activity;
    };

    @Override
    public ExoSocialActivity execute(ForumActivityContext ctx) {
      try {
        
        ActivityManager am = ForumActivityUtils.getActivityManager();
        ExoSocialActivity a = ForumActivityUtils.getActivityOfTopic(ctx);
        
        //
        a = processActivity(ctx, a);
        am.updateActivity(a);
        
        //FORUM_12 case: update topic's title
        ExoSocialActivity newComment = processComment(ctx);
        
        //
        Identity poster = ForumActivityUtils.getIdentity(ctx.getTopic().getModifiedBy());
        newComment.setUserId(poster.getId());
        //
        am.saveComment(a, newComment);
        
        return newComment;
      } catch (Exception e) {
        LOG.warn("Can not record Comment for when update topic: " + ctx.getTopic().getId());
        LOG.debug(e.getMessage(), e);
      }
      return null;
    }
    
  };
  
  public static TopicActivityTask UPDATE_TOPIC_CONTENT = new TopicActivityTask() {

    @Override
    protected ExoSocialActivity processTitle(ForumActivityContext ctx, ExoSocialActivity activity) {
      return ForumActivityType.UPDATE_TOPIC_CONTENT.getActivity(activity);
    }
    
    @Override
    protected ExoSocialActivity processActivity(ForumActivityContext ctx, ExoSocialActivity activity) {
      activity.setBody(ForumActivityBuilder.processContent(ctx.getTopic().getDescription(), 4));
      //processTitle(ctx, activity);
      return activity;
    };

    @Override
    public ExoSocialActivity execute(ForumActivityContext ctx) {
      try {
        
        ActivityManager am = ForumActivityUtils.getActivityManager();
        
        ExoSocialActivity a = ForumActivityUtils.getActivityOfTopic(ctx);
        
        a = processActivity(ctx, a);
        am.updateActivity(a);

        //FORUM_13 case: update topic's content
        ExoSocialActivity newComment = processComment(ctx);
        
        //
        Identity poster = ForumActivityUtils.getIdentity(ctx.getTopic().getModifiedBy());
        newComment.setUserId(poster.getId());
        //
        am.saveComment(a, newComment);
        
        return newComment;
      } catch (Exception e) {
        LOG.warn("Can not record Comment for when update topic's id: " + ctx.getTopic().getId());
        LOG.debug(e.getMessage(), e);
      }
      return null;
    }
    
  };
  
  public static TopicActivityTask UPDATE_TOPIC_RATE = new TopicActivityTask() {

    @Override
    protected ExoSocialActivity processTitle(ForumActivityContext ctx, ExoSocialActivity activity) {
      return ForumActivityType.UPDATE_TOPIC_RATE.getActivity(activity, "" + ctx.getTopic().getVoteRating());
    }
    
    @Override
    protected ExoSocialActivity processActivity(ForumActivityContext ctx, ExoSocialActivity activity) {
      Map<String, String> templateParams = activity.getTemplateParams();
      templateParams.put(ForumActivityBuilder.TOPIC_VOTE_RATE_KEY, "" + ctx.getTopic().getVoteRating());
      
      //processTitle(ctx, activity);
      
      return activity;
    };

    @Override
    public ExoSocialActivity execute(ForumActivityContext ctx) {
      try {
        
        ActivityManager am = ForumActivityUtils.getActivityManager();
        ExoSocialActivity a = ForumActivityUtils.getActivityOfTopic(ctx);
        
        a = processActivity(ctx, a);
        am.updateActivity(a);

        //FORUM_13 case: update topic's content
        ExoSocialActivity newComment = processComment(ctx);
        
        //
        Identity poster = ForumActivityUtils.getIdentity(UserHelper.getCurrentUser());
        newComment.setUserId(poster.getId());
        
        //
        am.saveComment(a, newComment);
        
        return newComment;
      } catch (Exception e) {
        LOG.warn("Can not record Comment for when update topic's id: " + ctx.getTopic().getId());
        LOG.debug(e.getMessage(), e);
      }
      return null;
    }
    
  };
  
  public static TopicActivityTask CLOSE_TOPIC = new TopicActivityTask() {

    @Override
    protected ExoSocialActivity processTitle(ForumActivityContext ctx, ExoSocialActivity activity) {
      return ForumActivityType.CLOSE_TOPIC.getActivity(activity);
    }
    
    @Override
    protected ExoSocialActivity processActivity(ForumActivityContext ctx, ExoSocialActivity activity) {
      
      activity.isLocked(true);
      return activity;
    };

    @Override
    public ExoSocialActivity execute(ForumActivityContext ctx) {
      try {
        
        ActivityManager am = ForumActivityUtils.getActivityManager();
        ExoSocialActivity a = ForumActivityUtils.getActivityOfTopic(ctx);
        
        //FORUM_15 case: close topic
        a = processActivity(ctx, a);
        am.updateActivity(a);
        
        ExoSocialActivity newComment = processComment(ctx);
        
        //
        Identity poster = ForumActivityUtils.getIdentity(ctx.getTopic().getModifiedBy());
        newComment.setUserId(poster.getId());
        
        //
        am.saveComment(a, newComment);
        
        return newComment;
      } catch (Exception e) {
        LOG.warn("Can not record Comment for when close topic's id: " + ctx.getTopic().getId());
        LOG.debug(e.getMessage(), e);
      }
      return null;
    }
  };
  
  public static TopicActivityTask OPEN_TOPIC = new TopicActivityTask() {

    @Override
    protected ExoSocialActivity processTitle(ForumActivityContext ctx, ExoSocialActivity activity) {
      return ForumActivityType.OPEN_TOPIC.getActivity(activity);
    }
    
    @Override
    protected ExoSocialActivity processActivity(ForumActivityContext ctx, ExoSocialActivity activity) {
      
      activity.isLocked(false);
      
      return activity;
    };

    @Override
    public ExoSocialActivity execute(ForumActivityContext ctx) {
      try {
        ActivityManager am = ForumActivityUtils.getActivityManager();
        ExoSocialActivity a = ForumActivityUtils.getActivityOfTopic(ctx);
        
        //FORUM_xx case: open topic
        a = processActivity(ctx, a);
        am.updateActivity(a);
        
        ExoSocialActivity newComment = processComment(ctx);
        
        //
        Identity poster = ForumActivityUtils.getIdentity(ctx.getTopic().getModifiedBy());
        newComment.setUserId(poster.getId());
        
        //
        am.saveComment(a, newComment);
        
        return newComment;
      } catch (Exception e) {
        LOG.warn("Can not record Comment for when open topic's id: " + ctx.getTopic().getId());
        LOG.debug(e.getMessage(), e);
      }
      return null;
    }
  };
  
  public static TopicActivityTask LOCK_TOPIC = new TopicActivityTask() {

    @Override
    protected ExoSocialActivity processTitle(ForumActivityContext ctx, ExoSocialActivity activity) {
      return ForumActivityType.LOCK_TOPIC.getActivity(activity);
    }
    
    @Override
    protected ExoSocialActivity processActivity(ForumActivityContext ctx, ExoSocialActivity activity) {
      
      activity.isLocked(true);
      
      return activity;
    };

    @Override
    public ExoSocialActivity execute(ForumActivityContext ctx) {
      try {
        ActivityManager am = ForumActivityUtils.getActivityManager();
        ExoSocialActivity a = ForumActivityUtils.getActivityOfTopic(ctx);
        
        //
        a = processActivity(ctx, a);
        
        //FORUM_16 case: lock topic
        am.updateActivity(a);
        
        ExoSocialActivity newComment = processComment(ctx);
        
        //
        Identity poster = ForumActivityUtils.getIdentity(ctx.getTopic().getModifiedBy());
        newComment.setUserId(poster.getId());
        
        //
        am.saveComment(a, newComment);
        
        return newComment;
      } catch (Exception e) {
        LOG.warn("Can not record Comment for when lock topic's id: " + ctx.getTopic().getId());
        LOG.debug(e.getMessage(), e);
      }
      return null;
    }
  };
  
  public static TopicActivityTask UNLOCK_TOPIC = new TopicActivityTask() {

    @Override
    protected ExoSocialActivity processTitle(ForumActivityContext ctx, ExoSocialActivity activity) {
      return ForumActivityType.UNLOCK_TOPIC.getActivity(activity);
    }
    
    @Override
    protected ExoSocialActivity processActivity(ForumActivityContext ctx, ExoSocialActivity activity) {
     
      activity.isLocked(false);
      
      return activity;
    };

    @Override
    public ExoSocialActivity execute(ForumActivityContext ctx) {
      try {
        
        ActivityManager am = ForumActivityUtils.getActivityManager();
        ExoSocialActivity a = ForumActivityUtils.getActivityOfTopic(ctx);
        
        //
        a = processActivity(ctx, a);
        
        //FORUM_17 case: unlock topic
        am.updateActivity(a);
        
        ExoSocialActivity newComment = processComment(ctx);
        
        //
        Identity poster = ForumActivityUtils.getIdentity(ctx.getTopic().getModifiedBy());
        newComment.setUserId(poster.getId());
        
        //
        am.saveComment(a, newComment);
        
        return newComment;
      } catch (Exception e) {
        LOG.warn("Can not record Comment for when unlock topic's id: " + ctx.getTopic().getId());
        LOG.debug(e.getMessage(), e);
      }
      return null;
    }
  };
  
  public static TopicActivityTask APPROVED_TOPIC = new TopicActivityTask() {

    @Override
    protected ExoSocialActivity processTitle(ForumActivityContext ctx, ExoSocialActivity activity) {
      return ForumActivityType.APPROVED_TOPIC.getActivity(activity);
    }
    
    @Override
    protected ExoSocialActivity processActivity(ForumActivityContext ctx, ExoSocialActivity activity) {
      
      activity.isHidden(true);
      
      return activity;
    };

    @Override
    public ExoSocialActivity execute(ForumActivityContext ctx) {
      try {
        ActivityManager am = ForumActivityUtils.getActivityManager();
        ExoSocialActivity a = ForumActivityUtils.getActivityOfTopic(ctx);
        
        //
        a = processActivity(ctx, a);
        
        //FORUM_xx case: approved topic
        am.updateActivity(a);
        
        ExoSocialActivity newComment = processComment(ctx);
        
        //
        Identity poster = ForumActivityUtils.getIdentity(ctx.getTopic().getModifiedBy());
        newComment.setUserId(poster.getId());
        
        //
        am.saveComment(a, newComment);
        
        return newComment;
      } catch (Exception e) {
        LOG.warn("Can not record Comment for when approved topic's id: " + ctx.getTopic().getId());
        LOG.debug(e.getMessage(), e);
      }
      return null;
    }
  };
  
  public static TopicActivityTask UNAPPROVED_TOPIC = new TopicActivityTask() {

    @Override
    protected ExoSocialActivity processTitle(ForumActivityContext ctx, ExoSocialActivity activity) {
      return ForumActivityType.UNAPPROVED_TOPIC.getActivity(activity);
    }
    
    @Override
    protected ExoSocialActivity processActivity(ForumActivityContext ctx, ExoSocialActivity activity) {
     
      activity.isHidden(true);
      
      return activity;
    };

    @Override
    public ExoSocialActivity execute(ForumActivityContext ctx) {
      try {
        
        ActivityManager am = ForumActivityUtils.getActivityManager();
        ExoSocialActivity a = ForumActivityUtils.getActivityOfTopic(ctx);
        
        //
        a = processActivity(ctx, a);
        
        //FORUM_xx case: unapproved topic
        am.updateActivity(a);
        
        ExoSocialActivity newComment = processComment(ctx);
        
        //
        Identity poster = ForumActivityUtils.getIdentity(ctx.getTopic().getModifiedBy());
        newComment.setUserId(poster.getId());
        
        //
        am.saveComment(a, newComment);
        
        return newComment;
      } catch (Exception e) {
        LOG.warn("Can not record Comment for when unapproved topic's id: " + ctx.getTopic().getId());
        LOG.debug(e.getMessage(), e);
      }
      return null;
    }
  };
  
  public static TopicActivityTask HIDDEN_TOPIC = new TopicActivityTask() {

    @Override
    protected ExoSocialActivity processTitle(ForumActivityContext ctx, ExoSocialActivity activity) {
      return activity;
    }
    
    @Override
    protected ExoSocialActivity processActivity(ForumActivityContext ctx, ExoSocialActivity activity) {
      
      activity.isHidden(true);
      return activity;
    };

    @Override
    public ExoSocialActivity execute(ForumActivityContext ctx) {
      try {

        ActivityManager am = ForumActivityUtils.getActivityManager();
        ExoSocialActivity a = ForumActivityUtils.getActivityOfTopic(ctx);
        

        //FORUM_25: hidding into a topic
        a = processActivity(ctx, a);
        am.updateActivity(a);
        
        return a;
      } catch (Exception e) {
        LOG.error("Can not record Comment for when hidden topic " + ctx.getTopic().getId(), e);
        return null;
      }
    }
  };
  
  public static TopicActivityTask CENSORING_TOPIC = new TopicActivityTask() {

    @Override
    protected ExoSocialActivity processTitle(ForumActivityContext ctx, ExoSocialActivity activity) {
      return activity;
    }
    
    @Override
    protected ExoSocialActivity processActivity(ForumActivityContext ctx, ExoSocialActivity activity) {
      activity.isHidden(true);
      return activity;
    };

    @Override
    public ExoSocialActivity execute(ForumActivityContext ctx) {
      try {
        ActivityManager am = ForumActivityUtils.getActivityManager();
        ExoSocialActivity a = ForumActivityUtils.getActivityOfTopic(ctx);

        //FORUM_24 case: censoring topic
        a = processActivity(ctx, a);
        am.updateActivity(a);
        
        return a;
      } catch (Exception e) {
        LOG.warn("Can not record Comment for when hidden topic's id: " + ctx.getTopic().getId());
        LOG.debug(e.getMessage(), e);
        return null;
      }
    }
  };
  
  public static TopicActivityTask UNCENSORING_TOPIC = new TopicActivityTask() {

    @Override
    public ExoSocialActivity processTitle(ForumActivityContext ctx, ExoSocialActivity activity) {
      return activity;
    }
    
    @Override
    protected ExoSocialActivity processActivity(ForumActivityContext ctx, ExoSocialActivity activity) {
      activity.isHidden(false);
      return activity;
    };

    @Override
    public ExoSocialActivity execute(ForumActivityContext ctx) {
      try {
        ActivityManager am = ForumActivityUtils.getActivityManager();
        ExoSocialActivity a = ForumActivityUtils.getActivityOfTopic(ctx);

        //FORUM_xx case: unscensoring topic
        a = processActivity(ctx, a);
        am.updateActivity(a);
        
        return a;
      } catch (Exception e) {
        LOG.error("Can not show activity for uncensoring topic " + ctx.getTopic().getId(), e);
        return null;
      }
    }
  };
  
  public static TopicActivityTask ACTIVE_TOPIC = new TopicActivityTask() {

    @Override
    protected ExoSocialActivity processTitle(ForumActivityContext ctx, ExoSocialActivity activity) {
      return activity;
    }
    
    @Override
    protected ExoSocialActivity processActivity(ForumActivityContext ctx, ExoSocialActivity activity) {
      activity.isHidden(false);
      return activity;
    };

    @Override
    public ExoSocialActivity execute(ForumActivityContext ctx) {
      try {
        ActivityManager am = ForumActivityUtils.getActivityManager();
        ExoSocialActivity a = ForumActivityUtils.getActivityOfTopic(ctx);

        //FORUM_26: showing into a topic
        a = processActivity(ctx, a);
        am.updateActivity(a);
        
        return a;
      } catch (Exception e) {
        LOG.error("Can not record Comment for when active topic " + ctx.getTopic().getId(), e);
        return null;
      }
    }
  };
  
  public static TopicActivityTask MOVE_TOPIC = new TopicActivityTask() {

    @Override
    protected ExoSocialActivity processTitle(ForumActivityContext ctx, ExoSocialActivity activity) {
      return ForumActivityType.MOVE_TOPIC.getActivity(activity, ctx.getToCategoryName(), ctx.getToForumName());
    }
    
    @Override
    protected ExoSocialActivity processActivity(ForumActivityContext ctx, ExoSocialActivity activity) {
      Topic topic = ctx.getTopic();
      
      //
      Map<String, String> templateParams = activity.getTemplateParams();
      templateParams.put(ForumActivityBuilder.TOPIC_OWNER_KEY, topic.getOwner());
      
      //
      templateParams.put(ForumActivityBuilder.FORUM_ID_KEY, topic.getForumId());
      templateParams.put(ForumActivityBuilder.CATE_ID_KEY, topic.getCategoryId());
      
      if (ForumActivityUtils.hasSpace(topic.getForumId())) {
        templateParams.put(SPACE_GROUP_ID, ForumActivityUtils.getSpaceGroupId(topic.getForumId()));
      }
      
      activity.setTemplateParams(templateParams);
      
      return activity;
    };

    @Override
    public ExoSocialActivity execute(ForumActivityContext ctx) {
      try {
        ActivityManager am = ForumActivityUtils.getActivityManager();
        ExoSocialActivity a = ForumActivityUtils.getActivityOfTopic(ctx);
        
        Identity streamOwner = getOwnerStream(ctx);
        
        //Update new streamOwner of poll
        if (ctx.getTopic().getIsPoll()) {
          ExoSocialActivity aPoll = ForumActivityUtils.getActivityOfPollTopic(ctx);
          if (aPoll != null) {
            aPoll.setStreamOwner(streamOwner.getRemoteId());
            am.updateActivity(aPoll);
          }
        }
        
        //update new streamOwner
        a.setStreamOwner(streamOwner.getRemoteId());
        a = processActivity(ctx, a);
        am.updateActivity(a);
        
        ////FORUM_22 case: move topic
        ExoSocialActivity newComment = processComment(ctx);
        
        //
        String currentUser = UserHelper.getCurrentUser();
        Identity poster = ForumActivityUtils.getIdentity(currentUser);
        newComment.setUserId(poster.getId());
        
        //
        am.saveComment(a, newComment);
        
        return newComment;
      } catch (Exception e) {
        LOG.error("Can not record Comment for when moves topic " + ctx.getTopic().getId(), e);
      }
      return null;
    }
  };
  
  public static TopicActivityTask MERGE_TOPIC = new TopicActivityTask() {

    @Override
    protected ExoSocialActivity processTitle(ForumActivityContext ctx, ExoSocialActivity activity) {
      return ForumActivityType.MERGE_TOPICS.getActivity(activity, activity.getTitle());
    }
    
    @Override
    protected ExoSocialActivity processActivity(ForumActivityContext ctx, ExoSocialActivity activity) {
      return processTitle(ctx, activity);
    };

    @Override
    public ExoSocialActivity execute(ForumActivityContext ctx) {
      try {

        //
        Identity streamOwner = getOwnerStream(ctx);
        
        ////FORUM_21 case: merge topic
        ExoSocialActivity newActivity = ForumActivityBuilder.createActivity(ctx.getTopic(), ctx);
        newActivity = processActivity(ctx, newActivity);
        
        //
        Identity poster = ForumActivityUtils.getIdentity(ctx.getTopic().getOwner());
        
        ForumActivityUtils.saveTopicActivity(poster, streamOwner, newActivity, ctx.getTopic());
        
        ForumActivityUtils.removeActivities(ctx.getRemoveActivities());
        return newActivity;
      } catch (Exception e) {
        LOG.error("Can not record Activity for merged topics " + ctx.getTopic().getId(), e);
      }
      return null;
    }
  };
  
  public static TopicActivityTask SPLIT_TOPIC = new TopicActivityTask() {

    @Override
    public ExoSocialActivity processTitle(ForumActivityContext ctx, ExoSocialActivity activity) {
      return ForumActivityType.SPLIT_TOPIC.getActivity(activity, activity.getTitle());
    }
    
    @Override
    protected ExoSocialActivity processActivity(ForumActivityContext ctx, ExoSocialActivity activity) {
      return processTitle(ctx, activity);
    };

    @Override
    public ExoSocialActivity execute(ForumActivityContext ctx) {
      try {
        //
        Identity streamOwner = getOwnerStream(ctx);
        
        ////FORUM_01 case: creates topic -> create new activity
        ExoSocialActivity newActivity = ForumActivityBuilder.createActivity(ctx.getTopic(), ctx);
        newActivity = processActivity(ctx, newActivity);
        
        //
        Identity poster = ForumActivityUtils.getIdentity(ctx.getTopic().getOwner());
        
        ForumActivityUtils.saveTopicActivity(poster, streamOwner, newActivity, ctx.getTopic());
        
        // Creates activity split
        ExoSocialActivity splitActivity = ForumActivityBuilder.createActivity(ctx.getSplitedTopic(), ctx);
        splitActivity = processActivity(ctx, splitActivity);
        
        //
        poster = ForumActivityUtils.getIdentity(ctx.getSplitedTopic().getOwner());
        
        //If streamOwner is not a space and the 2 topics have different owner
        if (! SpaceIdentityProvider.NAME.equals(streamOwner.getProviderId()) && 
            ! ctx.getTopic().getOwner().equals(ctx.getSplitedTopic().getOwner())) {
          streamOwner = poster;
        }
        
        ForumActivityUtils.saveTopicActivity(poster, streamOwner, splitActivity, ctx.getSplitedTopic());
        
        ForumActivityUtils.takeActivityBack(ctx.getSplitedTopic(), splitActivity);
  
        //remove old activity
        ForumActivityUtils.removeActivities(ctx.getRemoveActivities());
        
        return newActivity;
      } catch (Exception e) {
        LOG.error("Can not record Activity for splited topic " + ctx.getTopic().getId(), e);
      }
      return null;
    }
  };
  
  public static TopicActivityTask UPDATE_FORUM_TOPIC = new TopicActivityTask() {
    @Override
    protected ExoSocialActivity processTitle(ForumActivityContext ctx, ExoSocialActivity activity) {
      return activity;
    }
    
    @Override
    protected ExoSocialActivity processActivity(ForumActivityContext ctx, ExoSocialActivity activity) {
      Topic topic = ctx.getTopic();
      if (! topic.getIsActiveByForum() || topic.getIsLock())
        activity.isLocked(true);
      else
        activity.isLocked(false);
      return activity;
    }

    @Override
    public ExoSocialActivity execute(ForumActivityContext ctx) {
      try {
        ActivityManager am = ForumActivityUtils.getActivityManager();
        ExoSocialActivity a = ForumActivityUtils.getActivityOfTopic(ctx);

        a = processActivity(ctx, a);
        am.updateActivity(a);
        
        return a;
      } catch (Exception e) {
        LOG.error("Can not record Activity for when add topic's title " + ctx.getTopic().getId(), e);
      }
      return null;
    }
    
  };
  
  @Override
  public void start(ForumActivityContext ctx) {}
  
  @Override
  public void end(ForumActivityContext ctx) { }
  
  
  protected Identity getOwnerStream(ForumActivityContext ctx) {
    Identity ownerStream = null;

    Topic topic = ctx.getTopic();
    Identity userIdentity = ForumActivityUtils.getIdentity(topic.getOwner());
    
    try {

      //if (ForumActivityUtils.isTopicPublic(topic)) {
      String[] tab = topic.getPath().split("/");
      String forumId = tab[tab.length-2];
      if (ForumActivityUtils.hasSpace(forumId)) {
        // publish the activity in the space stream.
        ownerStream = ForumActivityUtils.getSpaceIdentity(forumId);
      }
      
      //For issue FORUM-284: a forum with restricted permission is not public forum
      //but we allow user to create an activity if he creates a topic in this forum
      
      /*if (ownerStream == null
          && ForumActivityUtils.isCategoryPublic(fs.getCategory(topic.getCategoryId()))
          && ForumActivityUtils.isForumPublic(fs.getForum(topic.getCategoryId(), topic.getForumId()))) {
        ownerStream = userIdentity;
       }*/
      
      if (ownerStream == null) {
        ownerStream = userIdentity;
      }
      return ownerStream;
    } catch (Exception e) { // ForumService
      LOG.error("Can not get OwnerStream for topic " + ctx.getTopic().getId(), e);
    }

    return userIdentity;
  }
}
