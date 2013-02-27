package org.exoplatform.forum.service.search;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

import org.exoplatform.commons.api.search.SearchServiceConnector;
import org.exoplatform.commons.api.search.data.SearchContext;
import org.exoplatform.commons.api.search.data.SearchResult;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.forum.service.Forum;
import org.exoplatform.forum.service.ForumSearch;
import org.exoplatform.forum.service.Post;
import org.exoplatform.forum.service.Topic;
import org.exoplatform.forum.service.Utils;
import org.exoplatform.forum.service.impl.JCRDataStorage;
import org.exoplatform.portal.config.UserPortalConfig;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.exoplatform.portal.mop.navigation.NavigationService;
import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.navigation.NodeModel;
import org.exoplatform.portal.mop.navigation.Scope;
import org.exoplatform.portal.mop.user.UserNavigation;
import org.exoplatform.portal.mop.user.UserPortalContext;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;


/**
 * @author <a href="mailto:alain.defrance@exoplatform.com">Alain Defrance</a>
 */
public class DiscussionSearchConnector extends SearchServiceConnector {

  private Pattern pattern;
  private JCRDataStorage storage;
  private static final Log LOG = ExoLogger.getLogger(DiscussionSearchConnector.class);
  
  public static final String  SPACES_GROUP           = "spaces";

  public static final String  CATEGORY               = "category";

  private static final String FORUM_PAGE_NAGVIGATION = "forum";

  private static final String FORUM_PORTLET_NAME     = "ForumPortlet";


  public DiscussionSearchConnector(InitParams initParams, JCRDataStorage storage) {
    super(initParams);
    this.storage = storage;
    this.pattern = Pattern.compile("/");
  }
  
  @Override
  public Collection<SearchResult> search(SearchContext context, String query, Collection<String> sites, int offset, int limit, String sort, String order) {
    String siteName = (sites == null || sites.isEmpty()) ? "" : sites.iterator().next();
    
    ExoContainerContext eXoContext = (ExoContainerContext)ExoContainerContext.getCurrentContainer()
        .getComponentInstanceOfType(ExoContainerContext.class);
    String portalName = eXoContext.getPortalContainerName();

    List<SearchResult> results = new ArrayList<SearchResult>();
    String currentUser = getCurrentUserName();
    try {
      List<ForumSearch> searchResults = storage.getQuickSearch(query, "false,post", null, currentUser, Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST, offset, limit, sort, order);
      for (ForumSearch searchResult : searchResults) {
        PostId id = new PostId(pattern, searchResult.getPath());
        Forum forum = storage.getForum(id.getCategoryId(), id.getForumId());
        Topic topic = storage.getTopic(id.getCategoryId(), id.getForumId(), id.getTopicId(), currentUser);
        Post post = storage.getPost(id.getCategoryId(), id.getForumId(), id.getTopicId(), id.getPostId());
        StringBuilder sb = new StringBuilder();
        sb.append(forum.getForumName());
        sb.append(" - " + topic.getPostCount() + " replies");
        sb.append(" - " + topic.getVoteRating());
        sb.append(" - " + post.getCreatedDate());
        String uri = buildLink(context, portalName, id.getCategoryId(), id.getForumId(), id.getTopicId(), siteName, topic.getLink());
        SearchResult result = new SearchResult(
            uri,
            post.getName(),
            post.getMessage(),
            sb.toString(),
            topic.getIcon(),
            post.getCreatedDate().getTime(),
            0);
        results.add(result);
      }

    } catch (Exception e) {
      LOG.error(e);
    }

    return results;
  }
  
  /**
   * Forum build link
   * @param portalName 
   * @param categoryId
   * @param forumId
   * @param topicId
   * @param defaultLink TODO
   * @param router
   * @return
   */
  private String buildLink(SearchContext context, String portalName, String categoryId, String forumId, String topicId, String siteName, String defaultLink) {
    try {
      String forumURI = null;
      
      //
      if (categoryId.indexOf(SPACES_GROUP) > 0) {
       forumURI = makeURIForSpaceContext(context, portalName, forumId, siteName);
       forumURI = URLDecoder.decode(forumURI, "UTF-8");
      } else {
        forumURI = makeURIForPortalContext(context, portalName, siteName);
      }
      if (!CommonUtils.isEmpty(forumURI)) {
        return String.format("%s/%s/%s", forumURI, Utils.TOPIC, topicId);
      } else {
        return defaultLink;
      }
    } catch (Exception ex) {
      return CommonUtils.EMPTY_STR;
    }
  }
  
  /**
   * builds URI under Portal site context
   * @param context
   * @param portalName
   * @param siteName
   * @return
   * @throws Exception
   */
  private String makeURIForPortalContext(SearchContext context,
                                        String portalName,
                                        String siteName) throws Exception {
    
    //
    String path = "";
    String siteType = "";
    
    UserPortalConfig prc = getUserPortalConfig();
    
    //
    
    siteName = CommonUtils.isEmpty(siteName) ? prc.getPortalConfig().getName() : siteName;
    SiteKey siteKey = SiteKey.portal(siteName);
    
    //
    siteType = SiteType.PORTAL.getName();
    
    //
    path = getSiteName(siteKey);
    
    //
    if (Utils.isEmpty(path)) {
      return CommonUtils.EMPTY_STR;
    }
    
    String forumURI = context.handler(portalName)
        .lang("")
        .siteName(siteName)
        .siteType(siteType)
        .path(path)
        .renderLink();
    return String.format("/%s%s", portalName, forumURI);
  }
  
  /**
   * builds URI for Forum what is inside Space context
   * 
   * @param context
   * @param portalName
   * @param forumId
   * @param siteName
   * @return
   * @throws Exception
   */
  private String makeURIForSpaceContext(SearchContext context,
                                        String portalName,
                                        String forumId,
                                        String siteName) throws Exception {
    //
    String path = "";
    String siteType = "";
    
    String prefixId = Utils.FORUM_SPACE_ID_PREFIX;
    String groupId = forumId.replaceFirst(prefixId, "");
    String spaceGroupId = String.format("/%s/%s", SPACES_GROUP, groupId);// /spaces/space1
    SiteKey siteKey = SiteKey.group(spaceGroupId);
    //
    siteName = spaceGroupId.replaceAll("/", ":");
    
    //
    siteType = SiteType.GROUP.getName();
    
    //
    String forumNavName = getSiteName(siteKey);
    //
    if (Utils.isEmpty(forumNavName)) {
      return CommonUtils.EMPTY_STR;
    }
    path = groupId + "/" + forumNavName;
    
    String forumURI = context.handler(portalName)
        .lang("")
        .siteName(siteName)
        .siteType(siteType)
        .path(path)
        .renderLink();
    return String.format("/%s%s", portalName, forumURI);
  }
  
  
  /**
   * Get user portal config.
   * 
   * @return
   * @throws Exception
   * @since 4.0.0
   */
  private UserPortalConfig getUserPortalConfig() throws Exception {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    UserPortalConfigService userPortalConfigSer = (UserPortalConfigService)
                                                  container.getComponentInstanceOfType(UserPortalConfigService.class);

    UserPortalContext NULL_CONTEXT = new UserPortalContext() {
      public ResourceBundle getBundle(UserNavigation navigation) {
        return null;
      }

      public Locale getUserLocale() {
        return Locale.ENGLISH;
      }
    };
    
    String remoteId = getCurrentUserName();
    UserPortalConfig userPortalCfg = userPortalConfigSer.
                                     getUserPortalConfig(userPortalConfigSer.getDefaultPortal(), remoteId, NULL_CONTEXT);
    return userPortalCfg;
  }

  /**
   * 
   * @param siteKey
   * @return
   */
  private String getSiteName(SiteKey siteKey) {
    try {
      ExoContainer container = ExoContainerContext.getCurrentContainer();
      NavigationService navService = (NavigationService) container.getComponentInstance(NavigationService.class);
      NavigationContext nav = navService.loadNavigation(siteKey);
      NodeContext<NodeContext<?>> parentNodeCtx = navService.loadNode(NodeModel.SELF_MODEL, nav, Scope.ALL, null);

      Collection<NodeContext<?>> children = parentNodeCtx.getNodes();
      if (siteKey.getType() == SiteType.GROUP) {
        children = parentNodeCtx.get(0).getNodes();
      }
      Iterator<NodeContext<?>> it = children.iterator();
      
      NodeContext<?> child = null;
      while (it.hasNext()) {
        child = it.next();
        if (FORUM_PAGE_NAGVIGATION.equals(child.getName()) || child.getName().indexOf(FORUM_PORTLET_NAME) >= 0) {
          return child.getName();
        }
      }
      return CommonUtils.EMPTY_STR;
    } catch (Exception e) {
      return CommonUtils.EMPTY_STR;
    }
  }
  
  /**
   * 
   * @return
   */
  private String getCurrentUserName() {
    return ConversationState.getCurrent().getIdentity().getUserId();
  }

}
