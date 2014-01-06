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
package org.exoplatform.forum.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.exoplatform.forum.service.ForumEventLifeCycle;
import org.exoplatform.forum.service.ForumEventListener;
import org.exoplatform.forum.service.Post;
import org.exoplatform.forum.service.Topic;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public abstract class ForumEventCompletion implements Callable<Boolean> {
  protected static final Log LOG = ExoLogger.getLogger(ForumServiceImpl.class);
  protected List<ForumEventListener> listeners_ = new ArrayList<ForumEventListener>(3);
  
  public ForumEventCompletion() {
  }
  
  public static class ProcessPost extends ForumEventCompletion {
    private Post    post;
    private boolean isNew;

    public ProcessPost(Post post, boolean isNew) {
      this.post = post;
      this.isNew = isNew;
    }

    @Override
    public Boolean call() throws Exception {
      for (ForumEventLifeCycle f : listeners_) {
        try {
          if (isNew)
            f.addPost(post);
          else
            f.updatePost(post);
        } catch (Exception e) {
          LOG.debug("Failed to run function addPost/updatePost in the class ForumEventLifeCycle. ", e);
          return false;
        }
      }
      return true;
    }
  }
  
  public static class ProcessTopic extends ForumEventCompletion {
    private boolean isNew;
    private Topic   topic;

    public ProcessTopic(Topic topic, boolean isNew) {
      this.topic = topic;
      this.isNew = isNew;
    }

    @Override
    public Boolean call() throws Exception {
      for (ForumEventLifeCycle f : listeners_) {
        try {
          if (isNew) {
            f.addTopic(topic);
          } else if (topic != null) {
            f.updateTopic(topic);
          }
        } catch (Exception e) {
          LOG.debug("Failed to run function addTopic/updateTopic in the class ForumEventLifeCycle. ", e);
          return false;
        }
      }
      return true;
    }
  }

  public ForumEventCompletion setListeners(List<ForumEventListener> listeners_) {
    this.listeners_ = listeners_;
    return this;
  }
}
