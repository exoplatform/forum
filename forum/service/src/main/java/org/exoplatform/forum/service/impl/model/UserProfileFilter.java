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

public class UserProfileFilter {
  
  private String searchKey;

  public UserProfileFilter(String searchKey) {
    this.searchKey = searchKey;
  }

  public String getSearchKey() {
    return this.searchKey;
  }
  
  private static boolean equals(String s1, String s2) {
    if (s1 == null) {
      return (s2 == null) ? true : false;
    }
    return s1.equals(s2);
  }
  
  @Override
  public boolean equals(Object o) {
    if (super.equals(o)) {
      return true;
    }
    if ((o instanceof UserProfileFilter) == false) {
      return false;
    }
    UserProfileFilter f = (UserProfileFilter) o;
    if(!equals(searchKey, f.searchKey)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return searchKey != null ? searchKey.hashCode() : 0;
  }

  @Override
  public String toString() {
    return new StringBuilder("UserProfileFilter{")
        .append("searchKey='").append(searchKey).append("'")
        .append('}').toString();
  }

}
