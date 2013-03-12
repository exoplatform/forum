package org.exoplatform.forum.service.search;

import org.exoplatform.forum.service.Utils;

/**
 * @author <a href="mailto:alain.defrance@exoplatform.com">Alain Defrance</a>
 */
public class PostId {
  
  String path;

  public PostId(String path) {
    this.path = path;
  }

  public String getTopicId() {
    return Utils.getTopicId(path);
  }

  public String getForumId() {
    return Utils.getForumId(path);
  }

  public String getCategoryId() {
    return Utils.getCategoryId(path);
  }

}
