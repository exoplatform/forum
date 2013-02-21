package org.exoplatform.forum.service.search;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:alain.defrance@exoplatform.com">Alain Defrance</a>
 */
public class PostId {

  private String[] pathParts;

  public PostId(Pattern pattern, String path) {
    pathParts = pattern.split(path);
  }

  public String getPostId() {
    return pathParts[pathParts.length - 1];
  }

  public String getTopicId() {
    return pathParts[pathParts.length - 2];
  }

  public String getForumId() {
    return pathParts[pathParts.length - 3];
  }

  public String getCategoryId() {
    return pathParts[pathParts.length - 4];
  }

}
