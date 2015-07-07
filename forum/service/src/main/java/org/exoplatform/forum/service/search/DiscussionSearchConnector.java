package org.exoplatform.forum.service.search;

import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.commons.lang.StringEscapeUtils;
import org.exoplatform.commons.api.search.SearchServiceConnector;
import org.exoplatform.commons.api.search.data.SearchContext;
import org.exoplatform.commons.api.search.data.SearchResult;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.forum.service.DataStorage;
import org.exoplatform.forum.service.Forum;
import org.exoplatform.forum.service.ForumSearchResult;
import org.exoplatform.forum.service.Topic;
import org.exoplatform.forum.service.Utils;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.exoplatform.portal.mop.navigation.NavigationService;
import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.navigation.NodeModel;
import org.exoplatform.portal.mop.navigation.Scope;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.security.ConversationState;


/**
 * @author <a href="mailto:alain.defrance@exoplatform.com">Alain Defrance</a>
 */
public class DiscussionSearchConnector extends SearchServiceConnector {
  private static final Log LOG = ExoLogger.getLogger(DiscussionSearchConnector.class);

  private DataStorage storage;
                              //"/forum/skin/DefaultSkin/webui/skinIcons/48x48/defaultTopic.png";
  private String FIX_ICON = "/eXoSkin/skin/images/themes/default/Icons/AppIcons/uiIconAppDefault.png";
  
  public static final String  SPACES_GROUP           = "spaces";

  public static final String  CATEGORY               = "category";

  private static final String FORUM_PAGE_NAGVIGATION = "forum";

  private static final String FORUM_PORTLET_NAME     = "ForumPortlet";

  private static final String FORMAT_DATE           = "EEEEE, MMMMMMMM d, yyyy K:mm a";

  protected String language;

  public DiscussionSearchConnector(InitParams initParams, DataStorage storage) {
    super(initParams);
    this.storage = storage;
  }
  
  @Override
  public Collection<SearchResult> search(SearchContext context, String query, Collection<String> sites, int offset, int limit, String sort, String order, String language) {
    List<SearchResult> results = new ArrayList<SearchResult>();
    if (CommonUtils.isEmpty(query)) {
      return results;
    }
    setLanguage(language);
    ExoContainerContext eXoContext = (ExoContainerContext)ExoContainerContext.getCurrentContainer()
        .getComponentInstanceOfType(ExoContainerContext.class);
    String portalName = eXoContext.getPortalContainerName();
    String currentUser = getCurrentUserName();
    ResourceBundle resourceBundle = null;
    ResourceBundleService resourceBundleService = (ResourceBundleService) ExoContainerContext.getCurrentContainer().getComponentInstance(ResourceBundleService.class);
    if(resourceBundleService != null) {
      String[] sharedResourceBundleNames = resourceBundleService.getSharedResourceBundleNames();
      for (String resourceBundleName : sharedResourceBundleNames) {
        if (resourceBundleName.equals("locale.forum.discussion")) {
          resourceBundle = resourceBundleService.getResourceBundle(resourceBundleName, Locale.forLanguageTag(getLanguage()));
          break;
        }
      }
    }
    try {
      List<ForumSearchResult> searchResults = storage.getUnifiedSearch(query, currentUser, offset, limit, sort, order);
      for (ForumSearchResult searchResult : searchResults) {
        PostId id = new PostId(searchResult.getPath());
        Forum forum = storage.getForum(id.getCategoryId(), id.getForumId());
        Topic topic = storage.getTopicByPath(id.getTopicPath(), false);
        StringBuilder sb = new StringBuilder();
        sb.append(forum.getForumName());
        sb.append(" - " + topic.getPostCount() + " " + (resourceBundle != null ? resourceBundle.getString("DiscussionSearchConnector.reply") : "replies"));
        //sb.append(" - " + topic.getVoteRating());
        SimpleDateFormat sdf = new SimpleDateFormat(FORMAT_DATE, Locale.forLanguageTag(getLanguage()));
        sb.append(" - " + sdf.format(searchResult.getCreatedDate()));        
        String uri = buildLink(context, portalName, id.getCategoryId(), id.getForumId(), id.getTopicId(), topic.getLink());
        String postUri = (searchResult.getType().equals(Utils.POST) ? CommonUtils.SLASH + searchResult.getId() : CommonUtils.EMPTY_STR); 
        UnifiedSearchResult result = new UnifiedSearchResult(
            uri + postUri,
            searchResult.getName(),
            StringEscapeUtils.unescapeHtml(searchResult.getExcerpt()),
            sb.toString(),
            FIX_ICON,
            searchResult.getCreatedDate().getTime(),
            searchResult.getRelevancy(),
            topic.getVoteRating());
        results.add(result);
      }
    } catch (Exception e) {
      LOG.error("Failed in searching.", e);
    }

    return results;
  }
  
  /**
   * Forum build link
   * @param portalName 
   * @param categoryId
   * @param forumId
   * @param topicId
   * @param defaultLink
   * @param router
   * @return
   */
  private String buildLink(SearchContext context, String portalName, String categoryId, String forumId, String topicId, String defaultLink) {
    try {
      String forumURI = null;
      
      //
      if (categoryId.indexOf(SPACES_GROUP) > 0) {
       forumURI = makeURIForSpaceContext(context, portalName, forumId);
       forumURI = URLDecoder.decode(forumURI, "UTF-8");
      } else {
        forumURI = makeURIForPortalContext(context, portalName);
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
                                        String portalName) throws Exception {
    
    //
    String path = "";
    String siteType = "";
    String siteName = "";
    
    List<String> allSites = getAllPortalSites();
    
    //
    for(String siteName_ : allSites) {
      SiteKey siteKey = SiteKey.portal(siteName_);
      siteType = SiteType.PORTAL.getName();
      path = getSiteName(siteKey);

      if(!CommonUtils.isEmpty(path)) {
        siteName = siteKey.getName();
        break;
      }
    }

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
                                        String forumId) throws Exception {
    //
    String path = "";
    String siteType = "";
    
    String prefixId = Utils.FORUM_SPACE_ID_PREFIX;
    String groupId = forumId.replaceFirst(prefixId, "");
    String spaceGroupId = String.format("/%s/%s", SPACES_GROUP, groupId);// /spaces/space1
    SiteKey siteKey = SiteKey.group(spaceGroupId);
    //
    String siteName = spaceGroupId.replaceAll(CommonUtils.SLASH, CommonUtils.COLON);
    
    //
    siteType = SiteType.GROUP.getName();
    
    //
    String forumNavName = getSiteName(siteKey);
    //
    if (Utils.isEmpty(forumNavName)) {
      return CommonUtils.EMPTY_STR;
    }
    path = groupId + CommonUtils.SLASH + forumNavName;
    
    String forumURI = context.handler(portalName)
        .lang("")
        .siteName(siteName)
        .siteType(siteType)
        .path(path)
        .renderLink();
    return String.format("/%s%s", portalName, forumURI);
  }
  
  
  /**
   * @return
   */
  private List<String> getAllPortalSites() {
    UserPortalConfigService dataStorage = (UserPortalConfigService) ExoContainerContext.getCurrentContainer()
                                                                                       .getComponentInstanceOfType(UserPortalConfigService.class);
    try {
      return dataStorage.getAllPortalNames();
    } catch (Exception e) {
      return new ArrayList<String>();
    }
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

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }
}
