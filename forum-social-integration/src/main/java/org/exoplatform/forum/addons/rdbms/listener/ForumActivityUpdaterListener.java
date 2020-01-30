package org.exoplatform.forum.addons.rdbms.listener;

import org.exoplatform.forum.ext.activity.ForumActivityBuilder;
import org.exoplatform.forum.ext.activity.ForumActivityUtils;
import org.exoplatform.forum.service.ForumService;
import org.exoplatform.forum.service.Post;
import org.exoplatform.forum.service.Topic;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;

public class ForumActivityUpdaterListener extends Listener<ExoSocialActivity, String> {
  private static final Log LOG = ExoLogger.getLogger(ForumActivityUpdaterListener.class);

  /**
   * Constructor
   * @param forumService 
   * Do not remove forum service on constructor, it use for order Startable of migration.
   */
  public ForumActivityUpdaterListener(ForumService forumService) {
  }

  @Override
  public void onEvent(Event<ExoSocialActivity, String> event) throws Exception {
    ExoSocialActivity activity = event.getSource();
    String newActivityId = event.getData();
    if (ForumActivityBuilder.FORUM_ACTIVITY_TYPE.equals(activity.getType())) {
      if (activity.isComment()) {
        Post post = ForumActivityUtils.getPost(activity);
        if (post != null) {
          ForumActivityUtils.takeCommentBack(post, newActivityId);
        } else {
          LOG.warn(String.format("Failed to migration the forum post comment width old id %s - new id %s. Because, missing post's Id on template-parameters", activity.getId(), event.getData()));
        }
      } else {
        Topic topic = ForumActivityUtils.getTopic(activity);
        if (topic != null) {
          ForumActivityUtils.takeActivityBack(topic, newActivityId);
        } else {
          LOG.warn(String.format("Failed to migration the forum topic activity width old id %s - new id %s. Because, missing post's Id on template-parameters", activity.getId(), event.getData()));
        }
      }
    }
  }
  
}