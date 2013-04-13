/***************************************************************************
 * Copyright 2001-2008 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.forum.service.ws;

import java.util.List;

import org.exoplatform.forum.service.Post;

public class MessageBean {
  private List<Post> data;

  public void setData(List<Post> postlist) {
    this.data = postlist;
  }

  public List<Post> getData() {
    return data;
  }

}
