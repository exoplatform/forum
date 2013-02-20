package org.exoplatform.forum.search;

import org.exoplatform.commons.api.search.data.SearchResult;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.forum.membership.AbstractJCRTestCase;
import org.exoplatform.forum.service.*;
import org.exoplatform.forum.service.impl.JCRDataStorage;
import org.exoplatform.forum.service.search.DiscussionSearchConnector;
import org.exoplatform.services.security.ConversationState;

import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:alain.defrance@exoplatform.com">Alain Defrance</a>
 */
public class DiscussionSearchConnectorTestCase extends AbstractJCRTestCase {

  private JCRDataStorage storage;
  private DiscussionSearchConnector discussionSearchConnector;
  private Category cat;
  private Post postA;

  @Override
  public void setUp() throws Exception {

    super.setUp();

    //
    storage = new JCRDataStorage();
    storage.setDataLocator(dataLocation);

    cat = new Category();
    cat.setCategoryName("Category A");
    storage.saveCategory(cat, true);

    Forum forum = new Forum();
    forum.setForumName("Forum A");
    storage.saveForum(cat.getId(), forum, true);

    Topic topic = new Topic();
    topic.setTopicName("Topic A");
    topic.setOwner("foo");
    storage.saveTopic(cat.getId(), forum.getId(), topic, true, false, new MessageBuilder());

    postA = new Post();
    postA.setName("Post A");
    postA.setMessage("This is the A message");
    postA.setOwner("foo");
    storage.savePost(cat.getId(), forum.getId(), topic.getId(), postA, true, new MessageBuilder());

    Post postB = new Post();
    postB.setName("Post B");
    postB.setMessage("This is the B message");
    postB.setOwner("foo");
    storage.savePost(cat.getId(), forum.getId(), topic.getId(), postB, true, new MessageBuilder());

    //
    InitParams params = new InitParams();
    params.put("constructor.params", new PropertiesParam());
    discussionSearchConnector = new DiscussionSearchConnector(params, storage);

  }

  @Override
  public void tearDown() throws Exception {
    if (cat != null) {
      storage.removeCategory(cat.getId());
    }
    super.tearDown();
  }

  public void testFilter() throws Exception {
    setUser("foo");
    assertEquals(2, discussionSearchConnector.search("Post", Collections.EMPTY_LIST, 0, 0, "relevancy", "ASC").size());
    assertEquals(1, discussionSearchConnector.search("A", Collections.EMPTY_LIST, 0, 0, "relevancy", "ASC").size());
    assertEquals(2, discussionSearchConnector.search("message", Collections.EMPTY_LIST, 0, 0, "relevancy", "ASC").size());
    assertEquals(1, discussionSearchConnector.search("B message", Collections.EMPTY_LIST, 0, 0, "relevancy", "ASC").size());

  }

  public void testData() throws Exception {
    setUser("foo");
    List<SearchResult> aResults = (List<SearchResult>) discussionSearchConnector.search("A", Collections.EMPTY_LIST, 0, 10, "relevancy", "ASC");
    SearchResult aResult = aResults.get(0);
    assertEquals("Post A", aResult.getTitle());
    assertEquals("This is the A message", aResult.getExcerpt());
    assertEquals("url://" + postA.getId(), aResult.getUrl());
    assertTrue(aResult.getDate() > 0);
    assertEquals(postA.getCreatedDate().getTime(), aResult.getDate());
  }

  public void testOrder() throws Exception {
    setUser("foo");
    List<SearchResult> rTitleAsc = (List<SearchResult>) discussionSearchConnector.search("Post", Collections.EMPTY_LIST, 0, 10, "title", "ASC");
    assertEquals("Post A", rTitleAsc.get(0).getTitle());
    assertEquals("Post B", rTitleAsc.get(1).getTitle());

    List<SearchResult> rTitleDesc = (List<SearchResult>) discussionSearchConnector.search("Post", Collections.EMPTY_LIST, 0, 10, "title", "DESC");
    assertEquals("Post B", rTitleDesc.get(0).getTitle());
    assertEquals("Post A", rTitleDesc.get(1).getTitle());

    List<SearchResult> rDateAsc = (List<SearchResult>) discussionSearchConnector.search("Post", Collections.EMPTY_LIST, 0, 10, "date", "ASC");
    assertEquals("Post A", rDateAsc.get(0).getTitle());
    assertEquals("Post B", rDateAsc.get(1).getTitle());

    List<SearchResult> rDateDesc = (List<SearchResult>) discussionSearchConnector.search("Post", Collections.EMPTY_LIST, 0, 10, "date", "DESC");
    assertEquals("Post B", rDateDesc.get(0).getTitle());
    assertEquals("Post A", rDateDesc.get(1).getTitle());
  }

  private void setUser(String user) {
    ConversationState.setCurrent(new ConversationState(new org.exoplatform.services.security.Identity(user)));
  }
}
