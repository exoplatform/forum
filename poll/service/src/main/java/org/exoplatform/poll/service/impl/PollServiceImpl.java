/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.poll.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.forum.common.jcr.KSDataLocation;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.poll.service.Poll;
import org.exoplatform.poll.service.Poll.PollAction;
import org.exoplatform.poll.service.PollEventLifeCycle;
import org.exoplatform.poll.service.PollEventListener;
import org.exoplatform.poll.service.PollService;
import org.exoplatform.poll.service.PollSummary;
import org.exoplatform.poll.service.Utils;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.picocontainer.Startable;

@NameTemplate(@Property(key = "service", value = "poll"))
public class PollServiceImpl implements Startable, PollService {
  private JCRDataStorage   storage_;

  private KSDataLocation   dataLocator;

  private static final Log log = ExoLogger.getLogger(PollServiceImpl.class);
  
  protected List<PollEventListener>   listeners_ = new ArrayList<PollEventListener>(3);

  public PollServiceImpl(InitParams params, KSDataLocation locator, NodeHierarchyCreator nodeHierarchyCreator) throws Exception {
    this.dataLocator = locator;
    storage_ = new JCRDataStorage(nodeHierarchyCreator, dataLocator);
  }

  public void addInitialDefaultDataPlugin(ComponentPlugin plugin) throws Exception {
    storage_.addInitialDefaultDataPlugin(plugin);
  }

  public void start() {
    try {
      log.info("initializing Poll default data...");
      storage_.initDefaultData();
    } catch (Exception e) {
      log.error("Failed to initializing default data for poll: ", e);
    }
  }

  public void stop() {
  }

  public Poll getPoll(String pollId) throws Exception {
    return storage_.getPoll(pollId);
  }

  public Poll removePoll(String pollId) {
    for (PollEventLifeCycle f : listeners_) {
      f.pollRemove(pollId);
    }
    return storage_.removePoll(pollId);
  }

  public void savePoll(Poll poll, boolean isNew, boolean isVote) throws Exception {
    if (!isVote) {
      if (isNew) {
        poll.setPollAction(PollAction.Create_Poll);
      } else {
        poll.setPollAction(PollAction.Update_Poll);
      }
    } else {
        Poll oldPoll = getPoll(poll.getId());
        int oldVoters = oldPoll.getUserVote().length;
        int newVoters = poll.getUserVote().length;
        boolean isVoteAgain = (oldVoters == newVoters);
        poll.setPollAction(PollAction.Vote_Poll);
        if(isVoteAgain) {
          poll.setPollAction(PollAction.Vote_Again_Poll);
        }
    }
    storage_.savePoll(poll, isNew, isVote);
    for (PollEventLifeCycle f : listeners_) {
      f.savePoll(poll);
    }
  }
  
  public void addListenerPlugin(PollEventListener listener) throws Exception {
    listeners_.add(listener);
  }

  public void removeListenerPlugin(PollEventListener listener) throws Exception {
    listeners_.remove(listener);
  }
  
  public void setClosedPoll(Poll poll){
    storage_.setClosedPoll(poll);
  }

  public List<Poll> getPagePoll() throws Exception {
    return storage_.getPagePoll();
  }

  public boolean hasPermissionInForum(String pollPath, List<String> allInfoOfUser) throws Exception {
    return storage_.hasPermissionInForum(pollPath, allInfoOfUser);
  }
  
  public PollSummary getPollSummary(List<String> groupOfUser) throws Exception {
    return storage_.getPollSummary(groupOfUser);
  }
  
  public void saveActivityIdForOwner(String ownerPath, String activityId) {
    storage_.saveActivityIdForOwner(ownerPath, activityId);
  }

  public String getActivityIdForOwner(String ownerPath) {
    return storage_.getActivityIdForOwner(ownerPath);
  }

}
