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
package org.exoplatform.forum.bbcode.api;

import java.util.List;

import org.exoplatform.forum.bbcode.spi.BBCodePlugin;

/**
 * Main Facade for all BBCode related operations.
 * @version $Revision$
 */
public interface BBCodeService {

  /**
   * Registers a new BBCode plugin.
   * 
   * @param plugin The plugin to be registered.
   * @throws Exception
   * @LevelAPI Platform
   */
  void registerBBCodePlugin(BBCodePlugin plugin) throws Exception;

  /**
   * Saves a list of BBCodes.
   * 
   * @param bbcodes BBCodes to be saved.
   * @throws Exception
   * @LevelAPI Platform
   */
  public void save(List<BBCode> bbcodes) throws Exception;

  /**
   * Retrieves all BBCodes.
   * 
   * @return BBCodes.
   * @throws Exception
   * @LevelAPI Platform
   */
  public List<BBCode> getAll() throws Exception;

  /**
   * Retrieves BBCode Ids that are active.
   * 
   * @return BBCode Ids.
   * @throws Exception
   * @LevelAPI Platform
   */
  public List<String> getActive() throws Exception;
  
  /**
   * Gets all active BBCodes.
   * @return BBCodes.
   * @throws Exception
   */
  List<BBCode> getBBCodeActive() throws Exception;

  /**
   * Loads a BBCode by its given Id.
   * 
   * @param bbcodeId Id of the loaded BBCode.
   * @return The BBCode.
   * @throws Exception
   * @LevelAPI Platform
   */
  public BBCode findById(String bbcodeId) throws Exception;

  /**
   * Deletes an existing BBCode.
   * 
   * @param bbcodeId Id of the deleted BBCode.
   * @throws Exception
   * @LevelAPI Platform
   */
  public void delete(String bbcodeId) throws Exception;

}
