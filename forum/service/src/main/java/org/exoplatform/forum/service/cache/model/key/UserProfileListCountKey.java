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
import org.exoplatform.forum.service.impl.model.UserProfileFilter;

public class UserProfileListCountKey extends ScopeCacheKey {

  private static final long serialVersionUID = 1L;
  
  private UserProfileFilter filter;
  
  public UserProfileListCountKey(UserProfileFilter filter) {
    this.filter = filter;
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof UserProfileListCountKey))
      return false;
    if (!super.equals(o))
      return false;

    UserProfileListCountKey that = (UserProfileListCountKey) o;

    if (filter == null || filter.equals(that.filter) == false) {
      return false;
    }

    return true;
  }
  
  private int getHashCode(int current, Object key) {
    return 31 * current + (key != null ? key.hashCode() : 0);
  }
  
  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = getHashCode(result, filter.getSearchKey());
    return result;
  }

}
