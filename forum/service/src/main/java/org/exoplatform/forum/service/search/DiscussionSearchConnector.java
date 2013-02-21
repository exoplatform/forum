package org.exoplatform.forum.service.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.exoplatform.commons.api.search.SearchServiceConnector;
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
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.exoplatform.portal.mop.navigation.NavigationService;
import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.navigation.NodeModel;
import org.exoplatform.portal.mop.navigation.Scope;
import org.exoplatform.portal.mop.user.UserNavigation;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.portal.mop.user.UserPortal;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.web.url.navigation.NavigationResource;
import org.exoplatform.web.url.navigation.NodeURL;

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
            buildLink(id.getCategoryId(), id.getForumId(), id.getTopicId()),
            post.getName(),
            post.getMessage(),
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
  
  /**
   * 
   * @param categoryId
   * @param forumId
   * @param topicId
   * @return
   */
  public static String buildLink(String categoryId, String forumId, String topicId) {
    return buildLink(categoryId, forumId, topicId, null);
  }

  /**
   * 
   * @param categoryId
   * @param forumId
   * @param topicId
   * @return
   */
  public static String buildLink(String categoryId, String forumId, String topicId, String siteName) {
    try {
      String link = "";

      String objectType = getType(categoryId, forumId, topicId);
      String objectId = getObjectId(categoryId, forumId, topicId);
      //
      if (categoryId.indexOf(SPACES_GROUP) > 0 && !objectType.equals(CATEGORY)) {
        String prefixId = Utils.FORUM_SPACE_ID_PREFIX;
        String spaceGroupId = "/".concat(SPACES_GROUP).concat("/").concat(forumId.replaceFirst(prefixId, ""));
        link = buildSpaceLink(spaceGroupId, objectType, objectId);
      } else {
        PortalRequestContext prc = Util.getPortalRequestContext();

        if (!CommonUtils.isEmpty(siteName) && !prc.getSiteKey().getName().equals(siteName)) {
          SiteKey siteKey = SiteKey.portal(siteName);

          String nodeURI = getSiteName(siteKey);

          //
          if (!CommonUtils.isEmpty(nodeURI)) {
            String siteHomeLink = getSiteHomeURL(siteName, nodeURI);
            link = String.format("%s/%s/%s", siteHomeLink, objectType, objectId);
          }
        } else {
          UserPortal userPortal = prc.getUserPortal();
          UserNavigation userNav = userPortal.getNavigation(prc.getSiteKey());
          UserNode userNode = userPortal.getNode(userNav, Scope.ALL, null, null);

          //
          UserNode forumNode = userNode.getChild(FORUM_PAGE_NAGVIGATION);
          if (forumNode != null) {
            String forumURI = getNodeURL(forumNode);
            link = String.format("%s/%s/%s", forumURI, objectType, objectId);
          }
        }
      }

      //
      return link;
    } catch (Exception ex) {
      return "";
    }
  }

  private static String getNodeURL(UserNode node) {
    RequestContext ctx = RequestContext.getCurrentInstance();
    NodeURL nodeURL = ctx.createURL(NodeURL.TYPE);
    return nodeURL.setNode(node).toString();
  }

  /**
   * 
   * @param spaceGroupId
   * @param objectType
   * @param objectId
   * @return
   * @throws Exception
   */
  private static String buildSpaceLink(String spaceGroupId, String objectType, String objectId) throws Exception {

    String nodeURI = getSiteName(SiteKey.group(spaceGroupId));
    
    if (!CommonUtils.isEmpty(nodeURI)) {
      String spaceLink = getSpaceHomeURL(spaceGroupId);
      String objectLink = String.format("%s/%s/%s/%s", spaceLink, nodeURI, objectType, objectId);
      return objectLink;
    }

    return CommonUtils.EMPTY_STR;
  }

  private static String getSiteName(SiteKey siteKey) {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    NavigationService navService = (NavigationService) container.getComponentInstance(NavigationService.class);
    NavigationContext nav = navService.loadNavigation(siteKey);
    NodeContext<NodeContext<?>> parentNodeCtx = navService.loadNode(NodeModel.SELF_MODEL, nav, Scope.ALL, null);

    if (parentNodeCtx.getSize() >= 1) {
      Collection<NodeContext<?>> children = parentNodeCtx.getNodes();
      if (siteKey.getType() == SiteType.GROUP) {
        children = parentNodeCtx.get(0).getNodes();
      }
      Iterator<NodeContext<?>> it = children.iterator();

      NodeContext<?> child = null;
      while (it.hasNext()) {
        child = it.next();
        if (FORUM_PAGE_NAGVIGATION.equals(child.getName()) || child.getName().indexOf(FORUM_PORTLET_NAME) >= 0) {
          break;
        }
      }
      return child.getName();
    }
    return CommonUtils.EMPTY_STR;
  }
  
  /**
   * 
   * @param portalName
   * @param nodeURI
   * @return
   */
  private static String getSiteHomeURL(String portalName, String nodeURI) {

    NodeURL nodeURL = RequestContext.getCurrentInstance().createURL(NodeURL.TYPE);
    NavigationResource resource = new NavigationResource(SiteType.PORTAL, portalName, nodeURI);

    return nodeURL.setResource(resource).toString();
  }
  
  /**
   * Gets the space home url of a space.
   * 
   * @param spaceGroupId
   * @return
   * @since 4.0
   */
  private static String getSpaceHomeURL(String spaceGroupId) {
    String permanentSpaceName = spaceGroupId.split("/")[2];

    NodeURL nodeURL = RequestContext.getCurrentInstance().createURL(NodeURL.TYPE);
    NavigationResource resource = new NavigationResource(SiteType.GROUP, spaceGroupId, permanentSpaceName);

    return nodeURL.setResource(resource).toString();
  }
  private static String getType(String categoryId, String forumId, String topicId) {
    if(!CommonUtils.isEmpty(topicId)) return Utils.TOPIC;
    if(!CommonUtils.isEmpty(forumId)) return Utils.FORUM;
    if(!CommonUtils.isEmpty(categoryId)) return CATEGORY;
    return "";
  }

  private static String getObjectId(String categoryId, String forumId, String topicId) {
    if(!CommonUtils.isEmpty(topicId)) return topicId;
    if(!CommonUtils.isEmpty(forumId)) return forumId;
    if(!CommonUtils.isEmpty(categoryId)) return categoryId;
    return "";
  }
  
  public String getCurrentUserName() {
    return ConversationState.getCurrent().getIdentity().getUserId();
  }

}
