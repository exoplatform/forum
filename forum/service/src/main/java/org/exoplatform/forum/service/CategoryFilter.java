/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
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

import java.util.HashMap;
import java.util.Map;

/**
 * Created by The eXo Platform SAS
 * Author : Vu Duy Tu
 *          tuvd@exoplatform.com
 * Dec 27, 2012  
 */
public class CategoryFilter {
  private String              categoryId;

  private String              categoryName;

  // The list forum's information <forumId, forumName>
  private Map<String, String> forumInfos;

  public CategoryFilter(String categoryId, String categoryName) {
    this.categoryId = categoryId;
    this.categoryName = categoryName;
    this.forumInfos = new HashMap<String, String>();
  }

  public CategoryFilter() {
    this.forumInfos = new HashMap<String, String>();
  }

  public String getCategoryId() {
    return categoryId;
  }

  public void setCategoryId(String categoryId) {
    this.categoryId = categoryId;
  }

  public String getCategoryName() {
    return categoryName;
  }

  public void setCategoryName(String categoryName) {
    this.categoryName = categoryName;
  }

  public Map<String, String> getForumInfos() {
    return forumInfos;
  }

  public void setForumInfos(Map<String, String> forumInfos) {
    this.forumInfos = forumInfos;
  }

  public void setForumInfos(String forumId, String forumName) {
    this.forumInfos.put(forumId, forumName);
  }
  
  public boolean equals(CategoryFilter categoryFilter) {
    if(categoryFilter.getCategoryId() == this.categoryId) {
      return true;
    }
    return false;
  }
}
