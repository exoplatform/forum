/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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

import java.io.Serializable;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class CacheUserProfile {
  private static final Log LOG = ExoLogger.getLogger(CacheUserProfile.class);

  private static ExoCache<Serializable, UserProfile> getCache() throws Exception {
    return CommonsUtils.getService(CacheService.class).getCacheInstance("forum.UserProfiles");
  }

  /**
   * Store the UserProfile of the user online storage in cache
   * @param userName
   * @param userProfile
   */
  public static void storeInCache(String userName, UserProfile userProfile) {
    try {
      ExoCache<Serializable, UserProfile> cache = getCache();
      Serializable cacheKey = getCacheKey(userName);
      cache.put(cacheKey, userProfile);
    } catch (Exception e) {
      LOG.warn("Failed to storage UserProfile to eXoCache", e);
    }
  }

  /**
   * Remove the UserProfile of the user online storage in cache
   * @param userName
   */
  public static void removeInCache(String userName) {
    ExoCache<Serializable, UserProfile> cache;
    try {
      cache = getCache();
      Serializable cacheKey = getCacheKey(userName);
      cache.remove(cacheKey);
    } catch (Exception e) {
      LOG.warn("Failed to remove UserProfile to eXoCache: " + userName);
    }
  }

  /**
   * Remove all UserProfile storage in cache
   */
  public static void clearCache() {
    ExoCache<Serializable, UserProfile> cache;
    try {
      cache = getCache();
      cache.clearCache();
    } catch (Exception e) {
      LOG.warn("Failed to clear all UserProfile cached.");
    }
  }

  /**
   * Load a UserProfile of the user online from expressions in cache
   * @param userName
   * @return UserProfile
   */
  public static UserProfile getFromCache(String userName) {
    if (Utils.isEmpty(userName) || UserProfile.USER_GUEST.equals(userName))
      return null;
    try {
      ExoCache<Serializable, UserProfile> cache = getCache();
      Serializable cacheKey = getCacheKey(userName);
      return cache.get(cacheKey);
    } catch (Exception e) {
      LOG.warn("Failed to get UserProfile to eXoCache: " + userName);
      return null;
    }
  }

  private static Serializable getCacheKey(String userName) {
    return userName;
  }
}
