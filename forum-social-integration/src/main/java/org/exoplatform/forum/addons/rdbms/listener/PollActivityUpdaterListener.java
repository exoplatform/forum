package org.exoplatform.forum.addons.rdbms.listener;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.forum.ext.activity.ForumActivityUtils;
import org.exoplatform.forum.ext.impl.PollSpaceActivityPublisher;
import org.exoplatform.poll.service.Poll;
import org.exoplatform.poll.service.PollService;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;

public class PollActivityUpdaterListener extends Listener<ExoSocialActivity, String> {
  private static final Log LOG = ExoLogger.getLogger(ForumActivityUpdaterListener.class);

  /**
   * Constructor
   * @param pollService 
   * Do not remove poll service on constructor, it use for order Startable of migration.
   */
  public PollActivityUpdaterListener(PollService pollService) {
  }

  @Override
  public void onEvent(Event<ExoSocialActivity, String> event) throws Exception {
    ExoSocialActivity activity = event.getSource();
    String newActivityId = event.getData();
    if (PollSpaceActivityPublisher.POLL_APP_ID.equals(activity.getType())) {
      Poll poll = ForumActivityUtils.getPoll(activity);
      String pollPath = poll.getParentPath() + "/" + poll.getId();
      CommonsUtils.getService(PollService.class).saveActivityIdForOwner(pollPath, newActivityId);
      LOG.info(String.format("Migration the poll activity '%s' with old id's %s and new id's %s", activity.getTitle(), activity.getId(), newActivityId));
    }
  }
  
}