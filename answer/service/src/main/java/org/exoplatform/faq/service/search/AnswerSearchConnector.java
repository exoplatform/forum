package org.exoplatform.faq.service.search;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
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
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.security.ConversationState;

public class AnswerSearchConnector extends SearchServiceConnector {
  
  public static final String  SPACES_GROUP           = "spaces";

  private static final String ANSWER_PORTLET_NAME     = "AnswerPortlet";

  private static final String ANSWER_PAGE_NAGVIGATION = "answers";
  
  private static final String FORMAT_DATE           = "EEEEE, MMMMMMMM d, yyyy K:mm a";  

  private JCRDataStorage storage;
  
  private LocaleConfigService localeConfigService;

  /** */
                                          //"/answer/skin/DefaultSkin/webui/skinIcons/48x48/defaultQuestion.png";
  private static final String FIX_ICON = "/eXoResources/skin/images/Icons/AppIcons/uiIconAppGray.png";
  
  /** */
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

    ExoContainerContext eXoContext = (ExoContainerContext)ExoContainerContext.getCurrentContainer()
        .getComponentInstanceOfType(ExoContainerContext.class);
    String portalName = eXoContext.getPortalContainerName();
    
    List<SearchResult> results = new ArrayList<SearchResult>();
    
    String asteriskQuery = CommonUtils.normalizeUnifiedSearchInput(query);

    FAQEventQuery eventQuery = new FAQEventQuery();
    eventQuery.setType(FAQEventQuery.FAQ_QUESTION);
    eventQuery.setAdmin(false);
    eventQuery.setComment(query);
    eventQuery.setQuestion(query);
    eventQuery.setResponse(query);
    eventQuery.setAsteriskConditionSearch(asteriskQuery);

    eventQuery.setUserId(getCurrentUserName());
    eventQuery.setUserMembers(UserHelper.getAllGroupAndMembershipOfUser(null));

    eventQuery.setLanguageLevelSearch(false);
    eventQuery.setSearchOnDefaultLanguage(false);

    eventQuery.setOffset(offset);
    eventQuery.setLimit(limit);
    eventQuery.setSort(sort);
    eventQuery.setOrder(order);
    eventQuery.setExcerpt(true);
    
    try {
      List<ObjectSearchResult> searchResults = storage.getUnifiedSearchResults(eventQuery);
      for (ObjectSearchResult searchResult : searchResults) {
        StringBuilder sb = new StringBuilder();        
        sb.append(searchResult.getNumberOfAnswer()).append(" answers");
        sb.append(" - ").append(searchResult.getNumberOfComment()).append(" comments");
        //sb.append(" - ").append(searchResult.getRatingOfQuestion());
        SimpleDateFormat sdf = new SimpleDateFormat(FORMAT_DATE);        
        sb.append(" - ").append(sdf.format(searchResult.getCreatedDate()));        
        String url = buildLink(context, portalName, searchResult.getPath(), searchResult.getLink());
        UnifiedSearchResult result = new UnifiedSearchResult(
            url,
            searchResult.getName(),
            StringEscapeUtils.unescapeHtml(searchResult.getExcerpt()),
            sb.toString(),
            FIX_ICON,
            searchResult.getCreatedDate().getTime(),
            searchResult.getRelevancy(),
            searchResult.getRatingOfQuestion());
        result.setContent(searchResult.getDescription());
        result.setType(searchResult.getType());
        results.add(result);
      }

    } catch (Exception e) {
      LOG.error(e);
    }

    return results;
  }

  
  /**
   * @param context
   * @param portalName
   * @param questionPath
   * @param siteName
   * @param defaultLink
   * @return
   */
  private String buildLink(SearchContext context, String portalName, String questionPath, String defaultLink) {
    try {
      String answersURI = "#";
      String categoryId = getCategoryId(questionPath);
      String questionId = questionPath.substring(questionPath.indexOf(Question.QUESTION_ID));
      
      if (categoryId.indexOf(Utils.CATE_SPACE_ID_PREFIX) == 0) {
        answersURI = makeURIForSpaceContext(context, portalName, categoryId);
      } else {
        answersURI = makeURIForPortalContext(context, portalName, categoryId);
      }
      if(!CommonUtils.isEmpty(answersURI)){
        answersURI = String.format("%s/%s%s", answersURI, Utils.QUESTION_ID, questionId);
      } else {
        return defaultLink;
      }

      //
      return answersURI;
    } catch (Exception ex) {
      return "#";
    }
  }
  
  /**
   * @param context
   * @param portalName
   * @param categoryId
   * @param siteName
   * @return
   * @throws Exception
   */
  private String makeURIForPortalContext(SearchContext context, String portalName, String categoryId) throws Exception {
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
    if (CommonUtils.isEmpty(path)) {
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
   * @param context
   * @param portalName
   * @param categoryId
   * @return
   * @throws Exception
   */
  private String makeURIForSpaceContext(SearchContext context, String portalName, String categoryId) throws Exception {
    
    String groupId = categoryId.replace(Utils.CATE_SPACE_ID_PREFIX, CommonUtils.EMPTY_STR);
    String spaceGroupId = String.format("%s/%s", SPACES_GROUP, groupId);
    SiteKey siteKey = SiteKey.group(spaceGroupId);
    //
    String siteName = spaceGroupId.replaceAll("/", ":");
    
    //
    String siteType = SiteType.GROUP.getName();
    
    //
    String forumNavName = getSiteName(siteKey);
    //
    if (CommonUtils.isEmpty(forumNavName)) {
      return CommonUtils.EMPTY_STR;
    }
    String path = groupId + "/" + forumNavName;
    
    String forumURI = context.handler(portalName)
        .lang("")
        .siteName(siteName)
        .siteType(siteType)
        .path(path)
        .renderLink();
    return String.format("/%s%s", portalName, forumURI);
    
  }

  /**
   * @param questionPath
   * @return
   */
  private String getCategoryId(String questionPath) {
    String categoryId = Utils.CATEGORY_HOME;
    int i = questionPath.indexOf(Category.CATEGORY_ID);
    if (i > 0) {
      categoryId = questionPath.substring(i, questionPath.indexOf("/", i));
    }
    return categoryId;
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
        child = it.next();// answers, AnswerPortlet
        if (ANSWER_PAGE_NAGVIGATION.equals(child.getName()) || child.getName().indexOf(ANSWER_PORTLET_NAME) >= 0) {
          return child.getName();
        }
      }
      return CommonUtils.EMPTY_STR;
    } catch (Exception e) {
      return CommonUtils.EMPTY_STR;
    }
  }

  private String getCurrentUserName() {
    return ConversationState.getCurrent().getIdentity().getUserId();
  }
  
}
