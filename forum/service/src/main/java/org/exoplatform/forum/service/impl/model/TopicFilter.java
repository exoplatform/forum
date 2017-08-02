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

import java.io.Serializable;

public class TopicFilter implements Serializable {
  private static final long serialVersionUID = 1L;
  private String   categoryId = null;
  private String   forumId = null;
  private String   userLogin = null;

  private String   orderBy = null;

  private String[] viewers;
  private boolean isApproved = false;
  private boolean isAdmin = false;
  
  private long date = 0l;
  private String forumPath = null;
  
  private String userName = null;

  public TopicFilter(String categoryId, String forumId) {
    this.categoryId = categoryId;
    this.forumId = forumId;
  }

  public TopicFilter(long date, String forumPath) {
    this.forumPath = forumPath;
    this.date = date;
  }

  public TopicFilter(String userName, boolean isAdmin, String orderBy) {
    this.userName = userName;
    this.orderBy = orderBy;
    this.isAdmin = isAdmin;
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

  public String forumPath() {
    return forumPath;
  }
  
  public TopicFilter forumPath(String forumPath) {
    this.forumPath = forumPath;
    return this;
  }

  public String userName() {
    return userName;
  }

  public TopicFilter userName(String userName) {
    this.userName = userName;
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

  public long date() {
    return date;
  }
  
  public TopicFilter date(long date) {
    this.date = date;
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
        date != f.date ||
        !equals(forumPath, f.forumPath) ||
        !equals(userName, f.userName) ||
        !equals(forumId, f.forumId) ||
        !equals(userLogin, f.userLogin) ||
        !equals(orderBy, f.orderBy)
        ) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = forumId != null ? forumId.hashCode() : 0;
    result = 31 * result + (userLogin != null ? userLogin.hashCode() : 0);
    result = 31 * result + (orderBy != null ? orderBy.hashCode() : 0);
    result = 31 * result + (isApproved ? 1 : 0);
    result = 31 * result + (isAdmin ? 1 : 0);
    result = 31 * result + (int) (date ^ (date >>> 32));
    result = 31 * result + (forumPath != null ? forumPath.hashCode() : 0);
    result = 31 * result + (userName != null ? userName.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return new StringBuilder("PostFilter{")
        .append("categoryId='").append(categoryId).append("'")
        .append(", forumId='").append(forumId).append("'")
        .append(", isAdmin='").append(isAdmin).append("'")
        .append(", isApproved='").append(isApproved).append("'")
        .append(", userLogin='").append(userLogin).append("'")
        .append(", userName='").append(userName).append("'")
        .append(", date='").append(date).append("'")
        .append(", forumPath='").append(forumPath).append("'")
        .append('}').toString();
  }
}
