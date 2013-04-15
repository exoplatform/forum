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
 * Main Facade for all Poll information operations.
 * 
 */
public interface PollService {
  /**
   * Adds the plugin.
   * 
   * @param plugin the plugin
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void addInitialDefaultDataPlugin(ComponentPlugin plugin) throws Exception;

  /**
   * Gets the poll by id.
   * 
   * @param pollId
   * @return the poll
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  Poll getPoll(String pollId) throws Exception;

  /**
   * Saves poll.
   * 
   * @param poll the poll
   * @param isNew is the new
   * @param isVote is the vote
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void savePoll(Poll poll, boolean isNew, boolean isVote) throws Exception;

  /**
   * Removes the poll.
   * 
   * @param pollId
   * @return the poll
   * @LevelAPI Platform
   */
  Poll removePoll(String pollId);

  /**
   * Sets the poll to closed.
   * 
   * @param poll
   * @LevelAPI Platform
   */
  void setClosedPoll(Poll poll);

  /**
   * Gets list of polls.
   * 
   * @return the list of polls
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public List<Poll> getPagePoll() throws Exception;
  
  /**
   * Checks has permission of user viewer the poll in the forum.
   * 
   * @param pollPath the path of the poll.
   * @param allInfoOfUser user, group and membership of the user.
   * @return boolean
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public boolean hasPermissionInForum(String pollPath, List<String> allInfoOfUser) throws Exception;

  /**
   * Gets the poll summary.
   * 
   * @param groupOfUser group
   * @return the poll summary
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public PollSummary getPollSummary(List<String> groupOfUser) throws Exception;

  /**
   * Defines Mixin type exo:activityInfo for node that means to add exo:activityId property 
   * into Node what is owner created activity via patch
   * 
   * @param ownerPath - the Path's Node what is owner created activity
   * @param activityId - the Id's activity created.
   * @LevelAPI Platform 
   * @since 4.0
   */
  public void saveActivityIdForOwner(String ownerPath, String activityId);

  /**
   * Gets value of exo:activityId property in specified node via path. 
   * If property is not existing then return null.
   * 
   * @param ownerPath - the Path's Node what is owner created activity
   * @return String - the Id's activity created. 
   * @LevelAPI Platform
   * @since 4.0
   */
  public String getActivityIdForOwner(String ownerPath);
  
  /**
   * Adds poll event listener.
   * 
   * @param listener
   * @throws Exception
   * @LevelAPI Platform
   */
  public void addListenerPlugin(PollEventListener listener) throws Exception;
  
  /**
   * Removes poll event listener.
   * 
   * @param listener
   * @throws Exception
   * @LevelAPI Platform
   */
  public void removeListenerPlugin(PollEventListener listener) throws Exception; 
}
