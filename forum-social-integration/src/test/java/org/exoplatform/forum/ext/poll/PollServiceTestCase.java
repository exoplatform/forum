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
package org.exoplatform.forum.ext.poll;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.storage.api.IdentityStorage;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.forum.ext.impl.PollSpaceActivityPublisher;
import org.exoplatform.poll.service.Poll;

import org.apache.commons.lang.StringEscapeUtils;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
@ConfiguredBy({
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.application-common.portal-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/forum.test.configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/poll.test.configuration.xml"),
})
public class PollServiceTestCase extends BaseTestCase {
  
  private List<Poll> tearDownPollList;
  
  private IdentityStorage identityStorage;
  private Identity rootIdentity;
  private Identity johnIdentity;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    identityStorage = getContainer().getComponentInstanceOfType(IdentityStorage.class);
    rootIdentity = new Identity(OrganizationIdentityProvider.NAME, "root");
    johnIdentity = new Identity(OrganizationIdentityProvider.NAME, "john");
    identityStorage.saveIdentity(rootIdentity);
    identityStorage.saveIdentity(johnIdentity);
    tearDownPollList = new ArrayList<>();
  }

  @Override
  public void tearDown() throws Exception {
    for (Poll poll : tearDownPollList) {
      pollService.removePoll(poll.getId());
    }
    identityStorage.deleteIdentity(rootIdentity);
    identityStorage.deleteIdentity(johnIdentity);
    super.tearDown();
  }

  public void testPollService() {
    assertNotNull(getPollService());
  }
  
  public void testPollTitleWithSpecialCharacters() throws Exception {
    Poll pollTopic = new Poll();
    pollTopic.setQuestion("&-*()");
    String[] options = { "red", "blue" };
    pollTopic.setOption(options);
    pollTopic.setOwner(rootIdentity.getRemoteId());
    pollTopic.setParentPath(topicPath);
    pollTopic.setInTopic(false);

    // When create poll, an activity will be save
    pollService.savePoll(pollTopic, true, false);
    String activityId = pollService.getActivityIdForOwner(pollTopic.getParentPath() + "/"+ pollTopic.getId());
    assertNotNull(activityId);
    ExoSocialActivity activity = getManager().getActivity(activityId);
    assertNotNull(activity);
    assertEquals("&-*()", StringEscapeUtils.unescapeHtml(activity.getTitle()));
    
    pollTopic.setQuestion("&-*() / --- == coucou #@");
    pollService.savePoll(pollTopic, false, false);
    activity = getManager().getActivity(activityId);
    assertEquals("&-*() / --- == coucou #@", StringEscapeUtils.unescapeHtml(activity.getTitle()));
    
    // remove poll will remove activity
    pollService.removePoll(pollTopic.getId());
  }

  /**
  * testSavePollWithActivity
  * 
  * @throws Exception
  */
  public void testSavePollWithActivity() throws Exception {
    // if poll of topic : parentPath = topic.getPath();
    Poll pollTopic = new Poll();
    pollTopic.setQuestion("What color?");
    String[] options = { "red", "blue" };
    pollTopic.setOption(options);
    pollTopic.setOwner(rootIdentity.getRemoteId());
    pollTopic.setParentPath(topicPath);
    pollTopic.setInTopic(false);

    // When create poll, an activity will be save
    pollService.savePoll(pollTopic, true, false);
    String activityId = pollService.getActivityIdForOwner(pollTopic.getParentPath() + "/"+ pollTopic.getId());
    assertNotNull(activityId);
    ExoSocialActivity activity = getManager().getActivity(activityId);
    assertNotNull(activity);
    assertEquals("What color?", activity.getTitle());
    // Number of comments must be 0
    assertEquals(0, getManager().getCommentsWithListAccess(activity).getSize());

    // update poll
    pollTopic.setQuestion("Hello");
    pollService.savePoll(pollTopic, false, false);
    activity = getManager().getActivity(activityId);
    List<ExoSocialActivity> comments = getManager().getCommentsWithListAccess(activity).loadAsList(0, 10);
    // Number of comments must be 1
    assertEquals(1, comments.size());
    assertEquals("Poll has been updated.", comments.get(0).getTitle());

    // delete activity
    getManager().deleteActivity(activity);

    // re-update, this will re-create activity and add new comment associated
    pollTopic.setQuestion("new question");
    pollService.savePoll(pollTopic, false, false);
    String newActivityId = pollService.getActivityIdForOwner(pollTopic.getParentPath() + "/"+ pollTopic.getId());
    activity = getManager().getActivity(newActivityId);
    comments = getManager().getCommentsWithListAccess(activity).loadAsList(0, 10);
    // Number of comments must be 1
    assertEquals(1, comments.size());
    assertEquals("Poll has been updated.", comments.get(0).getTitle());

    // remove poll will remove activity
    pollService.removePoll(pollTopic.getId());
    activity = getManager().getActivity(newActivityId);
    assertNull(activity);
  }

  private ActivityManager getManager() {
    return (ActivityManager) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(ActivityManager.class);
  }

}
