package org.exoplatform.faq.service.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.exoplatform.commons.api.search.SearchServiceConnector;
import org.exoplatform.commons.api.search.data.SearchContext;
import org.exoplatform.commons.api.search.data.SearchResult;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.faq.service.Category;
import org.exoplatform.faq.service.DataStorage;
import org.exoplatform.faq.service.FAQEventQuery;
import org.exoplatform.faq.service.ObjectSearchResult;
import org.exoplatform.faq.service.Question;
import org.exoplatform.faq.service.Utils;
import org.exoplatform.faq.service.impl.JCRDataStorage;
import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.forum.common.UserHelper;
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
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.web.url.navigation.NavigationResource;
import org.exoplatform.web.url.navigation.NodeURL;

/**
 * Created by The eXo Platform SAS
 * Author : Vu Duy Tu
 *          tuvd@exoplatform.com
 * Feb 21, 2013  
 */

public class AnswerSearchConnector extends SearchServiceConnector {
  
  public static final String  SPACES_GROUP           = "spaces";

  private static final String ANSWER_PORTLET_NAME     = "AnswerPortlet";

  private static final String ANSWER_PAGE_NAGVIGATION = "answer";

  private JCRDataStorage storage;
  private LocaleConfigService localeConfigService;
  private static final Log LOG = ExoLogger.getLogger(AnswerSearchConnector.class);


  public AnswerSearchConnector(InitParams initParams, DataStorage dataStorage, LocaleConfigService localeConfigService) throws Exception {
    super(initParams);
    this.storage = (JCRDataStorage) dataStorage;
    this.localeConfigService = localeConfigService;
  }

  public AnswerSearchConnector(InitParams initParams, DataStorage dataStorage) throws Exception {
    this(initParams, dataStorage, null);
  }

  @Override
  public Collection<SearchResult> search(SearchContext context, String query, Collection<String> sites, int offset, int limit, String sort, String order) {

    List<SearchResult> results = new ArrayList<SearchResult>();

    FAQEventQuery eventQuery = new FAQEventQuery();
    eventQuery.setType(FAQEventQuery.FAQ_QUESTION);
    eventQuery.setAdmin(false);
    eventQuery.setComment(query);
    eventQuery.setQuestion(query);
    eventQuery.setResponse(query);
    
    eventQuery.setUserId(UserHelper.getCurrentUser());
    eventQuery.setUserMembers(UserHelper.getAllGroupAndMembershipOfUser(null));
    eventQuery.setLanguageLevelSearch(false);
    
    String language = "English";
    if (localeConfigService != null) {
      language = localeConfigService.getDefaultLocaleConfig().getLocale().getDisplayLanguage();
    }
    eventQuery.setLanguage(language);
    eventQuery.setSearchOnDefaultLanguage(true);

    eventQuery.setOffset(offset);
    eventQuery.setLimit(limit);
    eventQuery.setSort(sort);
    eventQuery.setOrder(order);
    
    
    try {
      List<ObjectSearchResult> searchResults = storage.getUnifiedSearchResults(eventQuery);
      for (ObjectSearchResult searchResult : searchResults) {
        StringBuilder sb = new StringBuilder();
        sb.append(searchResult.getName());
        sb.append(" - ").append(searchResult.getNumberOfAnswer()).append(" answers");
        sb.append(" - ").append(searchResult.getNumberOfComment()).append(" comments");
        sb.append(" - ").append(searchResult.getRatingOfQuestion());
        sb.append(" - ").append(searchResult.getCreatedDate());
        SearchResult result = new SearchResult(
            //TODO: Wait for unified search context to build link.
            "", //buildLink(searchResult.getPath()),
            searchResult.getName(),
            searchResult.getDescription(),
            sb.toString(),
            searchResult.getIcon(),
            searchResult.getCreatedDate().getTime(),
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
   * @param questionPath
   * @return
   */
  public static String buildLink(String questionPath) {
    return buildLink(questionPath, null);
  }

  /**
   * @param questionPath
   * @param siteName
   * @return
   */
  public static String buildLink(String questionPath, String siteName) {
    try {
      String link = "";
      String categoryId = getCategoryId(questionPath);
      String questionId = questionPath.substring(questionPath.indexOf(Question.QUESTION_ID));
      //
      if (categoryId.indexOf(Utils.CATE_SPACE_ID_PREFIX) == 0) {
        String spaceGroupId = String.format("%s/%s", SPACES_GROUP, categoryId.replace(Utils.CATE_SPACE_ID_PREFIX, CommonUtils.EMPTY_STR));
        link = buildSpaceLink(spaceGroupId, questionId);
      } else {
        PortalRequestContext prc = Util.getPortalRequestContext();

        if (!CommonUtils.isEmpty(siteName) && !prc.getSiteKey().getName().equals(siteName)) {
          SiteKey siteKey = SiteKey.portal(siteName);

          String nodeURI = getSiteName(siteKey);

          //
          if (!CommonUtils.isEmpty(nodeURI)) {
            String siteHomeLink = getSiteHomeURL(siteName, nodeURI);
            link = String.format("%s/%s%s", siteHomeLink, Utils.QUESTION_ID, questionId);
          }
        } else {
          UserPortal userPortal = prc.getUserPortal();
          UserNavigation userNav = userPortal.getNavigation(prc.getSiteKey());
          UserNode userNode = userPortal.getNode(userNav, Scope.ALL, null, null);

          //
          UserNode forumNode = userNode.getChild(ANSWER_PAGE_NAGVIGATION);
          if (forumNode != null) {
            String forumURI = getNodeURL(forumNode);
            link = String.format("%s/%s%s", forumURI,Utils.QUESTION_ID, questionId);
          }
        }
      }

      //
      return link;
    } catch (Exception ex) {
      return CommonUtils.EMPTY_STR;
    }
  }

  private static String getCategoryId(String questionPath) {
    String categoryId = Utils.CATEGORY_HOME;
    int i = questionPath.indexOf(Category.CATEGORY_ID);
    if (i > 0) {
      categoryId = questionPath.substring(i, questionPath.indexOf("/", i));
    }
    return categoryId;
  }
  
  private static String getNodeURL(UserNode node) {
    RequestContext ctx = RequestContext.getCurrentInstance();
    NodeURL nodeURL = ctx.createURL(NodeURL.TYPE);
    return nodeURL.setNode(node).toString();
  }

  /**
   * @param spaceGroupId
   * @param questionId
   * @return
   * @throws Exception
   */
  private static String buildSpaceLink(String spaceGroupId, String questionId) throws Exception {

    String nodeURI = getSiteName(SiteKey.group(spaceGroupId));
    
    if (!CommonUtils.isEmpty(nodeURI)) {
      String spaceLink = getSpaceHomeURL(spaceGroupId);
      String objectLink = String.format("%s/%s/%s%s", spaceLink, nodeURI, Utils.QUESTION_ID, questionId);
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
        if (ANSWER_PAGE_NAGVIGATION.equals(child.getName()) || child.getName().indexOf(ANSWER_PORTLET_NAME) >= 0) {
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
}
