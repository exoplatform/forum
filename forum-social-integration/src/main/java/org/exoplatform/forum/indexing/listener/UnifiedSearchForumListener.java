package org.exoplatform.forum.indexing.listener;

import org.exoplatform.commons.api.indexing.IndexingService;
import org.exoplatform.commons.api.indexing.data.SearchEntry;
import org.exoplatform.commons.api.indexing.data.SearchEntryId;
import org.exoplatform.forum.service.*;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Indexing with :
 * - collection : "forum"
 * - type : (category|forum|topic|post)
 * - name : object id
 *
 * TODO No method for category and forum deletion
 */
public class UnifiedSearchForumListener extends ForumEventListener {

  private static Log log = ExoLogger.getLogger(UnifiedSearchForumListener.class);

  private final IndexingService indexingService;

  public UnifiedSearchForumListener(IndexingService indexingService) {
    this.indexingService = indexingService;
  }

  @Override
  public void saveCategory(Category category) {
    // TODO How to know of it is a new category or an update ?

    if(indexingService != null) {
      Map<String, Object> content = new HashMap<String, Object>();
      content.put("category", category);
      SearchEntry searchEntry = new SearchEntry("forum", "category", category.getId(), content);
      indexingService.add(searchEntry);
    }
  }

  @Override
  public void saveForum(Forum forum) {
    // TODO How to know of it is a new forum or an update ?

    if(indexingService != null) {
      Map<String, Object> content = new HashMap<String, Object>();
      content.put("forum", forum);
      SearchEntry searchEntry = new SearchEntry("forum", "forum", forum.getId(), content);
      indexingService.add(searchEntry);
    }
  }

  @Override
  public void addTopic(Topic topic) {
    if(indexingService != null) {
      Map<String, Object> content = new HashMap<String, Object>();
      content.put("topic", topic);
      SearchEntry searchEntry = new SearchEntry("forum", "topic", topic.getId(), content);
      indexingService.add(searchEntry);
    }
  }

  @Override
  public void updateTopic(Topic topic) {
    if(indexingService != null) {
      Map<String, Object> content = new HashMap<String, Object>();
      content.put("topic", topic);
      SearchEntryId searchEntryId = new SearchEntryId("forum", "topic", topic.getId());
      indexingService.update(searchEntryId, content);
    }
  }

  @Override
  public void updateTopics(List<Topic> topics, boolean isLock) {
    for(Topic topic: topics) {
      updateTopic(topic);
    }
  }

  @Override
  public void moveTopic(Topic topic, String toCategoryName, String toForumName) {
    // TODO No setCategoryId neither setForumId on Topic object. How to update it ?
  }

  public void movePost(List <Post> posts, List<String> srcPostActivityIds, String desTopicPath) {
  }
  
  @Override
  public void mergeTopic(Topic topic, String removeActivityId1, String removeActivityId2) {
    // TODO No mergeTopic(Topic topic1, Topic topic2, Topic mergedTopic) ...
  }

  @Override
  public void splitTopic(Topic newTopic, Topic splitedTopic, String removeActivityId) {
    if(indexingService != null) {
      Map<String, Object> content = new HashMap<String, Object>();
      content.put("topic", splitedTopic);
      SearchEntryId searchEntryId = new SearchEntryId("forum", "topic", splitedTopic.getId());
      indexingService.update(searchEntryId, content);

      Map<String, Object> contentNewTopic = new HashMap<String, Object>();
      contentNewTopic.put("topic", newTopic);
      SearchEntry searchEntry = new SearchEntry("forum", "topic", newTopic.getId(), contentNewTopic);
      indexingService.add(searchEntry);
    }
  }

  @Override
  public void addPost(Post post) {
    if(indexingService != null) {
      Map<String, Object> content = new HashMap<String, Object>();
      content.put("post", post);
      SearchEntry searchEntry = new SearchEntry("forum", "post", post.getId(), content);
      indexingService.add(searchEntry);
    }
  }

  @Override
  public void updatePost(Post post) {
    if(indexingService != null) {
      Map<String, Object> content = new HashMap<String, Object>();
      content.put("post", post);
      SearchEntryId searchEntryId = new SearchEntryId("forum", "post", post.getId());
      indexingService.update(searchEntryId, content);
    }
  }

  @Override
  public void updatePost(Post post, int type) {
    updatePost(post);
  }

  @Override
  public void removeActivity(String activityId) {
    // TODO No removeTopic(Topic topic) ...
  }

  @Override
  public void removeComment(String activityId, String commentId) {
    // TODO No removeTopic(Post post) ...
  }
}
