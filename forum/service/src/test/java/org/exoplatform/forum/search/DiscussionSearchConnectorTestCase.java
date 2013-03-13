package org.exoplatform.forum.search;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import org.exoplatform.commons.api.search.data.SearchContext;
import org.exoplatform.commons.api.search.data.SearchResult;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.forum.base.BaseForumServiceTestCase;
import org.exoplatform.forum.service.Category;
import org.exoplatform.forum.service.Forum;
import org.exoplatform.forum.service.MessageBuilder;
import org.exoplatform.forum.service.Post;
import org.exoplatform.forum.service.Topic;
import org.exoplatform.forum.service.Utils;
import org.exoplatform.forum.service.impl.JCRDataStorage;
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
import org.exoplatform.web.controller.metadata.ControllerDescriptor;
import org.exoplatform.web.controller.metadata.DescriptorBuilder;
import org.exoplatform.web.controller.router.Router;
import org.exoplatform.web.controller.router.RouterConfigException;

/**
 * @author <a href="mailto:alain.defrance@exoplatform.com">Alain Defrance</a>
 */

public class DiscussionSearchConnectorTestCase extends BaseForumServiceTestCase {
  private final static String CONTROLLER_PATH = "conf/standalone/controller.xml";

  private DiscussionSearchConnector discussionSearchConnector;
  private Post postA;
  private Post postG;
  private SearchContext context;
  private Router router;

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
    
    postG = createdPost();
    postG.setName("Space G");
    postG.setMessage("This is the G");
    forumService_.savePost(spCatId, forum.getId(), topic.getId(), postG, true, new MessageBuilder());
    

    forum = createdForum();
    forum.setForumName("Forum A");
    forumService_.saveForum(cateId, forum, true);

    topic = createdTopic(USER_ROOT);
    topic.setTopicName("Topic TEST");
    forumService_.saveTopic(cateId, forum.getId(), topic, true, false, new MessageBuilder());

    postA = new Post();
    postA.setName("Post A");
    postA.setMessage("This is the A message");
    postA.setOwner("foo");
    forumService_.savePost(cateId, forum.getId(), topic.getId(), postA, true, new MessageBuilder());

    Post postB = new Post();
    postB.setName("Post B");
    postB.setMessage("This is the B message");
    postB.setOwner("foo");
    forumService_.savePost(cateId, forum.getId(), topic.getId(), postB, true, new MessageBuilder());

    //
    InitParams params = new InitParams();
    params.put("constructor.params", new PropertiesParam());
    
    JCRDataStorage dataStorage = (JCRDataStorage) getService(JCRDataStorage.class);
    
    discussionSearchConnector = new DiscussionSearchConnector(params, dataStorage);

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

        PageState pageState = new PageState("Forum", "Forum portet", false, null, null, "*:" + ownerId);
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
      log.info(e.getMessage());
    } finally {
      in.close();
    }
  }
  
  public void testFilter() throws Exception {
    assertEquals(2, discussionSearchConnector.search(context, "Post", Collections.EMPTY_LIST, 0, 0, "relevancy", "ASC").size());
    assertEquals(1, discussionSearchConnector.search(context, "A", Collections.EMPTY_LIST, 0, 0, "relevancy", "ASC").size());
    assertEquals(2, discussionSearchConnector.search(context, "message", Collections.EMPTY_LIST, 0, 0, "relevancy", "ASC").size());
    assertEquals(1, discussionSearchConnector.search(context, "B message", Collections.EMPTY_LIST, 0, 0, "relevancy", "ASC").size());

  }

  public void testSiteData() throws Exception {
    List<SearchResult> aResults = (List<SearchResult>) discussionSearchConnector.search(context, "A", Collections.EMPTY_LIST, 0, 10, "relevancy", "ASC");
    SearchResult aResult = aResults.get(0);
    assertEquals("Post A", aResult.getTitle());
    assertTrue(aResult.getExcerpt().indexOf("<strong>A</strong>") >= 0);
    String gotURL = aResult.getUrl();
    assertTrue(gotURL.indexOf("/portal/classic/forum/topic/topic") >= 0);
    assertTrue(aResult.getDate() > 0);
    assertEquals(postA.getCreatedDate().getTime(), aResult.getDate());
  }

  public void testGroupData() throws Exception {
    createGroupSite();
    List<SearchResult> aResults = (List<SearchResult>) discussionSearchConnector.search(context, "G", Collections.EMPTY_LIST, 0, 10, "relevancy", "ASC");
    SearchResult aResult = aResults.get(0);
    assertEquals(postG.getName(), aResult.getTitle());
    assertTrue(aResult.getExcerpt().indexOf("<strong>G</strong>") >= 0);
    String gotURL = aResult.getUrl();
    log.info(gotURL);
//    assertTrue(gotURL.indexOf("/portal/classic/forum/topic/topic") >= 0);
    assertTrue(aResult.getDate() > 0);
    assertEquals(postG.getCreatedDate().getTime(), aResult.getDate());
  }

  public void testOrder() throws Exception {
    List<SearchResult> rTitleAsc = (List<SearchResult>) discussionSearchConnector.search(context, "Post", Collections.EMPTY_LIST, 0, 10, "title", "ASC");
    assertEquals("Post A", rTitleAsc.get(0).getTitle());
    assertEquals("Post B", rTitleAsc.get(1).getTitle());

    List<SearchResult> rTitleDesc = (List<SearchResult>) discussionSearchConnector.search(context, "Post", Collections.EMPTY_LIST, 0, 10, "title", "DESC");
    assertEquals("Post B", rTitleDesc.get(0).getTitle());
    assertEquals("Post A", rTitleDesc.get(1).getTitle());

    List<SearchResult> rDateAsc = (List<SearchResult>) discussionSearchConnector.search(context, "Post", Collections.EMPTY_LIST, 0, 10, "date", "ASC");
    assertEquals("Post A", rDateAsc.get(0).getTitle());
    assertEquals("Post B", rDateAsc.get(1).getTitle());

    List<SearchResult> rDateDesc = (List<SearchResult>) discussionSearchConnector.search(context, "Post", Collections.EMPTY_LIST, 0, 10, "date", "DESC");
    assertEquals("Post B", rDateDesc.get(0).getTitle());
    assertEquals("Post A", rDateDesc.get(1).getTitle());
  }

}
