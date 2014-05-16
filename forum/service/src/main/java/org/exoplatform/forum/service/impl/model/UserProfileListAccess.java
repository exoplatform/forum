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
package org.exoplatform.forum.service.impl.model;

import org.exoplatform.forum.service.DataStorage;
import org.exoplatform.forum.service.UserProfile;

public class UserProfileListAccess extends AbstractListAccess<UserProfile> {
  
  DataStorage storage;
  
  UserProfileFilter userProfileFilter;
  
  public UserProfileListAccess(DataStorage storage, UserProfileFilter userProfileFilter) {
    this.storage = storage;
    this.userProfileFilter = userProfileFilter;
  }

  @Override
  public UserProfile[] load(int index, int length) throws Exception, IllegalArgumentException {
    return storage.searchUserProfileByFilter(userProfileFilter, index, length).toArray(new UserProfile[0]);
  }

  @Override
  public int getSize() throws Exception {
    return storage.getUserProfileByFilterCount(userProfileFilter);
  }

  @Override
  public UserProfile[] load(int pageSelect) throws Exception, IllegalArgumentException {
    int offset = getOffset(pageSelect);
    int limit = getPageSize();
    setCurrentPage(pageSelect);
    return load(offset, limit);
  }

}
