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
package org.exoplatform.forum.service.task;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.forum.service.MessageBuilder;
import org.exoplatform.forum.service.Post;
import org.exoplatform.forum.service.Topic;
import org.exoplatform.forum.service.impl.JCRDataStorage;

public abstract class AbstractForumTask {

  private static JCRDataStorage storage;

  protected static JCRDataStorage getJCRDataStorage() {
    if (storage == null) {
      storage = CommonsUtils.getService(JCRDataStorage.class);
    }
    return storage;
  }
  
  @Override
  public boolean equals(Object obj) {
    return super.equals(obj);
  }
  
  /**
   * The method use to running task.
   * @throws Exception
   */
  public abstract void run() throws Exception;
  
  /**
   * The class is task to call query last post of forum 
   *
   */
  public static class QueryLastPostTask extends AbstractForumTask {

    /** The jcr path of forum node. **/
    private final String forumPath;

    public QueryLastPostTask(String forumPath) {
      this.forumPath = forumPath;
    }

    @Override
    public void run() throws Exception {
      getJCRDataStorage().queryLastPostForum(forumPath);
    }

    @Override
    public boolean equals(Object o) {
      if (super.equals(o)) {
        return true;
      }
      if (o instanceof QueryLastPostTask) {
        QueryLastPostTask that = (QueryLastPostTask) o;
        if (forumPath != null ? !forumPath.equals(that.forumPath) : that.forumPath != null) {
          return false;
        }
        return true;
      }
      return false;
    }
  }
  
  /**
   * The class is task to call send notification e-mails watched of forum.
   *
   */
  public static class SendNotificationTask extends AbstractForumTask {
    /** The Topic **/
    private final Topic          topic;
    /** The Post **/
    private final Post           post;
    /** The jcr path of node that watched e-mails notification **/
    private final String         nodePath;
    /** The status is approve post or not **/
    private final boolean        isApprovePost;
    /** The MessageBuilder **/
    private final MessageBuilder messageBuilder;

    public SendNotificationTask(String nodePath, Topic topic, Post post, MessageBuilder messageBuilder, boolean isApprovePost) {
      this.topic = topic;
      this.post = post;
      this.nodePath = nodePath;
      this.messageBuilder = messageBuilder;
      this.isApprovePost = isApprovePost;
    }

    @Override
    public void run() throws Exception {
      getJCRDataStorage().sendNotification(nodePath, topic, post, messageBuilder, isApprovePost);
    }

    @Override
    public boolean equals(Object o) {
      if(super.equals(o)) {
        return true;
      }
      if (o instanceof SendNotificationTask) {
        SendNotificationTask that = (SendNotificationTask) o;
        if (isApprovePost != that.isApprovePost) {
          return false;
        }
        if (nodePath != null ? !nodePath.equals(that.nodePath) : that.nodePath != null) {
          return false;
        }
        if (post != null ? !post.getId().equals((that.post == null) ? "" : that.post.getId()) : that.post != null) {
          return false;
        }
        if (topic != null ? !topic.getId().equals((that.topic == null) ? "" : that.topic.getId()) : that.topic != null) {
          return false;
        }
        return true;
      }
      return false;
    }
  }
}
