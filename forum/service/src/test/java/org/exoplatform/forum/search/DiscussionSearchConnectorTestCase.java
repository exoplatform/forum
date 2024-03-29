package org.exoplatform.forum.search;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.exoplatform.ecms.legacy.search.data.SearchContext;
import org.exoplatform.ecms.legacy.search.data.SearchResult;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.forum.base.BaseForumServiceTestCase;
import org.exoplatform.forum.service.Category;
import org.exoplatform.forum.service.Forum;
import org.exoplatform.forum.service.MessageBuilder;
import org.exoplatform.forum.service.Post;
import org.exoplatform.forum.service.Topic;
import org.exoplatform.forum.service.Utils;
import org.exoplatform.forum.service.search.DiscussionSearchConnector;
import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.exoplatform.portal.mop.navigation.NavigationService;
import org.exoplatform.portal.mop.navigation.NavigationState;
import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.navigation.NodeModel;
import org.exoplatform.portal.mop.navigation.NodeState;
import org.exoplatform.portal.mop.navigation.Scope;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.page.PageService;
import org.exoplatform.portal.mop.page.PageState;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.web.controller.metadata.ControllerDescriptor;
import org.exoplatform.web.controller.metadata.DescriptorBuilder;
import org.exoplatform.web.controller.router.Router;
import org.exoplatform.web.controller.router.RouterConfigException;

/**
 * @author <a href="mailto:alain.defrance@exoplatform.com">Alain Defrance</a>
 */
public class DiscussionSearchConnectorTestCase extends BaseForumServiceTestCase {
  private final static String CONTROLLER_PATH = "conf/controller/test-controller.xml";

  private DiscussionSearchConnector discussionSearchConnector;
  private Post postA;
  private Post postG;
  private SearchContext context;
  private Router router;
  
  final static Pattern    HIGHLIGHT_PATTERN  = Pattern.compile("(.*)<strong>(.*)</strong>(.*)");

  @Override
  public void setUp() throws Exception {

    super.setUp();
    
    loginUser(USER_ROOT);
    loadController();

    //
    String cateId = getId(Utils.CATEGORY);
    Category cat = createCategory(cateId);
    cat.setCategoryName("Category A");
    forumService_.saveCategory(cat, true);
    
    String spCatId = Utils.CATEGORY + Utils.CATEGORY_SPACE + "spaces";
    cat = createCategory(spCatId);
    cat.setCategoryName("spaces");
    forumService_.saveCategory(cat, true);
    
    Forum forum = createdForum();
    forum.setId(Utils.FORUM + "space_test");
    forum.setForumName("Space test");
    forumService_.saveForum(spCatId, forum, true);
    
    Topic topic = createdTopic(USER_ROOT);
    topic.setTopicName("Topic X");
    forumService_.saveTopic(spCatId, forum.getId(), topic, true, false, new MessageBuilder());
    
    Topic topicA = createdTopic(USER_ROOT);
    topicA.setTopicName("Topic A");
    forumService_.saveTopic(spCatId, forum.getId(), topicA, true, false, new MessageBuilder());
    
    Topic topicB = createdTopic(USER_ROOT);
    topicB.setTopicName("Topic B");
    forumService_.saveTopic(spCatId, forum.getId(), topicB, true, false, new MessageBuilder());
    
    //
    Topic topicC = createdTopic(USER_ROOT);
    topicC.setTopicName("With Clone word");
    forumService_.saveTopic(spCatId, forum.getId(), topicC, true, false, new MessageBuilder());
    
    postG = createdPost();
    postG.setName("Space GMAN");
    postG.setMessage("This is the GMAN");
    forumService_.savePost(spCatId, forum.getId(), topic.getId(), postG, true, new MessageBuilder());
    
    
    Post postA1 = createdPost();
    postA1.setName("Reply A1");
    postA1.setMessage("This is the Reply A1");
    forumService_.savePost(spCatId, forum.getId(), topicA.getId(), postA1, true, new MessageBuilder());
    
    Post postA2 = createdPost();
    postA2.setName("Reply A2");
    postA2.setMessage("This is the Reply A2");
    forumService_.savePost(spCatId, forum.getId(), topicA.getId(), postA2, true, new MessageBuilder());
    
    Post postB1 = createdPost();
    postB1.setName("Reply B1");
    postB1.setMessage("This is the Reply B1");
    forumService_.savePost(spCatId, forum.getId(), topicB.getId(), postB1, true, new MessageBuilder());
    
    Post postB2 = createdPost();
    postB2.setName("Reply B2");
    postB2.setMessage("This is the Reply B2");
    forumService_.savePost(spCatId, forum.getId(), topicB.getId(), postB2, true, new MessageBuilder());

    forum = createdForum();
    forum.setForumName("Forum A");
    forumService_.saveForum(cateId, forum, true);

    topic = createdTopic(USER_ROOT);
    topic.setTopicName("Topic TEST");
    forumService_.saveTopic(cateId, forum.getId(), topic, true, false, new MessageBuilder());

    postA = new Post();
    postA.setName("Reply ABCDEF");
    postA.setMessage("This is the BCDE message");
    postA.setOwner("foo");
    forumService_.savePost(cateId, forum.getId(), topic.getId(), postA, true, new MessageBuilder());

    Post postB = new Post();
    postB.setName("Reply B");
    postB.setMessage("This is the B message");
    postB.setOwner("foo");
    forumService_.savePost(cateId, forum.getId(), topic.getId(), postB, true, new MessageBuilder());

    //
    InitParams params = new InitParams();
    params.put("constructor.params", new PropertiesParam());
    
    org.exoplatform.forum.service.DataStorage dataStorage = getService(org.exoplatform.forum.service.DataStorage.class);

    SpaceService spaceService = getService(SpaceService.class);

    discussionSearchConnector = new DiscussionSearchConnector(params, dataStorage, spaceService);

  }

  @Override
  public void tearDown() throws Exception {
    context = null;
    router = null;
    super.tearDown();
  }

  private void createGroupSite() throws Exception {
    try {
      String ownerId = "/spaces/space_test";
      NavigationService navigationService = getService(NavigationService.class);
      SiteKey key = SiteKey.group(ownerId);
      NavigationContext existing = navigationService.loadNavigation(key);
      if (existing == null) {
        UserPortalConfigService userPortalConfigSer = getService(UserPortalConfigService.class);
        userPortalConfigSer.createGroupSite(ownerId);

        navigationService.saveNavigation(new NavigationContext(key, new NavigationState(0)));
        existing = navigationService.loadNavigation(key);

        NodeContext<NodeContext<?>> parentNodeCtx = navigationService.loadNode(NodeModel.SELF_MODEL, existing, Scope.ALL, null);

        NodeContext<NodeContext<?>> childNodeCtx = parentNodeCtx.add(0, "forum");

        PageKey pageKey = PageKey.parse("group::" + ownerId + "::forum");

        childNodeCtx.setState(new NodeState.Builder().label("Forum").icon("forum").pageRef(pageKey).build());

        PageState pageState = new PageState("Forum", "Forum portet", false, "factoryId", 
                                            null , "*:" + ownerId, null, null);
        
        PageContext pageCt = new PageContext(pageKey, pageState);
        PageService pageService = getService(PageService.class);
        pageService.savePage(pageCt);

        DataStorage dataStorage = getService(DataStorage.class);
        Page page = new Page(pageKey.getSite().getType().getName(), ownerId, pageKey.getName());
        page.setDescription("");
        page.setEditPermission("*:" + ownerId);
        dataStorage.save(page);

        navigationService.saveNode(parentNodeCtx, null);
      }
    } catch (Exception e) {

    }
  }

  private void loadController() throws Exception {
    ClassLoader loader = getClass().getClassLoader();
    InputStream in = loader.getResourceAsStream(CONTROLLER_PATH);
    try {
      ControllerDescriptor routerDesc = new DescriptorBuilder().build(in);
      router = new Router(routerDesc);
      context = new SearchContext(router, "");
    } catch (RouterConfigException e) {
      log.warn(e);
    } finally {
      in.close();
    }
  }
  
  public void testFilter() throws Exception {
    assertEquals(1, discussionSearchConnector.search(context, "Reply~0.5", Collections.<String> emptyList(), 0, 1, "relevancy", "ASC").size());
    assertEquals(1, discussionSearchConnector.search(context, "A~0.5", Collections.<String> emptyList(), 0, 1, "relevancy", "ASC").size());
    assertEquals(2, discussionSearchConnector.search(context, "message~0.5", Collections.<String> emptyList(), 0, 2, "relevancy", "ASC").size());
    assertEquals(1, discussionSearchConnector.search(context, "B~0.5 message~0.5", Collections.<String> emptyList(), 0, 1, "relevancy", "ASC").size());
    //
    //assertEquals(1, discussionSearchConnector.search(context, "B * message~0.5", Collections.<String> emptyList(), 0, 1, "relevancy", "ASC").size());
    assertEquals(1, discussionSearchConnector.search(context, "%message~0.5", Collections.<String> emptyList(), 0, 1, "relevancy", "ASC").size());
    assertEquals(1, discussionSearchConnector.search(context, "message*~0.5", Collections.<String> emptyList(), 0, 1, "relevancy", "ASC").size());
    assertEquals(1, discussionSearchConnector.search(context, "%*message~0.5", Collections.<String> emptyList(), 0, 1, "relevancy", "ASC").size());
    assertEquals(1, discussionSearchConnector.search(context, "message*%~0.5", Collections.<String> emptyList(), 0, 1, "relevancy", "ASC").size());
    assertEquals(1, discussionSearchConnector.search(context, "*message%~0.5", Collections.<String> emptyList(), 0, 1, "relevancy", "ASC").size());

  }
  
  public void testPrivateTopic() throws Exception {
    loginUser(USER_ROOT);
    
    assertEquals(5, discussionSearchConnector.search(context, "Topic\\~", Collections.<String> emptyList(), 0, 10, "relevancy", "ASC").size());
    
    String spCatId = Utils.CATEGORY + Utils.CATEGORY_SPACE + "spaces";
    String forumId = Utils.FORUM + "space_test";
    Topic topic = createdTopic(USER_ROOT);
    topic.setTopicName("Topic 3");
    topic.setCanView(new String[] {USER_JOHN});
    topic.setCanPost(new String[] {USER_JOHN});
    forumService_.saveTopic(spCatId, forumId, topic, true, false, new MessageBuilder());
    topic = createdTopic(USER_ROOT);
    topic.setTopicName("Clone word 3");
    topic.setCanView(new String[] {USER_DEMO});
    topic.setCanPost(new String[] {USER_DEMO});
    forumService_.saveTopic(spCatId, forumId, topic, true, false, new MessageBuilder());
    
    assertEquals(7, discussionSearchConnector.search(context, "Topic~0.5", Collections.<String> emptyList(), 0, 10, "relevancy", "ASC").size());
    
    loginUser(USER_DEMO);
    assertEquals(2, discussionSearchConnector.search(context, "Topic~0.5", Collections.<String> emptyList(), 0, 2, "relevancy", "ASC").size());
    
    //load more
    assertEquals(4, discussionSearchConnector.search(context, "Topic~0.5", Collections.<String> emptyList(), 2, 5, "relevancy", "ASC").size());
    
    //test Unified Search with special characters
    assertEquals(5, discussionSearchConnector.search(context, "Top~0.5", Collections.<String> emptyList(), 0, 5, "relevancy", "ASC").size());
    assertEquals(2, discussionSearchConnector.search(context, "clo~0.5", Collections.<String> emptyList(), 0, 5, "relevancy", "ASC").size());
    
  }
  
  public void testFilterOrder() throws Exception {
    Collection<SearchResult> results =  discussionSearchConnector.search(context, "Reply~0.5", Collections.<String> emptyList(), 0, 5, "relevancy", "ASC");
    assertEquals(4, results.size());
    
    //
    SearchResult previous = null;
    for (SearchResult e : results) {
      if (previous == null) {
        previous = e;
      } else {
        assertTrue(e.getRelevancy() >= previous.getRelevancy());
        previous = null;
      }
    }
  }

  public void testSiteData() throws Exception {
    List<SearchResult> aResults = (List<SearchResult>) discussionSearchConnector.search(context, "bcde~0.5", Collections.<String> emptyList(), 0, 10, "relevancy", "ASC");
    SearchResult aResult = aResults.get(0);
    assertEquals("Reply ABCDEF", aResult.getTitle());
    assertTrue(aResult.getExcerpt().toLowerCase().indexOf("bcde") >= 0);
    assertTrue(aResult.getDate() > 0);
    assertEquals(postA.getCreatedDate().getTime(), aResult.getDate());
  }

  public void testGroupData() throws Exception {
    createGroupSite();
    List<SearchResult> aResults = (List<SearchResult>) discussionSearchConnector.search(context, "GMAN~0.5", Collections.<String> emptyList(), 0, 10, "relevancy", "ASC");
    SearchResult aResult = aResults.get(0);
    assertEquals(postG.getName(), aResult.getTitle());
    assertTrue(aResult.getExcerpt().toLowerCase().indexOf("gman") >= 0);
    String gotURL = aResult.getUrl();
    log.info(gotURL);
    log.info("testGroupData: "+gotURL);
//    assertTrue(gotURL.indexOf("/portal/classic/forum/topic/topic") >= 0);
    assertTrue(aResult.getDate() > 0);
    assertEquals(postG.getCreatedDate().getTime(), aResult.getDate());
  }

  public void testOrder() throws Exception {
    List<SearchResult> rTitleAsc = (List<SearchResult>) discussionSearchConnector.search(context, "Reply~0.5", Collections.<String> emptyList(), 0, 10, "title", "ASC");
    assertEquals("Reply A1", rTitleAsc.get(0).getTitle());
    assertEquals("Reply A2", rTitleAsc.get(1).getTitle());

    List<SearchResult> rTitleDesc = (List<SearchResult>) discussionSearchConnector.search(context, "Reply~0.5", Collections.<String> emptyList(), 0, 10, "title", "DESC");
    assertEquals("Reply B2", rTitleDesc.get(0).getTitle());
    assertEquals("Reply B1", rTitleDesc.get(1).getTitle());
    assertEquals("Reply A2", rTitleDesc.get(2).getTitle());

    List<SearchResult> rDateAsc = (List<SearchResult>) discussionSearchConnector.search(context, "Reply~0.5", Collections.<String> emptyList(), 0, 10, "date", "ASC");
    assertEquals("Reply A1", rDateAsc.get(0).getTitle());
    assertEquals("Reply A2", rDateAsc.get(1).getTitle());

    List<SearchResult> rDateDesc = (List<SearchResult>) discussionSearchConnector.search(context, "Reply~0.5", Collections.<String> emptyList(), 0, 10, "date", "DESC");
    assertEquals("Reply B2", rDateDesc.get(0).getTitle());
    assertEquals("Reply B1", rDateDesc.get(1).getTitle());
  }
  
  public void testJapaneseData() throws Exception {
    String cateId = getId(Utils.CATEGORY);
    Category cat = createCategory(cateId);
    cat.setCategoryName("cat1");
    forumService_.saveCategory(cat, true);
    
    Forum forum = createdForum();
    forum.setForumName("forum1");
    forumService_.saveForum(cateId, forum, true);

    Topic topic1 = createdTopic(USER_ROOT);
    topic1.setTopicName("広いニーズ");
    topic1.setDescription("広いニーズに応えます。");
    forumService_.saveTopic(cateId, forum.getId(), topic1, true, false, new MessageBuilder());
    
    Post post = createdPost();
    post.setName("Reply A1");
    post.setMessage("広いニーズ");
    forumService_.savePost(cateId, forum.getId(), topic1.getId(), post, true, new MessageBuilder());
    assertEquals(0, discussionSearchConnector.search(context, null, Collections.<String> emptyList(), 0, 5, "relevancy", "ASC").size());
    assertEquals(0, discussionSearchConnector.search(context, "", Collections.<String> emptyList(), 0, 5, "relevancy", "ASC").size());
    assertEquals(2, discussionSearchConnector.search(context, "広いニーズ", Collections.<String> emptyList(), 0, 5, "relevancy", "ASC").size());
    assertEquals(2, discussionSearchConnector.search(context, "広いニーズ\\~", Collections.<String> emptyList(), 0, 5, "relevancy", "ASC").size());
    assertEquals(2, discussionSearchConnector.search(context, "広いニーズ~0.5", Collections.<String> emptyList(), 0, 5, "relevancy", "ASC").size());
    
    forumService_.removeCategory(cateId);
  }

  public void testSearchDataWithDoubleQuotes() throws Exception {
    String cateId = getId(Utils.CATEGORY);
    Category cat = createCategory(cateId);
    cat.setCategoryName("My Topic Category");
    forumService_.saveCategory(cat, true);

    Forum forum = createdForum();
    forum.setForumName("forum1");
    forumService_.saveForum(cateId, forum, true);

    Topic topic1 = createdTopic(USER_ROOT);
    topic1.setTopicName("My Topic Abc");
    topic1.setDescription("My Topic Abc Description");
    forumService_.saveTopic(cateId, forum.getId(), topic1, true, false, new MessageBuilder());

    Topic topic2 = createdTopic(USER_ROOT);
    topic2.setTopicName("My Topic Second Abc");
    topic2.setDescription("My Topic Second Abc Description");
    forumService_.saveTopic(cateId, forum.getId(), topic2, true, false, new MessageBuilder());

    Post post1 = createdPost();
    post1.setName("Reply A1");
    post1.setMessage("My Reply Abc");
    forumService_.savePost(cateId, forum.getId(), topic1.getId(), post1, true, new MessageBuilder());
    
    Post post2 = createdPost();
    post2.setName("Reply A2");
    post2.setMessage("My Reply Abc second");
    forumService_.savePost(cateId, forum.getId(), topic2.getId(), post2, true, new MessageBuilder());
    // Test without double quotes
    assertEquals(0, discussionSearchConnector.search(context, null, Collections.<String>emptyList(), 0, 5, "relevancy", "ASC").size());
    assertEquals(0, discussionSearchConnector.search(context, "", Collections.<String>emptyList(), 0, 5, "relevancy", "ASC").size());
    assertEquals(2, discussionSearchConnector.search(context, "My Topic Abc", Collections.<String>emptyList(), 0, 5, "relevancy", "ASC").size());
    assertEquals(2, discussionSearchConnector.search(context, "My Reply Abc", Collections.<String>emptyList(), 0, 5, "relevancy", "ASC").size());
    // Test with double quotes for topic
    assertEquals(2, discussionSearchConnector.search(context, "\\\"My Topic\\\"", Collections.<String>emptyList(), 0, 5, "relevancy", "ASC").size());
    assertEquals(1, discussionSearchConnector.search(context, "\\\"My Topic Second\\\"", Collections.<String>emptyList(), 0, 5, "relevancy", "ASC").size());
    // Test with double quotes for reply
    assertEquals(2, discussionSearchConnector.search(context, "\\\"My Reply Abc\\\"", Collections.<String>emptyList(), 0, 5, "relevancy", "ASC").size());
    assertEquals(2, discussionSearchConnector.search(context, "\\\"Second\\\"", Collections.<String>emptyList(), 0, 5, "relevancy", "ASC").size());
    assertEquals(0, discussionSearchConnector.search(context, "\\\"My Abc\\\"", Collections.<String>emptyList(), 0, 5,"relevancy", "ASC").size());

    forumService_.removeCategory(cateId);
  }
  
  public void testSpecialCaseComplex() throws Exception {
    String cateId = getId(Utils.CATEGORY);
    Category cat = createCategory(cateId);
    cat.setCategoryName("cat1");
    forumService_.saveCategory(cat, true);
    
    Forum forum = createdForum();
    forum.setForumName("forum1");
    forumService_.saveForum(cateId, forum, true);

    Topic topic1 = createdTopic(USER_ROOT);
    topic1.setTopicName("reply 1");
    topic1.setDescription("reply 1");
    forumService_.saveTopic(cateId, forum.getId(), topic1, true, false, new MessageBuilder());
    
    Post post = createdPost();
    post.setName("Reply A1");
    post.setMessage("reply A1");
    forumService_.savePost(cateId, forum.getId(), topic1.getId(), post, true, new MessageBuilder());
    assertEquals(0, discussionSearchConnector.search(context, "\" ' ( ) \"" , Collections.<String> emptyList(), 0, 5, "relevancy", "ASC").size());
    
    forumService_.removeCategory(cateId);
  }
}
