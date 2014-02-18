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
package org.exoplatform.forum.mock;


public class ResultTestForumListener {

  public enum STATUS {
    ADD_TOPIC, UPDATE_TOPIC, ADD_POST, UPDATE_POST
  }
  private int topicCount = 0;
  private int postCount = 0;
  private STATUS status = null;

  public ResultTestForumListener() {
  }

  public STATUS getStatus() {
    return this.status;
  }

  public void setStatus(STATUS status) {
    this.status = status;
    if(status.equals(STATUS.ADD_TOPIC)) {
      ++topicCount;
    }
    if(status.equals(STATUS.ADD_POST)) {
      ++postCount;
    }
  }

  public int getTopicCount() {
    return topicCount;
  }

  public int getPostCount() {
    return postCount;
  }
  
  public void reset() {
    topicCount = postCount = 0;
    status = null;
  }
  
}
