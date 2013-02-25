package org.exoplatform.forum.search;

import java.util.Collections;
import java.util.List;

import org.exoplatform.commons.api.search.data.SearchResult;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.forum.membership.AbstractJCRTestCase;
import org.exoplatform.forum.service.Category;
import org.exoplatform.forum.service.Forum;
import org.exoplatform.forum.service.MessageBuilder;
import org.exoplatform.forum.service.Post;
import org.exoplatform.forum.service.Topic;
import org.exoplatform.forum.service.impl.JCRDataStorage;
import org.exoplatform.forum.service.search.DiscussionSearchConnector;
import org.exoplatform.services.security.ConversationState;

/**
 * @author <a href="mailto:alain.defrance@exoplatform.com">Alain Defrance</a>
 */
public class DiscussionSearchConnectorTestCase extends AbstractJCRTestCase {

  private DiscussionSearchConnector discussionSearchConnector;
  private Category cat;
  private Post postA;

  @Override
  public void setUp() throws Exception {

    super.setUp();

    //

    cat = new Category();
    cat.setCategoryName("Category A");
    forumService_.saveCategory(cat, true);

    Forum forum = new Forum();
    forum.setForumName("Forum A");
    forumService_.saveForum(cat.getId(), forum, true);

    Topic topic = new Topic();
    topic.setTopicName("Topic A");
    topic.setOwner("foo");
    forumService_.saveTopic(cat.getId(), forum.getId(), topic, true, false, new MessageBuilder());

    postA = new Post();
    postA.setName("Post A");
    postA.setMessage("This is the A message");
    postA.setOwner("foo");
    forumService_.savePost(cat.getId(), forum.getId(), topic.getId(), postA, true, new MessageBuilder());

    Post postB = new Post();
    postB.setName("Post B");
    postB.setMessage("This is the B message");
    postB.setOwner("foo");
    forumService_.savePost(cat.getId(), forum.getId(), topic.getId(), postB, true, new MessageBuilder());

    //
    InitParams params = new InitParams();
    params.put("constructor.params", new PropertiesParam());
    
    JCRDataStorage dataStorage = (JCRDataStorage) getService(JCRDataStorage.class);
    
    discussionSearchConnector = new DiscussionSearchConnector(params, dataStorage);

  }

  @Override
  public void tearDown() throws Exception {
    if (cat != null) {
      forumService_.removeCategory(cat.getId());
    }
    super.tearDown();
  }

  public void testFilter() throws Exception {
    setUser("foo");
    assertEquals(2, discussionSearchConnector.search(null, "Post", Collections.EMPTY_LIST, 0, 0, "relevancy", "ASC").size());
    assertEquals(1, discussionSearchConnector.search(null, "A", Collections.EMPTY_LIST, 0, 0, "relevancy", "ASC").size());
    assertEquals(2, discussionSearchConnector.search(null, "message", Collections.EMPTY_LIST, 0, 0, "relevancy", "ASC").size());
    assertEquals(1, discussionSearchConnector.search(null, "B message", Collections.EMPTY_LIST, 0, 0, "relevancy", "ASC").size());

  }

  public void testData() throws Exception {
    setUser("foo");
    List<SearchResult> aResults = (List<SearchResult>) discussionSearchConnector.search(null, "A", Collections.EMPTY_LIST, 0, 10, "relevancy", "ASC");
    SearchResult aResult = aResults.get(0);
    assertEquals("Post A", aResult.getTitle());
    assertEquals("This is the A message", aResult.getExcerpt());
    assertEquals("", aResult.getUrl());
    assertTrue(aResult.getDate() > 0);
    assertEquals(postA.getCreatedDate().getTime(), aResult.getDate());
  }

  public void testOrder() throws Exception {
    setUser("foo");
    List<SearchResult> rTitleAsc = (List<SearchResult>) discussionSearchConnector.search(null, "Post", Collections.EMPTY_LIST, 0, 10, "title", "ASC");
    assertEquals("Post A", rTitleAsc.get(0).getTitle());
    assertEquals("Post B", rTitleAsc.get(1).getTitle());

    List<SearchResult> rTitleDesc = (List<SearchResult>) discussionSearchConnector.search(null, "Post", Collections.EMPTY_LIST, 0, 10, "title", "DESC");
    assertEquals("Post B", rTitleDesc.get(0).getTitle());
    assertEquals("Post A", rTitleDesc.get(1).getTitle());

    List<SearchResult> rDateAsc = (List<SearchResult>) discussionSearchConnector.search(null, "Post", Collections.EMPTY_LIST, 0, 10, "date", "ASC");
    assertEquals("Post A", rDateAsc.get(0).getTitle());
    assertEquals("Post B", rDateAsc.get(1).getTitle());

    List<SearchResult> rDateDesc = (List<SearchResult>) discussionSearchConnector.search(null, "Post", Collections.EMPTY_LIST, 0, 10, "date", "DESC");
    assertEquals("Post B", rDateDesc.get(0).getTitle());
    assertEquals("Post A", rDateDesc.get(1).getTitle());
  }

  private void setUser(String user) {
    ConversationState.setCurrent(new ConversationState(new org.exoplatform.services.security.Identity(user)));
  }
}
