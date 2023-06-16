package org.exoplatform.forum.integration.gamification;

import java.beans.PropertyChangeEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.exoplatform.forum.service.*;
import org.exoplatform.services.listener.Asynchronous;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.spi.SpaceService;

@Asynchronous
public class GamificationForumListener extends ForumEventListener {

  private static final Log      LOG                           = ExoLogger.getLogger(GamificationForumListener.class);

  private static final String   GAMIFICATION_FORUM_ADD_POST   = "addForumPost";

  private static final String   GAMIFICATION_FORUM_ADD_TOPIC  = "addForumTopic";

  private static final String   GAMIFICATION_FORUM_VOTE_TOPIC = "voteForumTopic";

  protected IdentityManager     identityManager;

  protected SpaceService        spaceService;

  protected ListenerService     listenerService;

  protected ActivityManager     activityManager;

  protected ForumService        forumService;

  public GamificationForumListener(IdentityManager identityManager,
                                   SpaceService spaceService,
                                   ListenerService listenerService,
                                   ActivityManager activityManager,
                                   ForumService forumService) {
    this.identityManager = identityManager;
    this.spaceService = spaceService;
    this.listenerService = listenerService;
    this.activityManager = activityManager;
    this.forumService = forumService;
  }

  @Override
  public void saveCategory(Category category) {
  }

  @Override
  public void saveForum(Forum forum) {
  }

  @Override
  public void addPost(Post post) {

    String actorId = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, post.getOwner(), false).getId();
    try {
      Map<String, String> gamificationRule = new HashMap<>();
      gamificationRule.put("ruleTitle", GAMIFICATION_FORUM_ADD_POST);
      gamificationRule.put("senderId", actorId);
      gamificationRule.put("receiverId", actorId);
      gamificationRule.put("object", ForumUtils.createdSubForumLink(post.toString(), post.getTopicId(), true));
      listenerService.broadcast("exo.gamification.generic.action", gamificationRule, "");
    } catch (Exception e) {
      LOG.error("Cannot broadcast add forum post Gamification event" + e.getMessage(), e);
    }
  }

  @Override
  public void updatePost(Post post) {
  }

  @Override
  public void updatePost(Post post, int type) {

  }

  @Override
  public void addTopic(Topic topic) {

    String actorId = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, topic.getOwner(), false).getId();
    try {
      Map<String, String> gamificationRule = new HashMap<>();
      gamificationRule.put("ruleTitle", GAMIFICATION_FORUM_ADD_TOPIC);
      gamificationRule.put("senderId", actorId);
      gamificationRule.put("receiverId", actorId);
      gamificationRule.put("object", ForumUtils.createdSubForumLink(topic.toString(), topic.getId(), true));
      listenerService.broadcast("exo.gamification.generic.action", gamificationRule, "");
    } catch (Exception e) {
      LOG.error("Cannot broadcast add forum topic Gamification event" + e.getMessage(), e);
    }

  }

  @Override
  public void updateTopic(Topic topic) {

    PropertyChangeEvent[] events = topic.getChangeEvent();

    for (int i = 0; i < events.length; i++) {
      processUpdateTopicType(events[i], topic);
    }

  }

  @Override
  public void updateTopics(List<Topic> topics, boolean isLock) {

  }

  @Override
  public void moveTopic(Topic topic, String toCategoryName, String toForumName) {

  }

  @Override
  public void mergeTopic(Topic newTopic, String removeActivityId1, String removeActivityId2) {

  }

  @Override
  public void splitTopic(Topic newTopic, Topic splitedTopic, String removeActivityId) {

  }

  @Override
  public void removeActivity(String activityId) {

  }

  @Override
  public void removeComment(String activityId, String commentId) {

  }

  @Override
  public void movePost(List<Post> list, List<String> list1, String s) {

  }

  @Override
  public void openTopic(String userId, String topicId) {

  }

  private void processUpdateTopicType(PropertyChangeEvent event, Topic topic) {

    // Start gamification process only when a topic is voted
    if (Topic.TOPIC_RATING.equals(event.getPropertyName())) {
      String actorId = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, topic.getOwner(), false).getId();
      try {
        Map<String, String> gamificationRule = new HashMap<>();
        gamificationRule.put("ruleTitle", GAMIFICATION_FORUM_VOTE_TOPIC);
        gamificationRule.put("senderId", actorId);
        gamificationRule.put("receiverId", actorId);
        gamificationRule.put("object", ForumUtils.createdSubForumLink(topic.toString(), topic.getId(), true));
        listenerService.broadcast("exo.gamification.generic.action", gamificationRule, "");
      } catch (Exception e) {
        LOG.error("Cannot broadcast add forum topic vote Gamification event" + e.getMessage(), e);
      }
    }

  }
}
