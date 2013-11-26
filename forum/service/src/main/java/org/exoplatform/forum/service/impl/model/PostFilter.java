/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.forum.service.impl.model;

public class PostFilter {

  private String categoryId = null;
  private String forumId = null;
  private String topicId = null; 
  private String isApproved = null;
  private String isWaiting = null;
  private String isHidden = null;
  private String userLogin = null;
  
  private String topicPath = null;
  
  private String ip = null;
  private String orderBy = null;

  public PostFilter(String categoryId, String forumId, String topicId, String isApproved, String isHidden, String isWaiting, String userLogin) {
    this.categoryId = categoryId;
    this.forumId = forumId;
    this.topicId = topicId;
    this.isApproved = isApproved;
    this.isWaiting = isWaiting;
    this.isHidden = isHidden;
    this.userLogin = userLogin;
  }
  
  public PostFilter(String topicPath) {
    this.topicPath = topicPath;
  }

  public PostFilter(String ip, String orderBy) {
    this.ip = ip;
    this.orderBy = orderBy;
  }
  
  public String getTopicPath() {
    return topicPath;
  }
  
  public String getCategoryId() {
    return categoryId;
  }
  public String getForumId() {
    return forumId;
  }
  public String getTopicId() {
    return topicId;
  }
  public String getIsApproved() {
    return isApproved;
  }
  public String getIsWaiting() {
    return isWaiting;
  }
  public String getIsHidden() {
    return isHidden;
  }
  public String getUserLogin() {
    return userLogin;
  }

  public String getIP() {
    return ip;
  }
  
  public String orderBy() {
    return orderBy;
  }

  public PostFilter orderBy(String orderBy) {
    this.orderBy = orderBy;
    return this;
  }

  @Override
  public String toString() {
    return new StringBuilder("PostFilter{")
        .append("categoryId='").append(categoryId).append("'")
        .append(", forumId='").append(forumId).append("'")
        .append(", topicId='").append(topicId).append("'")
        .append(", isApproved='").append(isApproved).append("'")
        .append(", isWaiting='").append(isWaiting).append("'")
        .append(", isHidden='").append(isHidden).append("'")
        .append(", userLogin='").append(userLogin).append("'")
        .append(", topicPath='").append(topicPath ).append("'")
        .append('}').toString();
  }
  
}