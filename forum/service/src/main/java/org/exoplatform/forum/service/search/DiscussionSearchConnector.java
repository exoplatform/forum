package org.exoplatform.forum.service.search;

import org.exoplatform.commons.api.search.SearchServiceConnector;
import org.exoplatform.commons.api.search.data.SearchResult;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.forum.service.*;
import org.exoplatform.forum.service.impl.JCRDataStorage;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:alain.defrance@exoplatform.com">Alain Defrance</a>
 */
public class DiscussionSearchConnector extends SearchServiceConnector {

  private Pattern pattern;
  private JCRDataStorage storage;
  private static final Log LOG = ExoLogger.getLogger(DiscussionSearchConnector.class);


  public DiscussionSearchConnector(InitParams initParams, JCRDataStorage storage) {
    super(initParams);
    this.storage = storage;
    this.pattern = Pattern.compile("/");
  }

  @Override
  public Collection<SearchResult> search(String query, Collection<String> sites, int offset, int limit, String sort, String order) {

    List<SearchResult> results = new ArrayList<SearchResult>();

    try {
      List<ForumSearch> searchResults = storage.getQuickSearch(query, "false,post", null, "foo", Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST, offset, limit, sort, order);
      for (ForumSearch searchResult : searchResults) {
        PostId id = new PostId(pattern, searchResult.getPath());
        Forum forum = storage.getForum(id.getCategoryId(), id.getForumId());
        Topic topic = storage.getTopic(id.getCategoryId(), id.getForumId(), id.getTopicId(), "foo");
        Post post = storage.getPost(id.getCategoryId(), id.getForumId(), id.getTopicId(), id.getPostId());
        StringBuilder sb = new StringBuilder();
        sb.append(forum.getForumName());
        sb.append(" - " + topic.getPostCount() + " replies");
        sb.append(" - " + topic.getVoteRating());
        sb.append(" - " + post.getCreatedDate());
        SearchResult result = new SearchResult(
            "url://" + post.getId(), // TODO : compute url
            post.getName(),
            post.getMessage(), // TODO : get excerpt
            sb.toString(),
            "image url", // TODO : set image url probably declared value
            post.getCreatedDate().getTime(),
            0);
        results.add(result);
      }

    } catch (Exception e) {
      LOG.error(e);
    }

    return results;
  }

}
