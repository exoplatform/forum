/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
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
package org.exoplatform.forum.common.utils;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.organization.User;

public class UserListAccess extends AbstractListAccess<User> {
  private ListAccess<User> listAccess;

  public UserListAccess(ListAccess<User> listAccess) throws Exception {
    this.listAccess = listAccess;
    this.size = listAccess.getSize();
  }

  @Override
  public User[] load(int offset, int limit) throws Exception, IllegalArgumentException {
    int min = Math.min(size, offset + limit);
    int newLimit = min - offset;
    return listAccess.load(offset, newLimit);
  }

  @Override
  public int getSize() throws Exception {
    return this.size;
  }

  @Override
  public User[] load(int pageSelect) throws Exception, IllegalArgumentException {
    setCurrentPage(pageSelect);
    int offset = getOffset();
    int limit = getPageSize();
    return load(offset, limit);
  }

}
