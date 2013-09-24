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
package org.exoplatform.forum.service;

/**
 * Manages operations of members in Forums.
 */
public interface ForumStatisticsService {

  /**
   * Increases the count of users when a new member is added.
   * 
   * @param userName Name of the added member.
   * @throws Exception 
   * @LevelAPI Platform
   */
  void addMember(String userName) throws Exception;

  /**
   * Decreases the count of users when a member is removed.
   * 
   * @param userName Name of the removed member.
   * @throws Exception
   * @LevelAPI Platform
   */
  void removeMember(String userName) throws Exception;

}
