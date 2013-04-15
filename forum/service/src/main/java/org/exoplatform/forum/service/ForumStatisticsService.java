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
 * APIs for Forum's members operations.
 */
public interface ForumStatisticsService {

  /**
   * Increases user count and last member.
   * 
   * @param userName
   * @throws Exception 
   * @LevelAPI Platform
   */
  void addMember(String userName) throws Exception;

  /**
   * Decreases user count.
   * 
   * @param userName
   * @throws Exception
   * @LevelAPI Platform
   */
  void removeMember(String userName) throws Exception;

}
