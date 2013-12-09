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
package org.exoplatform.forum.service.filter.model;

import java.io.Serializable;

import org.exoplatform.forum.service.Utils;

public class ForumFilter implements Serializable {
  private static final long serialVersionUID = 1L;
  private String categoryId;
  private String forumId;
  private String userId;
  private String strQuery;
  private String forumName;
  private boolean summary = false;
  private boolean isPublic= false;
  private int offset = 0;
  private int limit = 0;
  
  public ForumFilter() {
  }

  public ForumFilter(String forumId, String forumName) {
    this.forumId = forumId;
    this.forumName = forumName;
  }

  public ForumFilter(String categoryId, String strQuery, boolean summary) {
    this.categoryId = categoryId;
    this.strQuery = strQuery;
    this.summary = summary;
  }

  public ForumFilter(String categoryId, boolean summary) {
    this.categoryId = categoryId;
    this.summary = summary;
  }

  public String getForumId() {
    return forumId;
  }

  public ForumFilter setForumId(String forumId) {
    this.forumId = forumId;
    return this;
  }

  public String getForumName() {
    return forumName;
  }

  public ForumFilter setForumName(String forumName) {
    this.forumName = forumName;
    return this;
  }
  
  public String categoryId() {
    return categoryId;
  }

  public ForumFilter categoryId(String categoryId) {
    this.categoryId = categoryId;
    return this;
  }

  public boolean isSummary() {
    return summary;
  }

  public ForumFilter setSummary(boolean summary) {
    this.summary = summary;
    return this;
  }

  public boolean isPublic() {
    return isPublic;
  }

  public ForumFilter isPublic(boolean isPublic) {
    this.isPublic = isPublic;
    return this;
  }

  public String userId() {
    return userId;
  }

  public ForumFilter userId(String userId) {
    this.userId = userId;
    return this;
  }

  public int offset() {
    return offset;
  }
  
  public ForumFilter offset(int offset) {
    this.offset = offset;
    return this;
  }

  public int limit() {
    return limit;
  }
  
  public ForumFilter limit(int limit) {
    this.limit = limit;
    return this;
  }

  public String strQuery() {
    return strQuery;
  }

  public ForumFilter strQuery(String strQuery) {
    this.strQuery = strQuery;
    return this;
  }

  private boolean equals(String s, String that) {
    if (s != null ? !s.equals(that) : that != null) {
      return false;
    }
    return true;
  }
  
  @Override
  public boolean equals(Object o) {
    if (o instanceof ForumFilter) {
      ForumFilter forumFilter = (ForumFilter) o;
      if (Utils.isEmpty(forumName) == false && (equals(forumId, forumFilter.forumId))) {
        return true;
      }

      if(limit != forumFilter.limit) return false;
      if(offset != forumFilter.offset) return false;
      if(summary != forumFilter.summary) return false;
      if(isPublic != forumFilter.isPublic) return false;

      if(equals(categoryId, forumFilter.categoryId) == false) return false;
      if(equals(forumId, forumFilter.forumId) == false) return false;
      if(equals(strQuery, forumFilter.strQuery) == false) return false;
      if(equals(userId, forumFilter.userId) == false) return false;

    }
    return true;
  }

  private int hashCode(int current, Object o) {
    if (o != null) {
      return 31 * current + o.toString().hashCode();
    }
    return current;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = hashCode(result, categoryId);
    result = hashCode(result, forumId);
    result = hashCode(result, strQuery);
    result = hashCode(result, isPublic);
    result = hashCode(result, userId);
    result = hashCode(result, String.valueOf(summary));
    result = 31 * result + offset;
    result = 31 * result + limit;
    return result;
  }

}
