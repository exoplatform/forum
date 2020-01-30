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
package org.exoplatform.forum.ext.impl;

import java.util.HashMap;
import java.util.Map;

import org.exoplatform.commons.utils.StringCommonUtils;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.poll.service.Poll;
import org.exoplatform.poll.service.PollEventListener;
import org.exoplatform.poll.service.PollService;
import org.exoplatform.poll.service.Utils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.processor.I18NActivityUtils;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

public class PollSpaceActivityPublisher extends PollEventListener{

  public static final String POLL_APP_ID            = "ks-poll:spaces";
  public static final String POLL_COMMENT_APP_ID    = "poll:spaces";
  private static final Log   LOG                    = ExoLogger.getExoLogger(PollSpaceActivityPublisher.class);
  public static final String POLL_PARENT            = "PollLink";
  public static final String POLL_LINK              = "Link";
  public static final String UPDATE_POLL_TITLE_ID   = "update_poll";
  public static final String SPACE_GROUP_ID         = "SpaceGroupId";
  
  public static final String POLL_ID = "Id";
  
  private ExoSocialActivity activity(Identity author, String title, String body, Map<String, String> templateParams) throws Exception {
    ExoSocialActivity activity = new ExoSocialActivityImpl();
    activity.setUserId(author.getId());
    activity.setTitle(StringCommonUtils.decodeSpecialCharToHTMLnumber(title));
    activity.setBody(body);
    activity.setType(POLL_APP_ID);
    activity.setTemplateParams(templateParams);
    return activity;
  }
  
  private ExoSocialActivity createComment() {
    ExoSocialActivityImpl comment = new ExoSocialActivityImpl();
    comment.setTitle("Poll has been updated.");
    comment.setType(POLL_COMMENT_APP_ID);
    I18NActivityUtils.addResourceKey(comment, UPDATE_POLL_TITLE_ID);
    return comment;
  }
  
  private String getCurrentUserId() {
    ConversationState state = ConversationState.getCurrent();
    String currentUserId = null;
    if (state != null) {
      currentUserId = state.getIdentity().getUserId();
    }
    return currentUserId;
  }
  
  private void savePollForActivity(Poll poll, boolean isNew, boolean isVote) {
    PollService pollService = (PollService) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(PollService.class);
    IdentityManager identityManager = (IdentityManager) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(IdentityManager.class);
    try {
      Identity pollOwnerIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, poll.getOwner(), false);
      Map<String, String> templateParams = new HashMap<String, String>();
      String pollPath = poll.getParentPath()+"/"+poll.getId();
      String activityId = pollService.getActivityIdForOwner(pollPath);
      if (activityId != null) {
        ExoSocialActivity activity = getManager().getActivity(activityId);
        if (activity != null) {
          poll.setInfoVote();
          
          //update activity's content
          activity.setBody(Utils.getInfoVote(poll));
          activity.setTitle(StringCommonUtils.decodeSpecialCharToHTMLnumber(poll.getQuestion()));
          getManager().updateActivity(activity);
          
          if (! isVote) {
            ExoSocialActivity comment = createComment();
            String userId = getCurrentUserId();
            if (userId != null && ! userId.equals(pollOwnerIdentity.getRemoteId())) {
              Identity currentIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, userId, false); 
              comment.setUserId(currentIdentity.getId());
            } else {
              comment.setUserId(pollOwnerIdentity.getId());
            }
            getManager().saveComment(activity, comment);
          }
        } else {
          activityId = null;
          poll.setInfoVote();
        }
      }
      if (activityId == null) {
        ExoSocialActivity newActivity = activity(pollOwnerIdentity, poll.getQuestion(), Utils.getInfoVote(poll), templateParams);
        
        //set stream owner
        Identity spaceIdentity = getSpaceIdentity(poll);
        if (spaceIdentity != null) {
          pollOwnerIdentity = spaceIdentity;
          templateParams.put(SPACE_GROUP_ID, getSpaceGroupId(poll.getParentPath()));
        }
        templateParams.put(POLL_PARENT, poll.getParentPath());
        templateParams.put(POLL_ID, poll.getId());
        templateParams.put(POLL_LINK, poll.getLink());
        newActivity.setTemplateParams(templateParams);
        getManager().saveActivityNoReturn(pollOwnerIdentity, newActivity);
        
        //Case activity deleted and re-updated poll
        if (! isNew) {
          ExoSocialActivity comment = createComment();
          String userId = getCurrentUserId();
          if (userId != null && ! userId.equals(pollOwnerIdentity.getRemoteId())) {
            Identity currentIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, userId, false); 
            comment.setUserId(currentIdentity.getId());
          } else {
            comment.setUserId(pollOwnerIdentity.getId());
          }
          getManager().saveComment(newActivity, comment);
        }
        
        if (pollService.getActivityIdForOwner(pollPath) == null) {
          saveCommentToTopicActivity(poll, "A poll has been added to the topic.", "forum.add-poll");
        }
        pollService.saveActivityIdForOwner(pollPath, newActivity.getId());
      }
    } catch (Exception e) {
      LOG.error("Can not record Activity for space when create poll ", e.getMessage());
    }
  }
  
  public void saveCommentToTopicActivity(Poll poll, String title, String titleId) {
    PollService pollService = (PollService) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(PollService.class);
    IdentityManager identityManager = (IdentityManager) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(IdentityManager.class);
    String topicActivityId = pollService.getActivityIdForOwner(poll.getParentPath());
    if (topicActivityId != null) {
      ExoSocialActivity topicActivity = getManager().getActivity(topicActivityId);
      if (poll.isInTopic() && topicActivity != null) {
        ExoSocialActivityImpl comment = new ExoSocialActivityImpl();
        Identity pollOwnerIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, poll.getOwner(), false);
        
        String userId = getCurrentUserId();
        if (userId != null && ! userId.equals(pollOwnerIdentity.getRemoteId())) {
          Identity currentIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, userId, false); 
          comment.setUserId(currentIdentity.getId());
        } else {
          comment.setUserId(pollOwnerIdentity.getId());
        }
        
        comment.setType("ks-forum:spaces");
        comment.setTitle(title);
        I18NActivityUtils.addResourceKey(comment, titleId);
        getManager().saveComment(topicActivity, comment);
      }
    }
  }
  
  public void savePoll(Poll poll, boolean isNew, boolean isVote) {
    savePollForActivity(poll, isNew, isVote);
  }
  
  public void closePoll(Poll poll) {
    saveCommentToTopicActivity(poll, "Poll has been closed.", "forum.close-poll");
  }
  
  public void pollRemove(String pollId) {
    try {
      PollService pollService = (PollService) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(PollService.class);
      Poll poll = pollService.getPoll(pollId);
      String pollPath = poll.getParentPath()+"/"+poll.getId();
      String activityId = pollService.getActivityIdForOwner(pollPath);
      ExoSocialActivity activity = getManager().getActivity(activityId);
      getManager().deleteActivity(activity);
      saveCommentToTopicActivity(poll, "Poll has been removed.", "forum.remove-poll");
    } catch (Exception e) {
      LOG.error("Fail to remove poll "+e.getMessage());
    }
  }
  
  private ActivityManager getManager() {
    return (ActivityManager) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(ActivityManager.class);
  }
  
  private String getSpaceGroupId(String path) {
    if (path.contains(org.exoplatform.forum.service.Utils.CATEGORY_SPACE_ID_PREFIX)) {
      String[] tab = path.split(CommonUtils.SLASH);
      String spaceName = tab[tab.length-2].replace(org.exoplatform.forum.service.Utils.FORUM_SPACE_ID_PREFIX, "");
      return new StringBuffer(SpaceUtils.SPACE_GROUP).append(CommonUtils.SLASH).append(spaceName).toString();
    }
    return CommonUtils.EMPTY_STR;
  }
  
  private Identity getSpaceIdentity(Poll poll) {
    IdentityManager identityM = (IdentityManager) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(IdentityManager.class);
    String path = poll.getParentPath();
    String spaceGroupId = getSpaceGroupId(path);
    if ("".equals(spaceGroupId)) {
      return null;
    } else {
      SpaceService spaceService  = (SpaceService) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(SpaceService.class);
      Space space = spaceService.getSpaceByGroupId(spaceGroupId);
      return identityM.getOrCreateIdentity(SpaceIdentityProvider.NAME, space.getPrettyName(), false);
    }
  }
  
}
