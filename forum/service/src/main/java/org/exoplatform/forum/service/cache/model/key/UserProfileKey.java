/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.forum.service.cache.model.key;

import org.exoplatform.forum.common.cache.model.ScopeCacheKey;

public class UserProfileKey extends ScopeCacheKey {
  private static final long serialVersionUID = 1L;
  
  private final String userId;
  
  public UserProfileKey(String userId) {
    this.userId = userId;
  }
  
  public String getUserId() {
    return userId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof UserProfileKey)) return false;
    if (!super.equals(o)) return false;

    UserProfileKey profileKey = (UserProfileKey) o;

    if (userId != null ? !userId.equals(profileKey.userId) : profileKey.userId != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (userId != null ? userId.hashCode() : 0);
    return result;
  }

}
