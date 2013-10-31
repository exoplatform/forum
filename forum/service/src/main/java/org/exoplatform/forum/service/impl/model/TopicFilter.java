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
* along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.forum.service.impl.model;

public class TopicFilter {
  private String   categoryId;
  private String   forumId;
  private String   userLogin;

  private String   orderBy;

  private String[] viewers;
  private boolean isApproved = false;
  private boolean isAdmin = false;

  public TopicFilter(String categoryId, String forumId) {
    this.categoryId = categoryId;
    this.forumId = forumId;
  }

  public String categoryId() {
    return categoryId;
  }

  public TopicFilter categoryId(String categoryId) {
    this.categoryId = categoryId;
    return this;
  }

  public String forumId() {
    return forumId;
  }

  public TopicFilter forumId(String forumId) {
    this.forumId = forumId;
    return this;
  }

  public String userLogin() {
    return userLogin;
  }

  public TopicFilter userLogin(String userLogin) {
    this.userLogin = userLogin;
    return this;
  }

  public boolean isApproved() {
    return isApproved;
  }

  public TopicFilter isApproved(boolean isApproved) {
    this.isApproved = isApproved;
    return this;
  }

  public String orderBy() {
    return orderBy;
  }

  public TopicFilter orderBy(String orderBy) {
    this.orderBy = orderBy;
    return this;
  }

  public String[] viewers() {
    return viewers;
  }

  public TopicFilter viewers(String[] viewers) {
    this.viewers = viewers;
    return this;
  }

  public boolean isAdmin() {
    return isAdmin;
  }

  public TopicFilter isAdmin(boolean isAdmin) {
    this.isAdmin = isAdmin;
    return this;
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
    if ((o instanceof TopicFilter) == false) {
      return false;
    }
    TopicFilter f = (TopicFilter) o;
    if(isAdmin != f.isAdmin || 
        isApproved != f.isApproved ||
        !equals(forumId, f.forumId) ||
        !equals(userLogin, f.userLogin) ||
        !equals(orderBy, f.orderBy)
        ) {
      return false;
    }

    return true;
  }

  @Override
  public String toString() {
    return new StringBuilder("PostFilter{")
        .append("categoryId='").append(categoryId).append("'")
        .append(", forumId='").append(forumId).append("'")
        .append(", isAdmin='").append(isAdmin).append("'")
        .append(", isApproved='").append(isApproved).append("'")
        .append(", userLogin='").append(userLogin).append("'")
        .append('}').toString();
  }
}
