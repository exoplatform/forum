/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
package org.exoplatform.poll.service;

import java.util.List;

import org.exoplatform.container.component.ComponentPlugin;

/**
 * Main Facade for all Poll operations.
 * 
 */
public interface PollService {
  /**
   * Adds a plugin which initalizes the default data at first runtime.
   * 
   * @param plugin The plugin to be added.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void addInitialDefaultDataPlugin(ComponentPlugin plugin) throws Exception;

  /**
   * Gets a poll by its Id.
   * 
   * @param pollId Id of the poll.
   * @return The poll.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  Poll getPoll(String pollId) throws Exception;

  /**
   * Saves a poll.
   * 
   * @param poll The poll to be saved.
   * @param isNew If "true", the new poll is added. If "false", the poll is updated.
   * @param isVote If "true", the poll can be voted. If "false", the poll is updated.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void savePoll(Poll poll, boolean isNew, boolean isVote) throws Exception;

  /**
   * Removes a poll.
   * 
   * @param pollId Id of the poll which is removed.
   * @return The poll
   * @LevelAPI Platform
   */
  Poll removePoll(String pollId);

  /**
   * Sets a poll to "closed".
   * 
   * @param poll The poll which is set to "closed".
   * @LevelAPI Platform
   */
  void setClosedPoll(Poll poll);

  /**
   * Gets a list of polls.
   * 
   * @return Polls.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public List<Poll> getPagePoll() throws Exception;
  
  /**
   * Checks if a user has permission on a poll or not.
   * 
   * @param pollPath Path to the poll.
   * @param allInfoOfUser Information about user, group and membership of the user.
   * @return The returned value is "true" if the checked user has permission on the poll.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public boolean hasPermissionInForum(String pollPath, List<String> allInfoOfUser) throws Exception;

  /**
   * Gets summary of a poll.
   * 
   * @param groupOfUser All groups of the current user.
   * @return The poll summary.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public PollSummary getPollSummary(List<String> groupOfUser) throws Exception;

  /**
   * Saves information of a poll activity that is used for processing the activity streams.
   *
   * @param ownerPath Path to the poll activity.
   * @param activityId Id of the poll activity.
   * @LevelAPI Platform 
   * @since 4.0
   */
  public void saveActivityIdForOwner(String ownerPath, String activityId);

  /**
   * Gets information of a poll activity that is used for processing the activity streams.
   * 
   * @param ownerPath Path to the poll activity.
   * @return Id of the poll activity.
   * @LevelAPI Platform
   * @since 4.0
   */
  public String getActivityIdForOwner(String ownerPath);
  
  /**
   * Adds a listener plugin.
   * 
   * @param listener The listener plugin to be added.
   * @throws Exception
   * @LevelAPI Platform
   */
  public void addListenerPlugin(PollEventListener listener) throws Exception;
  
  /**
   * Removes a listener plugin.
   * 
   * @param listener The listener plugin to be removed.
   * @throws Exception
   * @LevelAPI Platform
   */
  public void removeListenerPlugin(PollEventListener listener) throws Exception; 
}
