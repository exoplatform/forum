/***************************************************************************
 * Copyright (C) 2003-2007 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 ***************************************************************************/
package org.exoplatform.forum.service.impl;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.jcr.*;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.observation.Event;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang.*;
import org.quartz.JobDataMap;
import org.w3c.dom.Document;

import com.rometools.rome.feed.synd.*;
import com.rometools.rome.io.*;

import org.exoplatform.commons.utils.*;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.forum.common.*;
import org.exoplatform.forum.common.conf.RoleRulesPlugin;
import org.exoplatform.forum.common.jcr.*;
import org.exoplatform.forum.common.jcr.KSDataLocation.Locations;
import org.exoplatform.forum.service.*;
import org.exoplatform.forum.service.EmailNotifyPlugin;
import org.exoplatform.forum.service.LazyPageList;
import org.exoplatform.forum.service.SortSettings.Direction;
import org.exoplatform.forum.service.SortSettings.SortField;
import org.exoplatform.forum.service.TopicListAccess;
import org.exoplatform.forum.service.cache.CachedDataStorage;
import org.exoplatform.forum.service.conf.*;
import org.exoplatform.forum.service.filter.model.CategoryFilter;
import org.exoplatform.forum.service.filter.model.ForumFilter;
import org.exoplatform.forum.service.impl.model.*;
import org.exoplatform.forum.service.jcr.listener.*;
import org.exoplatform.forum.service.search.UnifiedSearchOrder;
import org.exoplatform.forum.service.task.AbstractForumTask.QueryLastPostTask;
import org.exoplatform.forum.service.task.AbstractForumTask.SendNotificationTask;
import org.exoplatform.forum.service.task.QueryLastPostTaskManager;
import org.exoplatform.forum.service.task.SendNotificationTaskManager;
import org.exoplatform.forum.service.user.AutoPruneJob;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.ext.ActivityTypeUtils;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.impl.core.query.QueryImpl;
import org.exoplatform.services.jcr.impl.util.XPathUtils;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.mail.Message;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.scheduler.*;
import org.exoplatform.ws.frameworks.cometd.ContinuationService;
import org.exoplatform.ws.frameworks.json.impl.JsonGeneratorImpl;
import org.exoplatform.ws.frameworks.json.value.JsonValue;

/**
 * JCR implementation of Forum Data Storage
 * 
 * @author hung.nguyen@exoplatform.com
 * @author tu.duy@exoplatform.com
 * @version $Revision$
 */
@Managed
@NameTemplate({ @Property(key = "service", value = "forum"), @Property(key = "view", value = "storage") })
@ManagedDescription("Data Storage for this forum")
@SuppressWarnings("unchecked")
public class JCRDataStorage implements DataStorage, ForumNodeTypes {

  private static final Log             LOG                  = ExoLogger.getLogger(JCRDataStorage.class);

  private Map<String, String>          serverConfig         = new HashMap<String, String>();

  private Map<String, Object>          infoMap              = new HashMap<String, Object>();

  final Queue<SendMessageInfo>         pendingMessagesQueue = new ConcurrentLinkedQueue<SendMessageInfo>();

  private List<RoleRulesPlugin>        rulesPlugins         = new ArrayList<RoleRulesPlugin>();

  private List<InitializeForumPlugin>  defaultPlugins       = new ArrayList<InitializeForumPlugin>();

  private List<ForumInitialDataPlugin> dataPlugins          = new ArrayList<ForumInitialDataPlugin>();

  private Map<String, Integer>         updatingView         = new ConcurrentHashMap<String, Integer>();

  private Map<String, List<String>>    updatingRead         = new ConcurrentHashMap<String, List<String>>();

  private SessionManager               sessionManager;

  private KSDataLocation               dataLocator;

  private String                       repository;

  private String                       workspace;

  private static final Pattern         HIGHLIHT_PATTERN     = Pattern.compile("(.*)<strong>(.*)</strong>(.*)");
  
  private final ReentrantLock lock = new ReentrantLock();

  private DataStorage                  cachedStorage;

  private SendNotificationTaskManager  sendNotificationManager;

  private QueryLastPostTaskManager     queryLastPostManager;

  private static final String DOCTYPE_DECLARATION_DISALLOW = "http://apache.org/xml/features/disallow-doctype-decl";

  public JCRDataStorage() {
  }

  public JCRDataStorage(KSDataLocation dataLocator) {
    setDataLocator(dataLocator);
  }

  public DataStorage getCachedDataStorage() {
    if (cachedStorage == null) {
      cachedStorage = CommonsUtils.getService(DataStorage.class);
    }
    return cachedStorage;
  }

  public SendNotificationTaskManager getSendNotificationTaskManager() {
    if (sendNotificationManager == null) {
      sendNotificationManager = CommonsUtils.getService(SendNotificationTaskManager.class);
    }
    return sendNotificationManager;
  }

  public QueryLastPostTaskManager getQueryLastPostTaskManager() {
    if (queryLastPostManager == null) {
      queryLastPostManager = CommonsUtils.getService(QueryLastPostTaskManager.class);
    }

    return queryLastPostManager;
  }

  private void addQueryLastPostTask(String forumPath) {
    QueryLastPostTaskManager queryLastPostManager = getQueryLastPostTaskManager();
    if (queryLastPostManager != null) {
      queryLastPostManager.addTask(new QueryLastPostTask(forumPath));
    }
  }

  @Managed
  @ManagedDescription("repository for forum storage")
  public String getRepository() {
    return repository;
  }

  @Managed
  @ManagedDescription("workspace for the forum storage")
  public String getWorkspace() {
    return workspace;
  }

  @Managed
  @ManagedDescription("data path for forum storage")
  public String getPath() {
    return dataLocator.getForumHomeLocation();
  }
  
  protected Node getForumHomeNode(SessionProvider sProvider) throws Exception {
    String path = dataLocator.getForumHomeLocation();
    return sessionManager.getSession(sProvider).getRootNode().getNode(path);
  }

  private Node getForumSystemHome(SessionProvider sProvider) throws Exception {
    String path = dataLocator.getForumSystemLocation();
    return sessionManager.getSession(sProvider).getRootNode().getNode(path);
  }

  private Node getBanIPHome(SessionProvider sProvider) throws Exception {
    String path = dataLocator.getBanIPLocation();
    return sessionManager.getSession(sProvider).getRootNode().getNode(path);
  }

  protected Node getStatisticHome(SessionProvider sProvider) throws Exception {
    String path = dataLocator.getStatisticsLocation();
    return sessionManager.getSession(sProvider).getRootNode().getNode(path);
  }

  private Node getForumStatisticsNode(SessionProvider sProvider) throws Exception {
    String path = dataLocator.getForumStatisticsLocation();
    return sessionManager.getSession(sProvider).getRootNode().getNode(path);
  }

  private Node getAdminHome(SessionProvider sProvider) throws Exception {
    String path = dataLocator.getAdministrationLocation();
    return sessionManager.getSession(sProvider).getRootNode().getNode(path);
  }

  protected Node getUserProfileHome(SessionProvider sProvider) throws Exception {
    String path = dataLocator.getUserProfilesLocation();
    return sessionManager.getSession(sProvider).getRootNode().getNode(path);
  }

  protected Node getUserProfileNode(SessionProvider sProvider, String userId) throws Exception {
    StringBuffer path = new StringBuffer(dataLocator.getUserProfilesLocation()).append("/").append(userId);
    return sessionManager.getSession(sProvider).getRootNode().getNode(path.toString());
  }

  private Node getUserProfileHome() throws Exception {
    return getNodeAt(dataLocator.getUserProfilesLocation());
  }

  private Node getCategoryHome(SessionProvider sProvider) throws Exception {
    String path = dataLocator.getForumCategoriesLocation();
    return sessionManager.getSession(sProvider).getRootNode().getNode(path);
  }

  private Node getTagHome(SessionProvider sProvider) throws Exception {
    String path = dataLocator.getTagsLocation();
    return sessionManager.getSession(sProvider).getRootNode().getNode(path);
  }

  private Node getKSUserAvatarHomeNode() throws Exception {
    return getNodeAt(dataLocator.getAvatarsLocation());
  }

  private Node getForumBanNode(SessionProvider sProvider) throws Exception {
    String path = dataLocator.getForumBanIPLocation();
    return sessionManager.getSession(sProvider).getRootNode().getNode(path);
  }

  private Node getBBCodesHome(SessionProvider sProvider) throws Exception {
    String path = dataLocator.getBBCodesLocation();
    return sessionManager.getSession(sProvider).getRootNode().getNode(path);
  }

/**
 * Get a Node by path using the current session of {@link JCRSessionManager}.<br>
 * Note that a session must have been initalized by {@link JCRSessionManager#openSession() before calling this method
 * @param relPath path relative to root node of the workspace
 * @return JCR node located at relPath relative path from root node of the current workspace
 */

  private Node getNodeAt(String relPath) throws Exception {
    return sessionManager.getCurrentSession().getRootNode().getNode(relPath);
  }

  private Node getNodeAt(SessionProvider sProvider, String relPath) throws Exception {
    if (relPath.indexOf(CommonUtils.SLASH) == 0) {
      relPath = relPath.substring(1);
    } else if (relPath.indexOf(Utils.CATEGORY) == 0) {
      relPath = dataLocator.getForumCategoriesLocation() + CommonUtils.SLASH + relPath;
    }
    return sessionManager.getSession(sProvider).getRootNode().getNode(relPath);
  }

  public void addPlugin(ComponentPlugin plugin) throws Exception {
    try {
      if (plugin instanceof EmailNotifyPlugin) {
        serverConfig = ((EmailNotifyPlugin) plugin).getServerConfiguration();
      }
    } catch (Exception e) {
      LOG.error("Failed to add plugin", e);
    }
  }

  public void addRolePlugin(ComponentPlugin plugin) throws Exception {
    if (plugin instanceof RoleRulesPlugin) {
      rulesPlugins.add((RoleRulesPlugin) plugin);
    }
  }

  public void addInitialDefaultDataPlugin(ComponentPlugin plugin) throws Exception {
    if (plugin instanceof InitializeForumPlugin) {
      defaultPlugins.add((InitializeForumPlugin) plugin);
    }
  }

  public void addInitialDataPlugin(ComponentPlugin plugin) throws Exception {
    if (plugin instanceof ForumInitialDataPlugin) {
      dataPlugins.add((ForumInitialDataPlugin) plugin);
    }
  }

  public void addDeletedUserCalculateListener() {
    SessionProvider sProvider = SessionProvider.createSystemProvider();
    try {
      Node profileHome = getUserProfileHome(sProvider);
      if (profileHome.hasNode(Utils.USER_PROFILE_DELETED)) {
        deletedUserCalculateListener(profileHome.getNode(Utils.USER_PROFILE_DELETED));
      }
    } catch (Exception e) {
      LOG.error("Can not add caculation listerner for deleted user", e);
    } finally {
      sProvider.close();
    }
  }

  protected void deletedUserCalculateListener(Node node) throws Exception {
    try {
      ObservationManager observation = node.getSession().getWorkspace().getObservationManager();
      DeletedUserCalculateEventListener deleteUserListener = new DeletedUserCalculateEventListener();
      observation.addEventListener(deleteUserListener, Event.NODE_ADDED | Event.NODE_REMOVED, node.getPath(), false, null, new String[] { Utils.USER_PROFILES_TYPE, EXO_USER_DELETED }, false);
    } catch (Exception e) {
      LOG.error("Can not add listener for node " + node.getName(), e);
    }
  }

  public void initCategoryListener() {
    SessionProvider sProvider = SessionProvider.createSystemProvider();
    try {
      ObservationManager observation = sessionManager.getSession(sProvider).getWorkspace().getObservationManager();
      //
      CalculateModeratorEventListener moderatorListener = new CalculateModeratorEventListener();
      observation.addEventListener(moderatorListener, Event.NODE_ADDED | Event.NODE_REMOVED | Event.PROPERTY_CHANGED, "/", true, null,
                                   new String[] { EXO_CATEGORY_HOME, EXO_FORUM_CATEGORY, EXO_FORUM }, false);

      // statistic listener
      StatisticEventListener sListener = new StatisticEventListener();
      observation.addEventListener(sListener, Event.NODE_ADDED | Event.NODE_REMOVED, "/", true, null, new String[] { EXO_FORUM, EXO_TOPIC }, false);

    } catch (Exception e) {
      LOG.error("Failed to init category listenner", e);
    } finally {
      sProvider.close();
    }
  }

  private NodeIterator getNodeIteratorAutoPruneSetting(SessionProvider sProvider, boolean isActive) throws Exception {
    StringBuilder pathQuery = new StringBuilder("SELECT * FROM ").append(EXO_PRUNE_SETTING);
    if (isActive) {
      pathQuery.append(" WHERE ").append(EXO_IS_ACTIVE).append("='true'");
    } else {
      pathQuery.append(" ORDER BY ").append(EXO_ID).append(ASC);
    }

    return getNodeIteratorBySQLQuery(sProvider, pathQuery.toString(), 0, 0, false);
  }

  public void initAutoPruneSchedules() {
    RepositoryService repositoryService = getDataLocation().getRepositoryService();
    List<RepositoryEntry> entries = repositoryService.getConfig().getRepositoryConfigurations();
    String currentRepo = null;
    try {
      currentRepo = repositoryService.getCurrentRepository().getConfiguration().getName();
      for (RepositoryEntry repositoryEntry : entries) {
        repositoryService.setCurrentRepositoryName(repositoryEntry.getName());
        SessionProvider sProvider = SessionProvider.createSystemProvider();
        try {
          NodeIterator iter = getNodeIteratorAutoPruneSetting(sProvider, true);
          while (iter.hasNext()) {
            addOrRemoveSchedule(getPruneSetting(iter.nextNode()));
          }
        } catch (Exception e) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Could not perform pruning!", e);
          }
        } finally {
          sProvider.close();
        }
      }
    } catch (Exception e) {
      LOG.error("Repository is error!", e);
    }
    if (currentRepo != null) {
      try {
        repositoryService.setCurrentRepositoryName(currentRepo);
      } catch (RepositoryConfigurationException e) {
        LOG.error("Could not reset current repository's name", e);
      }
    }
  }

  public boolean isAdminRole(String userName) throws Exception {
    return isAdminRole(CommonUtils.createSystemProvider(), userName);
  }

  private boolean isAdminRole(SessionProvider sProvider, String userName) throws Exception {
    if (Utils.isEmpty(userName) || UserProfile.USER_GUEST.equals(userName)) {
      return false;
    }
    if (isAdminRoleConfig(userName)) {
      return true;
    } else {
      Node userHome = getUserProfileHome(sProvider);
      if (userHome.hasNode(userName)) {
        return (new PropertyReader(userHome.getNode(userName)).l(EXO_USER_ROLE, 2) == UserProfile.ADMIN);
      }
    }
    return false;
  }

  public boolean isAdminRoleConfig(String userName) throws Exception {
    if (Utils.isEmpty(userName)) {
      return false;
    }
    for (int i = 0; i < rulesPlugins.size(); ++i) {
      List<String> list = new ArrayList<String>();
      list.addAll(rulesPlugins.get(i).getRules(Utils.ADMIN_ROLE));
      if (list.contains(userName))
        return true;
      String[] adminrules = Utils.getStringsInList(list);
      if (ForumServiceUtils.isModerator(adminrules, userName))
        return true;
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  public void setDefaultAvatar(String userName) {
    Boolean wasReset = sessionManager.executeAndSave(new ResetAvatarTask(userName));
    if (LOG.isDebugEnabled()) {
      LOG.debug("Avatar for user " + userName + " was " + (wasReset ? "" : "not") + " reset");
    }
  }

  /**
   * Task that reset the user avatar
   * 
   * @author <a href="mailto:patrice.lamarque@exoplatform.com">Patrice Lamarque</a>
   * @version $Revision$
   */
  public class ResetAvatarTask implements JCRTask<Boolean> {

    String username;

    public ResetAvatarTask(String username) {
      this.username = username;
    }

    /**
     * Remove the nt:file node used as avatar for the given username username is used as the name of the avatar node
     */
    public Boolean execute(Session session) throws Exception {
      Boolean wasReset = false;
      Node ksAvatarHomnode = getKSUserAvatarHomeNode();
      if (ksAvatarHomnode.hasNode(username)) {
        Node node = ksAvatarHomnode.getNode(username);
        if (node.isNodeType(NT_FILE)) {
          node.remove();
          ksAvatarHomnode.save();
          wasReset = true;
        }
      }
      return wasReset;
    }
  }

  /**
   * {@inheritDoc}
   */
  public ForumAttachment getUserAvatar(String userName) throws Exception {
    ForumAttachment avatar = sessionManager.execute(new LoadAvatarTask(userName));
    return avatar;
  }

  /**
   * Loads an avatar for a given user
   * 
   * @author <a href="mailto:patrice.lamarque@exoplatform.com">Patrice Lamarque</a>
   * @version $Revision$
   */
  class LoadAvatarTask implements JCRTask<ForumAttachment> {
    String username;

    public LoadAvatarTask(String username) {
      this.username = username;
    }

    /**
     * Load the avatar file from JCR. The username is the name of a nt:file node looked inside the avatar home
     * 
     * @see JCRDataStorage#getKSUserAvatarHomeNode()
     */
    public ForumAttachment execute(Session session) throws Exception {
      Node ksAvatarHomnode = getKSUserAvatarHomeNode();
      if (ksAvatarHomnode.hasNode(username)) {
        return getAttachment(ksAvatarHomnode.getNode(username));
      } else {
        return null;
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  public void saveUserAvatar(String userId, ForumAttachment fileAttachment) throws Exception {
    Boolean wasNew = sessionManager.executeAndSave(new SaveAvatarTask(userId, fileAttachment));
    if (LOG.isDebugEnabled()) {
      LOG.error("avatar was " + ((wasNew) ? "added" : "updated") + " for user " + userId + ": " + fileAttachment);
    }
  }

  /**
   * Unit of work for saving an Avatar
   * 
   * @author <a href="mailto:patrice.lamarque@exoplatform.com">Patrice Lamarque</a>
   * @version $Revision$
   */
  class SaveAvatarTask implements JCRTask<Boolean> {

    String          userId;

    ForumAttachment fileAttachment;

    /**
     * @param userId
     *          owner of the avatar
     * @param fileAttachment
     *          file for the avatar picture to save
     */
    public SaveAvatarTask(String userId, ForumAttachment fileAttachment) {
      this.userId = userId;
      this.fileAttachment = fileAttachment;
    }

    /**
     * Create or update an nt:file node represented by the ForumAttachement. All permissions are granted to any on that file (!)
     * 
     * @param session
     *          unused
     */
    public Boolean execute(Session session) throws Exception {
      Node ksAvatarHomnode = getKSUserAvatarHomeNode();
      Node avatarNode = null;
      Boolean wasNew = false;
      if (ksAvatarHomnode.hasNode(userId)) {
        avatarNode = ksAvatarHomnode.getNode(userId);
      } else {
        avatarNode = ksAvatarHomnode.addNode(userId, NT_FILE);
        wasNew = true;
      }
      ForumServiceUtils.reparePermissions(avatarNode, "any");
      Node nodeContent = null;
      if (avatarNode.hasNode(JCR_CONTENT)) {
        nodeContent = avatarNode.getNode(JCR_CONTENT);
      } else {
        nodeContent = avatarNode.addNode(JCR_CONTENT, NT_RESOURCE);
      }
      nodeContent.setProperty(JCR_MIME_TYPE, fileAttachment.getMimeType());
      nodeContent.setProperty(JCR_DATA, fileAttachment.getInputStream());
      nodeContent.setProperty(JCR_LAST_MODIFIED, Calendar.getInstance().getTimeInMillis());
      return wasNew;
    }
  }

  public void saveForumAdministration(ForumAdministration forumAdministration) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      Node administrationHome = getAdminHome(sProvider);
      Node forumAdminNode;
      try {
        forumAdminNode = administrationHome.getNode(Utils.FORUMADMINISTRATION);
      } catch (PathNotFoundException e) {
        forumAdminNode = administrationHome.addNode(Utils.FORUMADMINISTRATION, EXO_ADMINISTRATION);
      }
      forumAdminNode.setProperty(EXO_FORUM_SORT_BY, forumAdministration.getForumSortBy());
      forumAdminNode.setProperty(EXO_FORUM_SORT_BY_TYPE, forumAdministration.getForumSortByType());
      forumAdminNode.setProperty(EXO_TOPIC_SORT_BY, forumAdministration.getTopicSortBy());
      forumAdminNode.setProperty(EXO_TOPIC_SORT_BY_TYPE, forumAdministration.getTopicSortByType());
      forumAdminNode.setProperty(EXO_CENSORED_KEYWORD, forumAdministration.getCensoredKeyword());
      forumAdminNode.setProperty(EXO_ENABLE_HEADER_SUBJECT, forumAdministration.getEnableHeaderSubject());
      forumAdminNode.setProperty(EXO_HEADER_SUBJECT, forumAdministration.getHeaderSubject());
      forumAdminNode.setProperty(EXO_NOTIFY_EMAIL_CONTENT, forumAdministration.getNotifyEmailContent());
      forumAdminNode.setProperty(EXO_NOTIFY_EMAIL_MOVED, forumAdministration.getNotifyEmailMoved());
      if (forumAdminNode.isNew()) {
        forumAdminNode.getSession().save();
      } else {
        forumAdminNode.save();
      }
    } catch (Exception e) {
      LOG.error("Failed to save forum administration.", e);
    }
  }

  public ForumAdministration getForumAdministration() throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    ForumAdministration forumAdministration = new ForumAdministration();
    try {
      Node forumAdminNode = getAdminHome(sProvider).getNode(Utils.FORUMADMINISTRATION);

      PropertyReader reader = new PropertyReader(forumAdminNode);

      forumAdministration.setForumSortBy(reader.string(EXO_FORUM_SORT_BY));
      forumAdministration.setForumSortByType(reader.string(EXO_FORUM_SORT_BY_TYPE));
      forumAdministration.setTopicSortBy(reader.string(EXO_TOPIC_SORT_BY));
      forumAdministration.setTopicSortByType(reader.string(EXO_TOPIC_SORT_BY_TYPE));
      forumAdministration.setCensoredKeyword(reader.string(EXO_CENSORED_KEYWORD));
      forumAdministration.setEnableHeaderSubject(reader.bool(EXO_ENABLE_HEADER_SUBJECT));
      forumAdministration.setHeaderSubject(reader.string(EXO_HEADER_SUBJECT));
      forumAdministration.setNotifyEmailContent(reader.string(EXO_NOTIFY_EMAIL_CONTENT));
      forumAdministration.setNotifyEmailMoved(reader.string(EXO_NOTIFY_EMAIL_MOVED));
      return forumAdministration;
    } catch (PathNotFoundException e) {
      return forumAdministration;
    }
  }

  public SortSettings getForumSortSettings() {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      Node adminHome = getAdminHome(sProvider);
      if (adminHome.hasNode(Utils.FORUMADMINISTRATION)) {
        Node forumAdminNode = getAdminHome(sProvider).getNode(Utils.FORUMADMINISTRATION);
        PropertyReader reader = new PropertyReader(forumAdminNode);
        return new SortSettings(reader.string(EXO_FORUM_SORT_BY, SortField.ORDER.toString()),
                                 reader.string(EXO_FORUM_SORT_BY_TYPE, Direction.ASC.toString()));
      } else {
        Node forumAdminNode = getAdminHome(sProvider).addNode(Utils.FORUMADMINISTRATION, EXO_ADMINISTRATION);
        forumAdminNode.setProperty(EXO_FORUM_SORT_BY, SortField.ORDER.toString());
        forumAdminNode.setProperty(EXO_FORUM_SORT_BY_TYPE, Direction.ASC.toString());
        forumAdminNode.getSession().save();
      }
      return new SortSettings(SortField.ORDER, Direction.ASC);
    } catch (Exception e) {
      return new SortSettings(SortField.ORDER, Direction.ASC);
    }
  }

  public SortSettings getTopicSortSettings() throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      Node forumAdminNode = getAdminHome(sProvider).getNode(Utils.FORUMADMINISTRATION);
      PropertyReader reader = new PropertyReader(forumAdminNode);
      return new SortSettings(reader.string(EXO_TOPIC_SORT_BY, SortField.LASTPOST.toString()),
                               reader.string(EXO_TOPIC_SORT_BY_TYPE, Direction.DESC.toString()));
    } catch (Exception e) {
      Node forumAdminNode = getAdminHome(sProvider).addNode(Utils.FORUMADMINISTRATION, EXO_ADMINISTRATION);
      forumAdminNode.getSession().save();
    }
    return new SortSettings(SortField.LASTPOST, Direction.DESC);
  }

  public void initDataPlugin() throws Exception {
    for (ForumInitialDataPlugin pln : dataPlugins) {
      List<ByteArrayInputStream> arrayInputStreams = pln.importData();
      if (arrayInputStreams != null) {
        for (ByteArrayInputStream bis : arrayInputStreams) {
          importXML(dataLocator.getForumHomeLocation(), bis, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
        }
      }
    }
  }

  public void initDefaultData() throws Exception {
    SessionProvider sProvider = SessionProvider.createSystemProvider();
    Set<String> set = new HashSet<String>();
    try {
      Node categoryHome = getCategoryHome(sProvider);
      if (categoryHome.hasNodes())
        return;
      List<CategoryData> categories;
      String ct = "";
      for (InitializeForumPlugin pln : defaultPlugins) {
        categories = pln.getForumInitialData().getCategories();
        for (CategoryData categoryData : categories) {
          Category category = new Category();
          category.setCategoryName(categoryData.getName());
          category.setDescription(categoryData.getDescription());
          category.setOwner(categoryData.getOwner());
          this.saveCategory(category, true);

          List<ForumData> forums = categoryData.getForums();
          for (ForumData forumData : forums) {
            Forum forum = new Forum();
            forum.setForumName(forumData.getName());
            forum.setDescription(forumData.getDescription());
            forum.setOwner(forumData.getOwner());
            this.saveForum(category.getId(), forum, true);

            List<TopicData> topics = forumData.getTopics();
            for (TopicData topicData : topics) {
              Topic topic = new Topic();
              topic.setTopicName(topicData.getName());
              ct = topicData.getContent();
              ct = StringUtils.replace(ct, "\\n", "<br/>");
              ct = Utils.removeCharterStrange(ct);
              topic.setDescription(ct);
              topic.setOwner(topicData.getOwner());
              topic.setIcon(topicData.getIcon());
              this.saveTopic(category.getId(), forum.getId(), topic, true, false, new MessageBuilder());
              set.add(topic.getOwner());

              List<PostData> posts = topicData.getPosts();
              for (PostData postData : posts) {
                Post post = new Post();
                post.setName(postData.getName());
                ct = postData.getContent();
                ct = StringUtils.replace(ct, "\\n", "<br/>");
                ct = Utils.removeCharterStrange(ct);
                post.setMessage(ct);
                post.setOwner(postData.getOwner());
                post.setIcon(postData.getIcon());
                MessageBuilder messageBuilder = new MessageBuilder();
                messageBuilder.setLink("link");
                this.savePost(category.getId(), forum.getId(), topic.getId(), post, true, messageBuilder);
                set.add(post.getOwner());
              }
            }
          }
        }
      }
      Node forumStatisticNode = getForumStatisticsNode(sProvider);
      PropertyReader reader = new PropertyReader(forumStatisticNode);
      forumStatisticNode.setProperty(EXO_MEMBERS_COUNT, (reader.l(EXO_MEMBERS_COUNT) + set.size()));
      forumStatisticNode.save();
    } catch (Exception e) {
      LOG.error("Init default data is failed!!", e);
    } finally {
      sProvider.close();
    }
  }

  public List<Category> getCategories() {
    return getCategories(CommonUtils.EMPTY_STR);
  }

  private NodeIterator getCategories(SessionProvider sProvider, String strQuery) throws Exception {
    String categoryHomePath = "/" + dataLocator.getForumCategoriesLocation();

    StringBuilder sqlQuery = jcrPathLikeAndNotLike(EXO_FORUM_CATEGORY, categoryHomePath);
    //
    if (!Utils.isEmpty(strQuery)) {
      sqlQuery.append(" AND ").append(strQuery);
    }
    // order
    sqlQuery.append(" ORDER BY ").append(EXO_CATEGORY_ORDER).append(ASC)
            .append(", ").append(EXO_CREATED_DATE).append(ASC);

    return getNodeIteratorBySQLQuery(sProvider, sqlQuery.toString(), 0, 0, false);
  }

  private List<Category> getCategories(String strQuery) {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    List<Category> categories = new ArrayList<Category>();
    try {
      NodeIterator iter = getCategories(sProvider, strQuery);
      while (iter.hasNext()) {
        Node cateNode = iter.nextNode();
        try {
          categories.add(getCategory(cateNode));
        } catch (RepositoryException e) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Failed to achieve category" + cateNode.getName(), e);
          }
        }
      }
      return categories;
    } catch (Exception e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Can not get all categories.", e);
      }
      return categories;
    }
  }

  public Category getCategory(String categoryId) {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      return getCategory(getCategoryHome(sProvider).getNode(categoryId));
    } catch (Exception e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Failed to get category, categoryId: " + categoryId, e);
      }
      return null;
    }
  }

  public Category getCategoryIncludedSpace() {
    Category category = getCategory(Utils.CATEGORY_SPACE_ID_PREFIX);
    if (category == null) {
      List<Category> categories = getCategories(new StringBuffer(EXO_INCLUDED_SPACE).append("='true'").toString());
      category = (categories.size() >= 1) ? categories.get(0) : null;
    }

    return category;
  }

  public String[] getPermissionTopicByCategory(String categoryId, String type) throws Exception {
    String[] canCreated = new String[] { " " };
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      Node cateNode = getCategoryHome(sProvider).getNode(categoryId);
      canCreated = new PropertyReader(cateNode).strings(type, new String[] { "" });
    } catch (Exception e) {
      LOG.error("Failed to get permission topic by category", e);
    }
    return canCreated;
  }

  private Category getCategory(Node cateNode) throws RepositoryException {
    Category cat = new Category(cateNode.getName());
    cat.setPath(cateNode.getPath());
    PropertyReader reader = new PropertyReader(cateNode);
    cat.setOwner(reader.string(EXO_OWNER));
    cat.setCategoryName(reader.string(EXO_NAME));
    cat.setCategoryOrder(reader.l(EXO_CATEGORY_ORDER));
    cat.setCreatedDate(reader.date(EXO_CREATED_DATE));
    cat.setDescription(reader.string(EXO_DESCRIPTION));
    cat.setModifiedBy(reader.string(EXO_MODIFIED_BY));
    cat.setModifiedDate(reader.date(EXO_MODIFIED_DATE));
    cat.setUserPrivate(reader.strings(EXO_USER_PRIVATE));
    cat.setModerators(reader.strings(EXO_MODERATORS));
    cat.setForumCount(reader.l(EXO_FORUM_COUNT));
    if (cateNode.isNodeType(EXO_FORUM_WATCHING)) {
      cat.setEmailNotification(reader.strings(EXO_EMAIL_WATCHING));
    }
    cat.setViewer(reader.strings(EXO_VIEWER));
    cat.setCreateTopicRole(reader.strings(EXO_CREATE_TOPIC_ROLE));
    cat.setPoster(reader.strings(EXO_POSTER));
    cat.setIncludedSpace(reader.bool(EXO_INCLUDED_SPACE));
    if (!cateNode.hasProperty(EXO_INCLUDED_SPACE)) {
      try {
        if (cateNode.canAddMixin(MIXIN_FORUM_CATEGORY)) {// mix:forumCategory
          cateNode.addMixin(MIXIN_FORUM_CATEGORY);
        }
        cateNode.setProperty(EXO_INCLUDED_SPACE, false);
        cateNode.save();
      } catch (Exception e) {
        LOG.debug(String.format("The category %s has not property exo:includedSpace. Please, excute the forum upgrade plugin.", cateNode.getName()));
      }
    }
    return cat;
  }

  public void saveCategory(Category category, boolean isNew) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    Node catNode = null;
    try {
      Node categoryHome = getCategoryHome(sProvider);
      Session session = categoryHome.getSession();
      if (isNew) {
        catNode = categoryHome.addNode(category.getId(), EXO_FORUM_CATEGORY);
        catNode.setProperty(EXO_ID, category.getId());
        catNode.setProperty(EXO_OWNER, category.getOwner());
        catNode.setProperty(EXO_CREATED_DATE, getGreenwichMeanTime());
        boolean isIncludedSpace = category.isIncludedSpace() || category.getId().equals(Utils.CATEGORY_SPACE_ID_PREFIX);
        try {
          catNode.setProperty(EXO_INCLUDED_SPACE, isIncludedSpace);
        } catch (Exception e) {
          catNode.addMixin("mix:forumCategory");
          catNode.setProperty(EXO_INCLUDED_SPACE, isIncludedSpace);
        }
        session.save();
      } else {
        catNode = categoryHome.getNode(category.getId());
        String[] oldcategoryMod = new PropertyReader(catNode).strings(EXO_MODERATORS, new String[] { "" });
        catNode.setProperty(EXO_TEMP_MODERATORS, oldcategoryMod);
      }
      catNode.setProperty(EXO_NAME, category.getCategoryName());
      catNode.setProperty(EXO_CATEGORY_ORDER, category.getCategoryOrder());
      catNode.setProperty(EXO_DESCRIPTION, category.getDescription());
      catNode.setProperty(EXO_MODIFIED_BY, category.getModifiedBy());
      catNode.setProperty(EXO_MODIFIED_DATE, getGreenwichMeanTime());
      catNode.setProperty(EXO_USER_PRIVATE, convertArray(category.getUserPrivate()));

      catNode.setProperty(EXO_CREATE_TOPIC_ROLE, convertArray(category.getCreateTopicRole()));
      catNode.setProperty(EXO_POSTER, convertArray(category.getPoster()));

      catNode.setProperty(EXO_VIEWER, convertArray(category.getViewer()));
      category.setPath(catNode.getPath());
      session.save();
      try {
        if ((isNew && category.getModerators().length > 0) || !isNew) {
          catNode.setProperty(EXO_MODERATORS, category.getModerators());
          session.save();
        }
      } catch (Exception e) {
        LOG.debug("Failed to save category moderators ", e);
      }
    } catch (Exception e) {
      LOG.error("Failed to save category", e);
      throw e;
    }
  }

  @Override
  public void saveUserPrivateOfCategory(String categoryId, String priInfo) {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      Node cateHome = getCategoryHome(sProvider);
      Node cateNode = cateHome.getNode(categoryId);
      Set<String> privates = new HashSet<String>(new PropertyReader(cateNode).list(EXO_USER_PRIVATE, new ArrayList<String>()));
      privates.add(priInfo);
      cateNode.setProperty(EXO_USER_PRIVATE, privates.toArray(new String[privates.size()]));
      cateHome.getSession().save();
    } catch (Exception e) {
        LOG.error("Failed to save user private of category", e);
    }
  }

  public void saveModOfCategory(List<String> moderatorCate, String userId, boolean isAdd) {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      Node cateHome = getCategoryHome(sProvider);
      Node cateNode = null;
      boolean isAddNew;
      List<String> list;
      List<String> listTemp;
      for (String cateId : moderatorCate) {
        isAddNew = true;
        try {
          cateNode = cateHome.getNode(cateId);
          listTemp = Utils.valuesToList(cateNode.getProperty(EXO_MODERATORS).getValues());
          list = new ArrayList<String>();
          list.addAll(listTemp);
          if (isAdd) {
            if (list.isEmpty() || (list.size() == 1 && Utils.isEmpty(list.get(0)))) {
              list = new ArrayList<String>();
              list.add(userId);
            } else if (!list.contains(userId)) {
              list.add(userId);
            } else {
              isAddNew = false;
            }
            if (isAddNew) {
              cateNode.setProperty(EXO_TEMP_MODERATORS, Utils.getStringsInList(listTemp));
              cateNode.setProperty(EXO_MODERATORS, Utils.getStringsInList(list));
            }
          } else {
            if (!list.isEmpty()) {
              if (list.contains(userId)) {
                list.remove(userId);
                if (list.isEmpty())
                  list.add("");
                cateNode.setProperty(EXO_MODERATORS, Utils.getStringsInList(list));//
                cateNode.setProperty(EXO_TEMP_MODERATORS, Utils.getStringsInList(listTemp));
              }
            }
          }
        } catch (Exception e) {
          LOG.debug("Failed to save moderater of categoryId: " + cateId, e);
        }
      }
      cateHome.save();
    } catch (Exception e) {
      LOG.error("Failed to save moderator of category", e);
    }
  }

  public void calculateModerator(String nodePath, boolean isNew) throws Exception {
    try {
      JCRSessionManager manager = new JCRSessionManager(workspace);
      Session session = manager.createSession();
      try {
        Node node = (Node) session.getItem(nodePath);

        if (node.isNodeType(EXO_FORUM) == false && node.isNodeType(EXO_FORUM_CATEGORY) == false) {
          return;
        }

        PropertyReader reader = new PropertyReader(node);
        String[] modTemp = reader.strings(EXO_TEMP_MODERATORS, new String[] {});

        if (node.isNodeType(EXO_FORUM_CATEGORY)) {
          Category category = new Category(node.getName());
          category.setCategoryName(reader.string(EXO_NAME));
          category.setModerators(reader.strings(EXO_MODERATORS, new String[] {}));
          if (isNew || Utils.arraysHaveDifferentContent(modTemp, category.getModerators())) {
            updateModeratorInForums(node, category.getModerators());
            updateUserProfileModInCategory(session, node, modTemp, category, isNew);
          }
        } else if (node.isNodeType(EXO_FORUM)) {
          Forum forum = new Forum();
          forum.setId(node.getName());
          forum.setForumName(reader.string(EXO_NAME, ""));
          forum.setModerators(reader.strings(EXO_MODERATORS, new String[] {}));
          if (isNew || Utils.arraysHaveDifferentContent(modTemp, forum.getModerators())) {
            String categoryId = nodePath.substring(nodePath.indexOf(Utils.CATEGORY), nodePath.lastIndexOf("/"));
            setModeratorForum(session, forum.getModerators(), modTemp, forum, categoryId, isNew);
          }
        }
        node.setProperty(EXO_TEMP_MODERATORS, new String[] {});
        node.save();
      } finally {
        session.logout();
      }
    } catch (Exception e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("PathNotFoundException  category node or forum node not found");
      }
    }
  }

  private void updateModeratorInForums(Node cateNode, String[] moderatorCat) throws RepositoryException {
    NodeIterator iter = cateNode.getNodes();
    List<String> list;
    String[] oldModeratoForums;
    String[] strModerators;
    while (iter.hasNext()) {
      list = new ArrayList<String>();
      Node node = iter.nextNode();
      if (node.isNodeType(EXO_FORUM)) {
        oldModeratoForums = new PropertyReader(node).strings(EXO_MODERATORS, new String[] {});
        list.addAll(Arrays.asList(oldModeratoForums));
        for (int i = 0; i < moderatorCat.length; i++) {
          if (!list.contains(moderatorCat[i])) {
            list.add(moderatorCat[i]);
          }
        }
        strModerators = Utils.getStringsInList(list);
        node.setProperty(EXO_MODERATORS, strModerators);
        node.setProperty(EXO_TEMP_MODERATORS, oldModeratoForums);
      }
    }
    cateNode.save();
  }

  private void updateUserProfileModInCategory(Session session, Node catNode, String[] oldcategoryMod, Category category, boolean isNew) throws Exception {
    Node userProfileHomeNode = session.getRootNode().getNode(dataLocator.getUserProfilesLocation());
    Node userProfileNode;
    String categoryId = category.getId(), cateName = category.getCategoryName();
    List<String> moderators = ForumServiceUtils.getUserPermission(category.getModerators());
    if (!moderators.isEmpty()) {
      for (String string : moderators) {
        try {
          boolean isAdd = true;
          userProfileNode = userProfileHomeNode.getNode(string);
          List<String> moderateCategory = new ArrayList<String>();
          try {
            moderateCategory = Utils.valuesToList(userProfileNode.getProperty(EXO_MODERATE_CATEGORY).getValues());
          } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
              LOG.debug("Failed to get list of moderated Category", e);
            }
          }
          for (String string2 : moderateCategory) {
            if (string2.indexOf(categoryId) > 0) {
              isAdd = false;
              break;
            }
          }
          if (isAdd) {
            moderateCategory.add(cateName + "(" + categoryId);
            userProfileNode.setProperty(EXO_MODERATE_CATEGORY, Utils.getStringsInList(moderateCategory));
          }
        } catch (Exception e) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Failed to get user profile ", e);
          }
        }
      }
    }
    if (!isNew && oldcategoryMod != null && oldcategoryMod.length > 0 && !Utils.isEmpty(oldcategoryMod[0])) {
      if (Utils.arraysHaveDifferentContent(oldcategoryMod, category.getModerators())) {
        // calculate moderator of category removed
        List<String> olds = new ArrayList<String>(Arrays.asList(oldcategoryMod));
        String[] mods = category.getModerators();
        for (int i = 0; i < mods.length; i++) {
          if (olds.contains(mods[i])) {
            olds.remove(mods[i]);
          }
        }
        List<String> oldmoderators = ForumServiceUtils.getUserPermission(olds.toArray(new String[olds.size()]));
        for (String oldUserId : oldmoderators) {
          if (moderators.contains(oldUserId))
            continue;
          // edit profile of old user.
          userProfileNode = userProfileHomeNode.getNode(oldUserId);
          List<String> moderateList = new ArrayList<String>();
          try {
            moderateList = Utils.valuesToList(userProfileNode.getProperty(EXO_MODERATE_CATEGORY).getValues());
          } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
              LOG.debug("Failed to get list of moderated Category", e);
            }
          }
          for (String string2 : moderateList) {
            if (string2.indexOf(categoryId) > 0) {
              moderateList.remove(string2);
              userProfileNode.setProperty(EXO_MODERATE_CATEGORY, Utils.getStringsInList(moderateList));
              break;
            }
          }
          moderateList = Utils.valuesToList(userProfileNode.getProperty(EXO_MODERATE_FORUMS).getValues());
          NodeIterator iter = catNode.getNodes();
          while (iter.hasNext()) {
            Node node = iter.nextNode();
            if (node.isNodeType(EXO_FORUM)) {
              for (String str : moderateList) {
                if (str.indexOf(node.getName()) >= 0) {
                  moderateList.remove(str);
                  break;
                }
              }
              List<String> forumMode = Utils.valuesToList(node.getProperty(EXO_MODERATORS).getValues());
              List<String> forumModeTemp = new ArrayList<String>();
              forumModeTemp.addAll(forumMode);
              for (String old : olds) {
                if (forumMode.contains(old)) {
                  forumMode.remove(old);
                }
              }
              node.setProperty(EXO_MODERATORS, Utils.getStringsInList(forumMode));
              node.setProperty(EXO_TEMP_MODERATORS, Utils.getStringsInList(forumModeTemp));
            }
          }
          catNode.save();
          if (moderateList.isEmpty() || (moderateList.size() == 1 && Utils.isEmpty(moderateList.get(0)))) {
            //hasRole == fasle or hasRole =true && is Moderator = true;
            if (userProfileNode.hasProperty(EXO_USER_ROLE) == false
                || userProfileNode.hasProperty(EXO_USER_ROLE)
                && userProfileNode.getProperty(EXO_USER_ROLE).getLong() == UserProfile.MODERATOR) {
                userProfileNode.setProperty(EXO_USER_ROLE, UserProfile.USER);
                userProfileNode.setProperty(EXO_USER_TITLE, Utils.USER);
            }
          } else {
            // moderator > 0
            userProfileNode.setProperty(EXO_USER_ROLE, UserProfile.MODERATOR);
            userProfileNode.setProperty(EXO_USER_TITLE, Utils.MODERATOR);
          }
          userProfileNode.setProperty(EXO_MODERATE_FORUMS, Utils.getStringsInList(moderateList));
        }
      }
    }
    if (userProfileHomeNode.isNew()) {
      session.save();
    } else {
      userProfileHomeNode.save();
    }
  }

  public Category removeCategory(String categoryId) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      Node categoryHome = getCategoryHome(sProvider);
      Node categoryNode = categoryHome.getNode(categoryId);
      Map<String, Long> userPostMap = getDeletePostByUser(sProvider, categoryNode);
      Category category = getCategory(categoryNode);
      Set<String> users = new HashSet<String>();
      PropertyReader reader;
      try {
        reader = new PropertyReader(categoryNode);
        users.addAll(reader.list(EXO_MODERATORS, new ArrayList<String>()));
        categoryNode.setProperty(EXO_TEMP_MODERATORS, reader.strings(EXO_MODERATORS, new String[] { "" }));
        categoryNode.setProperty(EXO_MODERATORS, new String[] { "" });
        NodeIterator iter = categoryNode.getNodes();
        while (iter.hasNext()) {
          Node node = iter.nextNode();
          if (node.isNodeType(EXO_FORUM)) {
            reader = new PropertyReader(node);
            users.addAll(reader.list(EXO_MODERATORS, new ArrayList<String>()));
            node.setProperty(EXO_TEMP_MODERATORS, reader.strings(EXO_MODERATORS, new String[] { "" }));
            node.setProperty(EXO_MODERATORS, new String[] { "" });
          }
        }
        categoryNode.save();
      } catch (Exception e) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Failed to get list of moderators", e);
        }
      }
      categoryNode.remove();
      categoryHome.save();
      addUpdateUserProfileJob(userPostMap);
      getTotalJobWatting(sProvider, users);
      return category;
    } catch (Exception e) {
      LOG.error("failed to remove category " + categoryId);
      return null;
    }
  }

  @Deprecated
  public List<Forum> getForums(String categoryId, String strQuery) throws Exception {
    return getForums(new ForumFilter(categoryId, false).strQuery(strQuery));
  }

  @Deprecated
  public List<Forum> getForumSummaries(String categoryId, String strQuery) throws Exception {
    return getForums(new ForumFilter(categoryId, true).strQuery(strQuery));
  }

  private List<Forum> getForumsPublic(ForumFilter filter) {
    StringBuilder sqlQuery = new StringBuilder();
    sqlQuery.append(Utils.getSQLQueryByProperty("", EXO_IS_CLOSED, "false"))
            .append(Utils.getSQLQueryByProperty("AND", EXO_IS_LOCK, "false"));
    return getForums(filter, sqlQuery.toString());
  }

  private List<Forum> getForumsOfCategoryByUser(ForumFilter filter) {
    try {
      StringBuilder sqlQuery = new StringBuilder();

      if (Utils.CATEGORY_SPACE_ID_PREFIX.equals(filter.categoryId())) {
        if (UserProfile.USER_GUEST.equals(filter.userId())) {
          return new ArrayList<Forum>();
        }
        String querySpace = Utils.buildSQLQueryForumInSpaceOfUser(filter.userId());
        if (Utils.isEmpty(querySpace)) {
          return new ArrayList<Forum>();
        } else {
          sqlQuery.append(querySpace);
        }
      }

      boolean isAdmin = getCachedDataStorage().isAdminRole(filter.userId());
      if (!isAdmin) {
        sqlQuery.append((sqlQuery.length() == 0) ? "(" : " AND (")
                .append(Utils.getSQLQueryByProperty("", EXO_IS_CLOSED, "false"))
                .append(" OR (")
                .append(Utils.buildSQLByUserInfo(EXO_MODERATORS, UserHelper.getAllGroupAndMembershipOfUser(filter.userId())))
                .append("))");
      }

      return getForums(filter, sqlQuery.toString());
    } catch (Exception e) {
      LOG.warn(String.format("Failed to get forums of category %s by user %s", filter.categoryId(), filter.userId()), e);
    }
    return new ArrayList<Forum>();
  }
  
  @Override
  public List<Forum> getForums(ForumFilter filter) {
    if (Utils.isEmpty(filter.userId()) == false) {
      return getForumsOfCategoryByUser(filter);
    } else if (filter.isPublic()) {
      return getForumsPublic(filter);
    } else {
      return getForums(filter, filter.strQuery());
    }
  }

  private List<Forum> getForums(ForumFilter filter, String strQuery) {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      SortSettings sort = getForumSortSettings();
      SortField orderBy = sort.getField();
      Direction orderType = sort.getDirection();

      Node catNode = getCategoryHome(sProvider).getNode(filter.categoryId());

      StringBuilder sqlQuery = jcrPathLikeAndNotLike(EXO_FORUM, catNode.getPath());

      if (!Utils.isEmpty(strQuery)) {
        sqlQuery.append(" AND ").append(strQuery);
      }

      // order
      sqlQuery.append(" ORDER BY exo:").append(orderBy).append(" ").append(orderType);
      if (orderBy != SortField.ORDER) {
        sqlQuery.append(", ").append(EXO_FORUM_ORDER).append(ASC);
        if (orderBy != SortField.CREATED) {
          sqlQuery.append(", ").append(EXO_CREATED_DATE).append(ASC);
        }
      } else {
        sqlQuery.append(", ").append(EXO_CREATED_DATE).append(ASC);
      }

      NodeIterator iter = getNodeIteratorBySQLQuery(sProvider, sqlQuery.toString(), filter.offset(), filter.limit(), true);

      List<Forum> forums = new ArrayList<Forum>();
      DataStorage storage = getCachedDataStorage();
      while (iter.hasNext()) {
        Node forumNode = null;
        try {
          forumNode = iter.nextNode();
          forums.add(storage.getForum(filter.categoryId(), forumNode.getName()));
        } catch (Exception e) {
          LOG.debug("Failed to load forum node " + forumNode.getPath(), e);
        }
      }
      return forums;
    } catch (Exception e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Failed to retrieving forums for category " + filter.categoryId(), e);
      }
      return new ArrayList<Forum>();
    }
  }

  private List<String> getCategoriesCanCreateTopics(SessionProvider sProvider, List<String> listOfUser, boolean isIgnoreSpace) throws Exception {
    Set<String> canCreateTopicIds = new HashSet<String>();

    // get all categories not in space that user can create topics
    StringBuilder cateBuilder = new StringBuilder();
    if (isIgnoreSpace) {
      cateBuilder.append(EXO_ID).append(" <> '").append(Utils.CATEGORY_SPACE_ID_PREFIX).append("'");
    }
    cateBuilder.append((isIgnoreSpace) ? " AND " : "").append(getCanCreateTopicQuery(listOfUser, true));

    NodeIterator iter = getCategories(sProvider, cateBuilder.toString());
    while (iter.hasNext()) {
      canCreateTopicIds.add(iter.nextNode().getName());
    }

    return new ArrayList<String>(canCreateTopicIds);
  }

  private String getCanCreateTopicQuery(List<String> listOfUser, boolean isForCategory) {
    StringBuilder strQuery = new StringBuilder("( (")
        .append(Utils.buildSQLByUserInfo(EXO_CREATE_TOPIC_ROLE, listOfUser))
        .append(") OR (").append(Utils.buildSQLByUserInfo(EXO_MODERATORS, listOfUser))
        .append(") OR (").append(Utils.buildSQLHasProperty(EXO_CREATE_TOPIC_ROLE));
    strQuery.append(") )");
    if(isForCategory) {
      strQuery.append(" AND (").append(Utils.buildSQLByUserInfo(EXO_USER_PRIVATE, listOfUser))
              .append(" OR ").append(Utils.buildSQLHasProperty(EXO_USER_PRIVATE))
              .append(")");
    }
    return strQuery.toString();
  }

  public List<CategoryFilter> filterForumByName(String forumNameFilter, String userName, int maxSize) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      Node categoryHome = getCategoryHome(sProvider);
      List<String> listOfUser = UserHelper.getAllGroupAndMembershipOfUser(userName);
      //removes all of group what contains "spaces" group 
      List<String> userListWithoutSpace = new ArrayList<String>();
      for(String group : listOfUser) {
        if (group != null && group.indexOf(Utils.CATEGORY_SPACE) == -1) {
          userListWithoutSpace.add(group);
        }
      }
      // get can create topic
      List<String> categoriesCanCreateTopics = getCategoriesCanCreateTopics(sProvider, listOfUser, true);

      // query forum by input-key
      StringBuffer strQuery = new StringBuffer("SELECT * FROM ");

      strQuery.append(EXO_FORUM).append(" WHERE ").append(JCR_PATH).append(" LIKE '").append(categoryHome.getPath()).append("/%' AND ");
      strQuery.append("( UPPER(").append(EXO_NAME).append(") LIKE '").append(forumNameFilter.toUpperCase())
              .append("%' OR UPPER(").append(EXO_NAME).append(") LIKE '% ").append(forumNameFilter.toUpperCase()).append("%')")
              .append(Utils.getSQLQueryByProperty("AND", EXO_IS_CLOSED, "false"))
              .append(Utils.getSQLQueryByProperty("AND", EXO_IS_LOCK, "false"))
              .append(" AND ").append(getCanCreateTopicQuery(userListWithoutSpace, false))
              .append(" ORDER BY ").append(EXO_NAME);
      
      LOG.debug("SQL statement: " + strQuery.toString());

      QueryManager qm = categoryHome.getSession().getWorkspace().getQueryManager();
      Query query = qm.createQuery(strQuery.toString(), Query.SQL);
      QueryImpl queryImpl = (QueryImpl) query;
      queryImpl.setCaseInsensitiveOrder(true);

      LinkedHashMap<String, CategoryFilter> categoryFilters = new LinkedHashMap<String, CategoryFilter>();
      CategoryFilter categoryFilter;
      String categoryId, categoryName, forumId, forumName;
      long gotItemNumber = 0;
      //
      NodeIterator iter = queryImpl.execute().getNodes();
      //
      while (iter.hasNext()) {
        Node node = iter.nextNode();
        categoryId = node.getParent().getName();

        if (Utils.CATEGORY_SPACE_ID_PREFIX.equalsIgnoreCase(categoryId)) {
          continue;
        }

        forumId = node.getName();

        // can create topic in category/forum
        if (categoriesCanCreateTopics.contains(categoryId)) {

          if (categoryFilters.containsKey(categoryId)) {
            categoryFilter = categoryFilters.get(categoryId);
          } else {
            categoryName = node.getParent().getProperty(EXO_NAME).getString();
            categoryFilter = new CategoryFilter(categoryId, categoryName);
            categoryFilters.put(categoryId, categoryFilter);
          }
          forumName = node.getProperty(EXO_NAME).getString();
          if (categoryFilter.setForumFilter(forumId, forumName)) {
            gotItemNumber++;
            if (gotItemNumber == maxSize) {
              break;
            }
          }
        }
      }

      return Collections.unmodifiableList(new ArrayList<CategoryFilter>(categoryFilters.values()));
    } catch (Exception e) {
      LOG.warn("\nCould not filter forum by name: " + forumNameFilter + "::"+ e.getMessage());
      if (LOG.isDebugEnabled()) {
        LOG.debug("\nCould not filter forum by name: " + forumNameFilter + e.getCause());
      }
    }
    return Collections.emptyList();
  }

  public Forum getForum(String categoryId, String forumId) {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      Node forumNode = getCategoryHome(sProvider).getNode(categoryId + "/" + forumId);
      return getForum(forumNode);
    } catch (Exception e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("\nCould not get " + forumId + " in " + categoryId + " fail: " + e.getCause());
      }
      return null;
    }
  }

  public void modifyForum(Forum forum, int type) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      Node forumHomeNode = getForumHomeNode(sProvider);
      String forumPath = forum.getPath();
      Node forumNode = (Node) forumHomeNode.getSession().getItem(forumPath);
      switch (type) {
      case Utils.CLOSE: {
        forumNode.setProperty(EXO_IS_CLOSED, forum.getIsClosed());
        setActiveTopicByForum(sProvider, forumNode, forum.getIsClosed());
        break;
      }
      case Utils.LOCK: {
        forumNode.setProperty(EXO_IS_LOCK, forum.getIsLock());
        break;
      }
      default:
        break;
      }
      forumNode.getSession().save();
    } catch (RepositoryException e) {
      LOG.error("Failed to modify forum " + forum.getForumName(), e);
    }
  }

  /**
   * Update the exo:moderators of a Node. Avoids duplicate.
   * 
   * @param node Forum node
   * @param mods list of values to add
   * @return The merged list of moderators without duplicates
   * @throws Exception
   */
  String[] updateModeratorInForum(Node node, String[] mods) throws Exception {
    PropertyReader reader = new PropertyReader(node);
    Set<String> set = reader.set(EXO_MODERATORS);
    if (set == null || set.contains("")) {
      return mods;
    }
    for (String mod : mods) {
      if (!mod.isEmpty()) {
        set.add(mod);
      }
    }
    return set.toArray(new String[set.size()]);
  }

  public void saveForum(String categoryId, Forum forum, boolean isNew) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    Node forumNode = null;
    String[] strModerators = forum.getModerators();
    try {
      Node catNode = getCategoryHome(sProvider).getNode(categoryId);
      boolean isNewModerateTopic = forum.getIsModerateTopic();
      boolean isModerateTopic = isNewModerateTopic;
      String[] oldMod = new String[] {};
      if (isNew) {
        forumNode = catNode.addNode(forum.getId(), EXO_FORUM);
        forumNode.setProperty(EXO_ID, forum.getId());
        forumNode.setProperty(EXO_OWNER, forum.getOwner());
        forumNode.setProperty(EXO_CREATED_DATE, getGreenwichMeanTime());
        forumNode.setProperty(EXO_LAST_TOPIC_PATH, forum.getLastTopicPath());
        forumNode.setProperty(EXO_POST_COUNT, 0);
        forumNode.setProperty(EXO_TOPIC_COUNT, 0);
        forumNode.setProperty(EXO_BAN_I_PS, new String[] {});
        forum.setPath(forumNode.getPath());
        long forumCount = 1;
        if (catNode.hasProperty(EXO_FORUM_COUNT))
          forumCount = catNode.getProperty(EXO_FORUM_COUNT).getLong() + 1;
        catNode.setProperty(EXO_FORUM_COUNT, forumCount);
        forumNode.setProperty(EXO_MODERATORS, strModerators);
      } else {
        forumNode = catNode.getNode(forum.getId());
        oldMod = Utils.valuesToArray(forumNode.getProperty(EXO_MODERATORS).getValues());
        forumNode.setProperty(EXO_MODERATORS, strModerators);
        forumNode.setProperty(EXO_TEMP_MODERATORS, oldMod);

        if (forumNode.hasProperty(EXO_IS_MODERATE_TOPIC))
          isModerateTopic = forumNode.getProperty(EXO_IS_MODERATE_TOPIC).getBoolean();
      }
      forumNode.setProperty(EXO_NAME, forum.getForumName());
      forumNode.setProperty(EXO_FORUM_ORDER, forum.getForumOrder());
      forumNode.setProperty(EXO_MODIFIED_BY, forum.getModifiedBy());
      forumNode.setProperty(EXO_MODIFIED_DATE, getGreenwichMeanTime());
      forumNode.setProperty(EXO_DESCRIPTION, forum.getDescription());

      forumNode.setProperty(EXO_IS_AUTO_ADD_EMAIL_NOTIFY, forum.getIsAutoAddEmailNotify());
      forumNode.setProperty(EXO_NOTIFY_WHEN_ADD_POST, forum.getNotifyWhenAddPost());
      forumNode.setProperty(EXO_NOTIFY_WHEN_ADD_TOPIC, forum.getNotifyWhenAddTopic());
      forumNode.setProperty(EXO_IS_MODERATE_TOPIC, isNewModerateTopic);
      forumNode.setProperty(EXO_IS_MODERATE_POST, forum.getIsModeratePost());
      forumNode.setProperty(EXO_IS_CLOSED, forum.getIsClosed());
      forumNode.setProperty(EXO_IS_LOCK, forum.getIsLock());

      forumNode.setProperty(EXO_CREATE_TOPIC_ROLE, convertArray(forum.getCreateTopicRole()));
      forumNode.setProperty(EXO_POSTER, convertArray(forum.getPoster()));
      // set from category
      strModerators = updateModeratorInForum(catNode, strModerators);
      boolean isEditMod = isNew;
      if (!isNew && Utils.arraysHaveDifferentContent(oldMod, strModerators)) {
        isEditMod = true;
      }
      // save list moderators in property categoryPrivate when list userPrivate of parent category not empty.
      if (isEditMod) {
        if (strModerators != null && strModerators.length > 0 && !Utils.isEmpty(strModerators[0])) {
          if (catNode.hasProperty(EXO_USER_PRIVATE)) {
            List<String> listPrivate = new ArrayList<String>();
            listPrivate.addAll(Utils.valuesToList(catNode.getProperty(EXO_USER_PRIVATE).getValues()));
            if (listPrivate.size() > 0 && !Utils.isEmpty(listPrivate.get(0))) {
              for (int i = 0; i < strModerators.length; i++) {
                if (!listPrivate.contains(strModerators[i])) {
                  listPrivate.add(strModerators[i]);
                }
              }
              catNode.setProperty(EXO_USER_PRIVATE, listPrivate.toArray(new String[listPrivate.size()]));
            }
          }
        }
      }

      forumNode.setProperty(EXO_VIEWER, convertArray(forum.getViewer()));
      catNode.getSession().save();

      PropertyReader reader = new PropertyReader(forumNode);
      forum.setPath(forumNode.getPath());
      forum.setTopicCount(reader.l(EXO_TOPIC_COUNT));
      forum.setPostCount(reader.l(EXO_POST_COUNT));
      forum.setLastTopicPath(getLastTopicPath(reader, forum));
      forum.setModerators(strModerators);

      StringBuilder id = new StringBuilder();
      id.append(catNode.getProperty(EXO_CATEGORY_ORDER).getString());
      id.append(catNode.getProperty(EXO_CREATED_DATE).getDate().getTimeInMillis());
      id.append(forum.getForumOrder());
      if (isNew) {
        id.append(getGreenwichMeanTime());
        PruneSetting pruneSetting = new PruneSetting();
        pruneSetting.setId(id.toString());
        pruneSetting.setForumPath(forum.getPath());
        savePruneSetting(pruneSetting);
      } else {
        forum.setCreatedDate(reader.date(EXO_CREATED_DATE, new Date()));
        id.append(forum.getCreatedDate().getTime());
        if (isModerateTopic != isNewModerateTopic) {
          //
          addQueryLastPostTask(forumNode.getPath());
        }
        // updatePruneId
        Node pruneSetting = forumNode.getNode(Utils.PRUNESETTING);
        pruneSetting.setProperty(EXO_ID, id.toString());
        pruneSetting.save();
      }
    } catch (Exception e) {
      LOG.error("Failed to save forum " + forum.getForumName(), e);
    }
  }

  /**
   * Converts the arrays String what has NULL or EMPTY to ""
   * It will be applied for these cases such as categories, forums, and topics with fields:
   *  exo:userPrivate, exo:viewer, exo:canView, exo:createTopicRole, exo:poster, exo:canPost
   * 
   * @param strs
   * @return
   */
  private static String[] convertArray(String[] strs) {
    if (Utils.isEmpty(strs)) {
      return new String[] { "" };
    }
    return strs;
  }

  private void setModeratorForum(Session session, String[] strModerators, String[] oldModeratoForums, Forum forum, String categoryId, boolean isNew) throws Exception {
    Node userProfileHomeNode = session.getRootNode().getNode(dataLocator.getUserProfilesLocation());
    Node userProfileNode;

    List<String> moderators = ForumServiceUtils.getUserPermission(strModerators);
    if (moderators.size() > 0) {
      for (String string : moderators) {
        string = string.trim();
        List<String> list = new ArrayList<String>();
        try {
          userProfileNode = userProfileHomeNode.getNode(string);

          List<String> moderatorForums = new ArrayList<String>();

          if (userProfileNode.hasProperty(EXO_MODERATE_FORUMS)) {
            moderatorForums = PropertyReader.valuesToList(userProfileNode.getProperty(EXO_MODERATE_FORUMS).getValues());
          }

          boolean hasMod = false;
          for (String string2 : moderatorForums) {
            if (string2.indexOf(forum.getId()) > 0) {
              hasMod = true;
            }
            if (!Utils.isEmpty(string2)) {
              list.add(string2);
            }
          }

          if (userProfileNode.getProperty(EXO_USER_ROLE).getLong() >= 2) {
            userProfileNode.setProperty(EXO_USER_ROLE, 1);
            userProfileNode.setProperty(EXO_USER_TITLE, Utils.MODERATOR);
          }

          if (!hasMod) {
            list.add(forum.getForumName() + "(" + categoryId + "/" + forum.getId());
            userProfileNode.setProperty(EXO_MODERATE_FORUMS, Utils.getStringsInList(list));

            getTotalJobWaitingForModerator(session, string);
          }
        } catch (PathNotFoundException e) {
          userProfileNode = userProfileHomeNode.addNode(string, EXO_FORUM_USER_PROFILE);
          String[] strings = new String[] { (forum.getForumName() + "(" + categoryId + "/" + forum.getId()) };
          userProfileNode.setProperty(EXO_MODERATE_FORUMS, strings);
          userProfileNode.setProperty(EXO_USER_ROLE, 1);
          userProfileNode.setProperty(EXO_USER_TITLE, Utils.MODERATOR);
          if (userProfileNode.isNew()) {
            userProfileNode.getSession().save();
          } else {
            userProfileNode.save();
          }
          getTotalJobWaitingForModerator(session, string);
        }
      }
    }
    // remove
    if (!isNew) {
      List<String> oldmoderators = ForumServiceUtils.getUserPermission(oldModeratoForums);
      for (String string : oldmoderators) {
        boolean isDelete = true;
        if (moderators.contains(string)) {
          isDelete = false;
        }
        if (isDelete) {
          try {
            List<String> list = new ArrayList<String>();
            userProfileNode = userProfileHomeNode.getNode(string);
            String[] moderatorForums = PropertyReader.valuesToArray(userProfileNode.getProperty(EXO_MODERATE_FORUMS).getValues());
            for (String string2 : moderatorForums) {
              if (string2.indexOf(forum.getId()) < 0) {
                list.add(string2);
              }
            }
            userProfileNode.setProperty(EXO_MODERATE_FORUMS, Utils.getStringsInList(list));
            if (list.isEmpty()) {
              //hasRole == fasle or hasRole =true && is Moderator = true;
              if (userProfileNode.hasProperty(EXO_USER_ROLE) == false
                  || userProfileNode.hasProperty(EXO_USER_ROLE)
                  && userProfileNode.getProperty(EXO_USER_ROLE).getLong() == UserProfile.MODERATOR) {
                  userProfileNode.setProperty(EXO_USER_ROLE, UserProfile.USER);
                  userProfileNode.setProperty(EXO_USER_TITLE, Utils.USER);
              }
            } else {
              // moderator > 0
              userProfileNode.setProperty(EXO_USER_ROLE, UserProfile.MODERATOR);
              userProfileNode.setProperty(EXO_USER_TITLE, Utils.MODERATOR);
            }
          } catch (Exception e) {
            logDebug("Failed to removing forumId storage in property moderator of user: " + string);
          }
        }
      }
    }
    if (userProfileHomeNode.isNew()) {
      userProfileHomeNode.getSession().save();
    } else {
      userProfileHomeNode.save();
    }
  }

  public void saveModerateOfForums(List<String> forumPaths, String userName, boolean isDelete) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    Node categoryHomeNode = getCategoryHome(sProvider);
    for (String path : forumPaths) {
      String forumPath = categoryHomeNode.getPath() + "/" + path;
      Node forumNode;
      try {
        forumNode = (Node) categoryHomeNode.getSession().getItem(forumPath);
        Node cateNode = forumNode.getParent();
        if (isDelete) {
          String[] cateMods = PropertyReader.valuesToArray(cateNode.getProperty(EXO_MODERATORS).getValues());
          if (cateMods != null && cateMods.length > 0 && !Utils.isEmpty(cateMods[0])) {
            if (ForumServiceUtils.isModerator(cateMods, userName))
              continue;
          }
          if (forumNode.hasProperty(EXO_MODERATORS)) {
            String[] oldUserNamesModerate = PropertyReader.valuesToArray(forumNode.getProperty(EXO_MODERATORS).getValues());
            List<String> list = new ArrayList<String>();
            for (String string : oldUserNamesModerate) {
              if (!string.equals(userName)) {
                list.add(string);
              }
            }
            forumNode.setProperty(EXO_MODERATORS, Utils.getStringsInList(list));
            forumNode.setProperty(EXO_TEMP_MODERATORS, oldUserNamesModerate);
          }
        } else {
          String[] oldUserNamesModerate = new String[] {};
          if (forumNode.hasProperty(EXO_MODERATORS)) {
            oldUserNamesModerate = PropertyReader.valuesToArray(forumNode.getProperty(EXO_MODERATORS).getValues());
          }
          List<String> list = new ArrayList<String>();
          for (String string : oldUserNamesModerate) {
            if (!string.equals(userName)) {
              list.add(string);
            }
          }
          list.add(userName);
          forumNode.setProperty(EXO_MODERATORS, Utils.getStringsInList(list));
          forumNode.setProperty(EXO_TEMP_MODERATORS, oldUserNamesModerate);
          if (cateNode.hasProperty(EXO_USER_PRIVATE)) {
            list = Utils.valuesToList(cateNode.getProperty(EXO_USER_PRIVATE).getValues());
            if (list.size() > 0 && !Utils.isEmpty(list.get(0)) && !list.contains(userName)) {
              String[] strings = new String[list.size() + 1];
              int i = 0;
              for (String string : list) {
                strings[i] = string;
                ++i;
              }
              strings[i] = userName;
              cateNode.setProperty(EXO_USER_PRIVATE, strings);
            }
          }
        }
      } catch (Exception e) {
        LOG.error("Failed to save moderate of forums", e);
      }
    }
    if (categoryHomeNode.isNew()) {
      categoryHomeNode.getSession().save();
    } else {
      categoryHomeNode.save();
    }
  }

  /**
   * Loads only part of the forum properties
   * 
   * @param forumNode
   * @return
   * @throws Exception
   */
  private Forum getForumSummary(Node forumNode) throws Exception {
    Forum forum = new Forum();
    PropertyReader reader = new PropertyReader(forumNode);
    forum.setId(forumNode.getName());
    forum.setPath(forumNode.getPath());
    forum.setForumName(reader.string(EXO_NAME));
    forum.setDescription(reader.string(EXO_DESCRIPTION));
    forum.setModerators(reader.strings(EXO_MODERATORS));
    forum.setPostCount(reader.l(EXO_POST_COUNT));
    forum.setTopicCount(reader.l(EXO_TOPIC_COUNT));
    forum.setIsModerateTopic(reader.bool(EXO_IS_MODERATE_TOPIC));
    forum.setLastTopicPath(getLastTopicPath(reader, forum));
    forum.setIsClosed(reader.bool(EXO_IS_CLOSED));
    forum.setIsLock(reader.bool(EXO_IS_LOCK));
    return forum;
  }

  private String getLastTopicPath(PropertyReader reader, Forum forum) {
    String lastTopic = reader.string(EXO_LAST_TOPIC_PATH, CommonUtils.EMPTY_STR);
    if (!Utils.isEmpty(lastTopic)) {
      lastTopic = Utils.getTopicId(lastTopic);
      lastTopic = new StringBuilder(forum.getPath()).append(CommonUtils.SLASH).append(lastTopic).toString();
    }
    return lastTopic;
  }

  private Forum getForum(Node forumNode) throws Exception {
    if (forumNode == null)
      return null;
    Forum forum = new Forum();
    PropertyReader reader = new PropertyReader(forumNode);
    forum.setId(forumNode.getName());
    forum.setPath(forumNode.getPath());
    forum.setOwner(reader.string(EXO_OWNER));
    forum.setForumName(reader.string(EXO_NAME));
    forum.setForumOrder(Integer.valueOf(reader.string(EXO_FORUM_ORDER)));
    forum.setCreatedDate(reader.date(EXO_CREATED_DATE));
    forum.setModifiedBy(reader.string(EXO_MODIFIED_BY));
    forum.setModifiedDate(reader.date(EXO_MODIFIED_DATE));
    forum.setLastTopicPath(getLastTopicPath(reader, forum));
    forum.setDescription(reader.string(EXO_DESCRIPTION));
    forum.setPostCount(reader.l(EXO_POST_COUNT));
    forum.setTopicCount(reader.l(EXO_TOPIC_COUNT));
    forum.setIsModerateTopic(reader.bool(EXO_IS_MODERATE_TOPIC));
    forum.setIsModeratePost(reader.bool(EXO_IS_MODERATE_POST));
    forum.setIsClosed(reader.bool(EXO_IS_CLOSED));
    forum.setIsLock(reader.bool(EXO_IS_LOCK));
    forum.setIsAutoAddEmailNotify(reader.bool(EXO_IS_AUTO_ADD_EMAIL_NOTIFY, false));
    forum.setNotifyWhenAddPost(reader.strings(EXO_NOTIFY_WHEN_ADD_POST));
    forum.setNotifyWhenAddTopic(reader.strings(EXO_NOTIFY_WHEN_ADD_TOPIC));
    forum.setViewer(reader.strings(EXO_VIEWER));
    forum.setCreateTopicRole(reader.strings(EXO_CREATE_TOPIC_ROLE));
    forum.setPoster(reader.strings(EXO_POSTER));
    forum.setModerators(reader.strings(EXO_MODERATORS));
    forum.setBanIP(reader.list(EXO_BAN_I_PS));

    if (forumNode.isNodeType(EXO_FORUM_WATCHING)) {
      if (forumNode.hasProperty(EXO_EMAIL_WATCHING))
        forum.setEmailNotification(reader.strings(EXO_EMAIL_WATCHING));
    }
    return forum;
  }

  public Forum removeForum(String categoryId, String forumId) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    Forum forum = null;
    try {
      Node catNode = getCategoryHome(sProvider).getNode(categoryId);
      Node forumNode = catNode.getNode(forumId);
      Map<String, Long> userPostMap = getDeletePostByUser(sProvider, forumNode);
      forum = getForum(forumNode);
      forumNode.setProperty(EXO_TEMP_MODERATORS, forum.getModerators());
      forumNode.setProperty(EXO_MODERATORS, new String[] { " " });
      forumNode.save();
      forumNode.remove();
      catNode.setProperty(EXO_FORUM_COUNT, catNode.getProperty(EXO_FORUM_COUNT).getLong() - 1);
      catNode.save();
      addUpdateUserProfileJob(userPostMap);
      getTotalJobWatting(sProvider, new HashSet<String>(Arrays.asList(forum.getModerators())));
    } catch (Exception e) {
      logDebug("Failed to remove forum: " + forumId);
      return null;
    }
    return forum;
  }

  public void moveForum(List<Forum> forums, String destCategoryPath) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      String oldCatePath = "";
      if (!forums.isEmpty()) {
        String forumPath = forums.get(0).getPath();
        oldCatePath = Utils.getCategoryPath(forumPath);
      } else {
        return;
      }
      Node forumHomeNode = getForumHomeNode(sProvider);
      Session session = forumHomeNode.getSession();
      Node oldCatNode = (Node) session.getItem(oldCatePath);
      Node newCatNode = (Node) session.getItem(destCategoryPath);
      PropertyReader reader = new PropertyReader(newCatNode);
      for (Forum forum : forums) {
        String newForumPath = destCategoryPath + "/" + forum.getId();
        //
        session.getWorkspace().move(forum.getPath(), newForumPath);
        //
        Node forumNode = (Node) session.getItem(newForumPath);
        forumNode.setProperty(EXO_PATH, newForumPath);
        String[] strModerators = forum.getModerators();
        forumNode.setProperty(EXO_MODERATORS, strModerators);
        if (!CommonUtils.isEmpty(strModerators)) {
          Set<String> listPrivate = reader.set(EXO_USER_PRIVATE, new HashSet<String>());
          if (!listPrivate.isEmpty()) {
            for (int i = 0; i < strModerators.length; i++) {
              listPrivate.add(strModerators[i]);
            }
            newCatNode.setProperty(EXO_USER_PRIVATE, listPrivate.toArray(new String[listPrivate.size()]));
          }
        }
      }
      long forumCount = forums.size();
      oldCatNode.setProperty(EXO_FORUM_COUNT, new PropertyReader(oldCatNode).l(EXO_FORUM_COUNT) - forumCount);
      //
      forumCount += reader.l(EXO_FORUM_COUNT);
      newCatNode.setProperty(EXO_FORUM_COUNT, forumCount);
      //
      session.save();
    } catch (Exception e) {
      LOG.error("Failed to move forum", e);
    }
  }

  private void setActiveTopicByForum(SessionProvider sProvider, Node forumNode, boolean isClosed) throws Exception {
    NodeIterator iter = forumNode.getNodes();
    Node topicNode = null;
    while (iter.hasNext()) {
      topicNode = iter.nextNode();
      if (topicNode.isNodeType(EXO_TOPIC)) {
        topicNode.setProperty(EXO_IS_ACTIVE_BY_FORUM, !isClosed);
        setActivePostByTopic(sProvider, topicNode, !isClosed);
      }
    }
    if (forumNode.isNew()) {
      forumNode.getSession().save();
    } else {
      forumNode.save();
    }
  }

  private void setActivePostByTopic(SessionProvider sProvider, Node topicNode, boolean isActiveTopic) throws Exception {
    PropertyReader reader = new PropertyReader(topicNode);
    if (isActiveTopic)
      isActiveTopic = reader.bool(EXO_IS_APPROVED);
    if (isActiveTopic)
      isActiveTopic = !(reader.bool(EXO_IS_WAITING));
    if (isActiveTopic)
      isActiveTopic = !(reader.bool(EXO_IS_CLOSED));
    if (isActiveTopic)
      isActiveTopic = reader.bool(EXO_IS_ACTIVE);
    Node postNode = null;
    NodeIterator iter = topicNode.getNodes();
    while (iter.hasNext()) {
      postNode = iter.nextNode();
      if (postNode.isNodeType(EXO_POST)) {
        postNode.setProperty(EXO_IS_ACTIVE_BY_TOPIC, isActiveTopic);
      }
    }
    if (topicNode.isNew()) {
      topicNode.getSession().save();
    } else {
      topicNode.save();
    }
  }

  /**
   * @deprecated use {@link #getTopics(TopicFilter, int, int)}
   */
  public JCRPageList getPageTopic(String categoryId, String forumId, String strQuery, String strOrderBy) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      Node categoryNode = getCategoryHome(sProvider).getNode(categoryId);
      Node forumNode = categoryNode.getNode(forumId);
      String forumPath = forumNode.getPath();
      String pathQuery = Utils.buildTopicQuery(getForumSortSettings(), strQuery, strOrderBy, forumPath);
      QueryManager qm = categoryNode.getSession().getWorkspace().getQueryManager();
      Query query = qm.createQuery(pathQuery, Query.XPATH);
      QueryResult result = query.execute();
      NodeIterator iter = result.getNodes();
      JCRPageList pagelist = new ForumPageList(iter, 10, pathQuery, true);
      return pagelist;
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * @deprecated use {@link #getTopics(TopicFilter, int, int)}

   */
  public LazyPageList<Topic> getTopicList(String categoryId, String forumId, String xpathConditions, String strOrderBy, int pageSize) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      Node categoryNode = getCategoryHome(sProvider).getNode(categoryId);
      Node forumNode = categoryNode.getNode(forumId);
      String forumPath = forumNode.getPath();
      if (xpathConditions != null && xpathConditions.length() > 0 && xpathConditions.contains("topicPermission")) {
        String str = buildXpath(sProvider, forumNode);
        if (str.length() > 0) {
          xpathConditions = StringUtils.replace(xpathConditions, "topicPermission", "(" + str + "))");
        }
      }
      String topicQuery = Utils.buildTopicQuery(getForumSortSettings(), xpathConditions, strOrderBy, forumPath);
      TopicListAccess topicListAccess = new TopicListAccess(sessionManager, topicQuery);
      return new LazyPageList<Topic>(topicListAccess, pageSize);
    } catch (Exception e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Failed to retrieve topic list for forum " + forumId, e);
      }
      return null;
    }
  }

  //
  private String buildXpath(SessionProvider sProvider, Node forumNode) throws Exception {
    QueryManager qm = getCategoryHome(sProvider).getSession().getWorkspace().getQueryManager();
    StringBuilder qrBuilder = new StringBuilder(JCR_ROOT);
    qrBuilder.append(forumNode.getPath())
             .append("/element(*,").append(EXO_TOPIC).append(")[@").append(EXO_IS_WAITING).append("='false' and @")
             .append(EXO_IS_ACTIVE).append("='true' and @").append(EXO_IS_CLOSED)
             .append("='false' and (not(@").append(EXO_CAN_VIEW).append(") or @").append(EXO_CAN_VIEW)
             .append("='' or @").append(EXO_CAN_VIEW).append("=' ')]");

    Query query = qm.createQuery(qrBuilder.toString(), Query.XPATH);
    QueryResult result = query.execute();
    NodeIterator iter = result.getNodes();
    StringBuilder builder = new StringBuilder();
    boolean isOr = false;
    while (iter.hasNext()) {
      Node node = iter.nextNode();
      if (isOr)
        builder.append(" and ");
      builder.append("@").append(EXO_ID).append("!='").append(node.getName()).append("'");
      isOr = true;
    }
    return builder.toString();
  }

  public List<Topic> getTopics(TopicFilter filter, int offset, int limit) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      NodeIterator iter = getNodeIteratorBySQLQuery(sProvider, buildTopicQuery(filter, true), offset, limit, true);
      List<Topic> topicList = new ArrayList<Topic>();
      if (iter != null && iter.getSize() > 0) {
        while (iter.hasNext()) {
          topicList.add(getTopicNode(iter.nextNode()));
        }
      }
      return topicList;
    } catch (Exception e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Failed to retrieve topic list for forum " + filter.forumId(), e);
      }
      return new ArrayList<Topic>();
    }
  }

  private String buildTopicQuery(TopicFilter filter, boolean hasOrder) throws Exception {
    SortSettings sortSettings = getTopicSortSettings();
    SortField orderBy = sortSettings.getField();
    Direction orderType = sortSettings.getDirection();

    String forumPath = new StringBuilder("/").append(dataLocator.getForumCategoriesLocation())
                        .append("/").append(filter.categoryId()).append("/").append(filter.forumId()).toString();

    StringBuilder sqlBuilder = jcrPathLikeAndNotLike(EXO_TOPIC, forumPath);

    if (filter.isAdmin() == false) {
      StringBuilder strQuery = new StringBuilder();
      strQuery.append(Utils.getSQLQueryByProperty("AND", EXO_IS_WAITING, "false"))
              .append(Utils.getSQLQueryByProperty("AND", EXO_IS_ACTIVE, "true"))
              .append(Utils.getSQLQueryByProperty("AND", EXO_IS_CLOSED, "false"));

      if (filter.isApproved()) {
        strQuery.append(Utils.getSQLQueryByProperty("AND", EXO_IS_APPROVED, "true"));
      }

      if (Utils.isEmpty(filter.viewers()) == true) {
        // public from parent ==> user is owner or user in can view or can view is empty
        strQuery.append(" AND (").append(Utils.EXO_OWNER).append("='").append(filter.userLogin()).append("' OR ")
                .append(Utils.buildSQLByUserInfo(EXO_CAN_VIEW, UserHelper.getAllGroupAndMembershipOfUser(null)))
                .append(" OR ").append(Utils.buildSQLHasProperty(EXO_CAN_VIEW)).append(")");
      } else if (ForumServiceUtils.hasPermission(filter.viewers(), filter.userLogin()) == false) {
        // has not permission from parent ==> user is owner or user in can view
        strQuery.append(" AND (").append(Utils.EXO_OWNER).append("='").append(filter.userLogin()).append("' OR ")
                .append(Utils.buildSQLByUserInfo(EXO_CAN_VIEW, UserHelper.getAllGroupAndMembershipOfUser(null)))
                .append(")");
      } else {
        // has permission from parent ==> empty
      }

      sqlBuilder.append(strQuery);
    }

    if (hasOrder == true) {
      sqlBuilder.append(" ORDER BY ").append(EXO_IS_STICKY).append(DESC);
      String strOrderBy = filter.orderBy();
      if (strOrderBy == null || Utils.isEmpty(strOrderBy)) {
        if (orderBy != null) {
          sqlBuilder.append(", exo:").append(orderBy.toString()).append(" ").append(orderType);
          if (!orderBy.equals(SortField.LASTPOST)) {
            sqlBuilder.append(", ").append(EXO_LAST_POST_DATE).append(DESC);
          }
        } else {
          sqlBuilder.append(", ").append(EXO_LAST_POST_DATE).append(DESC);
        }
      } else {
        sqlBuilder.append(", exo:").append(strOrderBy);
        if (strOrderBy.indexOf(SortField.LASTPOST.toString()) < 0) {
          sqlBuilder.append(", ").append(EXO_LAST_POST_DATE).append(DESC);
        }
      }
    }
    return sqlBuilder.toString();
  }

  public int getTopicsCount(TopicFilter filter) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      NodeIterator iter = null;
      if (Utils.isEmpty(filter.userName()) == false) {
        iter = getNodeIteratorBySQLQuery(sProvider, buildQueryTopicsByUser(filter, false), 0, 0, false);
      } else {
        iter = getNodeIteratorBySQLQuery(sProvider, buildTopicQuery(filter, false), 0, 0, false);
      }
      return (int) ((iter != null) ? iter.getSize() : 0);
    } catch (Exception e) {
      return 0;
    }
  }

  public List<Topic> getTopics(String categoryId, String forumId) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    List<Topic> topics = new ArrayList<Topic>();
    DataStorage storage = getCachedDataStorage();
    try {
      Node forumNode = getCategoryHome(sProvider).getNode(categoryId).getNode(forumId);
      NodeIterator iter = forumNode.getNodes();
      while (iter.hasNext()) {
        try {
          Node topicNode = iter.nextNode();
          if (topicNode.isNodeType(EXO_TOPIC)) {
            topics.add(storage.getTopicByPath(topicNode.getPath(), false));
          }
        } catch (Exception e) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Can not get topic", e);
          }
        }
      }
    } catch (Exception e) {
      return null;
    }
    return topics;
  }

  public void setViewCountTopic(String path, String userRead) {
    if (userRead != null && userRead.length() > 0 && !userRead.equals(UserProfile.USER_GUEST)) {
      if (updatingView.containsKey(path)) {
        int value = updatingView.get((path));
        updatingView.put(path, value + 1);
      } else {
        updatingView.put(path, 1);
      }
    }
  }

  public void writeViews() {
    //
    Map<String, Integer> map = updatingView;
    updatingView = new ConcurrentHashMap<String, Integer>();

    //
    SessionProvider sProvider = CommonUtils.createSystemProvider();

    for (Map.Entry<String, Integer> entry : map.entrySet()) {
      try {
        Node topicNode = getCategoryHome(sProvider).getNode(entry.getKey());
        long newViewCount = new PropertyReader(topicNode).l(EXO_VIEW_COUNT) + entry.getValue();
        topicNode.setProperty(EXO_VIEW_COUNT, newViewCount);
        if (topicNode.isNew()) {
          topicNode.getSession().save();
        } else {
          topicNode.save();
        }
      } catch (Exception e) {
        if (LOG.isDebugEnabled()) {
          LOG.debug(String.format("Failed to set view number for topic with path %s", entry.getKey()), e);
        }
      }
    }

  }

  public Topic getTopic(String categoryId, String forumId, String topicId, String userRead) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    String topicPath = topicId;
    try {
      if (!Utils.isEmpty(categoryId)) {
        topicPath = new StringBuilder(categoryId).append("/").append(forumId).append("/").append(topicId).toString();
      }
      Node topicNode = getCategoryHome(sProvider).getNode(topicPath);
      return getTopicNode(topicNode);
    } catch (Exception e) {
      logDebug("Getting the topic " + topicPath + " is unsuccessfully.", e);
      return null;
    }
  }

  public Topic getTopicSummary(String topicPath) {
    try {
      return getTopicSummary(topicPath, false);
    } catch (Exception e) {
      return null;
    }
  }

  @Override
  public boolean topicHasPoll(String topicPath) {
    try {
      return new PropertyReader(getTopicNodeByPath(topicPath, false)).bool(EXO_IS_POLL, false);
    } catch (Exception e) {
      return false;
    }
  }

  public Topic getTopicSummary(String topicPath, boolean isLastPost) throws Exception {
    return getTopicNodeSummary(getTopicNodeByPath(topicPath, isLastPost));
  }

  public Topic getTopicByPath(String topicPath, boolean isLastPost) throws Exception {
    return getTopicNode(getTopicNodeByPath(topicPath, isLastPost));
  }

  private Node getTopicNodeByPath(String topicPath, boolean isLastPost) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      Node catogoryHome = getCategoryHome(sProvider);
      if (topicPath == null || topicPath.length() <= 0)
        return null;
      if (topicPath.indexOf(catogoryHome.getName()) < 0)
        topicPath = catogoryHome.getPath() + "/" + topicPath;

      Node topicNode = (Node) catogoryHome.getSession().getItem(topicPath);
      if (topicNode == null && isLastPost) {
        String forumPath = Utils.getForumPath(topicPath);
        topicNode = queryLastTopic(sProvider, forumPath);
      }
      return topicNode;
    } catch (RepositoryException e) {
      if (topicPath != null && topicPath.length() > 0 && isLastPost) {
        String forumPath = Utils.getForumPath(topicPath);
        return queryLastTopic(sProvider, forumPath);
      }
    } catch (Exception e) {
      return null;
    }
    return null;
  }

  public void queryLastPostForum(String forumPath) throws Exception {
    SessionProvider sProvider = SessionProvider.createSystemProvider();
    try {
      queryLastTopic(sProvider, forumPath);
    } finally {
      sProvider.close();
    }
  }

  private Node queryLastTopic(SessionProvider sProvider, String forumPath) throws Exception {
    try {
      Node forumHomeNode = getForumHomeNode(sProvider);
      Node forumNode = (Node) forumHomeNode.getSession().getItem(forumPath);

      StringBuilder sqlQuery = jcrPathLikeAndNotLike(EXO_TOPIC, forumPath);

      sqlQuery.append(Utils.getSQLQueryByProperty("AND", EXO_IS_WAITING, "false"))
              .append(Utils.getSQLQueryByProperty("AND", EXO_IS_CLOSED, "false"))
              .append(Utils.getSQLQueryByProperty("AND", EXO_IS_ACTIVE, "true"))
              .append(" ORDER BY ").append(EXO_LAST_POST_DATE).append(DESC);

      NodeIterator iter = getNodeIteratorBySQLQuery(sProvider, sqlQuery.toString(), 0, 0, false);
      String lastTopicPath = StringUtils.EMPTY;
      boolean isModerateTopic = new PropertyReader(forumNode).bool(EXO_IS_MODERATE_TOPIC);
      Node topicNode = null;
      while (iter.hasNext()) {
        topicNode = iter.nextNode();
        if (isModerateTopic == false || topicNode.getProperty(EXO_IS_APPROVED).getBoolean()) {
          lastTopicPath = topicNode.getName();
          break;
        }
      }
      forumNode.setProperty(EXO_LAST_TOPIC_PATH, lastTopicPath);
      forumNode.getSession().save();
      return topicNode;
    } catch (PathNotFoundException e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Failed to query last topic", e);
      }
      return null;
    }
  }

  private Topic getTopicNodeSummary(Node topicNode) throws RepositoryException {
    if (topicNode == null)
      return null;
    Topic topicNew = new Topic();
    PropertyReader reader = new PropertyReader(topicNode);
    topicNew.setId(topicNode.getName());
    topicNew.setPath(topicNode.getPath());
    topicNew.setIcon(reader.string(EXO_ICON));
    topicNew.setOwner(reader.string(EXO_OWNER));
    topicNew.setTopicName(reader.string(EXO_NAME));
    topicNew.setLastPostBy(reader.string(EXO_LAST_POST_BY));
    topicNew.setLastPostDate(reader.date(EXO_LAST_POST_DATE));
    topicNew.setIsClosed(reader.bool(EXO_IS_CLOSED));
    topicNew.setIsApproved(reader.bool(EXO_IS_APPROVED));
    topicNew.setIsActive(reader.bool(EXO_IS_ACTIVE));
    topicNew.setIsPoll(reader.bool(EXO_IS_POLL));
    topicNew.setCanView(reader.strings(EXO_CAN_VIEW, new String[] {}));
    return topicNew;
  }

  private Topic getTopicUpdate(Node topicNode, Topic topic, boolean isSummary) throws Exception {
    PropertyReader reader = new PropertyReader(topicNode);
    if (isSummary) {
      topic.setLastPostDate(reader.date(EXO_LAST_POST_DATE));
      topic.setLastPostBy(reader.string(EXO_LAST_POST_BY));
      topic.setOwner(reader.string(EXO_OWNER));
      topic.setTopicName(reader.string(EXO_NAME));
      topic.setDescription(reader.string(EXO_DESCRIPTION));
      topic.setPostCount(reader.l(EXO_POST_COUNT));
      topic.setViewCount(reader.l(EXO_VIEW_COUNT));
      topic.setNumberAttachment(reader.l(EXO_NUMBER_ATTACHMENTS));
      topic.setIsSticky(reader.bool(EXO_IS_STICKY));
      topic.setUserVoteRating(reader.strings(EXO_USER_VOTE_RATING));
      topic.setVoteRating(reader.d(EXO_VOTE_RATING));
    }
    // some properties get again because update new data.
    topic.setIsWaiting(reader.bool(EXO_IS_WAITING));
    topic.setIsActive(reader.bool(EXO_IS_ACTIVE));
    topic.setIsActiveByForum(reader.bool(EXO_IS_ACTIVE_BY_FORUM));
    if (topicNode.getParent().getProperty(EXO_IS_LOCK).getBoolean())
      topic.setIsLock(true);
    else
      topic.setIsLock(reader.bool(EXO_IS_LOCK));
    topic.setIsClosed(reader.bool(EXO_IS_CLOSED));
    // update more properties for topic.
    topic.setCreatedDate(reader.date(EXO_CREATED_DATE));
    topic.setModifiedBy(reader.string(EXO_MODIFIED_BY));
    topic.setModifiedDate(reader.date(EXO_MODIFIED_DATE));
    topic.setIsModeratePost(reader.bool(EXO_IS_MODERATE_POST));
    topic.setIsNotifyWhenAddPost(reader.string(EXO_IS_NOTIFY_WHEN_ADD_POST, null));
    topic.setLink(reader.string(EXO_LINK));
    topic.setTagId(reader.strings(EXO_TAG_ID));
    topic.setCanView(reader.strings(EXO_CAN_VIEW, new String[] {}));
    topic.setCanPost(reader.strings(EXO_CAN_POST, new String[] {}));
    if (topicNode.isNodeType(EXO_FORUM_WATCHING))
      topic.setEmailNotification(reader.strings(EXO_EMAIL_WATCHING, new String[] {}));
    try {
      if (reader.l(EXO_NUMBER_ATTACHMENTS) > 0) {
        String idFirstPost = topicNode.getName().replaceFirst(Utils.TOPIC, Utils.POST);
        Node FirstPostNode = topicNode.getNode(idFirstPost);
        topic.setAttachments(getAttachmentsByNode(FirstPostNode));
      }
    } catch (Exception e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Failed to set attachments in topic.", e);
      }
    }
    return topic;
  }

  public Topic getTopicUpdate(Topic topic, boolean isSummary) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      Node forumHomeNode = getForumHomeNode(sProvider);
      Node topicNode = (Node) forumHomeNode.getSession().getItem(topic.getPath());
      return getTopicUpdate(topicNode, topic, isSummary);
    } catch (Exception e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Failed to get topic", e);
      }
    }
    return topic;
  }

  public Topic getTopicNode(Node topicNode) throws Exception {
    if (topicNode == null)
      return null;
    Topic topic = getTopicNodeSummary(topicNode);
    return getTopicUpdate(topicNode, topic, true);
  }

  /**
   * @deprecated use {@link #getTopicsByDate(long, String, int, int)}
   */
  public JCRPageList getPageTopicOld(long date, String forumPatch) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      String sqlQuery = buildSQLQueryGetTopicByDate(date, forumPatch);
      NodeIterator iter = getNodeIteratorBySQLQuery(sProvider, sqlQuery, 0, 0, false);
      return new ForumPageList(iter, 10, sqlQuery, true);
    } catch (Exception e) {
      return null;
    }
  }

  public List<Topic> getTopicsByDate(long date, String forumPath, int offset, int limit) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      List<Topic> topics = new ArrayList<Topic>();
      NodeIterator iter = getNodeIteratorBySQLQuery(sProvider, buildSQLQueryGetTopicByDate(date, forumPath), offset, limit, false);
      while (iter.hasNext()) {
        Node node = iter.nextNode();
        topics.add(getTopicNode(node));
      }
      return topics;
    } catch (Exception e) {
      return null;
    }
  }

  private String buildSQLQueryGetTopicByDate(long date, String forumPath) throws Exception {
    StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM ").append(EXO_TOPIC);
    sqlBuilder.append(" WHERE ").append(JCR_PATH).append(" LIKE '");
    if (Utils.isEmpty(forumPath) == false) {
      sqlBuilder.append(forumPath).append("/%' AND NOT ").append(JCR_PATH).append(" LIKE '").append(forumPath).append("/%/%'");
    } else {
      sqlBuilder.append("/").append(dataLocator.getForumCategoriesLocation()).append("/%'");
    }
    Calendar newDate = getGreenwichMeanTime();
    newDate.setTimeInMillis(newDate.getTimeInMillis() - date * 86400000);

    sqlBuilder.append(" AND (").append(EXO_LAST_POST_DATE).append(" <= TIMESTAMP '").append(ISO8601.format(newDate)).append("') ORDER BY ").append(EXO_CREATED_DATE).append(ASC);

    return sqlBuilder.toString();
  }

  public List<Topic> getAllTopicsOld(long date, String forumPath) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    List<Topic> topics = new ArrayList<Topic>();
    try {
      NodeIterator iter = getNodeIteratorBySQLQuery(sProvider, buildSQLQueryGetTopicByDate(date, forumPath), 0, 0, false);
      Topic topic;
      while (iter.hasNext()) {
        Node node = iter.nextNode();
        topic = new Topic();
        topic.setId(node.getName());
        topic.setPath(node.getPath());
        topic.setIsActive(node.getProperty(EXO_IS_ACTIVE).getBoolean());
        topic.setPostCount(node.getProperty(EXO_POST_COUNT).getLong());
        topics.add(topic);
      }
    } catch (Exception e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Failed to get all topic old", e);
      }
    }
    return topics;
  }

  public long getTotalTopicOld(long date, String forumPath) {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      NodeIterator iter = getNodeIteratorBySQLQuery(sProvider, buildSQLQueryGetTopicByDate(date, forumPath), 0, 0, false);
      return iter.getSize();
    } catch (Exception e) {
      return 0;
    }
  }

  /**
   * @deprecated use {@link #getTopicsByUser(TopicFilter, int, int)}
   */
  public JCRPageList getPageTopicByUser(String userName, boolean isMod, String strOrderBy) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      TopicFilter filter = new TopicFilter(userName, isMod, strOrderBy);
      String sqlQuery = buildQueryTopicsByUser(filter, true);
      NodeIterator iter = getNodeIteratorBySQLQuery(sProvider, sqlQuery, 0, 0, false);
      return new ForumPageList(iter, 10, sqlQuery, true);
    } catch (Exception e) {
      return null;
    }
  }

  private String buildQueryTopicsByUser(TopicFilter filter, boolean hasOrder) {
    StringBuilder sqlQuery = new StringBuilder("SELECT * FROM ").append(EXO_TOPIC);
    sqlQuery.append(" WHERE ").append(Utils.getSQLQueryByProperty("", EXO_OWNER, filter.userName()));

    if (filter.isAdmin() == false) {
      sqlQuery.append(Utils.getSQLQueryByProperty("AND", EXO_IS_CLOSED, "false"))
              .append(Utils.getSQLQueryByProperty("AND", EXO_IS_WAITING, "false"))
              .append(Utils.getSQLQueryByProperty("AND", EXO_IS_ACTIVE, "true"))
              .append(Utils.getSQLQueryByProperty("AND", EXO_IS_ACTIVE_BY_FORUM, "true"))
              .append(Utils.getSQLQueryByProperty("AND", EXO_IS_APPROVED, "true"));
    }
    if (hasOrder == true) {
      sqlQuery.append(" ORDER BY ").append(EXO_IS_STICKY).append(DESC);
      if (Utils.isEmpty(filter.orderBy()) == false) {
        sqlQuery.append(",exo:").append(filter.orderBy());
        if (EXO_CREATED_DATE.indexOf(filter.orderBy()) < 0) {
          sqlQuery.append(", ").append(EXO_CREATED_DATE).append(ASC);
        }
      } else {
        sqlQuery.append(", ").append(EXO_CREATED_DATE).append(ASC);
      }
    }

    return sqlQuery.toString();
  }

  public List<Topic> getTopicsByUser(TopicFilter filter, int offset, int limit) throws Exception {
    // String userName, boolean isAdmin, String orderBy
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      NodeIterator iter = getNodeIteratorBySQLQuery(sProvider, buildQueryTopicsByUser(filter, true), offset, limit, true);
      List<Topic> topics = new ArrayList<Topic>();
      while (iter.hasNext()) {
        Node node = iter.nextNode();
        topics.add(getTopicNode(node));
      }
      return topics;
    } catch (Exception e) {
      return null;
    }
  }

  public void modifyTopic(List<Topic> topics, int type) {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      Node forumHomeNode = getForumHomeNode(sProvider);
      long topicCount = 0;
      long postCount = 0;
      Node forumNode = null;
      try {
        String topicPath = topics.get(0).getPath();
        forumNode = (Node) forumHomeNode.getSession().getItem(topicPath).getParent();
        topicCount = forumNode.getProperty(EXO_TOPIC_COUNT).getLong();
        postCount = forumNode.getProperty(EXO_POST_COUNT).getLong();
      } catch (PathNotFoundException e) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Failed to get node by path", e);
        }
      }
      Set<String> userIdsp = new HashSet<String>(new PropertyReader(forumNode).list(EXO_MODERATORS, new ArrayList<String>()));
      DataStorage cachedDataStorage = getCachedDataStorage();
      for (Topic topic : topics) {
        try {
          String topicPath = topic.getPath();
          Node topicNode = (Node) forumHomeNode.getSession().getItem(topicPath);
          switch (type) {
          case Utils.CLOSE: {
            topicNode.setProperty(EXO_IS_CLOSED, topic.getIsClosed());
            setActivePostByTopic(sProvider, topicNode, !(topic.getIsClosed()));
            break;
          }
          case Utils.LOCK: {
            topicNode.setProperty(EXO_IS_LOCK, topic.getIsLock());
            break;
          }
          case Utils.APPROVE: {
            topicNode.setProperty(EXO_IS_APPROVED, topic.getIsApproved());
            addNotificationTask(topicNode.getParent().getPath(), topic, null, new MessageBuilder(), true);
            setActivePostByTopic(sProvider, topicNode, topic.getIsApproved());
            getTotalJobWatting(sProvider, userIdsp);
            break;
          }
          case Utils.STICKY: {
            topicNode.setProperty(EXO_IS_STICKY, topic.getIsSticky());
            break;
          }
          case Utils.WAITING: {
            boolean isWaiting = topic.getIsWaiting();
            topicNode.setProperty(EXO_IS_WAITING, isWaiting);
            setActivePostByTopic(sProvider, topicNode, !(isWaiting));
            if (!isWaiting) {
              addNotificationTask(topicNode.getParent().getPath(), topic, null, new MessageBuilder(), true);
            }
            getTotalJobWatting(sProvider, userIdsp);
            break;
          }
          case Utils.ACTIVE: {
            topicNode.setProperty(EXO_IS_ACTIVE, topic.getIsActive());
            setActivePostByTopic(sProvider, topicNode, topic.getIsActive());
            getTotalJobWatting(sProvider, userIdsp);
            break;
          }
          case Utils.CHANGE_NAME: {
            topicNode.setProperty(EXO_NAME, topic.getTopicName());
            try {
              Node nodeFirstPost = topicNode.getNode(topicNode.getName().replaceFirst(Utils.TOPIC, Utils.POST));
              nodeFirstPost.setProperty(EXO_NAME, topic.getTopicName());
            } catch (PathNotFoundException e) {
              if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to get node by path", e);
              }
            }
            break;
          }
          case Utils.VOTE_RATING: {
            topicNode.setProperty(EXO_USER_VOTE_RATING, topic.getUserVoteRating());
            topicNode.setProperty(EXO_VOTE_RATING, topic.getVoteRating());
            break;
          }
          default:
            break;
          }
          if (type == Utils.APPROVE || type == Utils.WAITING) {
            if (!topic.getIsWaiting() && topic.getIsApproved()) {
              topicCount = topicCount + 1;
              postCount = postCount + (topicNode.getProperty(EXO_POST_COUNT).getLong() + 1);
            }
          }
          if ((type == Utils.APPROVE || type == Utils.WAITING || type == Utils.ACTIVE || type == Utils.CLOSE) || forumNode.hasProperty(EXO_LAST_TOPIC_PATH)
              || (forumNode.getProperty(EXO_LAST_TOPIC_PATH).getString().equals(topicNode.getName()) || Utils.isEmpty(forumNode.getProperty(EXO_LAST_TOPIC_PATH).getString()))) {
            //
            addQueryLastPostTask(forumNode.getPath());
          }
          if (cachedDataStorage instanceof CachedDataStorage) {
            ((CachedDataStorage) cachedDataStorage).clearTopicCache(topic);
          }
        } catch (PathNotFoundException e) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Failed to get node by path", e);
          }
        }
      }
      if (type == Utils.APPROVE || type == Utils.WAITING) {
        forumNode.setProperty(EXO_TOPIC_COUNT, topicCount);
        forumNode.setProperty(EXO_POST_COUNT, postCount);
      }
      if (forumNode.isNew()) {
        forumNode.getSession().save();
      } else {
        forumNode.save();
      }
    } catch (Exception e) {
      LOG.error("Failed to modify topic.", e);
    }
  }

  /**
  * This method is call by StatisticEventListener to update user's profile when new topic is added
  *
  * @param owner user's name of an user that create the topic
  * @throws Exception
  */
  public void updateProfileAddTopic(String owner) throws Exception {
    SessionProvider sProvider = SessionProvider.createSystemProvider();
    try {
      Node profileHomeNode = getUserProfileHome(sProvider);
      if (profileHomeNode.hasNode(owner)) {
        Node profileNode = profileHomeNode.getNode(owner);
        long totalTopicByUser = profileNode.getProperty(EXO_TOTAL_TOPIC).getLong();
        profileNode.setProperty(EXO_TOTAL_TOPIC, totalTopicByUser + 1);
      } else if (Utils.isEmpty(owner) == false) {
        Node newProfileNode = profileHomeNode.addNode(owner, EXO_FORUM_USER_PROFILE);
        newProfileNode.setProperty(EXO_USER_ID, owner);
        newProfileNode.setProperty(EXO_USER_TITLE, Utils.USER);
        if (isAdminRole(sProvider, owner)) {
          newProfileNode.setProperty(EXO_USER_TITLE, Utils.ADMIN);
        }
        newProfileNode.setProperty(EXO_TOTAL_TOPIC, 1);
      }
      //
      profileHomeNode.getSession().save();
    } catch (Exception e) {
      LOG.warn("Failed to update user profile when add topic", e);
    } finally {
      sProvider.close();
    }
  }

  /**
  * Get the owner of created post or topic by path
  *
  * @param path the post or topic node path
  * @return user's name
  */
  public String getOwner(String path) {
    SessionProvider sProvider = SessionProvider.createSystemProvider();
    try {
      Node node = getNodeAt(sProvider, path);
      return new PropertyReader(node).string(EXO_OWNER, "");
    } catch (Exception e) {
      return null;
    } finally {
      sProvider.close();
    }
  }

  public void saveTopic(String categoryId, String forumId, Topic topic, boolean isNew, boolean isMove, MessageBuilder messageBuilder) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      Node forumNode = getCategoryHome(sProvider).getNode(categoryId + "/" + forumId);
      Node topicNode;
      boolean isChangeClose = false;
      if (isNew) {
        topicNode = forumNode.addNode(topic.getId(), EXO_TOPIC);
        topicNode.setProperty(EXO_ID, topic.getId());
        topicNode.setProperty(EXO_OWNER, topic.getOwner());
        Calendar calendar = getGreenwichMeanTime();
        topic.setCreatedDate(calendar.getTime());
        topicNode.setProperty(EXO_CREATED_DATE, calendar);
        topicNode.setProperty(EXO_LAST_POST_BY, topic.getOwner());
        if (isMove && topic.getLastPostDate() != null) {
          calendar.setTime(topic.getLastPostDate());
        }
        topicNode.setProperty(EXO_LAST_POST_DATE, calendar);
        topicNode.setProperty(EXO_POST_COUNT, -1);
        topicNode.setProperty(EXO_VIEW_COUNT, 0);
        topicNode.setProperty(EXO_TAG_ID, topic.getTagId());
        topicNode.setProperty(EXO_IS_ACTIVE_BY_FORUM, true);
        topicNode.setProperty(EXO_IS_POLL, topic.getIsPoll());
        topicNode.setProperty(EXO_LINK, CommonUtils.getURI(topic.getLink()));
        topicNode.setProperty(EXO_PATH, forumId);

        if (!forumNode.getProperty(EXO_IS_MODERATE_TOPIC).getBoolean() && !topic.getIsWaiting()) {
          long newTopicCount = forumNode.getProperty(EXO_TOPIC_COUNT).getLong() + 1;
          forumNode.setProperty(EXO_TOPIC_COUNT, newTopicCount);
        }
      } else {
        topicNode = forumNode.getNode(topic.getId());
        isChangeClose = (topic.getIsClosed() != topicNode.getProperty(EXO_IS_CLOSED).getBoolean());
      }
      topicNode.setProperty(EXO_NAME, topic.getTopicName());
      topicNode.setProperty(EXO_MODIFIED_BY, topic.getModifiedBy());
      topicNode.setProperty(EXO_MODIFIED_DATE, getGreenwichMeanTime());
      topicNode.setProperty(EXO_DESCRIPTION, topic.getDescription());
      topicNode.setProperty(EXO_ICON, topic.getIcon());

      topicNode.setProperty(EXO_IS_MODERATE_POST, topic.getIsModeratePost());
      topicNode.setProperty(EXO_IS_NOTIFY_WHEN_ADD_POST, topic.getIsNotifyWhenAddPost());
      topicNode.setProperty(EXO_IS_CLOSED, topic.getIsClosed());
      topicNode.setProperty(EXO_IS_LOCK, topic.getIsLock());
      topicNode.setProperty(EXO_IS_APPROVED, topic.getIsApproved());
      topicNode.setProperty(EXO_IS_STICKY, topic.getIsSticky());
      topicNode.setProperty(EXO_IS_WAITING, topic.getIsWaiting());
      topicNode.setProperty(EXO_IS_ACTIVE, topic.getIsActive());
      topicNode.setProperty(EXO_USER_VOTE_RATING, topic.getUserVoteRating());
      topicNode.setProperty(EXO_VOTE_RATING, topic.getVoteRating());
      topicNode.setProperty(EXO_NUMBER_ATTACHMENTS, topic.getNumberAttachment());
      topicNode.setProperty(EXO_CAN_POST, convertArray(topic.getCanPost()));
      topicNode.setProperty(EXO_CAN_VIEW, convertArray(topic.getCanView()));
      topic.setPath(topicNode.getPath());
      //
      if (isNew) {
        forumNode.getSession().save();
        //
        addNotificationTask(forumNode.getPath(), topic, null, messageBuilder, true);
      } else {
        forumNode.save();
      }
      //
      if (topic.getIsWaiting() || !topic.getIsApproved()) {
        getTotalJobWatting(sProvider, new HashSet<String>(new PropertyReader(forumNode).list(EXO_MODERATORS, new ArrayList<String>())));
      }
      if (isNew || isChangeClose) {
        //
        addQueryLastPostTask(forumNode.getPath());
      }
      if (!isMove) {
        if (isNew) {
          // createPost first
          String id = topic.getId().replaceFirst(Utils.TOPIC, Utils.POST);
          Post post = new Post();
          post.setId(id);
          post.setOwner(topic.getOwner());
          post.setCreatedDate(new Date());
          post.setName(topic.getTopicName());
          post.setMessage(topic.getDescription());
          post.setRemoteAddr("");
          post.setIcon(topic.getIcon());
          post.setIsApproved(true);
          post.setAttachments(topic.getAttachments());
          post.setUserPrivate(new String[] { EXO_USER_PRI });
          post.setLink(topic.getLink());
          post.setRemoteAddr(topic.getRemoteAddr());
          savePost(categoryId, forumId, topic.getId(), post, true, messageBuilder);
        } else {
          String id = topic.getId().replaceFirst(Utils.TOPIC, Utils.POST);
          if (topicNode.hasNode(id)) {
            Node fistPostNode = topicNode.getNode(id);
            Post post = getPost(fistPostNode);
            post.setModifiedBy(topic.getModifiedBy());
            post.setModifiedDate(new Date());
            post.setEditReason(topic.getEditReason());
            post.setName(topic.getTopicName());
            post.setMessage(topic.getDescription());
            post.setIcon(topic.getIcon());
            post.setAttachments(topic.getAttachments());
            savePost(categoryId, forumId, topic.getId(), post, false, messageBuilder);
          }
        }
      }
    } catch (Exception e) {
      LOG.error("Failed to save topic", e);
      if (isNew) {
        topic = null;
      }
    }
  }

  private Map<String, Long> getDeletePostByUser(SessionProvider sProvider, Node node) throws Exception {
    Map<String, Long> userPostMap = new HashMap<String, Long>();
    StringBuilder sqlQuery = new StringBuilder();
    if (node.isNodeType(EXO_TOPIC)) {
      sqlQuery = jcrPathLikeAndNotLike(EXO_POST, node.getPath());
    } else if (node.isNodeType(EXO_FORUM) || node.isNodeType(EXO_FORUM_CATEGORY)) {
      sqlQuery.append("SELECT * FROM ").append(EXO_POST).append(" WHERE ").append(JCR_PATH).append(" LIKE '").append(node.getPath()).append("/%'");
    }
    NodeIterator iter = getNodeIteratorBySQLQuery(sProvider, sqlQuery.toString(), 0, 0, false);
    Node post = null;
    String owner = null;
    while (iter.hasNext()) {
      post = iter.nextNode();
      try {
        owner = post.getProperty(EXO_OWNER).getString();
        userPostMap.put(owner, (userPostMap.get(owner) != null ? userPostMap.get(owner) : 0) + 1);
      } catch (Exception e) {
        LOG.error("Failed to get deleted post by user.", e);
      }
    }
    return userPostMap;
  }

  public void updateUserProfileInfo(String name) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      Node userProfileHome = getUserProfileHome(sProvider);
      Node userNode = null;
      Map<String, Long> userPostMap = (HashMap<String, Long>) infoMap.get(name);
      for (Map.Entry<String, Long> entry : userPostMap.entrySet()) {
        String user = entry.getKey();
        try {
          userNode = userProfileHome.getNode(user);
          long totalPost = userNode.getProperty(EXO_TOTAL_POST).getLong();
          userNode.setProperty(EXO_TOTAL_POST, totalPost - userPostMap.get(user));
          userNode.save();
        } catch (PathNotFoundException e) {
          LOG.debug("UserProfile of user: " + user + " not existing.");
        }
      }
      infoMap.remove(name);
    } catch (Exception e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Failed to update user profile info", e);
      }
    }
  }

  private void addUpdateUserProfileJob(Map<String, Long> userPostMap) {
    try {
      Calendar cal = new GregorianCalendar();
      PeriodInfo periodInfo = new PeriodInfo(cal.getTime(), null, 1, 86400000);
      String name = String.valueOf(cal.getTime().getTime());
      JobInfo info = new JobInfo(name, KNOWLEDGE_SUITE_FORUM_JOBS, UpdateUserProfileJob.class);
      JobSchedulerService schedulerService = CommonsUtils.getService(JobSchedulerService.class);
      String repoName = SessionProviderService.getRepository().getConfiguration().getName();
      JobDataMap jdatamap = new JobDataMap();
      jdatamap.put(Utils.CACHE_REPO_NAME, repoName);
      infoMap.put(name, userPostMap);
      schedulerService.addPeriodJob(info, periodInfo, jdatamap);
    } catch (Exception e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Failed to add job for update user profile ", e);
      }
    }
  }

  public Topic removeTopic(String categoryId, String forumId, String topicId) {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      Node forumNode = getCategoryHome(sProvider).getNode(categoryId + "/" + forumId);
      Topic topic = getTopic(categoryId, forumId, topicId, UserProfile.USER_GUEST);
      Node topicNode = forumNode.getNode(topicId);
      PropertyReader readerFor = new PropertyReader(forumNode);
      Map<String, Long> userPostMap = getDeletePostByUser(sProvider, topicNode);
      if (topic.getIsApproved() && !topic.getIsWaiting()) {
        // update TopicCount for Forum
        forumNode.setProperty(EXO_TOPIC_COUNT, readerFor.l(EXO_TOPIC_COUNT) - 1);
        // update PostCount for Forum
        long newPostCount = readerFor.l(EXO_POST_COUNT) - (topic.getPostCount() + 1);
        forumNode.setProperty(EXO_POST_COUNT, (newPostCount > 0) ? newPostCount : 0);
      }
      topicNode.remove();
      forumNode.save();
      if (!topic.getIsActive() || !topic.getIsApproved() || topic.getIsWaiting()) {
        getTotalJobWatting(sProvider, new HashSet<String>(readerFor.list(EXO_MODERATORS, new ArrayList<String>())));
      }
      //
      addQueryLastPostTask(forumNode.getPath());
      try {
        calculateLastRead(sProvider, null, forumId, topicId);
      } catch (Exception e) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Failed to update last read topic", e);
        }
      }
      addUpdateUserProfileJob(userPostMap);
      return topic;
    } catch (Exception e) {
      LOG.error("Failed to remove topic", e);
      return null;
    }
  }

  private String getEmailUser(SessionProvider sProvider, String userId) throws Exception {
    return new PropertyReader(getUserProfileNode(sProvider, userId)).string(EXO_EMAIL, CommonUtils.EMPTY_STR);
  }

  public void moveTopic(List<Topic> topics, String destForumPath, String mailContent, String link) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    Node forumHomeNode = getForumHomeNode(sProvider);
    long tmp = 0;
    String forumName = null;
    Node destForumNode = (Node) forumHomeNode.getSession().getItem(destForumPath);
    PropertyReader destForumReader = new PropertyReader(destForumNode);
    forumName = destForumReader.string(EXO_NAME);
    String owner = destForumReader.string(EXO_OWNER);
    if (!Utils.isEmpty(owner) && owner.indexOf(":") > 0) {
      owner = ForumServiceUtils.getUserPermission(new String[] { owner }).get(0);
    }
    if (Utils.isEmpty(owner)) {
      owner = topics.get(0).getEditReason();
    }
    String categoryName = new PropertyReader(destForumNode.getParent()).string(EXO_NAME, "");
    String headerSubject = new StringBuilder("[").append(categoryName).append("][").append(forumName).append("] ").toString();
    MessageBuilder messageBuilder = getInfoMessageMove(sProvider, mailContent, headerSubject, true);
    messageBuilder.setCatName(categoryName);
    messageBuilder.setForumName(forumName);
    messageBuilder.setTopicName(CommonUtils.EMPTY_STR);   
    messageBuilder.setOwner(StringCommonUtils.decodeSpecialCharToHTMLnumber(getScreenName(sProvider, owner)));
    messageBuilder.setAddType(forumName);
    messageBuilder.setTypes(Utils.FORUM, Utils.TOPIC, CommonUtils.EMPTY_STR, CommonUtils.EMPTY_STR);
    // ----------------------- finish ----------------------
    String destForumId = destForumNode.getName(), srcForumId = CommonUtils.EMPTY_STR;
    for (Topic topic : topics) {
      String topicPath = topic.getPath();
      String newTopicPath = destForumPath + "/" + topic.getId();
      // Forum remove Topic(srcForum)
      Node srcForumNode = (Node) forumHomeNode.getSession().getItem(topicPath).getParent();
      srcForumId = srcForumNode.getName();
      // Move Topic
      forumHomeNode.getSession().getWorkspace().move(topicPath, newTopicPath);
      // Set TopicCount srcForum
      tmp = srcForumNode.getProperty(EXO_TOPIC_COUNT).getLong();
      if (tmp > 0)
        tmp = tmp - 1;
      else
        tmp = 0;
      srcForumNode.setProperty(EXO_TOPIC_COUNT, tmp);
      // setPath for srcForum
      addQueryLastPostTask(srcForumNode.getPath());
      // Topic Move
      Node topicNode = (Node) forumHomeNode.getSession().getItem(newTopicPath);
      topicNode.setProperty(EXO_PATH, destForumNode.getName());
      long topicPostCount = topicNode.getProperty(EXO_POST_COUNT).getLong() + 1;
      // Forum add Topic (destForum)
      destForumNode.setProperty(EXO_TOPIC_COUNT, destForumReader.l(EXO_TOPIC_COUNT) + 1);
      // setPath destForum
      addQueryLastPostTask(destForumNode.getPath());
      // Set PostCount
      tmp = srcForumNode.getProperty(EXO_POST_COUNT).getLong();
      if (tmp > topicPostCount)
        tmp = tmp - topicPostCount;
      else
        tmp = 0;
      srcForumNode.setProperty(EXO_POST_COUNT, tmp);
      destForumNode.setProperty(EXO_POST_COUNT, destForumReader.l(EXO_POST_COUNT) + topicPostCount);

      // send email after move topic:
      messageBuilder.setObjName(topic.getTopicName());
      messageBuilder.setHeaderSubject(messageBuilder.getHeaderSubject() + topic.getTopicName());
      messageBuilder.setLink(link.replaceFirst("pathId", topic.getId()));
      Set<String> set = new HashSet<String>();
      // set email author this topic
      set.add(getEmailUser(sProvider, topic.getOwner()));
      // set email watch this topic, forum, category parent of this topic
      set.addAll(calculateMoveEmail(topicNode));
      // set email watch old category, forum parent of this topic
      set.addAll(calculateMoveEmail(srcForumNode));
      if (!Utils.isEmpty(set.toArray(new String[set.size()]))) {
        sendEmailNotification(new ArrayList<String>(set), messageBuilder.getContentEmailMoved());
      }
      try {
        calculateLastRead(sProvider, destForumId, srcForumId, topic.getId());
      } catch (Exception e) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Failed to calculate last read", e);
        }
      }
    }
    if (forumHomeNode.isNew()) {
      forumHomeNode.getSession().save();
    } else {
      forumHomeNode.save();
    }
  }

  private Set<String> calculateMoveEmail(Node node) throws Exception {
    Set<String> set = new HashSet<String>();
    while (!node.getName().equals(KSDataLocation.Locations.FORUM_CATEGORIES_HOME)) {
      if (node.isNodeType(EXO_FORUM_WATCHING)) {
        set.addAll(new PropertyReader(node).list(EXO_EMAIL_WATCHING, new ArrayList<String>()));
      }
      node = node.getParent();
    }
    return set;
  }

  private void calculateLastRead(SessionProvider sProvider, String destForumId, String srcForumId, String topicId) throws Exception {
    Node profileHome = getUserProfileHome(sProvider);

    StringBuilder sqlQuery = jcrPathLikeAndNotLike(EXO_FORUM_USER_PROFILE, profileHome.getPath());
    sqlQuery.append(" AND ").append(EXO_LAST_READ_POST_OF_FORUM).append(" LIKE '%").append(topicId).append("%'");
    // sqlQuery.append("AND (CONTAINS (").append(EXO_LAST_READ_POST_OF_FORUM).append(", '").append(topicId).append("'))");

    NodeIterator iter = getNodeIteratorBySQLQuery(sProvider, sqlQuery.toString(), 0, 0, false);
    List<String> list;
    List<String> list2;
    while (iter.hasNext()) {
      list = new ArrayList<String>();
      list2 = new ArrayList<String>();
      Node profileNode = iter.nextNode();
      PropertyReader reader = new PropertyReader(profileNode);
      list = reader.list(EXO_LAST_READ_POST_OF_FORUM, new ArrayList<String>());
      list2 = new ArrayList<String>(list);

      boolean isRead = false;
      for (String string : list) {
        if (destForumId != null && string.indexOf(destForumId) >= 0) { // this forum is read, can check last access topic of forum and topic
          isRead = true;
          try {
            long lastAccessTopicTime = 0;
            long lastAccessForumTime = 0;
            // check last read of src topic
            List<String> readTopics = reader.list(EXO_READ_TOPIC, new ArrayList<String>());
            for (String tpId : readTopics) {// for only run one.
              String[] info = tpId.split(CommonUtils.COLON);
              if (tpId.indexOf(topicId) >= 0 && info.length > 1) {
                lastAccessTopicTime = Long.parseLong(info[1]);
                if (lastAccessTopicTime > 0) {// check last read dest forum
                  List<String> values = reader.list(EXO_READ_FORUM, new ArrayList<String>());
                  for (String str : values) {// for only run one.
                    if (str.indexOf(destForumId) >= 0) {
                      if (str.indexOf(CommonUtils.COLON) > 0) {
                        lastAccessForumTime = Long.parseLong(str.split(CommonUtils.COLON)[1]);
                        break;
                      }
                    }
                  }
                }
                if (lastAccessTopicTime > lastAccessForumTime) {
                  list2.remove(string);
                  list2.add(destForumId + CommonUtils.COMMA + info[0] + CommonUtils.SLASH + info[1]); // replace topic,postId
                }
                break;
              }
            }
          } catch (Exception e) {
            LOG.warn("Can not calculate last read of user: " + profileNode.getName());
          }
        }
        if (string.indexOf(srcForumId) >= 0) {// remove last read src forum if last read this forum is this topic.
          list2.remove(string);
        }
      }
      if (!isRead && destForumId != null) {
        list2.add(destForumId + CommonUtils.COMMA + topicId + CommonUtils.SLASH + topicId.replace(Utils.TOPIC, Utils.POST));
      }
      profileNode.setProperty(EXO_LAST_READ_POST_OF_FORUM, list2.toArray(new String[list2.size()]));
    }
    profileHome.save();
  }

  public long getLastReadIndex(String path, String isApproved, String isHidden, String userLogin) {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      Node catNode = getCategoryHome(sProvider);
      Node postNode = catNode.getNode(path);
      if (postNode != null) {
        String topicPath = postNode.getParent().getPath();

        StringBuilder sqlQuery = jcrPathLikeAndNotLike(EXO_POST, topicPath);

        String query = Utils.getSQLQuery(isApproved, isHidden, isHidden, userLogin).toString();
        if (query.isEmpty() == false) {
          sqlQuery.append(" AND (").append(query).append(")");
        }
        Calendar cal = postNode.getProperty(EXO_CREATED_DATE).getDate();
        sqlQuery.append(" AND (").append(EXO_CREATED_DATE).append(" <= TIMESTAMP '").append(ISO8601.format(cal)).append("')");
        NodeIterator iter = getNodeIteratorBySQLQuery(sProvider, sqlQuery.toString(), 0, 0, false);
        long size = iter.getSize();
        boolean isView = false;
        while (iter.hasNext()) {
          if (iter.nextNode().getName().equals(postNode.getName())) {
            isView = true;
            break;
          }
        }
        // if user can not view post open, return page 1.
        if (isView == false) {
          size = 1;
        }
        return size;
      }
    } catch (Exception e) {
      LOG.error("Exception occurs when getting last read index", e);
    }
    return 0;
  }

  /**
   * @deprecated use {@link #getPostsSplitTopic(PostFilter, int, int)}
   */
  public JCRPageList getPostForSplitTopic(String topicPath) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      PostFilter filter = new PostFilter(topicPath);
      String sqlQuery = makePostsSplitSQLQuery(filter, true);

      NodeIterator iter = getNodeIteratorBySQLQuery(sProvider, sqlQuery, 0, 0, false);
      return new ForumPageList(iter, 10, sqlQuery.toString(), true);
    } catch (PathNotFoundException e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Failed to get post for split topic.", e);
      }
    }
    return null;
  }

  private String makePostsSplitSQLQuery(PostFilter filter, boolean hasOrder) throws Exception {
    StringBuilder topicPath = new StringBuilder();
    if (filter.getTopicPath().indexOf(dataLocator.getForumCategoriesLocation()) > 0) {
      topicPath.append(filter.getTopicPath());
    } else {
      topicPath.append("/").append(dataLocator.getForumCategoriesLocation()).append("/").append(filter.getTopicPath());
    }
    StringBuilder sqlQuery = jcrPathLikeAndNotLike(EXO_POST, topicPath.toString());

    sqlQuery.append(Utils.getSQLQueryByProperty("AND", EXO_USER_PRIVATE, EXO_USER_PRI))
            .append(Utils.getSQLQueryByProperty("AND", EXO_IS_FIRST_POST, "false"));
    if (hasOrder) {
      sqlQuery.append(" ORDER BY ").append(EXO_CREATED_DATE).append(" ASC");
    }

    return sqlQuery.toString();
  }

  public List<Post> getPostsSplitTopic(PostFilter filter, int offset, int limit) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      String sqlQuery = makePostsSplitSQLQuery(filter, true);
      //
      NodeIterator iter = getNodeIteratorBySQLQuery(sProvider, sqlQuery, 0, 0, false);
      return getPosts(iter);
    } catch (PathNotFoundException e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Failed to get post for split topic.", e);
      }
    }
    return null;
  }

  /**
   * @deprecated use {@link #getPosts(PostFilter, int, int)}
   */
  public JCRPageList getPosts(String categoryId, String forumId, String topicId, String isApproved, String isHidden, String strQuery, String userLogin) throws Exception {
    try {
      String isWaiting = strQuery.equals("true") || strQuery.equals("false") ? strQuery : "";
      PostFilter filter = new PostFilter(categoryId, forumId, topicId, isApproved, isHidden, isWaiting, userLogin);

      if (!Utils.isEmpty(strQuery)) {
        LOG.warn("This method doesn't support to add more query.");
      }

      return new ForumPageList(null, 10, makePostsSQLQuery(filter, true), true);
    } catch (PathNotFoundException e) {
      return null;
    }
  }

  private String makePostsSQLQuery(PostFilter filter, boolean hasOrder) throws Exception {
    String topicPath = filter.getTopicPath();
    if (Utils.isEmpty(topicPath)) {
      topicPath = new StringBuffer("/").append(dataLocator.getForumCategoriesLocation()).append("/").append(filter.getCategoryId())
                                        .append("/").append(filter.getForumId()).append("/").append(filter.getTopicId()).toString();
    }

    StringBuilder strBuilder = jcrPathLikeAndNotLike(EXO_POST, topicPath);

    String sqlQuery = Utils.getSQLQuery(filter.getIsApproved(), filter.getIsHidden(), filter.getIsWaiting(), filter.getUserLogin()).toString();
    if (sqlQuery.isEmpty() == false) {
      strBuilder.append(" AND (").append(sqlQuery).append(")");
    }
    if (hasOrder) {
      strBuilder.append(" ORDER BY ").append(EXO_CREATED_DATE).append(ASC);
    }

    return strBuilder.toString();
  }

  @Override
  public List<Post> getPosts(PostFilter filter, int offset, int limit) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      NodeIterator iter = getNodeIteratorBySQLQuery(sProvider, makePostsSQLQuery(filter, true), offset, limit, false);
      return getPosts(iter);
    } catch (Exception e) {
      logDebug("Failed to get posts by filter of topic " + filter.getTopicId(), e);
      return new ArrayList<Post>();
    }
  }

  private List<Post> getPosts(NodeIterator iter) throws Exception {
    Node currentNode = null;
    List<Post> posts = new ArrayList<Post>((int) iter.getSize());
    //
    DataStorage dataStorage = getCachedDataStorage();
    boolean hasCache = (dataStorage instanceof CachedDataStorage);
    while (iter.hasNext()) {
      currentNode = iter.nextNode();
      if (hasCache) {
        String path = currentNode.getPath();
        CachedDataStorage storage = (CachedDataStorage) dataStorage;
        String categoryId = Utils.getCategoryId(path), forumId = Utils.getForumId(path), topicId = Utils.getTopicId(path);
        Post post = storage.getPostFromCache(categoryId, forumId, topicId, currentNode.getName());
        if (post == null) {
          post = getPost(currentNode);
          storage.putPost(post);
        }
        posts.add(post);
      } else {
        posts.add(getPost(currentNode));
      }
    }
    return posts;
  }

  public int getPostsCount(PostFilter filter) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      if (filter.isSplit()) {
        return (int) getNodeIteratorBySQLQuery(sProvider, makePostsSplitSQLQuery(filter, false), 0, 0, false).getSize();
      } else if (Utils.isEmpty(filter.userName()) == false) {
        return (int) getNodeIteratorBySQLQuery(sProvider, queryPostsByUser(filter, false), 0, 0, false).getSize();
      } else if (Utils.isEmpty(filter.getIP()) == false) {
        return (int) getNodeIteratorBySQLQuery(sProvider, queryPostsByIP(filter, false), 0, 0, false).getSize();
      }
      //
      return (int) getNodeIteratorBySQLQuery(sProvider, makePostsSQLQuery(filter, false), 0, 0, false).getSize();
    } catch (Exception e) {
      return 0;
    }
  }

  public long getAvailablePost(String categoryId, String forumId, String topicId, String isApproved, String isHidden, String userLogin) throws Exception {
    PostFilter filter = new PostFilter(categoryId, forumId, topicId, isApproved, isHidden, isHidden, userLogin);
    return getPostsCount(filter);
  }

  public JCRPageList getPagePostByUser(String userName, String userId, boolean isMod, String strOrderBy) throws Exception {
    if (Utils.isEmpty(userName)) {
      throw new NullPointerException("userName");
    }

    if (Utils.isEmpty(userId)) {
      throw new NullPointerException("userLogin");
    }
    
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      String query = queryPostsByUser(new PostFilter(userName, userId, isMod, strOrderBy), true);
      NodeIterator iter = getNodeIteratorBySQLQuery(sProvider, query, 0, 0, false);
      return new ForumPageList(iter, 10, query, true);
    } catch (Exception e) {
      return null;
    }
  }

  private String queryPostsByUser(PostFilter filter, boolean hasOrder) {
    StringBuilder sqlQuery = new StringBuilder("SELECT * FROM ").append(EXO_POST);
    sqlQuery.append(" WHERE ")
            .append(Utils.getSQLQueryByProperty("", EXO_IS_FIRST_POST, "false"))
            .append(Utils.getSQLQueryByProperty("AND", EXO_OWNER, filter.userName()));

    if (filter.isAdmin() == false) {
      sqlQuery.append(Utils.getSQLQueryByProperty("AND", EXO_IS_APPROVED, "true"))
              .append(Utils.getSQLQueryByProperty("AND", EXO_IS_HIDDEN, "false"))
              .append(Utils.getSQLQueryByProperty("AND", EXO_IS_ACTIVE_BY_TOPIC, "true"))
              .append(Utils.getSQLQueryByProperty("AND", EXO_IS_WAITING, "false"));
    }
    sqlQuery.append(" AND (")
            .append(Utils.getSQLQueryByProperty("", EXO_USER_PRIVATE, filter.getUserLogin()))
            .append(Utils.getSQLQueryByProperty("OR", EXO_USER_PRIVATE, EXO_USER_PRI))
            .append(")");

    if (hasOrder) {
      sqlQuery.append(" ORDER BY ");
      if (!Utils.isEmpty(filter.orderBy())) {
        sqlQuery.append(filter.orderBy());
        if (filter.orderBy().indexOf(EXO_CREATED_DATE) < 0) {
          sqlQuery.append(", ").append(EXO_CREATED_DATE).append(DESC);
        }
      } else {
        sqlQuery.append(EXO_CREATED_DATE).append(DESC);
      }
    }

    return sqlQuery.toString();
  }

  public List<Post> getPostsByUser(PostFilter filter, int offset, int limit) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      String sqlQuery = queryPostsByUser(filter, true);
      NodeIterator iter = getNodeIteratorBySQLQuery(sProvider, sqlQuery, offset, limit, false);
      return getPosts(iter);
    } catch (Exception e) {
      return null;
    }
  }

  public Post getPost(String categoryId, String forumId, String topicId, String postId) throws Exception {
    if (StringUtils.isEmpty(postId)) {
      return null;
    }

    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      Node categoryHome = getCategoryHome(sProvider);
      Node postNode;      
      if (postId.lastIndexOf("/") > 0 || StringUtils.isEmpty(categoryId) || StringUtils.isEmpty(forumId) || StringUtils.isEmpty(topicId)) {
        if (postId.indexOf(categoryHome.getName()) < 0)
          postId = categoryHome.getPath() + "/" + postId;
        postNode = (Node) categoryHome.getSession().getItem(postId);
      } else {
        postNode = categoryHome.getNode(categoryId + "/" + forumId + "/" + topicId + "/" + postId);
      }
      return getPost(postNode);
    } catch (PathNotFoundException e) {
      return null;
    }
  }

  public JCRPageList getListPostsByIP(String ip, String strOrderBy) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      String pathQuery = queryPostsByIP(new PostFilter(ip, strOrderBy), true);
      //
      NodeIterator iter = getNodeIteratorBySQLQuery(sProvider, pathQuery, 0, 0, false);
      return new ForumPageList(iter, 5, pathQuery, true);
    } catch (Exception e) {
      return null;
    }
  }

  private String queryPostsByIP(PostFilter filter, boolean hasOrder) {
    StringBuilder sqlQuery = new StringBuilder("SELECT ").append(EXO_REMOTE_ADDR).append(" FROM ").append(EXO_POST);

    sqlQuery.append(" WHERE ").append(Utils.getSQLQueryByProperty("", EXO_REMOTE_ADDR, filter.getIP()));
    if (hasOrder) {
      if (Utils.isEmpty(filter.orderBy())) {
        sqlQuery.append("  ORDER BY ").append(EXO_LAST_POST_DATE).append(DESC);
      } else {
        sqlQuery.append(" ORDER BY exo:").append(filter.orderBy());
        if (EXO_LAST_POST_DATE.indexOf(filter.orderBy()) < 0) {
          sqlQuery.append(", ").append(EXO_LAST_POST_DATE).append(DESC);
        }
      }
    }
    return sqlQuery.toString();
  }

  public List<Post> getPostsByIP(PostFilter filter, int offset, int limit) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      NodeIterator iter = getNodeIteratorBySQLQuery(sProvider, queryPostsByIP(filter, true), offset, limit, false);
      List<Post> posts = new ArrayList<Post>((int) iter.getSize());
      Node currentNode;
      while (iter.hasNext()) {
        currentNode = iter.nextNode();
        posts.add(getPost(currentNode));
      }
      return posts;
    } catch (Exception e) {
      return null;
    }
  }

  public Post getPost(Node postNode) throws Exception {
    Post postNew = new Post();
    PropertyReader reader = new PropertyReader(postNode);
    postNew.setId(postNode.getName());
    postNew.setPath(postNode.getPath());

    postNew.setOwner(reader.string(EXO_OWNER));
    postNew.setCreatedDate(reader.date(EXO_CREATED_DATE));
    postNew.setModifiedBy(reader.string(EXO_MODIFIED_BY));
    postNew.setModifiedDate(reader.date(EXO_MODIFIED_DATE));
    postNew.setEditReason(reader.string(EXO_EDIT_REASON));
    postNew.setName(reader.string(EXO_NAME));
    postNew.setMessage(reader.string(EXO_MESSAGE));
    postNew.setRemoteAddr(reader.string(EXO_REMOTE_ADDR));
    postNew.setIcon(reader.string(EXO_ICON));
    postNew.setLink(reader.string(EXO_LINK));
    postNew.setIsApproved(reader.bool(EXO_IS_APPROVED));
    postNew.setIsHidden(reader.bool(EXO_IS_HIDDEN));
    postNew.setIsWaiting(reader.bool(EXO_IS_WAITING));
    postNew.setFirstPost(reader.bool(EXO_IS_FIRST_POST));
    postNew.setIsActiveByTopic(reader.bool(EXO_IS_ACTIVE_BY_TOPIC));
    postNew.setUserPrivate(reader.strings(EXO_USER_PRIVATE));
    postNew.setNumberAttach(reader.l(EXO_NUMBER_ATTACH));
    if (postNew.getNumberAttach() > 0) {
      postNew.setAttachments(getAttachmentsByNode(postNode));
    }
    return postNew;
  }

  private static ForumAttachment getAttachment(Node nodeContent) throws Exception {
    try {
      if (nodeContent.isNodeType(EXO_FORUM_ATTACHMENT) || nodeContent.isNodeType(NT_FILE)) {
        BufferAttachment attachment = new BufferAttachment();
        PropertyReader readerContent = new PropertyReader(nodeContent.getNode(JCR_CONTENT));
        attachment.setId(nodeContent.getName());
        attachment.setPathNode(nodeContent.getPath());
        attachment.setMimeType(readerContent.string(JCR_MIME_TYPE, CommonUtils.EMPTY_STR));
        attachment.setSize(readerContent.stream(JCR_DATA).available());
        String workspace = nodeContent.getSession().getWorkspace().getName();
        attachment.setWorkspace(workspace);
        attachment.setPath(CommonUtils.SLASH + workspace + nodeContent.getPath());
        String fileName = readerContent.string(EXO_FILE_NAME);
        if (CommonUtils.isEmpty(fileName)) {
          String type = attachment.getMimeType();
          if (type.indexOf(CommonUtils.SLASH) > 0) {
            type = type.substring(type.indexOf(CommonUtils.SLASH) + 1);
          }
          fileName = "avatar." + type;
        }
        attachment.setName(fileName);
        return attachment;
      }
    } catch (Exception e) {
      logDebug("Failed to get attachment in node: " + nodeContent.getName());
    }
    return null;
  }

  public static List<ForumAttachment> getAttachmentsByNode(Node node) throws Exception {
    List<ForumAttachment> attachments = new ArrayList<ForumAttachment>();
    NodeIterator postAttachments = node.getNodes();
    while (postAttachments.hasNext()) {
      Node nodeAttatch = postAttachments.nextNode();
      ForumAttachment attachment = getAttachment(nodeAttatch);
      if (attachment != null) {
        attachments.add(attachment);
      }
    }
    return attachments;
  }
  /**
  * This method is call by StatisticEventListener to update user's profile when new post is added
  *
  * @param owner user's name of an user that create the post
  * @param postPath node's path of the last post
  */
  public void updateProfileAddPost(String owner, String postPath) {
    ReentrantLock lock = this.lock;
    SessionProvider sProvider = SessionProvider.createSystemProvider();
    try {
      lock.lock();
      Node profileHomeNode = getUserProfileHome(sProvider);
      Calendar lastPost = getGreenwichMeanTime();
      Node postNode = (Node) profileHomeNode.getSession().getItem(postPath);
      PropertyReader reader = new PropertyReader(postNode);
      lastPost.setTime(reader.date(EXO_CREATED_DATE, lastPost.getTime()));
      //
      if (profileHomeNode.hasNode(owner)) {
        Node profileNode = profileHomeNode.getNode(owner);
        long totalPostByUser = 0;
        totalPostByUser = profileNode.getProperty(EXO_TOTAL_POST).getLong();
        profileNode.setProperty(EXO_TOTAL_POST, totalPostByUser + 1);
        profileNode.setProperty(EXO_LAST_POST_DATE, lastPost);
      } else if (Utils.isEmpty(owner) == false) {
        Node profileNode = profileHomeNode.addNode(owner, EXO_FORUM_USER_PROFILE);
        profileNode.setProperty(EXO_USER_ID, owner);
        profileNode.setProperty(EXO_USER_TITLE, Utils.USER);
        if (isAdminRole(sProvider, owner)) {
          profileNode.setProperty(EXO_USER_TITLE, Utils.ADMIN);
        }
        profileNode.setProperty(EXO_TOTAL_POST, 1);
        profileNode.setProperty(EXO_LAST_POST_DATE, lastPost);
      }
      profileHomeNode.getSession().save();
    } catch (Exception e) {
      LOG.warn("Failed to save user profile of user: " + owner, e);
    } finally {
      lock.unlock();
      sProvider.close();
    }
  }

  private void postSaveProperties(Node postNode, Post post) throws Exception {
    if (post.getModifiedBy() != null && post.getModifiedBy().length() > 0) {
      postNode.setProperty(EXO_MODIFIED_BY, post.getModifiedBy());
      postNode.setProperty(EXO_MODIFIED_DATE, getGreenwichMeanTime());
      postNode.setProperty(EXO_EDIT_REASON, post.getEditReason());
    }
    postNode.setProperty(EXO_NAME, post.getName());
    postNode.setProperty(EXO_MESSAGE, post.getMessage());
    postNode.setProperty(EXO_REMOTE_ADDR, post.getRemoteAddr());
    postNode.setProperty(EXO_ICON, post.getIcon());
    postNode.setProperty(EXO_IS_APPROVED, post.getIsApproved());
    postNode.setProperty(EXO_IS_HIDDEN, post.getIsHidden());
    postNode.setProperty(EXO_IS_WAITING, post.getIsWaiting());
    postNode.setProperty(EXO_NUMBER_ATTACH, post.getNumberAttach());
    postNode.setProperty(EXO_USER_PRIVATE, post.getUserPrivate());
  }

  private List<String> postAttachment(Node postNode, Post post) {
    List<String> listFileName = new ArrayList<String>();
    List<ForumAttachment> attachments = post.getAttachments();
    if (attachments != null) {
      Iterator<ForumAttachment> it = attachments.iterator();
      for (ForumAttachment attachment : attachments) {
        BufferAttachment file = null;
        listFileName.add(attachment.getId());
        try {
          file = (BufferAttachment) it.next();
          Node nodeFile = null;
          if (!postNode.hasNode(file.getId()))
            nodeFile = postNode.addNode(file.getId(), EXO_FORUM_ATTACHMENT);
          else
            nodeFile = postNode.getNode(file.getId());
          // Fix permission node
          ForumServiceUtils.reparePermissions(nodeFile, "any");
          Node nodeContent = null;
          if (!nodeFile.hasNode(JCR_CONTENT)) {
            nodeContent = nodeFile.addNode(JCR_CONTENT, EXO_FORUM_RESOURCE);
            nodeContent.setProperty(JCR_MIME_TYPE, file.getMimeType());
            nodeContent.setProperty(JCR_DATA, file.getInputStream());
            nodeContent.setProperty(JCR_LAST_MODIFIED, getGreenwichMeanTime().getTimeInMillis());
            nodeContent.setProperty(EXO_FILE_NAME, file.getName());
          }
        } catch (Exception e) {
          LOG.error("Failed to save attachment", e);
        }
      }
    }
    return listFileName;
  }

  public void savePost(String categoryId, String forumId, String topicId, Post post, boolean isNew, MessageBuilder messageBuilder) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      Node categoryNode = getCategoryHome(sProvider).getNode(categoryId);
      Node forumNode = categoryNode.getNode(forumId);
      Node topicNode = forumNode.getNode(topicId);
      Node postNode;
      if (isNew) {
        postNode = addPost(sProvider, categoryNode, forumNode, topicNode, post);
      } else {
        postNode = updatePost(sProvider, topicNode, post);
      }

      //
      try {
        forumNode.getSession().save();
      } catch (InvalidItemStateException e) {
        LOG.warn("Probably was save post by another session");
        post = null;
        return;
      }
      if (isNew) {
        addQueryLastPostTask(forumNode.getPath());
      }
      //
      post.setPath(postNode.getPath());
      //
      boolean sendAlertJob = (!messageBuilder.getLink().equals("link") && (post.getUserPrivate().length == 1) &&
                              (!post.getIsApproved() || post.getIsHidden() || post.getIsWaiting()));
      
      if (topicNode.getName().replaceFirst(Utils.TOPIC, Utils.POST).equals(post.getId()) == false && isNew) {
        addNotificationTask(topicNode.getPath(), null, post, messageBuilder, !sendAlertJob);
      }
      
      //
      if (sendAlertJob) {
        getTotalJobWatting(sProvider, new HashSet<String>(new PropertyReader(forumNode).list(EXO_MODERATORS, new ArrayList<String>())));
      }
      // send notification message to user's private post.
      if (post != null && post.getUserPrivate().length > 1) {
        ForumPrivateMessage message = new ForumPrivateMessage();
        message.setFrom(post.getOwner());
        message.setSendTo(String.join(",",post.getUserPrivate()));
        message.setType("PrivatePost");
        message.setName(post.getName());
        message.setMessage(post.getMessage());
        message.setId(topicId + "/" + post.getId());
        sendNotificationMessage(message);
      }
    } catch (Exception e) {
      LOG.error("Failed to save post " + post.getName(), e);
      post = null;
    }
  }

  private Node addPost(SessionProvider sProvider, Node categoryNode, Node forumNode, Node topicNode, Post post) throws Exception {
    Node postNode = topicNode.addNode(post.getId(), EXO_POST);
    Calendar calendar = getGreenwichMeanTime();
    postNode.setProperty(EXO_ID, post.getId());
    postNode.setProperty(EXO_PATH, forumNode.getName());
    postNode.setProperty(EXO_OWNER, post.getOwner());
    post.setCreatedDate(calendar.getTime());
    postNode.setProperty(EXO_CREATED_DATE, calendar);
    postNode.setProperty(EXO_USER_PRIVATE, post.getUserPrivate());
    postNode.setProperty(EXO_LINK, CommonUtils.getURI(post.getLink()));

    boolean isFistPost = topicNode.getName().replaceFirst(Utils.TOPIC, Utils.POST).equals(post.getId());
    postNode.setProperty(EXO_IS_FIRST_POST, isFistPost);

    //
    postSaveProperties(postNode, post);
    //
    postAttachment(postNode, post);

    return postNode;
  }
  
  
  /**
   * Update the number of post and also the information about the last post of topic when a new post is added
   * 
   * @param postPath the jcr's path of the new post
   * @param owner the owner of the new post
   * @throws Exception
   */
  public void updatePostCount(String postPath, String owner) throws Exception {
    final ReentrantLock localLock = lock;
    SessionProvider sProvider = SessionProvider.createSystemProvider();
    try {
      localLock.lock();
      Session session = sessionManager.getSession(sProvider); 
      Node postNode = (Node) session.getItem(postPath);
      Node topicNode = postNode.getParent();
      Node forumNode = topicNode.getParent();
      //
      boolean isFirstPost = topicNode.getName().replaceFirst(Utils.TOPIC, Utils.POST).equals(postNode.getName());
      PropertyReader postRead = new PropertyReader(postNode);
      PropertyReader topicRead = new PropertyReader(topicNode);
      PropertyReader forumRead = new PropertyReader(forumNode);
      //
      long topicPostCount = topicRead.l(EXO_POST_COUNT) + 1;
      long newNumberAttach = topicRead.l(EXO_NUMBER_ATTACHMENTS) + postRead.l(EXO_NUMBER_ATTACH);

      boolean topicActive = (!topicRead.bool(EXO_IS_CLOSED) && !topicRead.bool(EXO_IS_WAITING) && 
                              topicRead.bool(EXO_IS_APPROVED) && topicRead.bool(EXO_IS_ACTIVE) &&
                              topicRead.bool(EXO_IS_ACTIVE_BY_FORUM));

      boolean postActive =  (postRead.bool(EXO_IS_APPROVED) && postRead.bool(EXO_IS_HIDDEN) == false && 
                              postRead.bool(EXO_IS_WAITING) == false && postRead.list(EXO_USER_PRIVATE).size() != 2);

      // set active by topic
      postNode.setProperty(EXO_IS_ACTIVE_BY_TOPIC, (topicActive || isFirstPost));

      // update forum
      if ((isFirstPost && !forumRead.bool(EXO_IS_MODERATE_TOPIC)) || (!isFirstPost && topicActive)) {
        long forumPostCount = forumRead.l(EXO_POST_COUNT) + 1;
        forumNode.setProperty(EXO_POST_COUNT, Math.max(forumPostCount, 1));
      }

      // update topic
      if (postActive) {
        topicNode.setProperty(EXO_POST_COUNT, Math.max(topicPostCount, 0));
        topicNode.setProperty(EXO_LAST_POST_DATE, getGreenwichMeanTime());
        topicNode.setProperty(EXO_LAST_POST_BY, owner);
        topicNode.setProperty(EXO_NUMBER_ATTACHMENTS, newNumberAttach);
      }
      //
      session.save();
    } catch (Exception e) {
      LOG.warn("Failed to update forum post count when save post");
    } finally {
      sProvider.close();
      localLock.unlock();
    }
  }

  private Node updatePost(SessionProvider sProvider, Node topicNode, Post post) throws Exception {
    Node postNode = topicNode.getNode(post.getId());
    long oldNumberAttachments = postNode.getProperty(EXO_NUMBER_ATTACH).getLong();
    //
    postSaveProperties(postNode, post);

    //
    List<String> listFileName = postAttachment(postNode, post);
    // remove old attachments
    NodeIterator postAttachments = postNode.getNodes();
    Node postAttachmentNode = null;
    while (postAttachments.hasNext()) {
      postAttachmentNode = postAttachments.nextNode();
      if (listFileName.contains(postAttachmentNode.getName())) {
        continue;
      }
      postAttachmentNode.remove();
    }

    long temp = topicNode.getProperty(EXO_NUMBER_ATTACHMENTS).getLong() - oldNumberAttachments;
    topicNode.setProperty(EXO_NUMBER_ATTACHMENTS, (temp + post.getNumberAttach()));
    //
    return postNode;
  }

  private boolean hasProperty(Node node, String property) throws Exception {
    String[] strs = new PropertyReader(node).strings(property, new String[] {});
    return strs.length > 0;
  }

  private void addNotificationTask(String nodePath, Topic topic, Post post, MessageBuilder messageBuilder, boolean isApprovePost) throws Exception {
    SendNotificationTaskManager sendNotificationManager = getSendNotificationTaskManager();
    if (sendNotificationManager != null) {
      sendNotificationManager.addTask(new SendNotificationTask(nodePath, topic, post, messageBuilder, isApprovePost));
    }
  }

  /**
   * Check if a specific User have the permission to receive the email notification
   * 
   * @param topicNode the underlying watched topic 
   * @param user the user watching the topic
   * @param postAuthor the owner of the new post
   * @throws Exception
   */
  private boolean canReceiveNotification(Node topicNode, String user, String postAuthor) throws Exception {
    //if the watching user is the one who replied to the topic then don't send notification
    if(user==null || user.equals(postAuthor)){
      return false;
    }
    Node forumNode = topicNode.getParent();
    PropertyReader reader = new PropertyReader(forumNode);
    // viewer of topic
    Set<String> viewers = new PropertyReader(topicNode).set(EXO_CAN_VIEW, new HashSet<String>());
    // viewer of forum
    viewers.addAll(reader.set(EXO_VIEWER, new HashSet<String>()));
    // forum of space - only check permission on forum
    String[] moderators = reader.strings(EXO_MODERATE_FORUMS, new String[] {});
    if (forumNode.getParent().getName().equals(Utils.CATEGORY_SPACE_ID_PREFIX)) {
      // moderators
      if (!CommonUtils.isEmpty(moderators) && ForumServiceUtils.hasPermission(moderators, user)) {
        return true;
      }
    } else {
      // administrators or moderators
      if (isAdminRole(user) || (!CommonUtils.isEmpty(moderators) && ForumServiceUtils.hasPermission(moderators, user))) {
        return true;
      }
      // private categories
      String[] userPrivates = new PropertyReader(forumNode.getParent()).strings(EXO_USER_PRIVATE, new String[] {});
      if (!ForumServiceUtils.hasPermission(userPrivates, user)) {
        return false;
      }
      // all viewers on category
      viewers.addAll(new PropertyReader(forumNode.getParent()).list(EXO_VIEWER, new ArrayList<String>()));
    }
    return ForumServiceUtils.hasPermission(viewers.toArray(new String[viewers.size()]), user);
  }
  
  private void sendNotificationWhenCreateTopic(SessionProvider sProvider, Node forumNode, Topic topic, MessageBuilder messageBuilder) throws Exception {
    messageBuilder.setForumName(forumNode.getProperty(EXO_NAME).getString());
    Node categoryNode = forumNode.getParent();
    messageBuilder.setCatName(categoryNode.getProperty(EXO_NAME).getString());
    messageBuilder.setTopicName(topic.getTopicName());
    //
    List<String> emailList = new ArrayList<String>();
    List<String> emailListCate = new ArrayList<String>();
    Node node = categoryNode;
    Node topicNode = forumNode.getNode(topic.getId());
    while (true) {
      emailListCate.addAll(emailList);
      emailList = new ArrayList<String>();
      if (node.isNodeType(EXO_FORUM_WATCHING) && topic.getIsActive() && topic.getIsApproved() && topic.getIsActiveByForum() && !topic.getIsClosed() && !topic.getIsLock() && !topic.getIsWaiting()) {
        PropertyReader reader = new PropertyReader(node);
        List<String> users = reader.list(EXO_USER_WATCHING, new ArrayList<String>());
        if (!users.isEmpty()) {
          int i = 0;
          String[] emails = reader.strings(EXO_EMAIL_WATCHING, new String[] {});
          for (String user : users) {
            if (user.equals(topic.getOwner()) || canReceiveNotification(topicNode, user, null)) {
              emailList.add(emails[i]);
            }
            i++;
          }
        }
        emailList.addAll(reader.list(EXO_NOTIFY_WHEN_ADD_TOPIC, new ArrayList<String>()));
      }
      //
      emailList.removeAll(emailListCate);
      if (emailList.size() > 0) {
        String owner = getScreenName(sProvider, topic.getOwner());
        messageBuilder.setObjName(node.getProperty(EXO_NAME).getString());
        if (node.isNodeType(EXO_FORUM)) {
          messageBuilder.setWatchType(Utils.FORUM);
        } else {
          messageBuilder.setWatchType(Utils.CATEGORY);
        }
        messageBuilder.setId(topic.getId().replaceFirst(Utils.TOPIC, Utils.POST));
        messageBuilder.setAddType(Utils.TOPIC);
        messageBuilder.setAddName(topic.getTopicName());
        messageBuilder.setMessage(topic.getDescription());
        messageBuilder.setCreatedDate(topic.getCreatedDate());
        messageBuilder.setOwner(StringCommonUtils.decodeSpecialCharToHTMLnumber(owner));
        if(Utils.isEmpty(messageBuilder.getLink())) {
          messageBuilder.setLink(topic.getLink());
        }
        sendEmailNotification(emailList, messageBuilder.getContentEmail());
      }
      if(node.isNodeType(EXO_FORUM)) {
        break;
      }
      node = forumNode;
    }
  }
  
  private void sendNotificationWhenCreatePost(SessionProvider sProvider, Node topicNode, Post post, MessageBuilder messageBuilder, boolean isApprovePost) throws Exception {
    if (!topicNode.getName().replaceFirst(Utils.TOPIC, Utils.POST).equals(post.getId())) {
      Node userProfileHome = getUserProfileHome(sProvider);
      Node forumNode = topicNode.getParent();
      Node categoryNode = forumNode.getParent();
      messageBuilder.setCatName(categoryNode.getProperty(EXO_NAME).getString());
      messageBuilder.setForumName(forumNode.getProperty(EXO_NAME).getString());
      messageBuilder.setTopicName(topicNode.getProperty(EXO_NAME).getString());
      /*
       * check is approved, is activate by topic and is not hidden before send mail
       */
      if (post.getIsApproved() && post.getIsActiveByTopic() && !post.getIsHidden() && !post.getIsWaiting()) {
        List<String> userPrivates = new ArrayList<String>();
        if (!CommonUtils.isEmpty(post.getUserPrivate()) && post.getUserPrivate().length > 1) {
          userPrivates.addAll(Arrays.asList(post.getUserPrivate()));
        }

        PropertyReader catReader = new PropertyReader(categoryNode);
        PropertyReader forumReader = new PropertyReader(forumNode);
        PropertyReader topicReader = new PropertyReader(topicNode);
        // get all emails watched
        List<String> emailListCategory = catReader.list(EXO_EMAIL_WATCHING, new ArrayList<String>());
        List<String> emailListForum = forumReader.list(EXO_EMAIL_WATCHING, new ArrayList<String>());
        List<String> emailListTopic = topicReader.list(EXO_EMAIL_WATCHING, new ArrayList<String>());
        //
        List<String> userListCategory = catReader.list(EXO_USER_WATCHING, new ArrayList<String>());
        List<String> userListForum = forumReader.list(EXO_USER_WATCHING, new ArrayList<String>());
        List<String> userListTopic = topicReader.list(EXO_USER_WATCHING, new ArrayList<String>());
        // validate permission and remove duplicate email
        // Watched on category
        int i = 0;
        List<String> removeEmail = new ArrayList<String>();
        for (String user : userListCategory) {
          if(!canReceiveNotification(topicNode, user, post.getOwner())  || (userPrivates.size() > 1 && !userPrivates.contains(user))) {
            removeEmail.add(emailListCategory.get(i));
          }
          ++i;
        }
        emailListCategory.removeAll(removeEmail);
        removeEmail.clear();
        // Watched on forum
        i = 0;
        for (String user : userListForum) {
          if(userListCategory.contains(user) || (userPrivates.size() > 1 && !userPrivates.contains(user))
              || !canReceiveNotification(topicNode, user, post.getOwner())) {
            removeEmail.add(emailListForum.get(i));
          }
          ++i;
        }
        emailListForum.removeAll(removeEmail);
        removeEmail.clear();
        // Watched on topic
        i = 0;
        for (String user : userListTopic) {
          if(userListCategory.contains(user)
              || userListForum.contains(user)  || (userPrivates.size() > 1 && !userPrivates.contains(user))
              || !canReceiveNotification(topicNode, user, post.getOwner())) {
            removeEmail.add(emailListTopic.get(i));
          }
          ++i;
        }
        emailListTopic.removeAll(removeEmail);
        // Owner Notify
        if (isApprovePost && post.getUserPrivate() != null && post.getUserPrivate().length == 1) {
          String owner = topicReader.string(EXO_OWNER);
          String ownerTopicEmail = topicReader.string(EXO_IS_NOTIFY_WHEN_ADD_POST, StringUtils.EMPTY);
          if (!CommonUtils.isEmpty(ownerTopicEmail)) {
            try {
              Node userOwner = userProfileHome.getNode(owner);
              PropertyReader userReader = new PropertyReader(userOwner);
              String email = userReader.string(EXO_EMAIL, StringUtils.EMPTY);
              if(userReader.bool(EXO_IS_BANNED)) {
                ownerTopicEmail = StringUtils.EMPTY;
              } else if (!Utils.isEmpty(email)) {
                ownerTopicEmail = email;
              }
            } catch (PathNotFoundException e) {
              // owner not existing
              ownerTopicEmail = StringUtils.EMPTY;
            }
          }
          if(!CommonUtils.isEmpty(ownerTopicEmail)) {
            emailListTopic.add(ownerTopicEmail);
          }
        }
        //
        emailListForum.addAll(forumReader.list(EXO_NOTIFY_WHEN_ADD_POST, new ArrayList<String>()));

        //
        String fullName = getScreenName(sProvider, post.getOwner());
        messageBuilder.setOwner(StringCommonUtils.decodeSpecialCharToHTMLnumber(fullName));
        messageBuilder.setId(post.getId());
        messageBuilder.setAddType(Utils.POST);
        messageBuilder.setAddName(post.getName());
        messageBuilder.setMessage(post.getMessage());
        messageBuilder.setCreatedDate(post.getCreatedDate());
        if(Utils.isEmpty(messageBuilder.getLink())) {
          messageBuilder.setLink(post.getLink());
        }
        // send email by category
        if (emailListCategory.size() > 0) {
          messageBuilder.setObjName(messageBuilder.getCatName());
          messageBuilder.setWatchType(Utils.CATEGORY);
          sendEmailNotification(emailListCategory, messageBuilder.getContentEmail());
        }
        // send email by forum
        if (emailListForum.size() > 0) {
          messageBuilder.setObjName(messageBuilder.getForumName());
          messageBuilder.setWatchType(Utils.FORUM);
          sendEmailNotification(emailListForum, messageBuilder.getContentEmail());
        }
        // send email by topic
        if (emailListTopic.size() > 0) {
          messageBuilder.setObjName(messageBuilder.getTopicName());
          messageBuilder.setWatchType(Utils.TOPIC);
          sendEmailNotification(emailListTopic, messageBuilder.getContentEmail());
        }
      }
    }
  }

  public void sendNotification(String nodePath, Topic topic, Post post, MessageBuilder messageBuilder, boolean isApprovePost) throws Exception {
    SessionProvider sProvider = SessionProvider.createSystemProvider();
    try {
      Session session = sessionManager.getSession(sProvider);
      if (!session.itemExists(nodePath)) {
        return;
      }
      Node node = (Node) session.getItem(nodePath);
      messageBuilder = getInfoMessageMove(sProvider, messageBuilder.getContent(), messageBuilder.getHeaderSubject(), false);
      if (post == null) {
        sendNotificationWhenCreateTopic(sProvider, node, topic, messageBuilder);
      } else {
        sendNotificationWhenCreatePost(sProvider, node, post, messageBuilder, isApprovePost);
      }
    } catch (Exception e) {
      LOG.error("Failed to send notification.", e);
    } finally {
      sProvider.close();
    }
  }

  public void modifyPost(List<Post> posts, int type) {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      Node forumHomeNode = getForumHomeNode(sProvider);
      for (Post post : posts) {
        try {
          String postPath = post.getPath();
          String topicPath = postPath.substring(0, postPath.lastIndexOf("/"));
          String forumPath = postPath.substring(0, topicPath.lastIndexOf("/"));
          Node postNode = (Node) forumHomeNode.getSession().getItem(postPath);
          PropertyReader postReader = new PropertyReader(postNode);
          Node topicNode = (Node) forumHomeNode.getSession().getItem(topicPath);
          Node forumNode = (Node) forumHomeNode.getSession().getItem(forumPath);
          Calendar lastPostDate = topicNode.getProperty(EXO_LAST_POST_DATE).getDate();
          Calendar postDate = postReader.calendar(EXO_CREATED_DATE);
          long topicPostCount = topicNode.getProperty(EXO_POST_COUNT).getLong();
          long newNumberAttach = topicNode.getProperty(EXO_NUMBER_ATTACHMENTS).getLong();
          long forumPostCount = forumNode.getProperty(EXO_POST_COUNT).getLong();
          switch (type) {
          case Utils.APPROVE: {
            postNode.setProperty(EXO_IS_APPROVED, true);
            post.setIsApproved(true);
            //
            addNotificationTask(topicNode.getPath(), null, post, new MessageBuilder(), true);
            break;
          }
          case Utils.WAITING: {
            if (post.getIsWaiting()) {
              postNode.setProperty(EXO_IS_WAITING, true);
              Node postLastNode = getLastDatePost(forumHomeNode, topicNode, postNode);
              PropertyReader postLastNodeReader = new PropertyReader(postLastNode);
              if (postLastNode != null) {
                topicNode.setProperty(EXO_LAST_POST_DATE, postLastNodeReader.calendar(EXO_CREATED_DATE));
                topicNode.setProperty(EXO_LAST_POST_BY, postLastNodeReader.string(EXO_OWNER));
              }
              newNumberAttach = newNumberAttach - postReader.l(EXO_NUMBER_ATTACH);
              if (newNumberAttach < 0)
                newNumberAttach = 0;
              topicNode.setProperty(EXO_NUMBER_ATTACHMENTS, newNumberAttach);
              topicNode.setProperty(EXO_POST_COUNT, topicPostCount - 1);
              forumNode.setProperty(EXO_POST_COUNT, forumPostCount - 1);
            } else {
              postNode.setProperty(EXO_IS_WAITING, false);
              topicNode.setProperty(EXO_LAST_POST_DATE, postReader.calendar(EXO_CREATED_DATE));
              topicNode.setProperty(EXO_LAST_POST_BY, postReader.string(EXO_OWNER));
              newNumberAttach = newNumberAttach + postReader.l(EXO_NUMBER_ATTACH);
              topicNode.setProperty(EXO_NUMBER_ATTACHMENTS, newNumberAttach);
              topicNode.setProperty(EXO_POST_COUNT, topicPostCount + 1);
              //
              addNotificationTask(topicNode.getPath(), null, post, new MessageBuilder(), false);
            }
            break;
          }
          case Utils.HIDDEN: {
            if (post.getIsHidden()) {
              postNode.setProperty(EXO_IS_HIDDEN, true);
              Node postLastNode = getLastDatePost(forumHomeNode, topicNode, postNode);
              PropertyReader postLastNodeReader = new PropertyReader(postLastNode);
              if (postLastNode != null) {
                topicNode.setProperty(EXO_LAST_POST_DATE, postLastNodeReader.calendar(EXO_CREATED_DATE));
                topicNode.setProperty(EXO_LAST_POST_BY, postLastNodeReader.string(EXO_OWNER));
              }
              newNumberAttach = newNumberAttach - postReader.l(EXO_NUMBER_ATTACH);
              topicNode.setProperty(EXO_NUMBER_ATTACHMENTS, Math.max(newNumberAttach, 0));
              topicNode.setProperty(EXO_POST_COUNT, topicPostCount - 1);
              forumNode.setProperty(EXO_POST_COUNT, forumPostCount - 1);
            } else {
              postNode.setProperty(EXO_IS_HIDDEN, false);
              addNotificationTask(topicNode.getPath(), null, post, new MessageBuilder(), false);
            }
            break;
          }
          default:
            break;
          }
          if (!post.getIsHidden() && post.getIsApproved() && (Utils.WAITING != type)) {
            if (postDate.getTimeInMillis() > lastPostDate.getTimeInMillis()) {
              topicNode.setProperty(EXO_LAST_POST_DATE, postDate);
              topicNode.setProperty(EXO_LAST_POST_BY, post.getOwner());
            }
            newNumberAttach = newNumberAttach + postReader.l(EXO_NUMBER_ATTACH);
            topicNode.setProperty(EXO_NUMBER_ATTACHMENTS, newNumberAttach);
            topicNode.setProperty(EXO_POST_COUNT, topicPostCount + 1);
            forumNode.setProperty(EXO_POST_COUNT, forumPostCount + 1);
          }
          if (forumNode.isNew()) {
            forumNode.getSession().save();
          } else {
            forumNode.save();
          }
          //
          getTotalJobWatting(sProvider, new HashSet<String>(new PropertyReader(forumNode).list(EXO_MODERATORS, new ArrayList<String>())));
        } catch (PathNotFoundException e) {
          LOG.error("Failed to modify post" + post.getName(), e);
        }
      }

    } catch (Exception e) {
      LOG.error("Failed to modify posts", e);
    }
  }

  private Node getLastDatePost(Node forumHomeNode, Node node, Node postNode_) throws Exception {
    QueryManager qm = forumHomeNode.getSession().getWorkspace().getQueryManager();
    StringBuffer pathQuery = new StringBuffer();
    pathQuery.append(JCR_ROOT).append(node.getPath()).append("/element(*,exo:post)[@exo:isHidden='false' and @exo:isApproved='true'] order by @exo:createdDate descending");
    Query query = qm.createQuery(pathQuery.toString(), Query.XPATH);
    QueryResult result = query.execute();
    NodeIterator iter = result.getNodes();
    Node postNode = null;
    while (iter.hasNext()) {
      postNode = iter.nextNode();
      if (postNode.getName().equals(postNode_.getName())) {
        continue;
      } else {
        break;
      }
    }
    return postNode;
  }

  public Post removePost(String categoryId, String forumId, String topicId, String postId) {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    Post post;
    try {
      Node CategoryNode = getCategoryHome(sProvider).getNode(categoryId);
      post = getPost(categoryId, forumId, topicId, postId);
      Node forumNode = CategoryNode.getNode(forumId);
      Node topicNode = forumNode.getNode(topicId);
      Node postNode = topicNode.getNode(postId);
      long numberAttachs = postNode.getProperty(EXO_NUMBER_ATTACH).getLong();
      String owner = postNode.getProperty(EXO_OWNER).getString();
      Node userProfileNode = getUserProfileHome(sProvider);
      try {
        Node newProfileNode = userProfileNode.getNode(owner);
        newProfileNode.setProperty(EXO_TOTAL_POST, newProfileNode.getProperty(EXO_TOTAL_POST).getLong() - 1);
        newProfileNode.save();
      } catch (PathNotFoundException e) {
        LOG.debug("Failed to save category moderators ", e);
      }
      postNode.remove();
      // update information: setPostCount, lastpost for Topic
      if (!post.getIsHidden() && post.getIsApproved() && !post.getIsWaiting() && (post.getUserPrivate() == null || post.getUserPrivate().length == 1)) {
        long topicPostCount = topicNode.getProperty(EXO_POST_COUNT).getLong() - 1;
        topicNode.setProperty(EXO_POST_COUNT, topicPostCount);
        long newNumberAttachs = topicNode.getProperty(EXO_NUMBER_ATTACHMENTS).getLong();
        if (newNumberAttachs > numberAttachs)
          newNumberAttachs = newNumberAttachs - numberAttachs;
        else
          newNumberAttachs = 0;
        topicNode.setProperty(EXO_NUMBER_ATTACHMENTS, newNumberAttachs);
      }
      NodeIterator nodeIterator = topicNode.getNodes();
      /*
       * long last = nodeIterator.getSize() - 1; nodeIterator.skip(last);
       */
      while (nodeIterator.hasNext()) {
        Node node = nodeIterator.nextNode();
        if (node.isNodeType(EXO_POST))
          postNode = node;
      }
      topicNode.setProperty(EXO_LAST_POST_BY, postNode.getProperty(EXO_OWNER).getValue().getString());
      topicNode.setProperty(EXO_LAST_POST_DATE, postNode.getProperty(EXO_CREATED_DATE).getValue().getDate());
      forumNode.save();

      // TODO: Thinking for update forum and user profile by node observation?
      // setPostCount for Forum
      if (!post.getIsHidden() && post.getIsApproved() && (post.getUserPrivate() == null || post.getUserPrivate().length == 1)) {
        long forumPostCount = forumNode.getProperty(EXO_POST_COUNT).getLong() - 1;
        forumNode.setProperty(EXO_POST_COUNT, forumPostCount);
        forumNode.save();
      } else if (post.getUserPrivate() == null || post.getUserPrivate().length == 1) {
        getTotalJobWatting(sProvider, new HashSet<String>(new PropertyReader(forumNode).list(EXO_MODERATORS, new ArrayList<String>())));
      }
      return post;
    } catch (Exception e) {
      LOG.error("Failed to remove post in topic.");
      return null;
    }
  }

  public void movePost(String[] postPaths, String destTopicPath, boolean isCreatNewTopic, String mailContent, String link) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    Node forumHomeNode = getForumHomeNode(sProvider);
    // Node Topic move Post
    String srcTopicPath = postPaths[0];
    srcTopicPath = srcTopicPath.substring(0, srcTopicPath.lastIndexOf("/"));
    Node srcTopicNode = (Node) forumHomeNode.getSession().getItem(srcTopicPath);
    Node srcForumNode = (Node) srcTopicNode.getParent();
    Node destTopicNode = (Node) forumHomeNode.getSession().getItem(destTopicPath);
    Node destForumNode = (Node) destTopicNode.getParent();
    long totalAtt = 0;
    long totalpost = (long) postPaths.length;
    Node postNode = null;
    boolean destModeratePost = false;
    if (destTopicNode.hasProperty(EXO_IS_MODERATE_POST)) {
      destModeratePost = destTopicNode.getProperty(EXO_IS_MODERATE_POST).getBoolean();
    }
    boolean srcModeratePost = false;
    if (srcTopicNode.hasProperty(EXO_IS_MODERATE_POST)) {
      srcModeratePost = srcTopicNode.getProperty(EXO_IS_MODERATE_POST).getBoolean();
    }
    PropertyReader destTopicReader = new PropertyReader(destTopicNode);
    boolean isActiveByTopic = destTopicReader.bool(EXO_IS_APPROVED) && destTopicReader.bool(EXO_IS_ACTIVE)
                              && destTopicReader.bool(EXO_IS_ACTIVE_BY_FORUM) && !destTopicReader.bool(EXO_IS_CLOSED)
                              && !destTopicReader.bool(EXO_IS_WAITING);
    boolean unAproved = false;
    String path;
    for (int i = 0; i < totalpost; ++i) {
      // totalAtt = totalAtt + post.getNumberAttach();
      path = postPaths[i];
      String newPostPath = destTopicPath + path.substring(path.lastIndexOf("/"));
      forumHomeNode.getSession().getWorkspace().move(path, newPostPath);
      postPaths[i] = newPostPath;
      // Node Post move
      postNode = (Node) forumHomeNode.getSession().getItem(newPostPath);
      postNode.setProperty(EXO_PATH, destForumNode.getName());
      postNode.setProperty(EXO_CREATED_DATE, getGreenwichMeanTime());
      postNode.setProperty(EXO_LINK, CommonUtils.getURI(link.replace("pathId", destTopicNode.getName())));
      //
      if (isCreatNewTopic && i == 0) {
        postNode.setProperty(EXO_IS_FIRST_POST, true);
      } else {
        postNode.setProperty(EXO_IS_FIRST_POST, false);
      }
      if (!destModeratePost) {
        postNode.setProperty(EXO_IS_APPROVED, true);
      } else {
        if (!postNode.getProperty(EXO_IS_APPROVED).getBoolean()) {
          unAproved = true;
        }
      }
      postNode.setProperty(EXO_IS_ACTIVE_BY_TOPIC, isActiveByTopic);
    }

    // set destTopicNode
    destTopicNode.setProperty(EXO_NUMBER_ATTACHMENTS, destTopicNode.getProperty(EXO_NUMBER_ATTACHMENTS).getLong() + totalAtt);
    // update last post for destTopicNode
    destTopicNode.setProperty(EXO_LAST_POST_BY, postNode.getProperty(EXO_OWNER).getValue().getString());
    destTopicNode.setProperty(EXO_LAST_POST_DATE, postNode.getProperty(EXO_CREATED_DATE).getValue().getDate());

    // set srcTopicNode
    long temp = srcTopicNode.getProperty(EXO_POST_COUNT).getLong();
    temp = temp - totalpost;
    srcTopicNode.setProperty(EXO_POST_COUNT, Math.max(temp, -1));
    temp = srcTopicNode.getProperty(EXO_NUMBER_ATTACHMENTS).getLong();
    temp = temp - totalAtt;
    srcTopicNode.setProperty(EXO_NUMBER_ATTACHMENTS, Math.max(temp, 0));
    // update last post for srcTopicNode
    NodeIterator nodeIterator = srcTopicNode.getNodes();
    long posLast = nodeIterator.getSize() - 1;
    nodeIterator.skip(posLast);
    while (nodeIterator.hasNext()) {
      Node node = nodeIterator.nextNode();
      if (node.isNodeType(EXO_POST))
        postNode = node;
    }
    srcTopicNode.setProperty(EXO_LAST_POST_BY, postNode.getProperty(EXO_OWNER).getValue().getString());
    srcTopicNode.setProperty(EXO_LAST_POST_DATE, postNode.getProperty(EXO_CREATED_DATE).getValue().getDate());
    // set srcForumNode
    temp = srcForumNode.getProperty(EXO_POST_COUNT).getLong();
    temp = temp - totalpost;
    srcForumNode.setProperty(EXO_POST_COUNT, Math.max(temp, 0));

    if (forumHomeNode.isNew()) {
      forumHomeNode.getSession().save();
    } else {
      forumHomeNode.save();
    }
    String categoryName = destForumNode.getParent().getProperty(EXO_NAME).getString();
    String forumName = destForumNode.getProperty(EXO_NAME).getString();
    String objectName = new StringBuilder("[").append(categoryName).append("][").append(forumName).append("] ").toString();

    MessageBuilder messageBuilder = getInfoMessageMove(sProvider, mailContent, objectName, true);
    String topicName = destTopicNode.getProperty(EXO_NAME).getString();
    String ownerTopic = destTopicNode.getProperty(EXO_OWNER).getString();
    messageBuilder.setCatName(categoryName);
    messageBuilder.setForumName(forumName);
    messageBuilder.setTopicName(CommonUtils.EMPTY_STR);
    messageBuilder.setOwner(StringCommonUtils.decodeSpecialCharToHTMLnumber(getScreenName(sProvider, ownerTopic)));
    messageBuilder.setHeaderSubject(messageBuilder.getHeaderSubject() + topicName);
    messageBuilder.setAddType(topicName);
    link = link.replaceFirst("pathId", destTopicNode.getName());
    messageBuilder.setTypes(Utils.TOPIC, Utils.POST, "", "");
    for (int i = 0; i < totalpost; ++i) {
      postNode = (Node) forumHomeNode.getSession().getItem(postPaths[i]);
      messageBuilder.setObjName(postNode.getProperty(EXO_NAME).getString());
      messageBuilder.setLink(link + "/" + postNode.getName());
      Set<String> set = new HashSet<String>();
      // set email author this topic
      set.add(getEmailUser(sProvider, postNode.getProperty(EXO_OWNER).getString()));
      // set email watch this topic, forum, category parent of this post
      set.addAll(calculateMoveEmail(destTopicNode));
      // set email watch old category, forum, topic parent of this post
      set.addAll(calculateMoveEmail(srcTopicNode));
      if (!Utils.isEmpty(set.toArray(new String[set.size()]))) {
        sendEmailNotification(new ArrayList<String>(set), messageBuilder.getContentEmailMoved());
      }
    }

    Set<String> userIdsp = new HashSet<String>();
    if (destModeratePost && srcModeratePost) {
      if (srcForumNode.hasProperty(EXO_MODERATORS)) {
        userIdsp.addAll(Utils.valuesToList(srcForumNode.getProperty(EXO_MODERATORS).getValues()));
      }
      if (unAproved && destForumNode.hasProperty(EXO_MODERATORS)) {
        userIdsp.addAll(Utils.valuesToList(destForumNode.getProperty(EXO_MODERATORS).getValues()));
      }
    } else if (srcModeratePost && !destModeratePost) {
      if (srcForumNode.hasProperty(EXO_MODERATORS)) {
        userIdsp.addAll(Utils.valuesToList(srcForumNode.getProperty(EXO_MODERATORS).getValues()));
      }
    } else if (!srcModeratePost && destModeratePost) {
      if (unAproved && destForumNode.hasProperty(EXO_MODERATORS)) {
        userIdsp.addAll(Utils.valuesToList(destForumNode.getProperty(EXO_MODERATORS).getValues()));
      }
    }
    if (!userIdsp.isEmpty()) {
      getTotalJobWatting(sProvider, userIdsp);
    }
  }

  private MessageBuilder getInfoMessageMove(SessionProvider sProvider, String defaultContent, String defaultSubject, boolean isMove) throws Exception {
    MessageBuilder messageBuilder = new MessageBuilder();
    try {
      Node forumAdminNode = getAdminHome(sProvider).getNode(Utils.FORUMADMINISTRATION);
      PropertyReader reader = new PropertyReader(forumAdminNode);
      String property = (isMove) ? EXO_NOTIFY_EMAIL_MOVED : EXO_NOTIFY_EMAIL_CONTENT;
      messageBuilder.setContent(reader.string(property, defaultContent));
      if (reader.bool(EXO_ENABLE_HEADER_SUBJECT)) {
        messageBuilder.setHeaderSubject(reader.string(EXO_HEADER_SUBJECT, defaultSubject));
      } else {
        messageBuilder.setHeaderSubject(defaultSubject);
      }
    } catch (Exception e) {
      messageBuilder.setContent(defaultContent);
      messageBuilder.setHeaderSubject(defaultSubject);
    }
    return messageBuilder;
  }

  public void mergeTopic(String srcTopicPath, String destTopicPath, String mailContent, String link) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      Node srcTopicNode = getCategoryHome(sProvider).getNode(srcTopicPath);
      NodeIterator iter = srcTopicNode.getNodes();
      List<Post> posts = new ArrayList<Post>();
      Post post;
      boolean isTopicWaiting = new PropertyReader(srcTopicNode).bool(EXO_IS_WAITING);
      while (iter.hasNext()) {
        post = new Post();
        Node node = iter.nextNode();
        if (node.isNodeType(EXO_POST)) {
          if (new PropertyReader(node).bool(EXO_IS_FIRST_POST)) {
            node.setProperty(EXO_IS_WAITING, isTopicWaiting);
            node.save();
          }
          post.setPath(node.getPath());
          post.setCreatedDate(node.getProperty(EXO_CREATED_DATE).getDate().getTime());
          posts.add(post);
        }
      }
      if (posts.size() > 0) {
        Collections.sort(posts, new Utils.DatetimeComparatorPostDESC());
        String[] postPaths = new String[posts.size()];
        int i = 0;
        for (Post p : posts) {
          postPaths[i] = p.getPath();
          ++i;
        }
        movePost(postPaths, destTopicPath, false, mailContent, link);
        String ids[] = srcTopicPath.split("/");
        removeTopic(ids[0], ids[1], srcTopicNode.getName());
      }
    } catch (Exception e) {
      throw e;
    }
  }

  public void splitTopic(Topic newTopic, Post fistPost, List<String> postPathMove, String mailContent, String link) throws Exception {
    // save new topic
    saveTopic(newTopic.getCategoryId(), newTopic.getForumId(), newTopic, true, true, new MessageBuilder());
    // save first post
    savePost(fistPost.getCategoryId(), fistPost.getForumId(), fistPost.getTopicId(), fistPost, false, new MessageBuilder());
    // move all posts to new topic
    movePost(postPathMove.toArray(new String[postPathMove.size()]), newTopic.getPath(), true, mailContent, link);
  }

  public void addTag(List<Tag> tags, String userName, String topicPath) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      boolean isAdd;
      Node topicNode = (Node) getCategoryHome(sProvider).getSession().getItem(topicPath);
      List<String> listId = new ArrayList<String>();
      List<String> list = new ArrayList<String>();
      if (topicNode.hasProperty(EXO_TAG_ID)) {
        listId = Utils.valuesToList(topicNode.getProperty(EXO_TAG_ID).getValues());
      }
      list.addAll(listId);
      String userIdAndTagId;
      for (Tag tag : tags) {
        isAdd = true;
        userIdAndTagId = userName + ":" + tag.getId();
        for (String string1 : listId) {
          if (userIdAndTagId.equals(string1)) {
            isAdd = false;
            break;
          }
        }
        if (isAdd) {
          list.add(userIdAndTagId);
          saveTag(tag);
        }
      }
      topicNode.setProperty(EXO_TAG_ID, Utils.getStringsInList(list));
      if (topicNode.isNew()) {
        topicNode.getSession().save();
      } else {
        topicNode.save();
      }

    } catch (Exception e) {
      LOG.error("Failed to add tags", e);
    }
  }

  public void unTag(String tagId, String userName, String topicPath){
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      Node categoryHome = getCategoryHome(sProvider);
      Node topicNode = (Node) categoryHome.getSession().getItem(topicPath);
      List<String> oldTagsId = Utils.valuesToList(topicNode.getProperty(EXO_TAG_ID).getValues());
      // remove in topic.
      String userIdTagId = userName + ":" + tagId;
      QueryManager qm = categoryHome.getSession().getWorkspace().getQueryManager();
      StringBuilder builder = new StringBuilder();
      builder.append(JCR_ROOT).append(categoryHome.getPath()).append("//element(*,exo:topic)[@exo:tagId='").append(userIdTagId).append("']");
      Query query = qm.createQuery(builder.toString(), Query.XPATH);
      QueryResult result = query.execute();
      NodeIterator iter = result.getNodes();
      if (oldTagsId.contains(userIdTagId)) {
        oldTagsId.remove(userIdTagId);
        topicNode.setProperty(EXO_TAG_ID, oldTagsId.toArray(new String[oldTagsId.size()]));
        if (topicNode.isNew()) {
          topicNode.getSession().save();
        } else {
          topicNode.save();
        }
      }
      Tag tag = getTag(tagId);
      List<String> userTags = new ArrayList<String>();
      userTags.addAll(Arrays.asList(tag.getUserTag()));
      if (iter.getSize() == 1 && userTags.size() > 1) {
        if (userTags.contains(userName)) {
          userTags.remove(userName);
          tag.setUserTag(userTags.toArray(new String[userTags.size()]));
          Node tagNode = getTagHome(sProvider).getNode(tagId);
          long count = tagNode.getProperty(EXO_USE_COUNT).getLong();
          if (count > 1)
            tagNode.setProperty(EXO_USE_COUNT, count - 1);
          tagNode.setProperty(EXO_USER_TAG, userTags.toArray(new String[userTags.size()]));
          tagNode.save();
        }
      } else if (iter.getSize() == 1 && userTags.size() == 1) {
        Node tagHomNode = getTagHome(sProvider);
        tagHomNode.getNode(tagId).remove();
        tagHomNode.save();
      } else if (iter.getSize() > 1) {
        Node tagNode = getTagHome(sProvider).getNode(tagId);
        long count = tagNode.getProperty(EXO_USE_COUNT).getLong();
        if (count > 1)
          tagNode.setProperty(EXO_USE_COUNT, count - 1);
        tagNode.save();
      }
    } catch (Exception e) {
      LOG.error("Failed to untag.", e);
    }
  }

  public Tag getTag(String tagId) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      Node tagNode = getTagHome(sProvider).getNode(tagId);
      return getTagNode(tagNode);
    } catch (Exception e) {
      return null;
    }
  }

  public List<String> getTagNameInTopic(String userAndTopicId) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    List<String> tagNames = new ArrayList<String>();
    try {
      Node tagHome = getTagHome(sProvider);
      QueryManager qm = tagHome.getSession().getWorkspace().getQueryManager();
      int t = userAndTopicId.indexOf(",");
      String userId = userAndTopicId.substring(0, t);
      NodeIterator iter = getTopicNodeIteratorByTag(sProvider, qm, userAndTopicId);
      StringBuilder builder = new StringBuilder();
      StringBuilder builder1 = new StringBuilder();
      if (iter.getSize() > 0) {
        Node node = (Node) iter.nextNode();
        if (node.hasProperty(EXO_TAG_ID)) {
          boolean b = true;
          t = 0;
          List<String> list = new ArrayList<String>();
          for (String string : Utils.valuesToList(node.getProperty(EXO_TAG_ID).getValues())) {
            String[] temp = string.split(":");
            if (temp.length == 2) {
              if (temp[0].equals(userId)) {
                if (t == 0)
                  builder.append("(@exo:id != '").append(temp[1]).append("'");
                else
                  builder.append(" and @exo:id != '").append(temp[1]).append("'");
                list.add(temp[1]);
                t = 1;
              } else if (!list.contains(temp[1])) {
                if (b)
                  builder1.append(" (@exo:id='").append(temp[1]).append("'");
                else
                  builder1.append(" or @exo:id='").append(temp[1]).append("'");
                b = false;
              }
            }
          }
          if (!b)
            builder1.append(")");
          if (t == 1)
            builder.append(")");
        }
      }
      if (builder1.length() == 0) {
        return tagNames;
      }
      StringBuffer queryString = new StringBuffer();
      queryString.append(JCR_ROOT).append(tagHome.getPath()).append("//element(*,exo:forumTag)");
      boolean isQr = false;
      if (builder.length() > 0) {
        queryString.append("[").append(builder);
        isQr = true;
      }
      if (builder1.length() > 0) {
        if (isQr) {
          queryString.append(" and ").append(builder1);
        } else {
          queryString.append("[").append(builder1);
          isQr = true;
        }
      }
      if (isQr)
        queryString.append("]");
      queryString.append("order by @exo:useCount descending, @exo:name ascending ");
      Query query = qm.createQuery(queryString.toString(), Query.XPATH);
      iter = query.execute().getNodes();
      return getTagName(iter, tagNames);
    } catch (Exception e) {
      return tagNames;
    }
  }

  private List<String> getTagName(NodeIterator iter, List<String> tagNames) {
    StringBuilder str;
    PropertyReader reader;
    while (iter.hasNext()) {
      reader = new PropertyReader((Node) iter.nextNode());
      str = new StringBuilder(reader.string(EXO_NAME));
      str.append(Utils.SPACE).append("<font color=\"Salmon\">(").append(reader.string(EXO_USE_COUNT)).append(")</font>");
      tagNames.add(str.toString());
      if (tagNames.size() == 5)
        break;
    }
    return tagNames;
  }

  private NodeIterator getTopicNodeIteratorByTag(SessionProvider sProvider, QueryManager qm, String userAndTopicId) throws Exception {
    Node categoryHome = getCategoryHome(sProvider);
    StringBuffer queryString = new StringBuffer();
    String topicId = userAndTopicId.substring(userAndTopicId.indexOf(",") + 1);
    queryString.append(JCR_ROOT).append(categoryHome.getPath()).append("//element(*,exo:topic)[exo:id='").append(topicId).append("']");
    Query query = qm.createQuery(queryString.toString(), Query.XPATH);
    return query.execute().getNodes();
  }

  public List<String> getAllTagName(String keyValue, String userAndTopicId) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    List<String> tagNames = new ArrayList<String>();
    try {
      Node tagHome = getTagHome(sProvider);
      QueryManager qm = tagHome.getSession().getWorkspace().getQueryManager();
      int t = userAndTopicId.indexOf(",");
      String userId = userAndTopicId.substring(0, t);
      NodeIterator iter = getTopicNodeIteratorByTag(sProvider, qm, userAndTopicId);
      StringBuilder builder = new StringBuilder();
      if (iter.getSize() > 0) {
        Node node = (Node) iter.nextNode();
        if (node.hasProperty(EXO_TAG_ID)) {
          t = 0;
          for (String string : Utils.valuesToList(node.getProperty(EXO_TAG_ID).getValues())) {
            String[] temp = string.split(":");
            if (temp.length == 2 && temp[0].equals(userId)) {
              if (t == 0)
                builder.append("@exo:id != '").append(temp[1]).append("'");
              else
                builder.append(" and @exo:id != '").append(temp[1]).append("'");
              t = 1;
            }
          }
        }
      }

      StringBuffer queryString = new StringBuffer();
      queryString.append(JCR_ROOT).append(tagHome.getPath()).append("//element(*,exo:forumTag)[(jcr:contains(@exo:name, '").append(keyValue).append("*'))");
      if (builder.length() > 0) {
        queryString.append(" and (").append(builder).append(")");
      }
      queryString.append("]order by @exo:useCount descending, @exo:name ascending ");
      Query query = qm.createQuery(queryString.toString(), Query.XPATH);
      iter = query.execute().getNodes();
      return getTagName(iter, tagNames);
    } catch (Exception e) {
      return tagNames;
    }
  }

  public List<Tag> getAllTags() throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    List<Tag> tags = new ArrayList<Tag>();
    try {
      Node tagHome = getTagHome(sProvider);
      QueryManager qm = tagHome.getSession().getWorkspace().getQueryManager();
      StringBuffer queryString = new StringBuffer(JCR_ROOT + tagHome.getPath() + "/element(*,exo:forumTag)");
      Query query = qm.createQuery(queryString.toString(), Query.XPATH);
      QueryResult result = query.execute();
      NodeIterator iter = result.getNodes();
      while (iter.hasNext()) {
        tags.add(getTagNode((Node) iter.nextNode()));
      }
      return tags;
    } catch (Exception e) {
      return tags;
    }
  }

  private Tag getTagNode(Node tagNode) throws RepositoryException {
    Tag newTag = new Tag();
    newTag.setId(tagNode.getName());
    PropertyReader reader = new PropertyReader(tagNode);
    newTag.setUserTag(reader.strings(EXO_USER_TAG, new String[] {}));
    newTag.setName(reader.string(EXO_NAME, ""));
    newTag.setUseCount(reader.l(EXO_USE_COUNT));
    return newTag;
  }

  public List<Tag> getMyTagInTopic(String[] tagIds) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    List<Tag> tags = new ArrayList<Tag>();
    try {
      Node tagHome = getTagHome(sProvider);
      for (String id : tagIds) {
        try {
          tags.add(getTagNode(tagHome.getNode(id)));
        } catch (Exception e) {
          if (LOG.isDebugEnabled()){
            LOG.debug(String.format("Failed to get tag node with id %s", id), e);
          }
        }
      }
      return tags;
    } catch (Exception e) {
      return tags;
    }
  }

  public JCRPageList getTopicByMyTag(String userIdAndtagId, String strOrderBy) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      Node categoryHome = getCategoryHome(sProvider);
      QueryManager qm = categoryHome.getSession().getWorkspace().getQueryManager();
      StringBuilder builder = new StringBuilder();
      builder.append(JCR_ROOT).append(categoryHome.getPath()).append("//element(*,exo:topic)");
      if (userIdAndtagId.indexOf(":") > 0) {
        builder.append("[@exo:tagId='").append(userIdAndtagId).append("']");
      } else {
        builder.append("[jcr:contains(@exo:tagId,'").append(userIdAndtagId).append("')]");
      }
      builder.append(" order by @exo:isSticky descending");
      if (Utils.isEmpty(strOrderBy)) {
        builder.append(", @exo:lastPostDate descending");
      } else {
        builder.append(", @exo:").append(strOrderBy);
        if (strOrderBy.indexOf("lastPostDate") < 0) {
          builder.append(", @exo:lastPostDate descending");
        }
      }
      String pathQuery = builder.toString();
      Query query = qm.createQuery(pathQuery, Query.XPATH);
      QueryResult result = query.execute();
      NodeIterator iter = result.getNodes();
      JCRPageList pagelist = new ForumPageList(iter, 10, pathQuery, true);
      return pagelist;
    } catch (Exception e) {
      return null;
    }
  }

  public void saveTag(Tag newTag) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      Node tagHome = getTagHome(sProvider);
      Node newTagNode;
      boolean isNew = false;
      try {
        newTagNode = tagHome.getNode(newTag.getId());
      } catch (PathNotFoundException e) {
        isNew = true;
        String id = Utils.TAG + newTag.getName();
        newTagNode = tagHome.addNode(id, EXO_FORUM_TAG);
        newTagNode.setProperty(EXO_ID, id);
      }
      if (isNew) {
        newTagNode.setProperty(EXO_USER_TAG, newTag.getUserTag());
        newTagNode.setProperty(EXO_NAME, newTag.getName());
        newTagNode.setProperty(EXO_USE_COUNT, 1);
      } else {
        List<String> userTags = Utils.valuesToList(newTagNode.getProperty(EXO_USER_TAG).getValues());
        if (!userTags.contains(newTag.getUserTag()[0])) {
          userTags.add(newTag.getUserTag()[0]);
          newTagNode.setProperty(EXO_USER_TAG, userTags.toArray(new String[userTags.size()]));
        }
        long count = newTagNode.getProperty(EXO_USE_COUNT).getLong();
        newTagNode.setProperty(EXO_USE_COUNT, count + 1);
      }

      if (tagHome.isNew()) {
        tagHome.getSession().save();
      } else {
        tagHome.save();
      }
    } catch (Exception e) {
      LOG.error("Failed to save tag.", e);
    }
  }

  public JCRPageList getPageListUserProfile() throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      Node userProfileHome = getUserProfileHome(sProvider);
      QueryManager qm = userProfileHome.getSession().getWorkspace().getQueryManager();
      StringBuilder strQuery = jcrPathLikeAndNotLike(EXO_FORUM_USER_PROFILE, userProfileHome.getPath());
      strQuery.append(" AND (").append(EXO_IS_DISABLED).append(" IS NULL OR ").append(EXO_IS_DISABLED).append("<>'true')");
      
      Query query = qm.createQuery(strQuery.toString(), Query.SQL);
      QueryResult result = query.execute();
      NodeIterator iter = result.getNodes();
      return new ForumPageList(iter, 10, strQuery.toString(), true);
    } catch (Exception e) {
      return null;
    }
  }

  public JCRPageList searchUserProfile(String userSearch) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      Node userProfileHome = getUserProfileHome(sProvider);
      QueryManager qm = userProfileHome.getSession().getWorkspace().getQueryManager();
      StringBuilder strQuery = jcrPathLikeAndNotLike(EXO_FORUM_USER_PROFILE, userProfileHome.getPath());
      strQuery.append(" AND (").append(EXO_USER_ID).append(" LIKE '%").append(userSearch).append("%'")
              .append(" OR ").append(EXO_FIRST_NAME).append(" LIKE '%").append(userSearch).append("%'")
              .append(" OR ").append(EXO_LAST_NAME).append(" LIKE '%").append(userSearch).append("%'")
              .append(" OR ").append(EXO_FULL_NAME).append(" LIKE '%").append(userSearch).append("%'")
              .append(" OR ").append(EXO_EMAIL).append(" LIKE '%").append(userSearch).append("%'")
              .append(" OR ").append(EXO_USER_TITLE).append(" LIKE '%").append(userSearch).append("%'")
              .append(") AND (").append(EXO_IS_DISABLED).append(" IS NULL OR ").append(EXO_IS_DISABLED).append("<>'true')");

      Query query = qm.createQuery(strQuery.toString(), Query.SQL);
      QueryResult result = query.execute();
      NodeIterator iter = result.getNodes();
      return new ForumPageList(iter, 10, strQuery.toString(), true);
    } catch (Exception e) {
      return null;
    }
  }

  public UserProfile getDefaultUserProfile(String userName, String ip) throws Exception {
    UserProfile userProfile = new UserProfile();
    if (StringUtils.isBlank(userName))
      return null;
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      Node profileNode = getUserProfileNode(sProvider, userName);
      PropertyReader reader = new PropertyReader(profileNode);
      userProfile.setUserId(userName);
      if (isAdminRole(userName)) {
        userProfile.setUserRole((long) 0);
      } else {
        userProfile.setUserRole(reader.l(EXO_USER_ROLE, 2));
      }
      userProfile.setModerateForums(reader.strings(EXO_MODERATE_FORUMS, new String[] {}));
      userProfile.setModerateCategory(reader.strings(EXO_MODERATE_CATEGORY, new String[] {}));
      userProfile.setScreenName(getScreenName(userName, profileNode));
      userProfile.setNewMessage(reader.l(EXO_NEW_MESSAGE));
      userProfile.setTimeZone(reader.d(EXO_TIME_ZONE));
      userProfile.setShortDateFormat(reader.string(EXO_SHORT_DATEFORMAT, userProfile.getShortDateFormat()));
      userProfile.setLongDateFormat(reader.string(EXO_LONG_DATEFORMAT, userProfile.getLongDateFormat()));
      userProfile.setTimeFormat(reader.string(EXO_TIME_FORMAT, userProfile.getTimeFormat()));
      userProfile.setMaxPostInPage(reader.l(EXO_MAX_POST, 10));
      userProfile.setMaxTopicInPage(reader.l(EXO_MAX_TOPIC, 10));
      userProfile.setIsAutoWatchMyTopics(reader.bool(EXO_IS_AUTO_WATCH_MY_TOPICS));
      userProfile.setIsAutoWatchTopicIPost(reader.bool(EXO_IS_AUTO_WATCH_TOPIC_I_POST));
      userProfile.setLastReadPostOfForum(reader.strings(EXO_LAST_READ_POST_OF_FORUM, new String[] {}));
      userProfile.setLastReadPostOfTopic(reader.strings(EXO_LAST_READ_POST_OF_TOPIC, new String[] {}));
      userProfile.setIsBanned(reader.bool(EXO_IS_BANNED));
      userProfile.setCollapCategories(reader.strings(EXO_COLLAP_CATEGORIES, new String[] {}));
      userProfile.setEmail(reader.string(EXO_EMAIL, ""));
      List<String> values = reader.list(EXO_READ_TOPIC, new ArrayList<String>());
      String s = ":";
      for (String str : values) {
        if (str.indexOf(s) > 0) {
          String[] array = str.split(s);
          userProfile.setLastTimeAccessTopic(array[0], Long.parseLong(array[1]));
        }
      }
      values = reader.list(EXO_READ_FORUM, new ArrayList<String>());
      for (String str : values) {
        if (str.indexOf(s) > 0) {
          String[] array = str.split(s);
          userProfile.setLastTimeAccessForum(array[0], Long.parseLong(array[1]));
        }
      }
      if (userProfile.getIsBanned()) {
        if (profileNode.hasProperty(EXO_BAN_UNTIL)) {
          userProfile.setBanUntil(reader.l(EXO_BAN_UNTIL));
          if (userProfile.getBanUntil() <= getGreenwichMeanTime().getTimeInMillis()) {
            profileNode.setProperty(EXO_IS_BANNED, false);
            profileNode.save();
            userProfile.setIsBanned(false);
          }
        }
      } else if (ip != null) {
        userProfile.setIsBanned(isBanIp(ip));
      }
    } catch (Exception e) {
      userProfile.setUserId(userName);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Failed to get default userprofile of user: " + userName, e);
      }
    }
    return userProfile;
  }

  public UserProfile updateUserProfileSetting(UserProfile userProfile) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    Node profileNode = getUserProfileNode(sProvider, userProfile.getUserId());
    PropertyReader reader = new PropertyReader(profileNode);
    //
    if (userProfile.getIsBanned()) {
      userProfile.setBanUntil(reader.l(EXO_BAN_UNTIL));
      if (userProfile.getBanUntil() <= getGreenwichMeanTime().getTimeInMillis()) {
        profileNode.setProperty(EXO_IS_BANNED, false);
        profileNode.save();
        userProfile.setIsBanned(false);
      }
    }
    //
    userProfile.setTimeZone(reader.d(ForumNodeTypes.EXO_TIME_ZONE));
    userProfile.setShortDateFormat(reader.string(EXO_SHORT_DATEFORMAT, userProfile.getShortDateFormat()));
    userProfile.setLongDateFormat(reader.string(EXO_LONG_DATEFORMAT, userProfile.getLongDateFormat()));
    userProfile.setModerateForums(reader.strings(EXO_MODERATE_FORUMS, new String[] {}));
    userProfile.setModerateCategory(reader.strings(EXO_MODERATE_CATEGORY, new String[] {}));
    userProfile.setMaxTopicInPage(reader.l(EXO_MAX_TOPIC, 10));
    userProfile.setMaxPostInPage(reader.l(EXO_MAX_POST, 10));
    userProfile.setBanReason(reader.string(EXO_BAN_REASON, ""));
    userProfile.setBanCounter(Integer.parseInt(reader.string(EXO_BAN_COUNTER, "0")));
    userProfile.setBanReasonSummary(reader.strings(EXO_BAN_REASON_SUMMARY, new String[] {}));
    userProfile.setCreatedDateBan(reader.date(EXO_CREATED_DATE_BAN));
    
    return userProfile;
  }

  public String getScreenName(String userName) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      return getScreenName(sProvider, userName);
    } catch (Exception e) {
      return userName;
    }
  }

  private String getScreenName(SessionProvider sProvider, String userName) throws Exception {
    try {
      Node userProfile = getUserProfileNode(getUserProfileHome(sProvider), userName);
      return getScreenName(userName, userProfile);
    } catch (Exception e) {
      return getScreenName(userName, null);
    }
  }

  private String getScreenName(String userName, Node userProfile) throws Exception {
    String userTemp = userName;
    if (userProfile != null) {
      PropertyReader reader = new PropertyReader(userProfile);
      userName = reader.string(EXO_SCREEN_NAME, reader.string(EXO_FULL_NAME, userName));
    }
    return (userTemp.contains(Utils.DELETED)) ? "<s>" + ((userName.contains(Utils.DELETED)) ? userName.substring(0, userName.indexOf(Utils.DELETED)) : userName) + "</s>" : userName;
  }

  public boolean isBanIp(String ip) throws Exception {
    return (getBanList().contains(ip)) ? true : false;
  }

  public UserProfile getUserSettingProfile(String userName) throws Exception {
    UserProfile userProfile = new UserProfile();
    if (StringUtils.isBlank(userName))
      return null;
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    Session session = sessionManager.getSession(sProvider);
    try {
      if (!session.getRootNode().hasNode(dataLocator.getUserProfilesLocation() + "/" + userName)) {
        return null;
      }
      userProfile = getCachedDataStorage().getQuickProfile(userName);
      Node profileNode = session.getRootNode().getNode(dataLocator.getUserProfilesLocation() + "/" + userName);
      PropertyReader reader = new PropertyReader(profileNode);
      //some information of profile has been loaded by getQuickProfile, don't loading anymore.
      //userProfile.setUserId(userName);
      //userProfile.setUserTitle(reader.string(EXO_USER_TITLE, ""));
      userProfile.setScreenName(getScreenName(userName, profileNode));
      userProfile.setSignature(reader.string(EXO_SIGNATURE, ""));
      userProfile.setIsDisplaySignature(reader.bool(EXO_IS_DISPLAY_SIGNATURE, true));
      //userProfile.setIsDisplayAvatar(reader.bool(EXO_IS_DISPLAY_AVATAR, true));
      userProfile.setIsAutoWatchMyTopics(reader.bool(EXO_IS_AUTO_WATCH_MY_TOPICS));
      userProfile.setIsAutoWatchTopicIPost(reader.bool(EXO_IS_AUTO_WATCH_TOPIC_I_POST));
      userProfile.setUserRole(reader.l(EXO_USER_ROLE));
      userProfile.setTimeZone(reader.d(EXO_TIME_ZONE));
      userProfile.setShortDateFormat(reader.string(EXO_SHORT_DATEFORMAT, ""));
      userProfile.setLongDateFormat(reader.string(EXO_LONG_DATEFORMAT, ""));
      userProfile.setTimeFormat(reader.string(EXO_TIME_FORMAT, ""));
      userProfile.setMaxPostInPage(reader.l(EXO_MAX_POST));
      userProfile.setMaxTopicInPage(reader.l(EXO_MAX_TOPIC));
    } catch (Exception e) {
      userProfile.setUserId(userName);
      userProfile.setUserTitle(Utils.USER);
      userProfile.setUserRole((long) 2);
      // default Administration
      if (isAdminRole(userName)) {
        userProfile.setUserRole((long) 0);
        userProfile.setUserTitle(Utils.ADMIN);
        saveUserProfile(userProfile, false, false);
      }
    }
    return userProfile;
  }

  public void saveUserSettingProfile(UserProfile userProfile) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    Node profileNode = getUserProfileNode(sProvider, userProfile.getUserId());
    try {
      profileNode.setProperty(EXO_USER_TITLE, userProfile.getUserTitle());
      profileNode.setProperty(EXO_SCREEN_NAME, userProfile.getScreenName());
      profileNode.setProperty(EXO_SIGNATURE, userProfile.getSignature());
      profileNode.setProperty(EXO_IS_DISPLAY_SIGNATURE, userProfile.getIsDisplaySignature());
      profileNode.setProperty(EXO_IS_DISPLAY_AVATAR, userProfile.getIsDisplayAvatar());
      profileNode.setProperty(EXO_USER_ROLE, userProfile.getUserRole());
      profileNode.setProperty(EXO_TIME_ZONE, userProfile.getTimeZone());
      profileNode.setProperty(EXO_SHORT_DATEFORMAT, userProfile.getShortDateFormat());
      profileNode.setProperty(EXO_LONG_DATEFORMAT, userProfile.getLongDateFormat());
      profileNode.setProperty(EXO_TIME_FORMAT, userProfile.getTimeFormat());
      profileNode.setProperty(EXO_MAX_POST, userProfile.getMaxPostInPage());
      profileNode.setProperty(EXO_MAX_TOPIC, userProfile.getMaxTopicInPage());
      profileNode.setProperty(EXO_IS_AUTO_WATCH_MY_TOPICS, userProfile.getIsAutoWatchMyTopics());
      profileNode.setProperty(EXO_IS_AUTO_WATCH_TOPIC_I_POST, userProfile.getIsAutoWatchTopicIPost());
      profileNode.save();
    } catch (Exception e) {
      LOG.error("Failed to save setting profile.", e);
    }
  }

  public UserProfile getLastPostIdRead(UserProfile userProfile, String isOfForum) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    Node profileNode = getUserProfileNode(sProvider, userProfile.getUserId());
    PropertyReader reader = new PropertyReader(profileNode);
    if (isOfForum.equals("true")) {
      userProfile.setLastReadPostOfForum(reader.strings(EXO_LAST_READ_POST_OF_FORUM, new String[] {}));
    } else if (isOfForum.equals("false")) {
      userProfile.setLastReadPostOfTopic(reader.strings(EXO_LAST_READ_POST_OF_TOPIC, new String[] {}));
    } else {
      userProfile.setLastReadPostOfForum(reader.strings(EXO_LAST_READ_POST_OF_FORUM, new String[] {}));
      userProfile.setLastReadPostOfTopic(reader.strings(EXO_LAST_READ_POST_OF_TOPIC, new String[] {}));
    }
    return userProfile;
  }

  public void saveLastPostIdRead(String userId, String[] lastReadPostOfForum, String[] lastReadPostOfTopic) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    final ReentrantLock localLock = lock;
    try {
      localLock.lock(); // block until condition holds
      Node profileNode = getUserProfileNode(sProvider, userId);
      profileNode.setProperty(EXO_LAST_READ_POST_OF_FORUM, lastReadPostOfForum);
      profileNode.setProperty(EXO_LAST_READ_POST_OF_TOPIC, lastReadPostOfTopic);
      profileNode.getSession().save();
    } catch (Exception e) {
      LOG.debug("Failed to save last post id read.", e);
    } finally {
      localLock.unlock();
    }
  }

  public List<String> getUserModerator(String userName, boolean isModeCate) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    Node userProfileNode = getUserProfileHome(sProvider);
    List<String> list = new ArrayList<String>();
    try {
      Node profileNode = userProfileNode.getNode(userName);
      if (isModeCate)
        try {
          list.addAll(Utils.valuesToList(profileNode.getProperty(EXO_MODERATE_CATEGORY).getValues()));
        } catch (Exception e) {
          if (LOG.isDebugEnabled()){
            LOG.debug("Failed to get moderators of categories", e);
          }
        }
      else
        list.addAll(Utils.valuesToList(profileNode.getProperty(EXO_MODERATE_FORUMS).getValues()));
    } catch (Exception e) {
      if (LOG.isDebugEnabled()){
        LOG.debug("Failed to get moderators of forums", e);
      }
    }
    return list;
  }

  public void saveUserModerator(String userName, List<String> ids, boolean isModeCate) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    Node userProfileNode = getUserProfileHome(sProvider);
    try {
      Node profileNode = userProfileNode.getNode(userName);
      if (isModeCate)
        profileNode.setProperty(EXO_MODERATE_CATEGORY, Utils.getStringsInList(ids));
      else
        profileNode.setProperty(EXO_MODERATE_FORUMS, Utils.getStringsInList(ids));
      profileNode.save();
    } catch (Exception e) {
      LOG.error(String.format("Failed to set %s as moderator", userName));
    }
  }

  private Node getUserProfileNode(Node profileHome, String userName) throws Exception {
    try {
      return profileHome.getNode(userName);
    } catch (PathNotFoundException e) {
      if (profileHome.hasNode(Utils.USER_PROFILE_DELETED)) {
        Node deletedHome = profileHome.getNode(Utils.USER_PROFILE_DELETED);
        return deletedHome.getNode(userName);
      } else {
        return addNodeUserProfile(CommonUtils.createSystemProvider(), userName);
      }
    }
  }

  public UserProfile getUserInfo(String userName) throws Exception {
    UserProfile userProfile = new UserProfile();
    if (StringUtils.isBlank(userName))
      return null;
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    Node userProfileNode = getUserProfileHome(sProvider);
    Node newProfileNode;
    try {
      newProfileNode = getUserProfileNode(userProfileNode, userName);
      if (newProfileNode == null) {
        return null;
      }
      PropertyReader reader = new PropertyReader(newProfileNode);
      userProfile.setUserId(userName);
      userProfile.setScreenName(getScreenName(userName, newProfileNode));
      userProfile.setFullName(reader.string(EXO_FULL_NAME));
      userProfile.setFirstName(reader.string(EXO_FIRST_NAME));
      userProfile.setLastName(reader.string(EXO_LAST_NAME));
      userProfile.setEmail(reader.string(EXO_EMAIL));

      if (isAdminRole(userName)) {
        userProfile.setUserRole((long) 0); // admin role = 0
      } else {
        userProfile.setUserRole(reader.l(EXO_USER_ROLE));
      }

      userProfile.setUserTitle(reader.string(EXO_USER_TITLE, ""));
      userProfile.setSignature(reader.string(EXO_SIGNATURE));
      userProfile.setTotalPost(reader.l(EXO_TOTAL_POST));
      userProfile.setTotalTopic(reader.l(EXO_TOTAL_TOPIC));
      userProfile.setBookmark(reader.strings(EXO_BOOKMARK));
      userProfile.setLastLoginDate(reader.date(EXO_LAST_LOGIN_DATE));
      userProfile.setJoinedDate(reader.date(EXO_JOINED_DATE));
      userProfile.setLastPostDate(reader.date(EXO_LAST_POST_DATE));
      userProfile.setIsDisplaySignature(reader.bool(EXO_IS_DISPLAY_SIGNATURE));
      userProfile.setIsDisplayAvatar(reader.bool(EXO_IS_DISPLAY_AVATAR));
      userProfile.setDisabled(reader.bool(EXO_IS_DISABLED, false));
      return userProfile;
    } catch (PathNotFoundException e) {
      return null;
    }
  }

  public List<UserProfile> getQuickProfiles(List<String> userList) throws Exception {
    List<UserProfile> profiles = new ArrayList<UserProfile>();
    try {
      DataStorage storage = getCachedDataStorage();
      for (String userName : userList) {
        profiles.add(storage.getQuickProfile(userName));
      }
    } catch (Exception e) {
      LOG.trace("\nUser Name must exist: " + e.getMessage() + "\n" + e.getCause());
    }
    return profiles;
  }

  public UserProfile getQuickProfile(String userName) throws Exception {
    if (StringUtils.isBlank(userName)) {
      return null;
    }
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    Node userProfileHome = getUserProfileHome(sProvider);
    //
    return getUserProfile(getUserProfileNode(userProfileHome, userName));
  }

  public UserProfile getUserInformations(UserProfile userProfile) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    Node userProfileHome = getUserProfileHome(sProvider);
    Node profileNode = getUserProfileNode(userProfileHome, userProfile.getUserId());
    PropertyReader reader = new PropertyReader(profileNode);
    userProfile.setFirstName(reader.string(EXO_FIRST_NAME, ""));
    userProfile.setLastName(reader.string(EXO_LAST_NAME, ""));
    userProfile.setFullName(reader.string(EXO_FULL_NAME, ""));
    userProfile.setEmail(reader.string(EXO_EMAIL, ""));
    userProfile.setDisabled(reader.bool(EXO_IS_DISABLED, false));
    return userProfile;
  }

  public void saveUserProfile(UserProfile newUserProfile, boolean isOption, boolean isBan) throws Exception {
    Node newProfileNode;
    String userName = newUserProfile.getUserId();
    if (userName == null || userName.length() <= 0)
      return;
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    Node userProfileHome = getUserProfileHome(sProvider);
    long role = 2;
    try {
      newProfileNode = userProfileHome.getNode(userName);
      if (newProfileNode.hasProperty(EXO_USER_ROLE)) {
        role = new PropertyReader(newProfileNode).l(EXO_USER_ROLE);
      }
    } catch (PathNotFoundException e) {
      newProfileNode = userProfileHome.addNode(userName, EXO_FORUM_USER_PROFILE);
      newProfileNode.setProperty(EXO_USER_ID, userName);
      newProfileNode.setProperty(EXO_TOTAL_POST, 0);
      newProfileNode.setProperty(EXO_TOTAL_TOPIC, 0);
      newProfileNode.setProperty(EXO_READ_TOPIC, new String[] {});
      newProfileNode.setProperty(EXO_READ_FORUM, new String[] {});
      if (newUserProfile.getUserRole() >= 2) {
        newUserProfile.setUserRole((long) 2);
      }
      if (isAdminRole(userName)) {
        newUserProfile.setUserTitle(Utils.ADMIN);
      }
    }
    newProfileNode.setProperty(EXO_USER_ROLE, newUserProfile.getUserRole());
    newProfileNode.setProperty(EXO_USER_TITLE, newUserProfile.getUserTitle());
    newProfileNode.setProperty(EXO_SCREEN_NAME, newUserProfile.getScreenName());
    newProfileNode.setProperty(EXO_SIGNATURE, newUserProfile.getSignature());
    newProfileNode.setProperty(EXO_IS_AUTO_WATCH_MY_TOPICS, newUserProfile.getIsAutoWatchMyTopics());
    newProfileNode.setProperty(EXO_IS_AUTO_WATCH_TOPIC_I_POST,
                               newUserProfile.getIsAutoWatchTopicIPost());
    newProfileNode.setProperty(EXO_MODERATE_CATEGORY, newUserProfile.getModerateCategory());
    Calendar calendar = getGreenwichMeanTime();
    if (newUserProfile.getLastLoginDate() != null)
      calendar.setTime(newUserProfile.getLastLoginDate());
    newProfileNode.setProperty(EXO_LAST_LOGIN_DATE, calendar);
    newProfileNode.setProperty(EXO_IS_DISPLAY_SIGNATURE, newUserProfile.getIsDisplaySignature());
    newProfileNode.setProperty(EXO_IS_DISPLAY_AVATAR, newUserProfile.getIsDisplayAvatar());
    // UserOption
    if (isOption) {
      newProfileNode.setProperty(EXO_TIME_ZONE, newUserProfile.getTimeZone());
      newProfileNode.setProperty(EXO_SHORT_DATEFORMAT, newUserProfile.getShortDateFormat());
      newProfileNode.setProperty(EXO_LONG_DATEFORMAT, newUserProfile.getLongDateFormat());
      newProfileNode.setProperty(EXO_TIME_FORMAT, newUserProfile.getTimeFormat());
      newProfileNode.setProperty(EXO_MAX_POST, newUserProfile.getMaxPostInPage());
      newProfileNode.setProperty(EXO_MAX_TOPIC, newUserProfile.getMaxTopicInPage());
    }
    // UserBan
    if (isBan) {
      if (newProfileNode.hasProperty(EXO_IS_BANNED)) {
        if (!newProfileNode.getProperty(EXO_IS_BANNED).getBoolean() && newUserProfile.getIsBanned()) {
          newProfileNode.setProperty(EXO_CREATED_DATE_BAN, getGreenwichMeanTime());
        }
      } else {
        newProfileNode.setProperty(EXO_CREATED_DATE_BAN, getGreenwichMeanTime());
      }
      newProfileNode.setProperty(EXO_IS_BANNED, newUserProfile.getIsBanned());
      newProfileNode.setProperty(EXO_BAN_UNTIL, newUserProfile.getBanUntil());
      newProfileNode.setProperty(EXO_BAN_REASON, newUserProfile.getBanReason());
      newProfileNode.setProperty(EXO_BAN_COUNTER, "" + newUserProfile.getBanCounter());
      newProfileNode.setProperty(EXO_BAN_REASON_SUMMARY, newUserProfile.getBanReasonSummary());
    }
    if (userProfileHome.isNew()) {
      userProfileHome.getSession().save();
    } else {
      userProfileHome.save();
    }
    if (role >= 2 && newUserProfile.getUserRole() < 2 && !isAdminRole(userName)) {
      getTotalJobWaitingForModerator(userProfileHome.getSession(), userName);
    }
  }

  public UserProfile getUserProfileManagement(String userName) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      Node userProfileNode = getUserProfileNode(getUserProfileHome(sProvider), userName);
      return getUserProfile(userProfileNode);
    } catch (Exception e) {
      return null;
    }
  }

  private UserProfile getUserProfile(Node userProfileNode) throws Exception {
    if (userProfileNode == null) {
      return null;
    }
    UserProfile userProfile = new UserProfile();
    String userName = userProfileNode.getName();
    PropertyReader reader = new PropertyReader(userProfileNode);
    userProfile.setUserId(userName);
    userProfile.setUserTitle(reader.string(EXO_USER_TITLE, ""));
    userProfile.setUserRole((userName.contains(Utils.DELETED)) ? 4 : reader.l(EXO_USER_ROLE, 2));
    userProfile.setScreenName(getScreenName(userName, userProfileNode));
    userProfile.setJoinedDate(reader.date(EXO_JOINED_DATE, new Date()));
    userProfile.setIsDisplayAvatar(reader.bool(EXO_IS_DISPLAY_AVATAR));
    userProfile.setNewMessage(reader.l(EXO_NEW_MESSAGE));
    userProfile.setTimeZone(reader.d(EXO_TIME_ZONE));
    userProfile.setShortDateFormat(reader.string(EXO_SHORT_DATEFORMAT, ""));
    userProfile.setLongDateFormat(reader.string(EXO_LONG_DATEFORMAT, ""));
    userProfile.setTimeFormat(reader.string(EXO_TIME_FORMAT, ""));
    userProfile.setMaxPostInPage(reader.l(EXO_MAX_POST));
    userProfile.setMaxTopicInPage(reader.l(EXO_MAX_TOPIC));
    userProfile.setIsBanned(reader.bool(EXO_IS_BANNED));
    userProfile.setDisabled(reader.bool(EXO_IS_DISABLED, false));
    if (userProfile.getIsBanned()) {
      if (userProfileNode.hasProperty(EXO_BAN_UNTIL)) {
        userProfile.setBanUntil(reader.l(EXO_BAN_UNTIL));
        if (userProfile.getBanUntil() <= getGreenwichMeanTime().getTimeInMillis()) {
          userProfileNode.setProperty(EXO_IS_BANNED, false);
          userProfileNode.save();
          userProfile.setIsBanned(false);
        }
      }
    }
    userProfile.setTotalPost(reader.l(EXO_TOTAL_POST));
    if (userProfile.getTotalPost() > 0) {
      userProfile.setLastPostDate(reader.date(EXO_LAST_POST_DATE));
    }
    userProfile.setLastLoginDate(reader.date(EXO_LAST_LOGIN_DATE));
    userProfile.setIsDisplaySignature(reader.bool(EXO_IS_DISPLAY_SIGNATURE, false));
    userProfile.setSignature(reader.string(EXO_SIGNATURE, ""));
    
    return userProfile;
  }

  public void saveUserBookmark(String userName, String bookMark, boolean isNew) throws Exception {
    Node newProfileNode;
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      Node userProfileNode = getUserProfileHome(sProvider);
      try {
        newProfileNode = userProfileNode.getNode(userName);
        if (newProfileNode.hasProperty(EXO_BOOKMARK)) {
          List<String> listOld = Utils.valuesToList(newProfileNode.getProperty(EXO_BOOKMARK).getValues());
          List<String> listNew = new ArrayList<String>();
          String pathNew = bookMark.substring(bookMark.lastIndexOf("//") + 1);
          String pathOld = "";
          boolean isAdd = true;
          for (String string : listOld) {
            pathOld = string.substring(string.lastIndexOf("//") + 1);
            if (pathNew.equals(pathOld)) {
              if (isNew) {
                listNew.add(bookMark);
              }
              isAdd = false;
              continue;
            }
            listNew.add(string);
          }
          if (isAdd) {
            listNew.add(bookMark);
          }
          String[] bookMarks = listNew.toArray(new String[listNew.size()]);
          newProfileNode.setProperty(EXO_BOOKMARK, bookMarks);
          if (newProfileNode.isNew()) {
            newProfileNode.getSession().save();
          } else {
            newProfileNode.save();
          }
        } else {
          newProfileNode.setProperty(EXO_BOOKMARK, new String[] { bookMark });
          if (newProfileNode.isNew()) {
            newProfileNode.getSession().save();
          } else {
            newProfileNode.save();
          }
        }
      } catch (PathNotFoundException e) {
        newProfileNode = userProfileNode.addNode(userName, EXO_FORUM_USER_PROFILE);
        newProfileNode.setProperty(EXO_USER_ID, userName);
        newProfileNode.setProperty(EXO_USER_TITLE, Utils.USER);
        if (isAdminRole(userName)) {
          newProfileNode.setProperty(EXO_USER_TITLE, Utils.ADMIN);
        }
        newProfileNode.setProperty(EXO_USER_ROLE, 2);
        newProfileNode.setProperty(EXO_BOOKMARK, new String[] { bookMark });
        if (newProfileNode.isNew()) {
          newProfileNode.getSession().save();
        } else {
          newProfileNode.save();
        }
      }
    } catch (Exception e) {
      LOG.error("Failed to save UserBookmark.", e);
    }
  }

  public void saveCollapsedCategories(String userName, String categoryId, boolean isAdd) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    Node newProfileNode;
    Node userProfileHome = getUserProfileHome(sProvider);
    try {
      newProfileNode = userProfileHome.getNode(userName);
      if (newProfileNode.hasProperty(EXO_COLLAP_CATEGORIES)) {
        List<String> listCategoryId = Utils.valuesToList(newProfileNode.getProperty(EXO_COLLAP_CATEGORIES).getValues());
        if (listCategoryId.contains(categoryId)) {
          if (!isAdd) {
            listCategoryId.remove(categoryId);
            isAdd = true;
          }
        } else {
          if (isAdd) {
            listCategoryId.add(categoryId);
          }
        }
        if (isAdd) {
          String[] categoryIds = listCategoryId.toArray(new String[listCategoryId.size()]);
          newProfileNode.setProperty(EXO_COLLAP_CATEGORIES, categoryIds);
          if (newProfileNode.isNew()) {
            newProfileNode.getSession().save();
          } else {
            newProfileNode.save();
          }
        }
      } else {
        newProfileNode.setProperty(EXO_COLLAP_CATEGORIES, new String[] { categoryId });
        if (newProfileNode.isNew()) {
          newProfileNode.getSession().save();
        } else {
          newProfileNode.save();
        }
      }
    } catch (PathNotFoundException e) {
      newProfileNode = userProfileHome.addNode(userName, EXO_FORUM_USER_PROFILE);
      newProfileNode.setProperty(EXO_USER_ID, userName);
      newProfileNode.setProperty(EXO_USER_TITLE, Utils.USER);
      if (isAdminRole(userName)) {
        newProfileNode.setProperty(EXO_USER_TITLE, Utils.ADMIN);
      }
      newProfileNode.setProperty(EXO_USER_ROLE, 2);
      newProfileNode.setProperty(EXO_COLLAP_CATEGORIES, new String[] { categoryId });
      if (newProfileNode.isNew()) {
        newProfileNode.getSession().save();
      } else {
        newProfileNode.save();
      }
    } catch (Exception e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Failed to save collapsed categories.", e);
      }
    }
  }

  public void saveReadMessage(String messageId, String userName, String type) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    Node userProfileNode = getUserProfileHome(sProvider);
    try {
      Node profileNode = userProfileNode.getNode(userName);
      long totalNewMessage = 0;
      boolean isNew = false;
      try {
        Node messageNode = profileNode.getNode(messageId);
        if (messageNode.hasProperty(EXO_IS_UNREAD)) {
          isNew = messageNode.getProperty(EXO_IS_UNREAD).getBoolean();
        }
        if (isNew) {// First read message.
          messageNode.setProperty(EXO_IS_UNREAD, false);
        }
      } catch (PathNotFoundException e) {
        LOG.error("Failed to save read massage", e);
      }
      if (type.equals(Utils.RECEIVE_MESSAGE) && isNew) {
        if (profileNode.hasProperty(EXO_NEW_MESSAGE)) {
          totalNewMessage = profileNode.getProperty(EXO_NEW_MESSAGE).getLong();
          if (totalNewMessage > 0) {
            profileNode.setProperty(EXO_NEW_MESSAGE, (totalNewMessage - 1));
          }
        }
      }
      if (isNew) {
        if (userProfileNode.isNew()) {
          userProfileNode.getSession().save();
        } else {
          userProfileNode.save();
        }
      }
    } catch (Exception e) {
      LOG.error("Failed to save read message.");
    }
  }

  public JCRPageList getPrivateMessage(String userName, String type) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    Node userProfileNode = getUserProfileHome(sProvider);
    try {
      Node profileNode = userProfileNode.getNode(userName);
      QueryManager qm = profileNode.getSession().getWorkspace().getQueryManager();
      String pathQuery = JCR_ROOT + XPathUtils.escapeIllegalXPathName(profileNode.getPath()) + "/element(*,exo:privateMessage)[@exo:type='" + type + "'] order by @exo:receivedDate descending";
      Query query = qm.createQuery(pathQuery, Query.XPATH);
      QueryResult result = query.execute();
      NodeIterator iter = result.getNodes();
      JCRPageList pagelist = new ForumPageList(iter, 10, pathQuery, true);
      return pagelist;
    } catch (Exception e) {
      LOG.error("Fail to get private message.", e);
      return null;
    }
  }

  public long getNewPrivateMessage(String userName) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    Node userProfileNode = getUserProfileHome(sProvider);
    try {
      Node profileNode = userProfileNode.getNode(userName);
      if (!profileNode.getProperty(EXO_IS_BANNED).getBoolean()) {
        return profileNode.getProperty(EXO_NEW_MESSAGE).getLong();
      }
    } catch (PathNotFoundException e) {
      return -1;
    }
    return -1;
  }

  public void savePrivateMessage(ForumPrivateMessage privateMessage) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    Node userProfileNode = getUserProfileHome(sProvider);
    Node profileNode = null;
    Node profileNodeFirst = null;
    Node messageNode = null;
    String sendTo = privateMessage.getSendTo();
    sendTo = sendTo.replaceAll(";", ",");
    String[] strUserNames = sendTo.split(",");
    List<String> userNames;
    try {
      userNames = ForumServiceUtils.getUserPermission(strUserNames);
    } catch (Exception e) {
      userNames = Arrays.asList(strUserNames);
    }
    String id;
    String userNameFirst = privateMessage.getFrom();
    try {
      profileNodeFirst = userProfileNode.getNode(userNameFirst);
    } catch (PathNotFoundException e) {
      profileNodeFirst = addNodeUserProfile(sProvider, userNameFirst);
    }
    long totalMessage = 0;
    if (profileNodeFirst != null) {
      id = userNameFirst + IdGenerator.generate();
      messageNode = profileNodeFirst.addNode(id, EXO_PRIVATE_MESSAGE);
      messageNode.setProperty(EXO_FROM, privateMessage.getFrom());
      messageNode.setProperty(EXO_SEND_TO, privateMessage.getSendTo());
      messageNode.setProperty(EXO_NAME, privateMessage.getName());
      messageNode.setProperty(EXO_MESSAGE, privateMessage.getMessage());
      messageNode.setProperty(EXO_RECEIVED_DATE, getGreenwichMeanTime());
      messageNode.setProperty(EXO_IS_UNREAD, true);
      messageNode.setProperty(EXO_TYPE, Utils.RECEIVE_MESSAGE);
    }
    StringBuilder sent = new StringBuilder();
    for (String userName : userNames) {
      if (userName.equals(userNameFirst))
        continue;
      try {
        profileNode = userProfileNode.getNode(userName);
        totalMessage = profileNode.getProperty(EXO_NEW_MESSAGE).getLong() + 1;
        id = profileNode.getPath() + "/" + userName + IdGenerator.generate();
        userProfileNode.getSession().getWorkspace().copy(messageNode.getPath(), id);
        profileNode.setProperty(EXO_NEW_MESSAGE, totalMessage);
      } catch (Exception e) {
        profileNode = addNodeUserProfile(sProvider, userName);
        id = profileNode.getPath() + "/" + userName + IdGenerator.generate();
        userProfileNode.getSession().getWorkspace().copy(messageNode.getPath(), id);
        profileNode.setProperty(EXO_NEW_MESSAGE, 1);
      }
      if(sent.length() > 0) {
        sent.append(",");
      }
      sent.append(userName);
    }
    // send notification message for user
    privateMessage.setType("PrivateMessage");
    //
    privateMessage.setSendTo(sent.toString());
    sendNotificationMessage(privateMessage);
    if (messageNode != null) {
      messageNode.setProperty(EXO_TYPE, Utils.SEND_MESSAGE);
    }
    if (userProfileNode.isNew()) {
      userProfileNode.getSession().save();
    } else {
      userProfileNode.save();
    }
  }

  private Node addNodeUserProfile(SessionProvider sProvider, String userName) throws Exception {
    Node userProfileHome = getUserProfileHome(sProvider);
    Node profileNode = userProfileHome.addNode(userName, EXO_FORUM_USER_PROFILE);
    profileNode.setProperty(EXO_USER_ID, userName);
    profileNode.setProperty(EXO_USER_TITLE, Utils.USER);
    if (isAdminRole(userName)) {
      profileNode.setProperty(EXO_USER_ROLE, 0);
      profileNode.setProperty(EXO_USER_TITLE, Utils.ADMIN);
    }
    profileNode.setProperty(EXO_USER_ROLE, 2);
    if (userProfileHome.isNew()) {
      userProfileHome.getSession().save();
    } else {
      userProfileHome.save();
    }
    return profileNode;
  }

  public void removePrivateMessage(String messageId, String userName, String type) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    Node userProfileNode = getUserProfileHome(sProvider);
    try {
      Node profileNode = userProfileNode.getNode(userName);
      Node messageNode = profileNode.getNode(messageId);
      if (type.equals(Utils.RECEIVE_MESSAGE)) {
        if (messageNode.hasProperty(EXO_IS_UNREAD)) {
          if (messageNode.getProperty(EXO_IS_UNREAD).getBoolean()) {
            long totalMessage = profileNode.getProperty(EXO_NEW_MESSAGE).getLong();
            if (totalMessage > 0) {
              profileNode.setProperty(EXO_NEW_MESSAGE, (totalMessage - 1));
            }
          }
        }
      }
      messageNode.remove();
      profileNode.save();
    } catch (PathNotFoundException e) {
      LOG.error("Failed to remove private message", e);
    }
  }

  public ForumSubscription getForumSubscription(String userId){
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    ForumSubscription forumSubscription = new ForumSubscription();
    try {
      Node subscriptionNode = getUserProfileHome(sProvider).getNode(userId + "/" + Utils.FORUM_SUBSCRIOTION + userId);
      PropertyReader reader = new PropertyReader(subscriptionNode);
        forumSubscription.setCategoryIds(reader.strings(EXO_CATEGORY_IDS, new String[] {}));
        forumSubscription.setForumIds(reader.strings(EXO_FORUM_IDS, new String[] {}));
        forumSubscription.setTopicIds(reader.strings(EXO_TOPIC_IDS, new String[] {}));
    } catch (Exception e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug(String.format("Failed to get forum subscription for user %s", userId), e);
      }
    }
    return forumSubscription;
  }

  public void saveForumSubscription(ForumSubscription forumSubscription, String userId) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      Node profileNode = getUserProfileHome(sProvider).getNode(userId);
      Node subscriptionNode;
      String id = Utils.FORUM_SUBSCRIOTION + userId;
      try {
        subscriptionNode = profileNode.getNode(id);
      } catch (PathNotFoundException e) {
        subscriptionNode = profileNode.addNode(id, EXO_FORUM_SUBSCRIPTION);
      }
      subscriptionNode.setProperty(EXO_CATEGORY_IDS, forumSubscription.getCategoryIds());
      subscriptionNode.setProperty(EXO_FORUM_IDS, forumSubscription.getForumIds());
      subscriptionNode.setProperty(EXO_TOPIC_IDS, forumSubscription.getTopicIds());
      if (profileNode.isNew()) {
        profileNode.getSession().save();
      } else {
        profileNode.save();
      }
    } catch (Exception e) {
      LOG.error("Failed to save forum subscription.", e);
    }
  }

  private String[] getValueProperty(Node node, String property, String objectId) throws Exception {
    List<String> list = new ArrayList<String>();
    if (node.hasProperty(property)) {
      list.addAll(Utils.valuesToList(node.getProperty(property).getValues()));
      if (!list.contains(objectId))
        list.add(objectId);
    } else {
      list.add(objectId);
    }
    return list.toArray(new String[list.size()]);
  }

  public ForumStatistic getForumStatistic() throws Exception {
    SessionProvider sProvider = SessionProvider.createSystemProvider();
    ForumStatistic forumStatistic = new ForumStatistic();
    try {
      Node forumStatisticNode = getForumStatisticsNode(sProvider);
      PropertyReader reader = new PropertyReader(forumStatisticNode);
      forumStatistic.setPostCount(reader.l(EXO_POST_COUNT, 0));
      forumStatistic.setTopicCount(reader.l(EXO_TOPIC_COUNT, 0));
      forumStatistic.setMembersCount(reader.l(EXO_MEMBERS_COUNT, 0));
      forumStatistic.setActiveUsers(reader.l(EXO_ACTIVE_USERS, 0));
      forumStatistic.setNewMembers(reader.string(EXO_NEW_MEMBERS, ""));
      forumStatistic.setMostUsersOnline(reader.string(EXO_MOST_USERS_ONLINE, ""));
    } catch (Exception e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Failed to load forum statistics", e);
      }
    } finally {
      sProvider.close();
    }
    
    return forumStatistic;
  }

  public void saveForumStatistic(ForumStatistic forumStatistic) throws Exception {
    SessionProvider sProvider = SessionProvider.createSystemProvider();
    try {
      Node forumStatisticNode = getForumStatisticsNode(sProvider);
      forumStatisticNode.setProperty(EXO_POST_COUNT, forumStatistic.getPostCount());
      forumStatisticNode.setProperty(EXO_TOPIC_COUNT, forumStatistic.getTopicCount());
      forumStatisticNode.setProperty(EXO_MEMBERS_COUNT, forumStatistic.getMembersCount());
      forumStatisticNode.setProperty(EXO_ACTIVE_USERS, forumStatistic.getActiveUsers());
      forumStatisticNode.setProperty(EXO_MOST_USERS_ONLINE, forumStatistic.getMostUsersOnline());
      if (!Utils.isEmpty(forumStatistic.getNewMembers()))
        forumStatisticNode.setProperty(EXO_NEW_MEMBERS, forumStatistic.getNewMembers());
      if (forumStatisticNode.isNew()) {
        forumStatisticNode.getSession().save();
      } else {
        forumStatisticNode.save();
      }
    } catch (Exception e) {
      LOG.error("Failed to save forum statistics", e);
    } finally {
      sProvider.close();
    }
  }

  public Object getObjectNameByPath(String path) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    Object object;
    try {
      if (path.indexOf(KSDataLocation.Locations.FORUM_CATEGORIES_HOME) < 0 && (path.indexOf(Utils.CATEGORY) >= 0)) {
        path = getCategoryHome(sProvider).getPath() + "/" + path;
      } else if(path.indexOf(Utils.TAG) == 0){
        path = getTagHome(sProvider).getPath() + "/" + path;
      }
      Node myNode = (Node) getForumHomeNode(sProvider).getSession().getItem(path);
      if (path.indexOf(Utils.POST) > 0) {
        Post post = new Post();
        post.setId(myNode.getName());
        post.setPath(path);
        post.setName(myNode.getProperty(EXO_NAME).getString());
        object = post;
      } else if (path.indexOf(Utils.TOPIC) > 0) {
        Topic topic = new Topic();
        topic.setId(myNode.getName());
        topic.setPath(path);
        topic.setTopicName(myNode.getProperty(EXO_NAME).getString());
        object = topic;
      } else if (path.indexOf(Utils.FORUM) > 0 && (path.lastIndexOf(Utils.FORUM) > path.indexOf(Utils.CATEGORY))) {
        Forum forum = new Forum();
        forum.setId(myNode.getName());
        forum.setPath(path);
        forum.setForumName(myNode.getProperty(EXO_NAME).getString());
        object = forum;
      } else if (path.indexOf(Utils.CATEGORY) > 0) {
        Category category = new Category();
        category.setId(myNode.getName());
        category.setPath(path);
        category.setCategoryName(myNode.getProperty(EXO_NAME).getString());
        object = category;
      } else if (path.indexOf(Utils.TAG) > 0) {
        Tag tag = new Tag();
        tag.setId(myNode.getName());
        tag.setName(myNode.getProperty(EXO_NAME).getString());
        object = tag;
      } else
        return null;
      return object;
    } catch (RepositoryException e) {
      return null;
    }
  }

  public Object getObjectNameById(String id, String type) throws Exception {
    Object object = null;
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      Node node = getNodeById(sProvider, id, type);
      if (type.equals(Utils.CATEGORY)) {
        Category category = getCategory(node);
        object = category;
      } else if (type.equals(Utils.FORUM)) {
        Forum forum = getForum(node);
        object = forum;
      } else if (type.equals(Utils.TOPIC)) {
        Topic topic = getTopicNode(node);
        object = topic;
      } else {
        Post post = getPost(node);
        object = post;
      }
    } catch (Exception e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Can not get " + type + " by Id: " + id, e);
      }
    }
    return object;
  }

  private Node getNodeById(SessionProvider sProvider, String id, String type) {
    try {
      Node categoryHome = getCategoryHome(sProvider);
      if (type.equals(Utils.CATEGORY))
        return categoryHome.getNode(id);
      QueryManager qm = categoryHome.getSession().getWorkspace().getQueryManager();
      StringBuffer stringBuffer = new StringBuffer(JCR_ROOT).append(categoryHome.getPath()).append("//element(*,exo:").append(type)
                  .append(")[(@").append(EXO_ID).append("='").append(id).append("') or (fn:name()='").append(id).append("')]");
      Query query = qm.createQuery(stringBuffer.toString(), Query.XPATH);
      QueryResult result = query.execute();
      NodeIterator iter = result.getNodes();
      if (iter.getSize() > 0) return iter.nextNode();
    } catch (Exception e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Can not get Node by Id: " + id, e);
      }
    }
    return null;
  }

  public List<ForumLinkData> getAllLink(String strQueryCate, String strQueryForum) throws Exception {
    List<ForumLinkData> forumLinks = new ArrayList<ForumLinkData>();
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    Node categoryHome = getCategoryHome(sProvider);
    QueryManager qm = categoryHome.getSession().getWorkspace().getQueryManager();
    StringBuffer queryString = new StringBuffer();
    queryString.append(JCR_ROOT)
               .append(categoryHome.getPath())
               .append("/element(*,exo:forumCategory)")
               .append(strQueryCate)
               .append(" order by @exo:categoryOrder ascending, @exo:createdDate ascending");
    Query query = qm.createQuery(queryString.toString(), Query.XPATH);
    QueryResult result = query.execute();
    NodeIterator iter = result.getNodes();
    ForumLinkData linkData;
    while (iter.hasNext()) {
      linkData = new ForumLinkData();
      Node cateNode = iter.nextNode();
      linkData.setId(cateNode.getName());
      linkData.setName(cateNode.getProperty(EXO_NAME).getString());
      linkData.setType(Utils.CATEGORY);
      linkData.setPath(cateNode.getName());
      forumLinks.add(linkData);
      {
        queryString = new StringBuffer();
        queryString.append(JCR_ROOT)
                   .append(cateNode.getPath())
                   .append("/element(*,exo:forum)")
                   .append(strQueryForum)
                   .append(" order by @exo:forumOrder ascending,@exo:createdDate ascending");
        query = qm.createQuery(queryString.toString(), Query.XPATH);
        result = query.execute();
        NodeIterator iterForum = result.getNodes();
        while (iterForum.hasNext()) {
          linkData = new ForumLinkData();
          Node forumNode = iterForum.nextNode();
          linkData.setId(forumNode.getName());
          linkData.setName(forumNode.getProperty(EXO_NAME).getString());
          linkData.setType(Utils.FORUM);
          linkData.setPath(cateNode.getName() + "/" + forumNode.getName());
          if (forumNode.hasProperty(EXO_IS_LOCK))
            linkData.setIsLock(forumNode.getProperty(EXO_IS_LOCK).getBoolean());
          if (forumNode.hasProperty(EXO_IS_CLOSED))
            linkData.setIsClosed(forumNode.getProperty(EXO_IS_CLOSED).getBoolean());
          forumLinks.add(linkData);
        }
      }
    }
    return forumLinks;
  }

  public List<ForumSearchResult> getQuickSearch(String textQuery, String type_, String pathQuery, String userId, List<String> listCateIds, List<String> listForumIds, List<String> forumIdsOfModerator) throws Exception {
    List<ForumSearchResult> listSearchEvent = new ArrayList<ForumSearchResult>();
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      Node categoryHome = getCategoryHome(sProvider);
      QueryManager qm = categoryHome.getSession().getWorkspace().getQueryManager();

      // Check path query
      if (pathQuery == null || pathQuery.length() <= 0) {
        pathQuery = categoryHome.getPath();
      }
      textQuery = StringUtils.replace(textQuery, "'", "&apos;");
      String[] values = type_.split(",");// user(admin or not admin), type(forum, topic, post)
      boolean isAdmin = false;
      if (values[0].equals("true"))
        isAdmin = true;
      String types[] = new String[] { Utils.CATEGORY, Utils.FORUM, Utils.TOPIC, Utils.POST };

      if (!values[1].equals("all")) {
        types = values[1].split("/");
      }

      boolean isAnd = false;
      String searchBy = null;
      List<String> listOfUser = new ArrayList<String>();

      // If user isn't admin , get all membership of user
      if (!isAdmin) {
        listOfUser = UserHelper.getAllGroupAndMembershipOfUser(userId);

        // Get all category & forum that user can view
        Map<String, List<String>> mapList = getCategoryViewer(categoryHome, listOfUser, listCateIds, listForumIds, EXO_USER_PRIVATE);
        listCateIds = mapList.get(Utils.CATEGORY);
        listForumIds = mapList.get(Utils.FORUM);
      }
      for (String type : types) {
        StringBuilder queryString = new StringBuilder();
        // select * from exo:type -- category, forum ,topic , post
        queryString.append("SELECT * FROM ").append("exo:").append(type).append(" WHERE (");

        // if search in category and list category that user can view not null
        if (type.equals(Utils.CATEGORY)) {
          if (listCateIds != null && listCateIds.size() > 0) {
            queryString.append("(");
            // select all category have name in list user can view
            for (int i = 0; i < listCateIds.size(); i++) {
              queryString.append("fn:name() = '").append(listCateIds.get(i)).append("'");
              if (i < listCateIds.size() - 1) {
                  queryString.append(" or ");
              }
            }
            queryString.append(") and ");
          }
          // Select all forum that user can view
        } else if (listForumIds != null && listForumIds.size() > 0) {
          if (type.equals(Utils.FORUM)) {
            searchBy = "fn:name()";
          } else {
            searchBy = "exo:path";
          }
          queryString.append("(");
          for (int i = 0; i < listForumIds.size(); i++) {
            queryString.append(searchBy).append("='").append(listForumIds.get(i)).append("'");
            if (i < listForumIds.size() - 1) {
                queryString.append(" or ");
            }
          }
          queryString.append(") and ");
        }
        // Append text query
        if (textQuery != null && textQuery.length() > 0 && !textQuery.equals("null")) {
          queryString.append("CONTAINS(., '").append(textQuery).append("')").append(" AND NOT CONTAINS(")
                  .append(EXO_USER_PRIVATE).append(",'").append(textQuery).append("')");
          isAnd = true;
        } 

        // if user isn't admin
        if (!isAdmin) {
          StringBuilder builder = new StringBuilder();

          // check user if user is moderator
          if (forumIdsOfModerator != null && !forumIdsOfModerator.isEmpty()) {
            for (String string : forumIdsOfModerator) {
              builder.append(" or (exo:path='").append(string).append("')");
            }
          }

          // search all open forums that the user can access (view) or he is their owner
          if (type.equals(Utils.FORUM)) {
            String whoCanView = Utils.buildSQLByUserInfo(EXO_VIEWER, listOfUser);

            if (!Utils.isEmpty(whoCanView)) {
              if (isAnd){
                queryString.append(" and ");
              }
              queryString.append("(").append(Utils.EXO_OWNER).append("='").append(userId).append("' or ")
                  .append(Utils.buildSQLHasProperty(EXO_CAN_VIEW)).append(" or ").append(whoCanView)
                  .append(")");
            }
            if (isAnd) {
              queryString.append(" and ");
            }
            queryString.append("(exo:isClosed='false'");
            for (String forumId : forumIdsOfModerator) {
              queryString.append(" or fn:name()='").append(forumId).append("'");
            }
            queryString.append(")");
          } else {
            // search topic
            if (type.equals(Utils.TOPIC)) {
              if (isAnd) {
                queryString.append(" and ");
              }
              queryString.append("((exo:isClosed='false' and exo:isWaiting='false' and exo:isApproved='true' and exo:isActive='true' and exo:isActiveByForum='true')");
              if (builder.length() > 0) {
                queryString.append(builder);
              }
              queryString.append(")");
              String whoCanView = Utils.buildSQLByUserInfo(EXO_CAN_VIEW, listOfUser);
              if (!Utils.isEmpty(whoCanView)) {
                if (isAnd){
                  queryString.append(" and ");
                }
                queryString.append("(").append(Utils.EXO_OWNER).append("='").append(userId).append("' or ")
                           .append(Utils.buildSQLHasProperty(EXO_CAN_VIEW)).append(" or ").append(whoCanView)
                           .append(")");
              }

              // seach post
            } else if (type.equals(Utils.POST)) {
              if (isAnd) {
                queryString.append(" and ");
              }
              queryString.append("((exo:isApproved='true' and exo:isHidden='false' and exo:isActiveByTopic='true')");
              if (builder.length() > 0) {
                queryString.append(builder);
              }
              queryString.append(") and (exo:userPrivate='exoUserPri'").append(" or exo:userPrivate='").append(userId).append("') and exo:isFirstPost='false'");
            }
          }
        } else {
          if (type.equals(Utils.POST)) {
            if (isAnd)
              queryString.append(" and ");
            queryString.append("(exo:userPrivate='exoUserPri'").append(" or exo:userPrivate='").append(userId).append("') and exo:isFirstPost='false'");
          }
        }
        queryString.append(")");

        Query query = qm.createQuery(queryString.toString(), Query.SQL);
        
        QueryResult result = query.execute();
        NodeIterator iter = result.getNodes();
        while (iter.hasNext()) {
          Node nodeObj = iter.nextNode();
          listSearchEvent.add(setPropertyForForumSearch(nodeObj, type));
        }

        if (type.equals(Utils.POST)) {
          listSearchEvent.addAll(getSearchByAttachment(categoryHome, pathQuery, textQuery, listForumIds, listOfUser, isAdmin, ""));
        }
      }
      // System.out.println("\n\n=======>listSearchEvent: "+listSearchEvent.size());
      if (!isAdmin && listSearchEvent.size() > 0) {
        List<String> categoryCanView = new ArrayList<String>();
        List<String> forumCanView = new ArrayList<String>();
        Map<String, List<String>> mapList = getCategoryViewer(categoryHome, listOfUser, listCateIds, new ArrayList<String>(), EXO_VIEWER);
        categoryCanView = mapList.get(Utils.CATEGORY) != null ? mapList.get(Utils.CATEGORY) : new ArrayList<>();
        forumCanView.addAll(getCachedDataStorage().getForumUserCanView(listOfUser, listForumIds));
        if (categoryCanView.size() > 0 || forumCanView.size() > 0)
          listSearchEvent = removeItemInList(listSearchEvent, forumCanView, categoryCanView);
      }
      
    } catch (Exception e) {
      throw e;
    }
    return listSearchEvent;
  }

  public List<ForumSearchResult> getUnifiedSearch(String textQuery, String userId, Integer offset, Integer limit, String sort, String order) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    List<ForumSearchResult> list = new ArrayList<ForumSearchResult>();
    try {
      Node categoryHome = getCategoryHome(sProvider);
      QueryManager qm = categoryHome.getSession().getWorkspace().getQueryManager();

      //String rootPath = categoryHome.getPath();

      //process query for asterisk
      textQuery = textQuery.trim();
      textQuery = CommonUtils.processUnifiedSearchSearchCondition(textQuery);
      String asteriskQuery = CommonUtils.normalizeUnifiedSearchInput(textQuery);
      
      if (Utils.isEmpty(asteriskQuery)) return list;

      boolean isAdmin = isAdminRole(userId);

      List<String> listOfUser = UserHelper.getAllGroupAndMembershipOfUser(null);
      List<String> listForumIds = getCachedDataStorage().getForumUserCanView(listOfUser, new ArrayList<String>());

      //for (String type : types) {
        StringBuilder queryString = buildSQLQueryUnifiedSearch(listForumIds, asteriskQuery, textQuery, isAdmin, sort, order, userId, listOfUser);
        LOG.debug("UnifiedSearch statement query: " + queryString.toString());
        QueryImpl query = (QueryImpl)qm.createQuery(queryString.toString(), Query.SQL);
        query.setLimit(30);
        query.setOffset(offset);
        //query.setCaseInsensitiveOrder(true);
        QueryResult result = query.execute();
        NodeIterator iter = result.getNodes();
        //RowIterator rowIterator = result.getRows();
        
        while (iter.hasNext() && limit > 0) {
          Node nodeObj = iter.nextNode();
          //Row row = rowIterator.nextRow();
          if(hasPermssionViewerPost(nodeObj, listOfUser) == false) {
            continue;
          }
          list.add(setPropertyUnifiedSearch(nodeObj, textQuery));
          limit--;
        }

    } catch (Exception e) {
      throw e;
    }
    
    //
    return UnifiedSearchOrder.processOrder(list, sort, order);
  }
  
  private StringBuilder buildSQLQueryUnifiedSearch(List<String> listForumIds, String asteriskQuery,
                                                   String textQuery, boolean isAdmin, String sort, String order,
                                                   String userId, List<String> listOfUser) {
    
    StringBuilder queryString = new StringBuilder();
    
    queryString.append("select exo:name, exo:message, rep:excerpt() from exo:post where ");
    
    if (listForumIds != null && listForumIds.size() > 0) {
      queryString.append("(");
      for (int i = 0; i < listForumIds.size(); i++) {
        queryString.append(EXO_PATH).append("='").append(listForumIds.get(i)).append("'");
        if (i < listForumIds.size() - 1)
          queryString.append(" or ");
      }
      queryString.append(") and ");
    }
    queryString.append("(")
               .append("CONTAINS (exo:message, '").append(asteriskQuery).append("')")
               .append(" or (exo:isFirstPost='true' and ")
               .append("CONTAINS (exo:name, '").append(asteriskQuery).append("'))")
               .append(")");

    // if user isn't admin
    if (!isAdmin) {
      queryString.append(" and ");
      queryString.append("(exo:isApproved='true' and exo:isHidden='false' and exo:isActiveByTopic='true')");
      queryString.append(" and (exo:userPrivate='exoUserPri'").append(" or exo:userPrivate='").append(userId).append("')");
    } else {
      queryString.append(" and ");
      queryString.append("(exo:userPrivate='exoUserPri'").append(" or exo:userPrivate='").append(userId).append("')");
    }

    if ("date".equalsIgnoreCase(sort)) {
      queryString.append(" order by ").append(EXO_CREATED_DATE);
    } else if ("title".equalsIgnoreCase(sort) || Utils.isEmpty(sort)) {
      queryString.append(" order by ").append(EXO_NAME);
    } if("relevancy".equalsIgnoreCase(sort)) {
      queryString.append(" order by ").append(JCR_SCORE);
    }

    queryString.append(" ").append(order);
    return queryString;
  }
  
  private boolean hasPermssionViewerPost(Node postNode, List<String> listOfUser) throws Exception {
    Node topicNode = postNode.getParent();
    PropertyReader reader = new PropertyReader(topicNode);
    List<String> listOfCanviewrs = reader.list(EXO_CAN_VIEW, new ArrayList<String>());
    if (listOfUser != null && listOfUser.size() > 0 && reader.string(EXO_OWNER, "").equals(listOfUser.get(0))) {
      return true;
    }
    return listOfCanviewrs.isEmpty() || Utils.hasPermission(listOfCanviewrs, listOfUser);
  }

  private boolean hasPermissionViewForum(Node forumNode, List<String> listOfUser) throws Exception {
    PropertyReader reader = new PropertyReader(forumNode);
    List<String> whoCanView = reader.list(EXO_VIEWER, new ArrayList<>());
    if (listOfUser != null && listOfUser.size() > 0 && reader.string(EXO_OWNER, "").equals(listOfUser.get(0))) {
      return true;
    }
    return whoCanView.isEmpty() || Utils.hasPermission(whoCanView, listOfUser);
  }

  private ForumSearchResult setPropertyUnifiedSearch(Node nodeObj, String originQuery) throws Exception {
    ForumSearchResult forumSearch = setPropertyForForumSearch(nodeObj, Utils.POST);
    //forumSearch.setExcerpt(originQuery);
    //forumSearch.setRelevancy(1);
    try {
      //
      forumSearch.setRelevancy(1);
      originQuery = CommonUtils.removeSpecialCharacterForUnifiedSearch(originQuery);

      //
      String excerpt = highlightText(nodeObj.getProperty(EXO_MESSAGE).getString(), originQuery);
      // if excerpt does not contain highlight text and text query, using field
      // name to display excerpt
      if (!HIGHLIHT_PATTERN.matcher(excerpt).find()
          && excerpt.toLowerCase().indexOf(originQuery) < 0) {
        excerpt = highlightText(nodeObj.getProperty(EXO_NAME).getString(), originQuery);
      }
      forumSearch.setExcerpt(excerpt);
    } catch (Exception e) {
      LOG.error("Failed to set property for unified search.", e);
    }
    return forumSearch;
  }
  
  private String highlightText(String message, String termToHighlight) {
    for (String term : termToHighlight.split(" ")) {
      message = message.replace(term, "<strong>" + term + "</strong>");
    }
    return message;
  }
  
  private List<ForumSearchResult> removeItemInList(List<ForumSearchResult> listSearchEvent, List<String> forumCanView, List<String> categoryCanView) {
    List<ForumSearchResult> tempListSearchEvent = new ArrayList<ForumSearchResult>();
    String path = null;
    String[] strs;
    for (ForumSearchResult forumSearch : listSearchEvent) {
      path = forumSearch.getPath();
      if (!path.contains(Utils.TOPIC)) {// search category or forum
        tempListSearchEvent.add(forumSearch);
        continue;
      }
      strs = path.split("/");
      if (categoryCanView.contains(strs[5]) || forumCanView.contains(strs[6])) {
        tempListSearchEvent.add(forumSearch);
      }
    }

    return tempListSearchEvent;
  }

  public List<String> getForumUserCanView(List<String> listOfUser, List<String> listForumIds) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    List<String> listForum = new ArrayList<String>();
    Node categoryHome = getCategoryHome(sProvider);
    QueryManager qm = categoryHome.getSession().getWorkspace().getQueryManager();
    StringBuilder queryString = new StringBuilder();
    if(listOfUser == null || listOfUser.isEmpty()) {
      listOfUser = new ArrayList<String>();
      listOfUser.add(UserProfile.USER_GUEST);
    }
    // select all forum
//    queryString.append(JCR_ROOT).append(categoryHome.getPath()).append("//element(*,").append(EXO_FORUM).append(")[")
//               .append("(").append(Utils.buildXpathHasProperty(EXO_VIEWER))
//               .append(" or ").append(Utils.buildXpathByUserInfo(EXO_VIEWER, listOfUser)).append(")")
//               .append(" or (").append(Utils.buildXpathByUserInfo(EXO_MODERATORS, listOfUser)).append(")")
//               .append("]");
//    Query query = qm.createQuery(queryString.toString(), Query.XPATH);
    
    queryString.append("SELECT * FROM ").append(EXO_FORUM).append(" WHERE (")
        .append(Utils.buildSQLHasProperty(EXO_VIEWER))
        .append(" OR ").append(Utils.buildSQLByUserInfo(EXO_VIEWER, listOfUser)).append(")")
        .append(" OR (").append(Utils.buildSQLByUserInfo(EXO_MODERATORS, listOfUser)).append(")");
    Query query = qm.createQuery(queryString.toString(), Query.SQL);
    QueryResult result = query.execute();
    NodeIterator iter = result.getNodes();
    Node forumNode = null;
    String forumId = null;
    while (iter.hasNext()) {
      forumNode = iter.nextNode();
      forumId = forumNode.getName();
      if (listForumIds != null && !listForumIds.isEmpty()) {
        if (listForumIds.contains(forumId)) {
          listForum.add(forumId);
        }
      } else {
        listForum.add(forumId);
      }
    }
    
    // If user isn't admin , get all membership of user
    if (!isAdminRole(listOfUser.get(0))) {
      // Get all category & forum that user can view
      Map<String, List<String>> mapList = getCategoryViewer(categoryHome, listOfUser, new ArrayList<String>(), listForum, EXO_USER_PRIVATE);
      listForum = mapList.get(Utils.FORUM);
    }
    
    return listForum;
  }

  public List<ForumSearchResult> getAdvancedSearch(ForumEventQuery eventQuery, List<String> listCateIds, List<String> listForumIds){
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    List<ForumSearchResult> listSearchEvent = new ArrayList<ForumSearchResult>();
    try {
      Node categoryHome = getCategoryHome(sProvider);
      QueryManager qm = categoryHome.getSession().getWorkspace().getQueryManager();
      String path = eventQuery.getPath();
      if (path == null || path.length() <= 0) {
        path = categoryHome.getPath();
      }
      eventQuery.setPath(path);
      String type = eventQuery.getType();
      String queryString = null;
      List<String> listOfUser = eventQuery.getListOfUser();
      if (eventQuery.getUserPermission() > 0) {
        Map<String, List<String>> mapList = getCategoryViewer(categoryHome, listOfUser, listCateIds, listForumIds, EXO_USER_PRIVATE);
        listCateIds = mapList.get(Utils.CATEGORY);
        listForumIds = mapList.get(Utils.FORUM);
      }
      if (type.equals(Utils.CATEGORY)) {
        queryString = eventQuery.getPathQuery(listCateIds);
      } else {
        queryString = eventQuery.getPathQuery(listForumIds);
      }
      Query query = qm.createQuery(queryString, Query.XPATH);
      QueryResult result = query.execute();
      NodeIterator iter = result.getNodes();
      while (iter.hasNext()) {
        Node nodeObj = iter.nextNode();
        listSearchEvent.add(setPropertyForForumSearch(nodeObj, type));
      }
      // Note: Query Attachment in post.
      if ((type.equals(Utils.POST) || type.equals(Utils.TOPIC)) && !Utils.isEmpty(eventQuery.getKeyValue())) {
        boolean isAdmin = false;
        if (eventQuery.getUserPermission() == 0)
          isAdmin = true;
        listSearchEvent.addAll(getSearchByAttachment(categoryHome, eventQuery.getPath(), eventQuery.getKeyValue(), listForumIds, eventQuery.getListOfUser(), isAdmin, type));
      }
      if (eventQuery.getUserPermission() > 0) {
        List<String> categoryCanView = new ArrayList<String>();
        List<String> forumCanView = new ArrayList<String>();
        Map<String, List<String>> mapList = getCategoryViewer(categoryHome, listOfUser, listCateIds, listForumIds, "@exo:viewer");
        categoryCanView = mapList.get(Utils.CATEGORY);
        forumCanView.addAll(mapList.get(Utils.FORUM));
        forumCanView.addAll(getCachedDataStorage().getForumUserCanView(listOfUser, listForumIds));
        if (categoryCanView.size() > 0 || forumCanView.size() > 0)
          listSearchEvent = removeItemInList(listSearchEvent, forumCanView, categoryCanView);
      }
    } catch (Exception e) {
      if (LOG.isDebugEnabled()){
        LOG.debug("Failed to do advanced search", e);
      }
      
    }
    return listSearchEvent;
  }

  private List<ForumSearchResult> getSearchByAttachment(Node categoryHome, String path, String key, List<String> listForumIds, List<String> listOfUser, boolean isAdmin, String type) throws Exception {
    List<ForumSearchResult> listSearchEvent = new ArrayList<ForumSearchResult>();
    try {
      QueryManager qm = categoryHome.getSession().getWorkspace().getQueryManager();
      StringBuilder strQuery = new StringBuilder();
      strQuery.append(JCR_ROOT).append(path).append("//element(*,nt:resource) [");
      strQuery.append("(jcr:contains(., '").append(key).append("*'))]");
      Query query = qm.createQuery(strQuery.toString(), Query.XPATH);
      QueryResult result = query.execute();
      NodeIterator iter = result.getNodes();
      boolean isAdd = true;

      String type_ = type;
      while (iter.hasNext()) {
        Node nodeObj = iter.nextNode().getParent().getParent();
        if (nodeObj.isNodeType(EXO_POST)) {
          if (type == null || type.length() == 0) {
            if (nodeObj.getProperty(EXO_IS_FIRST_POST).getBoolean()) {
              type_ = Utils.TOPIC;
            } else {
              type_ = Utils.POST;
            }
          } else {
            if (nodeObj.getProperty(EXO_IS_FIRST_POST).getBoolean()) {
              if (!type.equals(Utils.TOPIC))
                continue;
            } else {
              if (type.equals(Utils.TOPIC))
                continue;
            }
          }
          // check scoping, private by category.
          if (!isAdmin && !listForumIds.isEmpty()) {
            String path_ = nodeObj.getPath();
            path_ = path_.substring(path_.lastIndexOf(Utils.FORUM), path_.lastIndexOf("/" + Utils.TOPIC));
            if (listForumIds.contains(path_))
              isAdd = true;
            else
              isAdd = false;
          }
          if (isAdd) {
            // check post private
            List<String> list = Utils.valuesToList(nodeObj.getProperty(EXO_USER_PRIVATE).getValues());
            if (!list.get(0).equals(EXO_USER_PRI) && !Utils.isListContentItemList(listOfUser, list))
              isAdd = false;
            // not is admin
            if (isAdd && !isAdmin) {
              // not is moderator
              list = Utils.valuesToList(nodeObj.getParent().getParent().getProperty(EXO_MODERATORS).getValues());
              if (!Utils.hasPermission(listOfUser, list)) {
                // can view by topic
                list = Utils.valuesToList(nodeObj.getParent().getProperty(EXO_CAN_VIEW).getValues());
                if (list != null && list.size() > 0 && !Utils.isEmpty(list.get(0))) {
                  if (!Utils.hasPermission(listOfUser, list))
                    isAdd = false;
                }
                if (isAdd) {
                  // check by post
                  Post post = getPost(nodeObj);
                  if (!post.getIsActiveByTopic() || !post.getIsApproved() || post.getIsHidden() || post.getIsWaiting())
                    isAdd = false;
                }
              }
            }
          }
          if (isAdd) {
            if (type_.equals(Utils.TOPIC))
              nodeObj = nodeObj.getParent();
            listSearchEvent.add(setPropertyForForumSearch(nodeObj, type_));
          }
        }
      }
    } catch (Exception e) {
      LOG.error("Search by attachment has failed", e);
    }
    return listSearchEvent;
  }

  private ForumSearchResult setPropertyForForumSearch(Node nodeObj, String type) throws Exception {
    ForumSearchResult forumSearch = new ForumSearchResult();
    forumSearch.setId(nodeObj.getName());
    forumSearch.setPath(nodeObj.getPath());
    PropertyReader reader = new PropertyReader(nodeObj);
    forumSearch.setName(reader.string(EXO_NAME, ""));
    forumSearch.setType(type);
    forumSearch.setCreatedDate(reader.date(EXO_CREATED_DATE));
    if (type.equals(Utils.FORUM)) {
      if (reader.bool(EXO_IS_CLOSED))
        forumSearch.setIcon("ForumCloseIcon");
      else if (reader.bool(EXO_IS_LOCK))
        forumSearch.setIcon("ForumLockedIcon");
      else
        forumSearch.setIcon("ForumNormalIcon");
    } else if (type.equals(Utils.TOPIC)) {
      forumSearch.setContent(reader.string(EXO_DESCRIPTION));
      if (reader.bool(EXO_IS_CLOSED))
        forumSearch.setIcon("HotThreadNoNewClosePost");
      else if (reader.bool(EXO_IS_LOCK))
        forumSearch.setIcon("HotThreadNoNewLockPost");
      else
        forumSearch.setIcon("HotThreadNoNewPost");
    } else if (type.equals(Utils.CATEGORY)) {
      forumSearch.setIcon("CategoryIcon");
    } else {
      forumSearch.setIcon(reader.string(EXO_ICON, ""));
      forumSearch.setContent(reader.string(EXO_MESSAGE));
    }
    return forumSearch;
  }

  /**
   * 
   * @param categoryHome
   * @param listOfUser
   *          all group and membership user belong to
   * @param listCateIds
   *          all category visible
   * @param listForumIds
   *          all forum visible
   * @return
   * @throws Exception
   */
  private Map<String, List<String>> getCategoryViewer(Node categoryHome, List<String> listOfUser, List<String> listCateIds, List<String> listForumIds, String property) throws Exception {
    Map<String, List<String>> mapList = new HashMap<String, List<String>>();
    if (listOfUser == null || listOfUser.isEmpty()) {
      listOfUser = new ArrayList<String>();
      listOfUser.add(UserProfile.USER_GUEST);
    }

    QueryManager qm = categoryHome.getSession().getWorkspace().getQueryManager();
    StringBuilder queryString = new StringBuilder();

    queryString.append(JCR_ROOT).append(categoryHome.getPath()).append("/element(*,").append(EXO_FORUM_CATEGORY).append(")[")
               
               .append("(").append(Utils.buildXpathHasProperty(property))
               .append(" or ").append(Utils.buildXpathByUserInfo(property, listOfUser)).append(")")
               .append(" or (").append(Utils.buildXpathByUserInfo(EXO_MODERATORS, listOfUser)).append(")")
               .append("]");

    Query query = qm.createQuery(queryString.toString(), Query.XPATH);
    QueryResult result = query.execute();
    NodeIterator iter = result.getNodes();
    NodeIterator iter1 = null;

    // Check if the result is not all
    if (iter.getSize() > 0) {
      String forumId, cateId;
      List<String> listForumId = new ArrayList<String>();
      List<String> listCateId = new ArrayList<String>();

      // Check all category in result.If it can visible then add it to list.
      while (iter.hasNext()) {
        Node catNode = iter.nextNode();
        cateId = catNode.getName();
        if (listCateIds != null && !listCateIds.isEmpty()) {
          if (listCateIds.contains(cateId)) {
            listCateId.add(cateId);
          }
        } else {
          listCateId.add(cateId);
        }

        // Check all forum in result if it visible then get it
        iter1 = catNode.getNodes();
        while (iter1.hasNext()) {
          Node forumNode = iter1.nextNode();
          if (forumNode.isNodeType(EXO_FORUM) && hasPermissionViewForum(forumNode,listOfUser)) {
            forumId = forumNode.getName();
            if (listForumIds != null && !listForumIds.isEmpty()) {
              if (listForumIds.contains(forumId)) {
                listForumId.add(forumId);
              }
            } else {
              listForumId.add(forumId);
            }
          }
        }
      }
      mapList.put(Utils.FORUM, listForumId);
      mapList.put(Utils.CATEGORY, listCateId);
    } else if (iter.getSize() == 0) {
      if (!property.equals(EXO_VIEWER)) {
        listForumIds = new ArrayList<String>();
        listForumIds.add("forumId");
        mapList.put(Utils.FORUM, listForumIds);
        listCateIds = new ArrayList<String>();
        listCateIds.add("cateId");
        mapList.put(Utils.CATEGORY, listCateIds);
      } else {
        mapList.put(Utils.FORUM, new ArrayList<String>());
        mapList.put(Utils.CATEGORY, new ArrayList<String>());
      }
    } else {
      mapList.put(Utils.FORUM, listForumIds);
      mapList.put(Utils.CATEGORY, listCateIds);
    }
    return mapList;
  }

  public void addWatch(int watchType, String path, List<String> values, String currentUser) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      Node categoryHome = getCategoryHome(sProvider);
      if (watchType == -1) {
        QueryManager qm = categoryHome.getSession().getWorkspace().getQueryManager();
        StringBuffer queryString = new StringBuffer();
        queryString.append(JCR_ROOT).append(categoryHome.getPath()).append("//*[@exo:id='").append(path).append("']");
        Query query = qm.createQuery(queryString.toString(), Query.XPATH);
        QueryResult result = query.execute();
        NodeIterator iterator = result.getNodes();
        path = iterator.nextNode().getPath();
      }

      if (path.indexOf(categoryHome.getName()) < 0)
        path = categoryHome.getPath() + "/" + path;
      Node watchingNode = (Node) categoryHome.getSession().getItem(path);

      // add watching for node
      List<String> listUsers = new ArrayList<String>();
      if (watchingNode.isNodeType(EXO_FORUM_WATCHING)) {
        if (watchType == 1) {// send email when had changed on category
          List<String> listEmail = new ArrayList<String>();
          if (watchingNode.hasProperty(EXO_EMAIL_WATCHING))
            listEmail.addAll(Utils.valuesToList(watchingNode.getProperty(EXO_EMAIL_WATCHING).getValues()));
          if (watchingNode.hasProperty(EXO_USER_WATCHING))
            listUsers.addAll(Utils.valuesToList(watchingNode.getProperty(EXO_USER_WATCHING).getValues()));
          for (String str : values) {
            if (listEmail.contains(str))
              continue;
            listEmail.add(0, str);
            listUsers.add(0, currentUser);
          }
          watchingNode.setProperty(EXO_EMAIL_WATCHING, Utils.getStringsInList(listEmail));
          watchingNode.setProperty(EXO_USER_WATCHING, Utils.getStringsInList(listUsers));
        } else {
          watchingNode.setProperty(EXO_RSS_WATCHING, getValueProperty(watchingNode, EXO_RSS_WATCHING, currentUser));
        }
      } else {
        watchingNode.addMixin(EXO_FORUM_WATCHING);
        if (watchType == 1) { // send email when had changed on category
          for (int i = 0; i < values.size(); i++) {
            listUsers.add(currentUser);
          }
          watchingNode.setProperty(EXO_EMAIL_WATCHING, Utils.getStringsInList(values));
          watchingNode.setProperty(EXO_USER_WATCHING, Utils.getStringsInList(listUsers));
        } else { // add RSS watching
          watchingNode.setProperty(EXO_RSS_WATCHING, new String[] { currentUser });
        }
      }
      if (watchingNode.isNew()) {
        watchingNode.getSession().save();
      } else {
        watchingNode.save();
      }
      // if(watchType == -1)addForumSubscription(sProvider, currentUser, watchingNode.getName());
    } catch (Exception e) {
      LOG.error("Can not add Watch for user: " + currentUser, e);
    }
  }

  public void removeWatch(int watchType, String path, String values) throws Exception {
    if (Utils.isEmpty(values))
      return;
    Node watchingNode = null;
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    Node categoryHome = getCategoryHome(sProvider);
    String string = categoryHome.getPath();
    if (path.indexOf(categoryHome.getName()) < 0)
      path = string + "/" + path;
    try {
      watchingNode = (Node) categoryHome.getSession().getItem(path);
      List<String> newValues = new ArrayList<String>();
      List<String> listNewUsers = new ArrayList<String>();
      List<String> userRSS = new ArrayList<String>();
      // add watching for node
      if (watchingNode.isNodeType(EXO_FORUM_WATCHING)) {
        if (watchType == 1) {
          String[] emails = new String[] {};
          String[] listOldUsers = new String[] {};
          String[] listRss = new String[] {};
          PropertyReader reader = new PropertyReader(watchingNode);
          emails = reader.strings(EXO_EMAIL_WATCHING, new String[] {});
          listOldUsers = reader.strings(EXO_USER_WATCHING, new String[] {});
          listRss = reader.strings(EXO_RSS_WATCHING, new String[] {});

          int n = (listRss.length > listOldUsers.length) ? listRss.length : listOldUsers.length;
          for (int i = 0; i < n; i++) {
            if (listOldUsers.length > i && !values.contains("/" + emails[i])) {
              newValues.add(emails[i]);
              listNewUsers.add(listOldUsers[i]);
            }
            if (listRss.length > i && !values.contains(listRss[i] + "/"))
              userRSS.add(listRss[i]);
          }
          watchingNode.setProperty(EXO_EMAIL_WATCHING, Utils.getStringsInList(newValues));
          watchingNode.setProperty(EXO_USER_WATCHING, Utils.getStringsInList(listNewUsers));
          watchingNode.setProperty(EXO_RSS_WATCHING, Utils.getStringsInList(userRSS));
          if (watchingNode.isNew()) {
            watchingNode.getSession().save();
          } else {
            watchingNode.save();
          }
        }
      }
    } catch (Exception e) {
      LOG.error("Failed to remove watch.", e);
    }
  }

  public void updateEmailWatch(List<String> listNodeId, String newEmailAdd, String userId) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      Node parentNode = getForumHomeNode(sProvider);
      QueryManager qm = parentNode.getSession().getWorkspace().getQueryManager();
      StringBuffer queryString = new StringBuffer(JCR_ROOT).append(parentNode.getPath()).append("//element(*,exo:forumWatching)[(");
      for (int i = 0; i < listNodeId.size(); i++) {
        if (i > 0)
          queryString.append(" or ");
        queryString.append("@exo:id='").append(listNodeId.get(i)).append("'");
      }
      queryString.append(")]");
      Query query = qm.createQuery(queryString.toString(), Query.XPATH);
      QueryResult result = query.execute();
      NodeIterator iterator = result.getNodes();
      Node watchingNode = null;
      List<String> listEmail = null;
      List<String> listUsers = null;
      while (iterator.hasNext()) {
        watchingNode = iterator.nextNode();
        PropertyReader reader = new PropertyReader(watchingNode);
        listEmail = reader.list(EXO_EMAIL_WATCHING, new ArrayList<String>());
        listUsers = reader.list(EXO_USER_WATCHING, new ArrayList<String>());
        if (listUsers.contains(userId)) {
          for (int i = 0; i < listUsers.size(); i++) {
            if (listUsers.get(i).equals(userId)) {
              listEmail.set(i, newEmailAdd);
            }
          }
        } else {
          listUsers.add(userId);
          listEmail.add(newEmailAdd);
        }
        watchingNode.setProperty(EXO_EMAIL_WATCHING, listEmail.toArray(new String[listEmail.size()]));
        watchingNode.setProperty(EXO_USER_WATCHING, listUsers.toArray(new String[listUsers.size()]));
        watchingNode.save();
      }
    } catch (Exception e) {
      LOG.error("Failed to update email watch.", e);
    }
  }

  public List<Watch> getWatchByUser(String userId) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    List<Watch> listWatches = new ArrayList<Watch>();
    try {
      Node categoryHome = getCategoryHome(sProvider);
      StringBuffer rootPath = new StringBuffer(categoryHome.getPath());
      QueryManager qm = categoryHome.getSession().getWorkspace().getQueryManager();
      StringBuffer queryString = new StringBuffer();
      queryString.append(JCR_ROOT).append(rootPath.toString()).append("//element(*,").append(EXO_FORUM_WATCHING).append(")[(@").append(EXO_USER_WATCHING).append("='").append(userId).append("') or (@").append(EXO_RSS_WATCHING).append("='").append(userId).append("')]");

      Query query = qm.createQuery(queryString.toString(), Query.XPATH);
      QueryResult result = query.execute();
      NodeIterator iterator = result.getNodes();
      Watch watch;
      Node node;
      List<String> users;
      List<String> RSSUsers;
      String emails[];
      String path;
      StringBuffer pathName = new StringBuffer();
      String typeNode;
      PropertyReader reader;
      while (iterator.hasNext()) {
        node = iterator.nextNode();
        reader = new PropertyReader(node);
        users = reader.list(EXO_USER_WATCHING, new ArrayList<String>());
        emails = reader.strings(EXO_EMAIL_WATCHING, new String[] {});
        RSSUsers = reader.list(EXO_RSS_WATCHING, new ArrayList<String>());
        rootPath.setLength(0);
        rootPath.append(categoryHome.getPath());
        path = node.getPath();
        pathName.setLength(0);
        if (node.isNodeType(Utils.TYPE_CATEGORY)) {
          typeNode = Utils.TYPE_CATEGORY;
        } else if (node.isNodeType(Utils.TYPE_FORUM)) {
          typeNode = Utils.TYPE_FORUM;
        } else {
          typeNode = Utils.TYPE_TOPIC;
        }
        
        for (String str : (path.replace(rootPath.toString() + "/", "")).split("/")) {
          rootPath.append("/");
          rootPath.append(str);
          if (!Utils.isEmpty(pathName.toString())) {
            pathName.append(" > ");
          }
          pathName.append(((Node) categoryHome.getSession().getItem(rootPath.toString())).getProperty(EXO_NAME).getString());
        }
        watch = new Watch();
        watch.setId(node.getName());
        watch.setNodePath(path);
        watch.setUserId(userId);
        watch.setPath(pathName.toString());
        watch.setTypeNode(typeNode);
        if (users.contains(userId)) {
          watch.setEmail(emails[users.indexOf(userId)]);
          watch.setIsAddWatchByEmail(true);
        } else {
          watch.setIsAddWatchByEmail(false);
        }
        watch.setIsAddWatchByRSS(RSSUsers.contains(userId));
        listWatches.add(watch);
      }
      return listWatches;
    } catch (Exception e) {
      return listWatches;
    }
  }
  
  private void sendEmailNotification(List<String> addresses, Message message) throws Exception {
    pendingMessagesQueue.add(new SendMessageInfo(addresses, message));
  }

  public Iterator<SendMessageInfo> getPendingMessages() throws Exception {
    Iterator<SendMessageInfo> pending = new ArrayList<SendMessageInfo>(pendingMessagesQueue).iterator();
    pendingMessagesQueue.clear();
    return pending;
  }

  public void updateForum(String path) throws Exception {
    if (path == null || path.length() <= 0 || path.equals("/" + dataLocator.getForumHomeLocation())) {
      path = dataLocator.getForumHomeLocation();
      updateForum(path, true);
    } else {
      updateForum(path, false);
    }
  }

  private void updateForum(String path, boolean isReset) throws Exception {
    Map<String, Long> topicMap = new HashMap<String, Long>();
    Map<String, Long> postMap = new HashMap<String, Long>();
    if (path.indexOf("/") > 0)
      path = "/" + path;
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      Node forumStatisticNode = getStatisticHome(sProvider).getNode(Locations.FORUM_STATISTIC);
      QueryManager qm = forumStatisticNode.getSession().getWorkspace().getQueryManager();
      Query query = qm.createQuery(JCR_ROOT + path + "//element(*,exo:topic)", Query.XPATH);
      QueryResult result = query.execute();
      NodeIterator topicIter = result.getNodes();
      query = qm.createQuery(JCR_ROOT + path + "//element(*,exo:post)", Query.XPATH);
      result = query.execute();
      NodeIterator postIter = result.getNodes();

      // Update Forum statistic
      if (isReset) {
        forumStatisticNode.setProperty(EXO_POST_COUNT, postIter.getSize());
        forumStatisticNode.setProperty(EXO_TOPIC_COUNT, topicIter.getSize());
      } else if (path.indexOf(Utils.FORUM) == path.lastIndexOf(Utils.FORUM)) {
        PropertyReader statisticReader = new PropertyReader(forumStatisticNode);
        forumStatisticNode.setProperty(EXO_POST_COUNT, statisticReader.l(EXO_POST_COUNT, 0) + postIter.getSize());
        forumStatisticNode.setProperty(EXO_TOPIC_COUNT, statisticReader.l(EXO_TOPIC_COUNT, 0) + topicIter.getSize());
      }
      forumStatisticNode.save();
      // put post and topic to maps by user
      Node node;
      while (topicIter.hasNext()) {
        node = topicIter.nextNode();
        String owner = node.getProperty(EXO_OWNER).getString();
        if (topicMap.containsKey(owner)) {
          long l = topicMap.get(owner) + 1;
          topicMap.put(owner, l);
        } else {
          long l = 1;
          topicMap.put(owner, l);
        }
      }

      while (postIter.hasNext()) {
        node = postIter.nextNode();
        String owner = node.getProperty(EXO_OWNER).getString();
        if (postMap.containsKey(owner)) {
          long l = postMap.get(owner) + 1;
          postMap.put(owner, l);
        } else {
          long l = 1;
          postMap.put(owner, l);
        }
      }
      Node profileHome = getUserProfileHome(sProvider);
      Node profile;
      // update topic to user profile
      Iterator<Entry<String, Long>> it = topicMap.entrySet().iterator();
      String userId;
      Calendar cal = getGreenwichMeanTime();
      while (it.hasNext()) {
        userId = it.next().getKey();
        if (userId.indexOf(Utils.DELETED) < 0) {
          if (profileHome.hasNode(userId)) {
            profile = profileHome.getNode(userId);
          } else {
            profile = profileHome.addNode(userId, EXO_FORUM_USER_PROFILE);
            profile.setProperty(EXO_USER_ID, userId);
            profile.setProperty(EXO_LAST_LOGIN_DATE, cal);
            profile.setProperty(EXO_JOINED_DATE, cal);
            profile.setProperty(EXO_LAST_POST_DATE, cal);
          }
          long l = (isReset) ? topicMap.get(userId) : profile.getProperty(EXO_TOTAL_TOPIC).getLong() + topicMap.get(userId);
          profile.setProperty(EXO_TOTAL_TOPIC, l);

          if (postMap.containsKey(userId)) {
            long t = (isReset) ? postMap.get(userId) : profile.getProperty(EXO_TOTAL_POST).getLong() + postMap.get(userId);
            profile.setProperty(EXO_TOTAL_POST, t);
            profile.setProperty(EXO_LAST_POST_DATE, cal);
            postMap.remove(userId);
          }
        }
      }
      // update post to user profile
      it = postMap.entrySet().iterator();
      while (it.hasNext()) {
        userId = it.next().getKey();
        if (userId.indexOf(Utils.DELETED) < 0) {
          if (profileHome.hasNode(userId)) {
            profile = profileHome.getNode(userId);
          } else {
            profile = profileHome.addNode(userId, EXO_FORUM_USER_PROFILE);
            profile.setProperty(EXO_USER_ID, userId);
            profile.setProperty(EXO_LAST_LOGIN_DATE, cal);
            profile.setProperty(EXO_JOINED_DATE, cal);
          }
          long t = (isReset) ? postMap.get(userId) : profile.getProperty(EXO_TOTAL_POST).getLong() + postMap.get(userId);
          profile.setProperty(EXO_TOTAL_POST, t);
          profile.setProperty(EXO_LAST_POST_DATE, cal);
        }
      }
      if (profileHome.isNew()) {
        profileHome.getSession().save();
      } else {
        profileHome.save();
      }
      int t = (profileHome.hasNode(Utils.USER_PROFILE_DELETED)) ? 1 : 0;
      forumStatisticNode.setProperty(EXO_MEMBERS_COUNT, profileHome.getNodes().getSize() - t);
      forumStatisticNode.save();
    } catch (Exception e) {
      LOG.error("Failed to update forum", e);
    }
  }

  public SendMessageInfo getMessageInfo(String name) throws Exception {
    SendMessageInfo messageInfo = (SendMessageInfo) infoMap.get(name);
    infoMap.remove(name);
    return messageInfo;
  }

  private String getPath(String index, String path) throws Exception {
    int t = path.lastIndexOf(index);
    if (t > 0) {
      path = path.substring(t + 1);
    }
    return path;
  }

  public List<ForumSearchResult> getJobWattingForModerator(String[] paths){
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    List<ForumSearchResult> list = new ArrayList<ForumSearchResult>();
    try {
      Node categoryHome = getCategoryHome(sProvider);
      String string = categoryHome.getPath();
      QueryManager qm = categoryHome.getSession().getWorkspace().getQueryManager();
      StringBuilder builder = new StringBuilder();
      int l = paths.length;
      if (l > 0) {
        builder.append(" and (");
        for (int i = 0; i < l; i++) {
          if (i > 0)
            builder.append(" or ");
          String str = getPath(("/" + Utils.FORUM), paths[i]);
          builder.append("@exo:path='").append(str).append("'");
        }
        builder.append(")");
      }
      Query query;
      NodeIterator iter;
      QueryResult result;
      StringBuilder stringBuilder = new StringBuilder(JCR_ROOT).append(string).append("//element(*,").append(EXO_TOPIC).append(")")
                                  .append("[(@").append(EXO_IS_APPROVED).append("='false' or @")
                                  .append(EXO_IS_WAITING).append("='true')").append(builder).append("] order by @exo:modifiedDate descending");
      
      query = qm.createQuery(stringBuilder.toString(), Query.XPATH);
      result = query.execute();
      iter = result.getNodes();
      ForumSearchResult forumSearch;
      while (iter.hasNext()) {
        forumSearch = new ForumSearchResult();
        Node node = iter.nextNode();
        forumSearch.setId(node.getName());
        forumSearch.setPath(node.getPath());
        forumSearch.setType(Utils.TOPIC);
        forumSearch.setName(node.getProperty(EXO_NAME).getString());
        forumSearch.setContent(node.getProperty(EXO_DESCRIPTION).getString());
        forumSearch.setCreatedDate(node.getProperty(EXO_CREATED_DATE).getDate().getTime());
        list.add(forumSearch);
      }
      
      stringBuilder = new StringBuilder(JCR_ROOT).append(string).append("//element(*,").append(EXO_POST).append(")")
                    .append("[(@").append(EXO_IS_APPROVED).append("='false' or @").append(EXO_IS_HIDDEN)
                    .append("='true' or @").append(EXO_IS_WAITING).append("='true')").append(builder).append("] order by @exo:modifiedDate descending");
      
      query = qm.createQuery(stringBuilder.toString(), Query.XPATH);
      result = query.execute();
      iter = result.getNodes();
      while (iter.hasNext()) {
        forumSearch = new ForumSearchResult();
        Node node = iter.nextNode();
        forumSearch.setId(node.getName());
        forumSearch.setPath(node.getPath());
        forumSearch.setType(Utils.POST);
        forumSearch.setName(node.getProperty(EXO_NAME).getString());
        forumSearch.setContent(node.getProperty(EXO_MESSAGE).getString());
        forumSearch.setCreatedDate(node.getProperty(EXO_CREATED_DATE).getDate().getTime());
        list.add(forumSearch);
      }
    } catch (Exception e) {
      LOG.error("Failed to get waiting jobs for moderator", e);
    }
    return list;
  }

  public int getJobWattingForModeratorByUser(String userId) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    int job = 0;
    Node newProfileNode = getUserProfileHome(sProvider).getNode(userId);
    long t;// = 3
    if (isAdminRole(userId)) {
      t = 0;
    } else {
      t = newProfileNode.getProperty(EXO_USER_ROLE).getLong();
    }
    if (t < 2) {
      try {
        job = (int) newProfileNode.getProperty(EXO_JOB_WATTING_FOR_MODERATOR).getLong();
      } catch (Exception e) {
        job = 0;
      }
    }
    return job;
  }

  private int getTotalJobWaitingForModerator(Session session, String userId) throws Exception {
    int totalJob = 0;
    try {
      Node newProfileNode = session.getRootNode().getNode(dataLocator.getUserProfilesLocation()).getNode(userId);
      long t;// = 3;
      if (isAdminRole(userId)) {
        t = 0;
      } else {
        t = newProfileNode.getProperty(EXO_USER_ROLE).getLong();
      }
      if (t < 2) {
        Node categoryHome = session.getRootNode().getNode(dataLocator.getForumCategoriesLocation());
        String string = categoryHome.getPath();
        QueryManager qm = categoryHome.getSession().getWorkspace().getQueryManager();
        StringBuffer buffer = new StringBuffer();
        if (t > 0) {
          String[] paths = Utils.valuesToArray(newProfileNode.getProperty(EXO_MODERATE_FORUMS).getValues());
          int l = paths.length;
          if (l > 0) {
            buffer.append(" and (");
            for (int i = 0; i < l; i++) {
              if (i > 0)
                buffer.append(" or ");
              String str = getPath(("/" + Utils.FORUM), paths[i]);
              buffer.append("@exo:path='").append(str).append("'");
            }
            buffer.append(")");
          }
        }
        StringBuffer stringBuffer = new StringBuffer(JCR_ROOT).append(string).append("//element(*,").append(EXO_TOPIC).append(")")
                                        .append("[(@").append(EXO_IS_APPROVED).append("='false' or @")
                                        .append(EXO_IS_WAITING).append("='true')").append(buffer).append("]");
        
        Query query = qm.createQuery(stringBuffer.toString(), Query.XPATH);
        QueryResult result = query.execute();
        NodeIterator iter = result.getNodes();
        totalJob = (int) iter.getSize();

        stringBuffer = new StringBuffer(JCR_ROOT).append(string).append("//element(*,").append(EXO_POST).append(")")
                        .append("[(@").append(EXO_IS_APPROVED).append("='false' or @").append(EXO_IS_HIDDEN)
                        .append("='true' or @").append(EXO_IS_WAITING).append("='true')").append(buffer).append("]");
        
        query = qm.createQuery(stringBuffer.toString(), Query.XPATH);
        result = query.execute();
        iter = result.getNodes();
        totalJob = totalJob + (int) iter.getSize();
        newProfileNode.setProperty(EXO_JOB_WATTING_FOR_MODERATOR, totalJob);
        newProfileNode.save();
      }
    } catch (Exception e) {
      LOG.error("Failed to get total job watting for moderator", e);
    }
    return totalJob;
  }

  private void getTotalJobWatting(SessionProvider sProvider, Set<String> userIds) {
    try {
      ContinuationService continuation = CommonUtils.getComponent(ContinuationService.class);
      if (continuation != null) {
        Set<String> set = new HashSet<String>(ForumServiceUtils.getUserPermission(userIds.toArray(new String[userIds.size()])));
        set.addAll(getAllAdministrator(sProvider));
        JsonGeneratorImpl generatorImpl = new JsonGeneratorImpl();
        Category cat = new Category();
        for (String userId : set) {
          if (Utils.isEmpty(userId) || userId.indexOf(CommonUtils.SLASH) > 0 || userId.indexOf(CommonUtils.COLON) > 0) continue;
          int job = getTotalJobWaitingForModerator(getForumHomeNode(sProvider).getSession(), userId);
          if (job >= 0) {
            cat.setCategoryName(String.valueOf(job));
            JsonValue json = generatorImpl.createJsonObject(cat);
            continuation.sendMessage(userId, "/eXo/Application/Forum/messages", json, cat.toString());
          }
        }
      }
    } catch (Exception e) {
      LOG.error("Failed to get total job waiting for moderator", e);
    }
  }

  public void sendNotificationMessage(ForumPrivateMessage message) {
    try {
      if (message != null) {
        ContinuationService continuation = CommonUtils.getComponent(ContinuationService.class);
        String[] sendTo = message.getSendTo().replaceAll(";", ",").split(",");
        String from = message.getFrom();
        message.setFrom(getScreenName(from));
        JsonGeneratorImpl generatorImpl = new JsonGeneratorImpl();
        JsonValue json = generatorImpl.createJsonObject(message);
        for (int i = 0; i < sendTo.length; i++) {
          String to = sendTo[i].trim(); 
          if (to.equals(from)) {
            continue;
          }
          continuation.sendMessage(to, "/eXo/Application/Forum/NotificationMessage", json, message.toString());
        }
      }
    } catch (Exception e) {
      LOG.error("Failed to send notification message:" +  e.getMessage());
    }
  }

  public NodeIterator search(String queryString) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      QueryManager qm = getForumHomeNode(sProvider).getSession().getWorkspace().getQueryManager();
      Query query = qm.createQuery(queryString, Query.XPATH);
      QueryResult result = query.execute();
      return result.getNodes();
    } catch (Exception e) {
      LOG.error("Failed to search", e);
    }
    return null;
  }

  public void evaluateActiveUsers(String strQuery){
    SessionProvider sProvider = SessionProvider.createSystemProvider();
    try {
      String path = getUserProfileHome(sProvider).getPath();
      StringBuilder stringBuilder = new StringBuilder();
      if (strQuery == null || strQuery.length() == 0) {
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTimeInMillis(calendar.getTimeInMillis() - 864000000);
        stringBuilder.append(JCR_ROOT).append(path).append("//element(*,").append(EXO_FORUM_USER_PROFILE).append(")[").append("@exo:lastPostDate >= xs:dateTime('").append(ISO8601.format(calendar)).append("')]");
      } else {
        stringBuilder.append(JCR_ROOT).append(path).append(strQuery);
      }
      QueryManager qm = getForumHomeNode(sProvider).getSession().getWorkspace().getQueryManager();
      Query query = qm.createQuery(stringBuilder.toString(), Query.XPATH);
      QueryResult result = query.execute();
      NodeIterator iter = result.getNodes();

      Node statisticHome = getStatisticHome(sProvider);
      if (statisticHome.hasNode(Locations.FORUM_STATISTIC)) {
        statisticHome.getNode(Locations.FORUM_STATISTIC).setProperty(EXO_ACTIVE_USERS, iter.getSize());
        statisticHome.save();
      } else {
        ForumStatistic forumStatistic = new ForumStatistic();
        forumStatistic.setActiveUsers(iter.getSize());
        saveForumStatistic(forumStatistic);
      }
    } catch (Exception e) {
      LOG.error("Failed to evaluate active users", e);
    } finally {
      sProvider.close();
    }
  }

  protected List<File> createCategoryFiles(List<String> objectIds, SessionProvider sessionProvider) throws Exception {
    List<File> listFiles = new ArrayList<File>();
    ByteArrayOutputStream outputStream = null;
    Node categoryHome = getCategoryHome(sessionProvider);
    Node cateNode = null;
    for (String categoryId : objectIds) {
        try {
          cateNode = categoryHome.getNode(categoryId);
          outputStream = new ByteArrayOutputStream();
          Calendar date = new GregorianCalendar();
          categoryHome.getSession().exportSystemView(cateNode.getPath(), outputStream, false, false);
          listFiles.add(CommonUtils.getXMLFile(outputStream, "eXo Knowledge Suite - Forum", "Category", date.getTime(), cateNode.getName()));
        } finally {
          outputStream.close();
        }
    }
    return listFiles;
  }

  protected List<File> createForumFiles(String categoryId, List<String> objectIds, SessionProvider sessionProvider) throws Exception {
    List<File> listFiles = new ArrayList<File>();
    List<Forum> forums = getCachedDataStorage().getForums(new ForumFilter(categoryId, true));
    for (Forum forum : forums) {
      if (objectIds.size() > 0 && !objectIds.contains(forum.getId()))
        continue;
      ByteArrayOutputStream outputStream = null;
      try {
        outputStream = new ByteArrayOutputStream();
        Calendar calendar = GregorianCalendar.getInstance();
        getCategoryHome(sessionProvider).getSession().exportSystemView(forum.getPath(), outputStream, false, false);

        listFiles.add(CommonUtils.getXMLFile(outputStream, "eXo Knowledge Suite - Forum", "Forum", calendar.getTime(), forum.getId()));
      } finally {
        outputStream.close();
      }
    }
    return listFiles;
  }

  protected List<File> createFilesFromNode(Node node, String type) throws Exception {
    List<File> listFiles = new ArrayList<File>();
    if (node != null) {
      ByteArrayOutputStream outputStream = null;
      try {
        outputStream = new ByteArrayOutputStream();
        Calendar calendar = GregorianCalendar.getInstance();
        node.getSession().exportSystemView(node.getPath(), outputStream, false, false);
        listFiles.add(CommonUtils.getXMLFile(outputStream, "eXo Knowledge Suite - Forum", type, calendar.getTime(), node.getName()));

      } finally {
        outputStream.close();
      }
    }
    return listFiles;
  }

  protected List<File> createAllForumFiles(SessionProvider sessionProvider) throws Exception {
    List<File> listFiles = new ArrayList<File>();

    /*
     * // Create Statistic file listFiles.addAll(createFilesFromNodeIter(categoryHome, null, getStatisticHome(sessionProvider), ""));
     */

    // Create Administration file
    listFiles.addAll(createFilesFromNode(getAdminHome(sessionProvider), Locations.ADMINISTRATION_HOME));

    // Create UserProfile files
    listFiles.addAll(createFilesFromNode(getUserProfileHome(sessionProvider), Locations.USER_PROFILE_HOME));

    // create tag files
    listFiles.addAll(createFilesFromNode(getTagHome(sessionProvider), Locations.TAG_HOME));

    // Create BBCode file
    listFiles.addAll(createFilesFromNode(getBBCodesHome(sessionProvider), Locations.BBCODE_HOME));

    // Create BanIP file
    listFiles.addAll(createFilesFromNode(getBanIPHome(sessionProvider), Locations.BANIP_HOME));

    // Create category home file
    listFiles.addAll(createFilesFromNode(getCategoryHome(sessionProvider), Locations.FORUM_CATEGORIES_HOME));

    return listFiles;
  }

  public Object exportXML(String categoryId, String forumId, List<String> objectIds, String nodePath, ByteArrayOutputStream bos, boolean isExportAll) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      List<File> listFiles = new ArrayList<File>();

      if (!isExportAll) {
        if (categoryId != null) {
          if (Utils.isEmpty(forumId)) {
            listFiles.addAll(createForumFiles(categoryId, objectIds, sProvider));
          } else {
            Node categoryHome = getCategoryHome(sProvider);
            categoryHome.getSession().exportSystemView(nodePath, bos, false, false);
            return null;
          }
        } else {
          listFiles.addAll(createCategoryFiles(objectIds, sProvider));
        }
      } else {
        listFiles.addAll(createAllForumFiles(sProvider));
      }

      ZipOutputStream zipOutputStream = null;
      try {
        zipOutputStream = new ZipOutputStream(new FileOutputStream("exportCategory.zip"));
        int byteReads;
        byte[] buffer = new byte[4096]; // Create a buffer for copying
        FileInputStream inputStream = null;
        ZipEntry zipEntry = null;
        for (File f : listFiles) {
          inputStream = new FileInputStream(f);
          try {
            zipEntry = new ZipEntry(f.getPath());
            zipOutputStream.putNextEntry(zipEntry);
            while ((byteReads = inputStream.read(buffer)) != -1)
              zipOutputStream.write(buffer, 0, byteReads);
          } finally {
            inputStream.close();
          }
        }

      } finally {
        zipOutputStream.close();
      }
      File file = new File("exportCategory.zip");
      for (File f : listFiles)
        f.deleteOnExit();
      return file;
    } catch (Exception e) {
      return null;
    }
  }

  public void importXML(String nodePath, ByteArrayInputStream bis, int typeImport) throws Exception {
    String nodeName = "";
    byte[] bdata = new byte[bis.available()];
    bis.read(bdata);
    DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
    //disallow doctype declaration in order to prevent xxe attack
    docBuilderFactory.setFeature(DOCTYPE_DECLARATION_DISALLOW,true);
    DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
    ByteArrayInputStream is = new ByteArrayInputStream(bdata);
    try {
      Document doc = docBuilder.parse(is);
      doc.getDocumentElement().normalize();
      String typeNodeExport = ((org.w3c.dom.Node) doc.getFirstChild().getChildNodes().item(0).getChildNodes().item(0)).getTextContent();
      SessionProvider sProvider = CommonUtils.createSystemProvider();
      List<String> patchNodeImport = new ArrayList<String>();
      Node forumHome = getForumHomeNode(sProvider);
      is = new ByteArrayInputStream(bdata);
      if (!typeNodeExport.equals(EXO_FORUM_CATEGORY) && !typeNodeExport.equals(EXO_FORUM)) {
        // All nodes when import need reset childnode
        if (typeNodeExport.equals(EXO_CATEGORY_HOME)) {
          nodePath = getCategoryHome(sProvider).getPath();
          Node categoryHome = getCategoryHome(sProvider);
          nodeName = "CategoryHome";
          addDataFromXML(categoryHome, nodePath, sProvider, is, nodeName);
        } else if (typeNodeExport.equals(EXO_USER_PROFILE_HOME)) {
          Node userProfile = getUserProfileHome(sProvider);
          nodeName = "UserProfileHome";
          nodePath = getUserProfileHome(sProvider).getPath();
          addDataFromXML(userProfile, nodePath, sProvider, is, nodeName);
        } else if (typeNodeExport.equals(EXO_TAG_HOME)) {
          Node tagHome = getTagHome(sProvider);
          nodePath = getTagHome(sProvider).getPath();
          nodeName = "TagHome";
          addDataFromXML(tagHome, nodePath, sProvider, is, nodeName);
        } else if (typeNodeExport.equals(EXO_FORUM_BB_CODE_HOME)) {
          nodePath = dataLocator.getBBCodesLocation();
          Node bbcodeNode = getBBCodesHome(sProvider);
          nodeName = "forumBBCode";
          addDataFromXML(bbcodeNode, nodePath, sProvider, is, nodeName);
        }
        // Node import but don't need reset childnodes
        else if (typeNodeExport.equals(EXO_ADMINISTRATION_HOME)) {
          nodePath = getForumSystemHome(sProvider).getPath();
          Node node = getAdminHome(sProvider);
          node.remove();
          getForumSystemHome(sProvider).save();
          typeImport = ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING;
          Session session = forumHome.getSession();
          session.importXML(nodePath, is, typeImport);
          session.save();
        } else if (typeNodeExport.equals(EXO_BAN_IP_HOME)) {
          nodePath = getForumSystemHome(sProvider).getPath();
          Node node = getBanIPHome(sProvider);
          node.remove();
          getForumSystemHome(sProvider).save();
          typeImport = ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING;
          Session session = forumHome.getSession();
          session.importXML(nodePath, is, typeImport);
          session.save();
        } else {
          throw new RuntimeException("unknown type of node to export :" + typeNodeExport);
        }
      } else {
        if (typeNodeExport.equals(EXO_FORUM_CATEGORY)) {
          // Check if import forum but the data import have structure of a category --> Error
          if (nodePath.split("/").length == 6) {
            throw new ConstraintViolationException();
          }

          nodePath = getCategoryHome(sProvider).getPath();
        }

        Session session = forumHome.getSession();
        NodeIterator iter = ((Node) session.getItem(nodePath)).getNodes();
        while (iter.hasNext()) {
          patchNodeImport.add(iter.nextNode().getName());
        }
        session.importXML(nodePath, is, typeImport);
        session.save();
        NodeIterator newIter = ((Node) session.getItem(nodePath)).getNodes();
        while (newIter.hasNext()) {
          Node node = newIter.nextNode();
          if (patchNodeImport.contains(node.getName()))
            patchNodeImport.remove(node.getName());
          else
            patchNodeImport.add(node.getName());
        }
      }
      // update forum statistic and profile of owner post.
      if (typeNodeExport.equals(EXO_FORUM_CATEGORY) || typeNodeExport.equals(EXO_FORUM)) {
        for (String string : patchNodeImport) {
          updateForum(nodePath + "/" + string, false);
        }
      } else if (typeNodeExport.equals(EXO_CATEGORY_HOME)) {
        updateForum(null);
      }
    } finally {
      is.close();
    }
  }

  private void addDataFromXML(Node sourceNode, String nodePath, SessionProvider sessionProvider, InputStream is, String nodeName) throws Exception {
    Node forumHomeNode = getForumHomeNode(sessionProvider);
    Session session = forumHomeNode.getSession();
    Node tempNode = forumHomeNode.getParent().addNode("DataTemp");

    // Add child node of DataTemp
    session.importXML(tempNode.getPath(), is, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
    session.save();

    // Node store data from XML file.
    Node importNode = tempNode.getNode(nodeName);

    try {
      copyFullNodes(sourceNode, importNode, session);
    } finally {
      tempNode.remove();
      forumHomeNode.getParent().save();
    }
  }

  private void copyFullNodes(Node sourceNode, Node importNode, Session session) throws Exception {
    // Check if importNode have different child than sourceNode then add it.
    NodeIterator sourceIter = sourceNode.getNodes();
    NodeIterator importIter = importNode.getNodes();
    Node srcTemp = null;
    Node importTemp = null;
    boolean flag = false;

    while (importIter.hasNext()) {
      flag = true;
      importTemp = importIter.nextNode();
      while (sourceIter.hasNext()) {
        srcTemp = sourceIter.nextNode();
        if (importTemp.getName().equals(srcTemp.getName())) {
          copyFullNodes(srcTemp, importTemp, session);
          flag = false;
          break;
        }
      }

      if (flag) {
        String path = sourceNode.getPath() + "/" + importTemp.getName();
        try {
          session.getWorkspace().copy(importTemp.getPath(), path);
        } catch (Exception e) {
          if (LOG.isDebugEnabled()) {
            LOG.debug(path + " or " + importTemp.getPath() + " does not exist: " + e.getMessage() + "\n" + e.getCause());
          }
        }
      }
    }
  }

  public void updateTopicAccess(String userId, String topicId) {
    if (updatingRead.containsKey(userId)) {
      updatingRead.get(userId).add(topicId);
    } else {
      List<String> value = new ArrayList<String>();
      value.add(topicId);
      updatingRead.put(userId, value);
    }
  }

  public void writeReads() {
    //
    Map<String, List<String>> map = updatingRead;
    updatingRead = new ConcurrentHashMap<String, List<String>>();
    //
    SessionProvider sProvider = SessionProvider.createSystemProvider();
    try {
      Node userHome = getUserProfileHome(sProvider);
      long currentTime = getGreenwichMeanTime().getTimeInMillis();
      for (Map.Entry<String, List<String>> entry : map.entrySet()) {
        try {
          if (!userHome.hasNode(entry.getKey())) {
            return;
          }
          //
          Node profile = userHome.getNode(entry.getKey());
          List<String> values = new PropertyReader(profile).list(EXO_READ_TOPIC, new ArrayList<String>());
          //
          for (String topicId : entry.getValue()) {
            //
            int i = 0;
            boolean isUpdated = false;
            for (String vl : values) {
              if (vl.indexOf(topicId) == 0) {
                values.set(i, topicId + ":" + currentTime);
                isUpdated = true;
                break;
              }
              i++;
            }
            //
            if (!isUpdated) {
              values.add(topicId + ":" + currentTime);
            }
            //
            profile.setProperty(EXO_READ_TOPIC, values.toArray(new String[values.size()]));
          }
        } catch (Exception e) {
          logDebug(String.format("Failed to update user %s acess for topics %s", entry.getKey(), entry.getValue().toString()), e);
        }
      }
      userHome.getSession().save();
    } catch (Exception e) {
      logDebug("Writing reads topic is unsuccessfully.", e);
    } finally {
      sProvider.close();
    }
  }

  public void updateForumAccess(String userId, String forumId){
    SessionProvider sysSession = CommonUtils.createSystemProvider();
    try {
      Node userProfileHome = getUserProfileHome(sysSession);
      Node profile = getUserProfileNode(userProfileHome, userId);
      List<String> values = new ArrayList<String>();
      if (profile.hasProperty(EXO_READ_FORUM)) {
        values = Utils.valuesToList(profile.getProperty(EXO_READ_FORUM).getValues());
      }
      int i = 0;
      boolean isUpdated = false;
      for (String vl : values) {
        if (vl.indexOf(forumId) == 0) {
          values.set(i, forumId + ":" + getGreenwichMeanTime().getTimeInMillis());
          isUpdated = true;
          break;
        }
        i++;
      }
      if (!isUpdated) {
        values.add(forumId + ":" + getGreenwichMeanTime().getTimeInMillis());
      }
      if (values.size() == 2 && Utils.isEmpty(values.get(0)))
        values.remove(0);
      profile.setProperty(EXO_READ_FORUM, values.toArray(new String[values.size()]));
      profile.save();

    } catch (Exception e) {
      LOG.warn(String.format("Failed to update user %s acess for forum %s", userId, forumId));
    }
  }

  public List<String> getBookmarks(String userName) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    Node profile = getUserProfileHome(sProvider).getNode(userName);
    if (profile.hasProperty(EXO_BOOKMARK)) {
      return Utils.valuesToList(profile.getProperty(EXO_BOOKMARK).getValues());
    }
    return new ArrayList<String>();
  }

  public List<String> getBanList() throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      Node banNode = getForumBanNode(sProvider);
      if (banNode.hasProperty(EXO_IPS))
        return Utils.valuesToList(banNode.getProperty(EXO_IPS).getValues());
    } catch (Exception e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Failed to get ban list", e);
      }
    }
    return new ArrayList<String>();
  }

  public boolean addBanIP(String ip) throws Exception {
    List<String> ips = getBanList();
    if (ips.contains(ip))
      return false;
    ips.add(ip);
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      Node banNode = getForumBanNode(sProvider);
      banNode.setProperty(EXO_IPS, ips.toArray(new String[ips.size()]));
      if (banNode.isNew()) {
        banNode.getSession().save();
      } else {
        banNode.save();
      }
      return true;
    } catch (Exception e) {
      LOG.error("Failed to add ban ip: " + ip, e);
    }
    return false;
  }

  public void removeBan(String ip) throws Exception {
    List<String> ips = getBanList();
    if (ips.contains(ip)) {
      ips.remove(ip);
      SessionProvider sProvider = CommonUtils.createSystemProvider();
      try {
        Node banNode = getForumBanNode(sProvider);
        banNode.setProperty(EXO_IPS, Utils.getStringsInList(ips));
        banNode.save();
      } catch (Exception e) {
        LOG.error("Failed to remove ban, ip: " + ip, e);
      }
    }
  }

  public List<String> getForumBanList(String forumId) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    List<String> list = new ArrayList<String>();
    try {
      if (forumId.indexOf(".") > 0)
        forumId = StringUtils.replace(forumId, ".", "/");
      Node forumNode = getCategoryHome(sProvider).getNode(forumId);
      if (forumNode.hasProperty(EXO_BAN_I_PS))
        list.addAll(Utils.valuesToList(forumNode.getProperty(EXO_BAN_I_PS).getValues()));
    } catch (Exception e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Failed to get forum ban list.", e);
      }
    }
    return list;
  }

  public boolean addBanIPForum(String ip, String forumId) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    List<String> ips = new ArrayList<String>();
    try {
      Node forumNode = getCategoryHome(sProvider).getNode(forumId);
      if (forumNode.hasProperty(EXO_BAN_I_PS))
        ips.addAll(Utils.valuesToList(forumNode.getProperty(EXO_BAN_I_PS).getValues()));
      if (ips.contains(ip))
        return false;
      ips.add(ip);
      forumNode.setProperty(EXO_BAN_I_PS, Utils.getStringsInList(ips));
      if (forumNode.isNew()) {
        forumNode.getSession().save();
      } else {
        forumNode.save();
      }
      return true;
    } catch (Exception e) {
      LOG.error("Failed to add ban ip forum.", e);
    }
    return false;
  }

  public void removeBanIPForum(String ip, String forumId) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    List<String> ips = new ArrayList<String>();
    try {
      Node forumNode = getCategoryHome(sProvider).getNode(forumId);
      if (forumNode.hasProperty(EXO_BAN_I_PS))
        ips.addAll(Utils.valuesToList(forumNode.getProperty(EXO_BAN_I_PS).getValues()));
      if (ips.contains(ip)) {
        ips.remove(ip);
        forumNode.setProperty(EXO_BAN_I_PS, Utils.getStringsInList(ips));
        if (forumNode.isNew()) {
          forumNode.getSession().save();
        } else {
          forumNode.save();
        }
      }

    } catch (Exception e) {
      LOG.error("Failed to remove ban IP from forum", e);
    }
  }

  private List<String> getAllAdministrator(SessionProvider sProvider) throws Exception {
    QueryManager qm = getForumHomeNode(sProvider).getSession().getWorkspace().getQueryManager();
    StringBuilder pathQuery = new StringBuilder();
    pathQuery.append(JCR_ROOT).append(getUserProfileHome(sProvider).getPath()).append("//element(*,").append(EXO_FORUM_USER_PROFILE).append(")[@exo:userRole=0]");
    Query query = qm.createQuery(pathQuery.toString(), Query.XPATH);
    QueryResult result = query.execute();
    NodeIterator iter = result.getNodes();
    List<String> list = new ArrayList<String>();
    while (iter.hasNext()) {
      Node userNode = iter.nextNode();
      list.add(userNode.getName());
    }
    return list;
  }

  public void updateStatisticCounts(long topicCount, long postCount) throws Exception {
    SessionProvider sProvider = SessionProvider.createSystemProvider();
    ReentrantLock lock = this.lock;
    try {
      lock.lock();
      Node forumStatisticNode = getForumStatisticsNode(sProvider);
      PropertyReader reader = new PropertyReader(forumStatisticNode);
      if (topicCount != 0) {
        long count = reader.l(EXO_TOPIC_COUNT);
        if (count < 0)
          count = 0;
        forumStatisticNode.setProperty(EXO_TOPIC_COUNT, count + topicCount);
      }
      if (postCount != 0) {
        long count = reader.l(EXO_POST_COUNT);
        if (count < 0)
          count = 0;
        forumStatisticNode.setProperty(EXO_POST_COUNT, count + postCount);
      }
      forumStatisticNode.save();
    } catch (Exception e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Failed to update statistic counts", e);
      }
    } finally {
      lock.unlock();
      sProvider.close();
    }
  }

  private PruneSetting getPruneSetting(Node prunNode) throws Exception {
    PruneSetting pruneSetting = new PruneSetting();
    pruneSetting.setId(prunNode.getName());
    pruneSetting.setForumPath(prunNode.getParent().getPath());
    pruneSetting.setActive(prunNode.getProperty(EXO_IS_ACTIVE).getBoolean());
    pruneSetting.setCategoryName(prunNode.getParent().getParent().getProperty(EXO_NAME).getString());
    pruneSetting.setForumName(prunNode.getParent().getProperty(EXO_NAME).getString());
    pruneSetting.setInActiveDay(prunNode.getProperty(EXO_IN_ACTIVE_DAY).getLong());
    pruneSetting.setPeriodTime(prunNode.getProperty(EXO_PERIOD_TIME).getLong());
    if (prunNode.hasProperty(EXO_LAST_RUN_DATE))
      pruneSetting.setLastRunDate(prunNode.getProperty(EXO_LAST_RUN_DATE).getDate().getTime());
    return pruneSetting;
  }

  public PruneSetting getPruneSetting(String forumPath) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    PruneSetting pruneSetting = new PruneSetting();
    try {
      Node forumNode = (Node) getCategoryHome(sProvider).getSession().getItem(forumPath);
      pruneSetting = getPruneSetting(forumNode.getNode(Utils.PRUNESETTING));
    } catch (Exception e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Failed to get Prune Settings: " + e.getMessage() + "\n" + e.getCause());
      }
    }
    return pruneSetting;
  }

  public List<PruneSetting> getAllPruneSetting() throws Exception {
    List<PruneSetting> prunList = new ArrayList<PruneSetting>();
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    NodeIterator iter = getNodeIteratorAutoPruneSetting(sProvider, false);
    while (iter.hasNext()) {
      Node prunNode = iter.nextNode();
      prunList.add(getPruneSetting(prunNode));
    }
    return prunList;
  }

  public void savePruneSetting(PruneSetting pruneSetting) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      String path = pruneSetting.getForumPath();
      Node forumNode = (Node) getForumHomeNode(sProvider).getSession().getItem(path);
      Node pruneNode;
      try {
        pruneNode = forumNode.getNode(Utils.PRUNESETTING);
      } catch (Exception e) {
        pruneNode = forumNode.addNode(Utils.PRUNESETTING, EXO_PRUNE_SETTING);
        pruneNode.setProperty(EXO_ID, pruneSetting.getId());
      }
      pruneNode.setProperty(EXO_IN_ACTIVE_DAY, pruneSetting.getInActiveDay());
      pruneNode.setProperty(EXO_PERIOD_TIME, pruneSetting.getPeriodTime());
      pruneNode.setProperty(EXO_IS_ACTIVE, pruneSetting.isActive());
      if (pruneSetting.getLastRunDate() != null) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(pruneSetting.getLastRunDate());
        pruneNode.setProperty(EXO_LAST_RUN_DATE, calendar);
      }
      if (pruneNode.isNew())
        forumNode.getSession().save();
      else
        forumNode.save();
      try {
        addOrRemoveSchedule(pruneSetting);
      } catch (Exception e) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Failed to add or remove prune jobs", e);
        }
      }
    } catch (Exception e) {
      LOG.error("Failed to save prune setting.", e);
    }
  }

  private void addOrRemoveSchedule(PruneSetting pSetting) throws Exception{
    Calendar cal = new GregorianCalendar();
    PeriodInfo periodInfo = new PeriodInfo(cal.getTime(), null, -1, (pSetting.getPeriodTime() * 86400000)); // pSetting.getPeriodTime() return value
    JobInfo info = new JobInfo(pSetting.getId(), KNOWLEDGE_SUITE_FORUM_JOBS, AutoPruneJob.class);
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    JobSchedulerService schedulerService = (JobSchedulerService) container.getComponentInstanceOfType(JobSchedulerService.class);
    schedulerService.removeJob(info);
    if (pSetting.isActive()) {
      info = new JobInfo(pSetting.getId(), KNOWLEDGE_SUITE_FORUM_JOBS, AutoPruneJob.class);
      info.setDescription(pSetting.getForumPath());
      RepositoryService repositoryService = (RepositoryService) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(RepositoryService.class);
      String repoName = repositoryService.getCurrentRepository().getConfiguration().getName();
      JobDataMap jdatamap = new JobDataMap();
      jdatamap.put(Utils.CACHE_REPO_NAME, repoName);
      schedulerService.addPeriodJob(info, periodInfo, jdatamap);
      if (LOG.isInfoEnabled()) {
        LOG.info("\n\nActivated " + info.getJobName());
      }
    }
  }

  public void runPrune(String forumPath) throws Exception {
    runPrune(getPruneSetting(forumPath));
  }

  public void runPrune(PruneSetting pSetting) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      Node forumNode = (Node) getForumHomeNode(sProvider).getSession().getItem(pSetting.getForumPath());
      NodeIterator iter = getIteratorPrune(sProvider, pSetting);
      List<String> topicPruned = new ArrayList<String>();
      while (iter.hasNext()) {
        Node topic = iter.nextNode();
        topic.setProperty(EXO_IS_ACTIVE, false);
        topic.save();
        topicPruned.add(topic.getPath());
        try {
          Node forumN = topic.getParent();
          if (new PropertyReader(forumN).string(EXO_LAST_TOPIC_PATH, "").indexOf(topic.getName()) >= 0) {
            queryLastTopic(sProvider, forumN.getPath());
          }
        } catch (Exception e) {
          LOG.warn("Failed to save new value last post date in forum", e); 
        }
      }
      // update last run for prune setting
      Node setting = forumNode.getNode(pSetting.getId());
      setting.setProperty(EXO_LAST_RUN_DATE, getGreenwichMeanTime());
      forumNode.save();
      //
      DataStorage storage = getCachedDataStorage();
      if (storage instanceof CachedDataStorage) {
        for (String topicPath : topicPruned) {
          ((CachedDataStorage) storage).clearTopicCache(storage.getTopicByPath(topicPath, false));
        }
      }
    } catch (Exception e) {
      LOG.error("Failed to run prune", e);
    }
  }

  private NodeIterator getIteratorPrune(SessionProvider sProvider, PruneSetting pSetting) throws Exception {
    Node forumHome = getForumHomeNode(sProvider);
    Node forumNode = (Node) forumHome.getSession().getItem(pSetting.getForumPath());
    Calendar newDate = getGreenwichMeanTime();
    newDate.setTimeInMillis(newDate.getTimeInMillis() - pSetting.getInActiveDay() * 86400000);
    QueryManager qm = forumHome.getSession().getWorkspace().getQueryManager();
    StringBuffer stringBuffer = new StringBuffer();
    stringBuffer.append(JCR_ROOT).append(forumNode.getPath()).append("/element(*,").append(EXO_TOPIC).append(")[ @").append(EXO_IS_ACTIVE).append("='true' and @").append(EXO_LAST_POST_DATE).append(" <= xs:dateTime('").append(ISO8601.format(newDate)).append("')]");
    Query query = qm.createQuery(stringBuffer.toString(), Query.XPATH);
    QueryResult result = query.execute();
    return result.getNodes();
  }

  public long checkPrune(PruneSetting pSetting) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      return getIteratorPrune(sProvider, pSetting).getSize();
    } catch (Exception e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Failed to check prune", e);
      }
    }
    return 0;
  }

  public InputStream createForumRss(String objectId, String link) throws Exception {
    List<SyndEntry> entries = new ArrayList<SyndEntry>();
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      Node node_ = null;
      if (objectId.indexOf(Utils.CATEGORY) == 0) {
        node_ = getNodeById(sProvider, objectId, Utils.CATEGORY);
        entries.addAll(categoryUpdated(node_));
      } else if (objectId.indexOf(Utils.FORUM) == 0) {
        node_ = getNodeById(sProvider, objectId, Utils.FORUM);
        entries.addAll(forumUpdated(node_));
      } else {
        node_ = getNodeById(sProvider, objectId, Utils.TOPIC);
        String link_ = node_.getProperty(EXO_LINK).getString();
        link = Utils.isEmpty(link_) ? link : link_;
        entries.addAll(topicUpdated(node_));
      }
      SyndFeed feed = createNewFeed(node_, link);
      feed.setEntries(entries);
      SyndFeedOutput output = new SyndFeedOutput();
      String s = output.outputString(feed);
      s = StringUtils.replace(s, "&amp;", "&");
      s = s.replaceAll("&lt;", "<").replaceAll("&gt;", ">");
      s = StringUtils.replace(s, "ST[CDATA[", "<![CDATA[");
      s = StringUtils.replace(s, "END]]", "]]>");
      return new ByteArrayInputStream(s.getBytes());
    } catch (Exception e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Failed to create forum rss", e);
      }
    }
    return null;
  }

  private List<SyndEntry> categoryUpdated(Node cateNode) throws Exception {
    List<SyndEntry> entries = new ArrayList<SyndEntry>();
    NodeIterator iterator = cateNode.getNodes();
    while (iterator.hasNext()) {
      Node node = iterator.nextNode();
      if (node.isNodeType(EXO_FORUM) && !node.getProperty(EXO_IS_CLOSED).getBoolean()) {
        entries.addAll(forumUpdated(node));
      }
    }
    return entries;
  }

  private List<SyndEntry> forumUpdated(Node forumNode) throws Exception {
    List<SyndEntry> entries = new ArrayList<SyndEntry>();
    try {
      QueryManager qm = forumNode.getSession().getWorkspace().getQueryManager();
      StringBuilder queryString = new StringBuilder(JCR_ROOT).append(forumNode.getPath()).append("//element(*,exo:topic)[@exo:isWaiting='false' and @exo:isActive='true' and @exo:isClosed='false' and (not(@exo:canView) or @exo:canView='' or @exo:canView=' ')]").append(" order by @exo:isSticky descending, @exo:lastPostDate descending");
      Query query = qm.createQuery(queryString.toString(), Query.XPATH);
      QueryResult result = query.execute();
      NodeIterator iter = result.getNodes();
      while (iter.hasNext()) {
        entries.addAll(topicUpdated(iter.nextNode()));
      }
    } catch (Exception e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Failed to update syndEntry by forum.", e);
      }
    }
    return entries;
  }

  private List<SyndEntry> topicUpdated(Node topicNode) {
    List<SyndEntry> entries = new ArrayList<SyndEntry>();
    try {
      Node forumNode = topicNode.getParent();
      Node categoryNode = forumNode.getParent();
      boolean categoryHasRestrictedAudience = (hasProperty(categoryNode, EXO_VIEWER));
      boolean forumHasRestrictedAudience = (hasProperty(forumNode, EXO_VIEWER));
      String topicName = topicNode.getName();
      if (categoryHasRestrictedAudience || forumHasRestrictedAudience) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Post" + topicName + " was not added to feed because category or forum has restricted audience");
        }
        return null;
      }
      // update posts in topic
      QueryManager qm = topicNode.getSession().getWorkspace().getQueryManager();
      StringBuffer stringBuffer = new StringBuffer(JCR_ROOT)
         .append(topicNode.getPath()).append("//element(*,").append(EXO_POST).append(")")
         .append(Utils.getPathQuery("true", "false", "false", EXO_USER_PRI))
         .append(" order by @exo:createdDate ascending");
      Query query = qm.createQuery(stringBuffer.toString(), Query.XPATH);
      QueryResult result = query.execute();
      NodeIterator iter = result.getNodes();
      Node postNode = null;
      while (iter.hasNext()) {
        postNode = iter.nextNode();
        try {
          entries.add(postUpdated(postNode));
        } catch (Exception e) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("Failed to generate feed for post " + postNode.getPath(), e);
          }
        }
      }

    } catch (Exception e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Failed to update sundEntry by topic.", e);
      }
    }
    return entries;
  }

  private SyndEntry postUpdated(Node postNode) throws Exception {
    Node topicNode = postNode.getParent();
    String postName = postNode.getName();
    PropertyReader post = new PropertyReader(postNode);
    boolean notApproved = !post.bool(EXO_IS_APPROVED);
    boolean isPrivatePost = !post.list(EXO_USER_PRIVATE, new ArrayList<String>()).contains(EXO_USER_PRI);
    boolean topicHasLimitedViewers = hasProperty(topicNode, EXO_CAN_VIEW);

    if ((notApproved) || isPrivatePost || topicHasLimitedViewers) {
      logDebug("Post" + postName + " was not added to feed because it is private or topic has restricted audience or it is waiting for approval");
      return null;
    }

    Node forumNode = topicNode.getParent();
    Node categoryNode = forumNode.getParent();
    boolean categoryHasRestrictedAudience = (hasProperty(categoryNode, EXO_VIEWER));
    boolean forumHasRestrictedAudience = (hasProperty(forumNode, EXO_VIEWER));

    if (categoryHasRestrictedAudience || forumHasRestrictedAudience) {
      logDebug("Post" + postName + " was not added to feed because category or forum has restricted audience");
      return null;
    }

    List<String> listContent = new ArrayList<String>();
    String message = post.string(EXO_MESSAGE);
    listContent.add(message);
    SyndContent description = new SyndContentImpl();
    description.setType("text/html");
    description.setValue(getTitleRSS(message));
    final String title = post.string(EXO_NAME);
    final Date created = post.date(EXO_CREATED_DATE);
    final String owner = post.string(EXO_OWNER);

    final String linkItem = post.string(EXO_LINK);
    SyndEntry entry = createNewEntry(postName, title, linkItem, listContent, description, created, owner);

    return entry;
  }

  private SyndFeed createNewFeed(Node node, String link) throws Exception {
    PropertyReader reader = new PropertyReader(node);
    String desc = reader.string(EXO_DESCRIPTION, " ");
    SyndFeed feed = new SyndFeedImpl();
    feed.setFeedType("rss_2.0");
    feed.setTitle(getTitleRSS(reader.string(EXO_NAME))); 
    feed.setPublishedDate(reader.date(EXO_CREATED_DATE, new Date()));
    feed.setLink(link);
    feed.setDescription(getTitleRSS(desc)); 
    feed.setEncoding("UTF-8");
    return feed;
  }

  private SyndEntry createNewEntry(String uri, String title, String link, List<String> listContent, SyndContent description, Date pubDate, String author) {
    SyndEntry entry = new SyndEntryImpl();
    entry.setUri(uri);
    entry.setTitle(getTitleRSS(title)); 
    entry.setLink(link);
    List<SyndPerson> contributors = listContent.stream().map(s -> {
      SyndPerson person = new SyndPersonImpl();
      person.setName(s);
      return person;
    }).collect(Collectors.toList());
    entry.setContributors(contributors);
    entry.setDescription(description);
    entry.setPublishedDate(pubDate);
    entry.setAuthor(author);
    return entry;
  }
  
  private String getTitleRSS(String title) {
    title = StringCommonUtils.decodeSpecialCharToHTMLnumber(TransformHTML.getPlainText(title));
    return new StringBuilder("ST[CDATA[").append(StringEscapeUtils.unescapeHtml(TransformHTML.getTitleInHTMLCode(title,null)))
                                         .append("END]]")
                                         .toString();
  } 

  public InputStream createUserRss(String userId, String link) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    List<SyndEntry> entries = new ArrayList<SyndEntry>();
    try {
      Node subscriptionNode = getUserProfileHome(sProvider).getNode(userId + "/" + Utils.FORUM_SUBSCRIOTION + userId);
      PropertyReader reader = new PropertyReader(subscriptionNode);
      List<String> cateIds = reader.list(EXO_CATEGORY_IDS, new ArrayList<String>());
      Node node;
      for (String id : cateIds) {
        node = getNodeById(sProvider, id, Utils.CATEGORY);
        if (node != null) {
          entries.addAll(categoryUpdated(node));
        }
      }
      List<String> forumIds = reader.list(EXO_FORUM_IDS, new ArrayList<String>());
      for (String id : forumIds) {
        node = getNodeById(sProvider, id, Utils.FORUM);
        if (node != null && !cateIds.contains(node.getParent().getName())) {
          entries.addAll(forumUpdated(node));
        }
      }
      List<String> topicIds = reader.list(EXO_TOPIC_IDS, new ArrayList<String>());
      for (String id : topicIds) {
        node = getNodeById(sProvider, id, Utils.TOPIC);
        if (node != null && !forumIds.contains(node.getParent().getName())) {
          entries.addAll(topicUpdated(node));
        }
      }
    } catch (PathNotFoundException e) {
      LOG.info(String.format("User %s doesn't subscribe anything.", userId));
    } catch (RepositoryException e) {
      logDebug("Can not create feed data for user: " + userId, e);
    }

    try {
      SyndFeed feed = new SyndFeedImpl();
      feed.setFeedType("rss_2.0");
      feed.setTitle("Forum subscriptions for " + userId);
      feed.setPublishedDate(new Date());
      feed.setLink(link);
      feed.setDescription(" ");
      feed.setEncoding("UTF-8");
      feed.setEntries(entries);

      SyndFeedOutput output = new SyndFeedOutput();
      String s = output.outputString(feed);
      s = StringUtils.replace(s, "&amp;", "&");
      s = s.replaceAll("&lt;", "<").replaceAll("&gt;", ">");
      s = StringUtils.replace(s, "ST[CDATA[", "<![CDATA[");
      s = StringUtils.replace(s, "END]]", "]]>");
      return new ByteArrayInputStream(s.getBytes());
    } catch (FeedException e) {
      LOG.error("Can not create RSS for user: " + userId, e);
      return new ByteArrayInputStream(("Can not create RSS for user: " + userId + "<br/>" + e).getBytes());
    }
  }

  /**
   * {@inheritDoc}
   */
  public boolean populateUserProfile(User user, UserProfile profileTemplate, boolean isNew) throws Exception {
    boolean added = false;
    sessionManager.openSession();

    try {
      Node profile = null;
      Node profileHome = getUserProfileHome();
      final String userName = user.getUserName();
      if (profileHome.hasNode(userName)) {
        if (isNew) {
          LOG.warn("Request to add user " + userName + " was ignored because it already exists.");
        }
        profile = profileHome.getNode(userName);
        added = false;
      } else {
        profile = profileHome.addNode(userName, EXO_FORUM_USER_PROFILE);
        added = true;
      }

      Calendar cal = getGreenwichMeanTime();
      profile.setProperty(EXO_USER_ID, userName);
      profile.setProperty(EXO_LAST_LOGIN_DATE, cal);
      profile.setProperty(EXO_EMAIL, user.getEmail());
      profile.setProperty(EXO_FULL_NAME, user.getDisplayName());
      profile.setProperty(EXO_JOINED_DATE, cal);
      if (isAdminRole(userName)) {
        profile.setProperty(EXO_USER_TITLE, "Administrator");
        profile.setProperty(EXO_USER_ROLE, UserProfile.ADMIN); // 
      } else {
        profile.setProperty(EXO_USER_ROLE, UserProfile.USER); // 
      }

      if (profileTemplate != null) {
        profile.setProperty(EXO_TIME_ZONE, profileTemplate.getTimeZone());
        profile.setProperty(EXO_SHORT_DATEFORMAT, profileTemplate.getShortDateFormat());
        profile.setProperty(EXO_LONG_DATEFORMAT, profileTemplate.getLongDateFormat());
        profile.setProperty(EXO_TIME_FORMAT, profileTemplate.getTimeFormat());
        profile.setProperty(EXO_MAX_TOPIC, profileTemplate.getMaxTopicInPage());
        profile.setProperty(EXO_MAX_POST, profileTemplate.getMaxPostInPage());
      }
      if (profileHome.isNew()) {
        profileHome.getSession().save();
      } else {
        profileHome.save();
      }
      return added;
    } catch (Exception e) {
      LOG.error("Error while populating user profile: " + e.getMessage());
      throw e;
    } finally {
      sessionManager.closeSession(true);
    }
  }

  public String getLatestUser() throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      Node profileHome = getUserProfileHome(sProvider);
      if (profileHome.hasNodes()) {
        QueryManager qm = profileHome.getSession().getWorkspace().getQueryManager();
        StringBuilder pathQuery = new StringBuilder();
        pathQuery.append("/jcr:root").append(profileHome.getPath()).append("/element(*,exo:forumUserProfile) order by @exo:joinedDate descending");
        Query query = qm.createQuery(pathQuery.toString(), Query.XPATH);
        QueryResult result = query.execute();
        NodeIterator iter = result.getNodes();
        if (iter.getSize() > 0) {
          Node node = iter.nextNode();
          return node.getName();
        }
      }
    } catch (Exception e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Failed to get latest user login.", e);
      }
    }
    return "";
  }

  public void updateLastLoginDate(String userId) throws Exception {
    sessionManager.openSession();
    try {
      Node userProfileHome = getUserProfileHome();
      if (userProfileHome.hasNode(userId)) {
        userProfileHome.getNode(userId).setProperty(EXO_LAST_LOGIN_DATE, getGreenwichMeanTime());
        userProfileHome.save();
      }
    } finally {
      sessionManager.closeSession();
    }
  }

  //get all categories for user can view.
  private List<String> getCategoriesUserCanview(Node categoryHome, List<String> listOfUser) throws Exception {
  /*      
   * cateids = list query cateids = listinput -> public for property (null for view) 
   * cateids = {cateid} for private --> can not view 
   * cateids = new array for view --> can not view
   */
    List<String> categoryCanView = new ArrayList<String>();
    Map<String, List<String>> mapPrivate = getCategoryViewer(categoryHome, listOfUser, new ArrayList<String>(), new ArrayList<String>(), EXO_USER_PRIVATE);
    // all categoryid public for private user
    List<String> categoryIds = mapPrivate.get(Utils.CATEGORY);
    // all categoryid public for Viewer
    Map<String, List<String>> mapList = getCategoryViewer(categoryHome, listOfUser, null, new ArrayList<String>(), EXO_VIEWER);
    List<String> categoryView = mapList.get(Utils.CATEGORY);
    // user not in restricted audience or can not viewer
    if (categoryIds.contains("cateId") || (categoryView != null && categoryView.isEmpty())){
      return null;
    }
    if (categoryIds.isEmpty()) {
      categoryCanView.addAll((categoryView == null ? new ArrayList<String>() : categoryView));
    } else {
      for (String string : categoryIds) {
        if (categoryView == null || categoryView.contains(string)) {
          categoryCanView.add(string);
        }
      }
    }
    return categoryCanView;
  }

  // check permission for everyone can view the post
  private boolean postIsPublicByParent(Node postNode) throws Exception {
    Node node = postNode.getParent();
    // checking from the topic parent. Check by canView
    if (!new PropertyReader(node).list(EXO_CAN_VIEW, Collections.EMPTY_LIST).isEmpty())
      return false;
    // checking from the forum parent. Check by canView
    if (!new PropertyReader(node = node.getParent()).list(EXO_VIEWER, Collections.EMPTY_LIST).isEmpty())
      return false;
    // checking from the category parent. Check by canView
    if (!new PropertyReader(node = node.getParent()).list(EXO_VIEWER, Collections.EMPTY_LIST).isEmpty())
      return false;
    //  Check by restricted audience  
    if (!new PropertyReader(node).list(EXO_USER_PRIVATE, Collections.EMPTY_LIST).isEmpty())
      return false;
    return true;
  }
  // check permssion user can view
  private boolean checkPermssionCanView(Node postNode, boolean isUserLogin, List<String> categoryCanView, List<String> forumCanView) throws Exception {
    if(isUserLogin) {
      String []path = postNode.getPath().split("/");
      return (categoryCanView.isEmpty() || categoryCanView.contains(path[path.length - 4])) && (forumCanView.isEmpty() || forumCanView.contains(path[path.length - 3]));
    } else {
      return postIsPublicByParent(postNode);
    }
  }
  // get post by query.
  private List<Post> getPostByQuery(Node categoryHome, QueryImpl impl, int number, String userName, boolean isAdmin) throws Exception {
    List<Post> list = new ArrayList<Post>();
    List<String> categoryCanView = new ArrayList<String>();
    List<String> forumCanView = new ArrayList<String>();
    boolean isUserLogin = false;
    if (!Utils.isEmpty(userName) && !UserProfile.USER_GUEST.equals(userName)) {
      isUserLogin = true;
      if (!isAdmin) {
        List<String> listOfUser = UserHelper.getAllGroupAndMembershipOfUser(null);
        categoryCanView = getCategoriesUserCanview(categoryHome, listOfUser);
        if (categoryCanView == null){
          return list;
        }
        forumCanView.addAll(getCachedDataStorage().getForumUserCanView(listOfUser, new ArrayList<String>()));
      }
    }

    int offset = 0, count = 0, limit;
    QueryResult qr;
    NodeIterator iter;
    Node node;
    while (count < number) {
      impl.setOffset(offset);
      limit = number + offset;
      impl.setLimit(limit);
      qr = impl.execute();
      iter = qr.getNodes();
      if (iter.getSize() <= 0) {
        return list;
      }
      while (iter.hasNext()) {
        node = iter.nextNode();
        if (isAdmin || checkPermssionCanView(node, isUserLogin, categoryCanView, forumCanView)) {
          list.add(getPost(node));
          count++;
          if (count == number){
            break;
          }
        }
      }
      offset = limit;
    }
    return list;
  }

// the function use to get recent post for user.
  public List<Post> getRecentPostsForUser(String userName, int number) throws Exception {
    if (number <= 0){
      return new ArrayList<Post>();
    }
    if (Utils.isEmpty(userName) || UserProfile.USER_GUEST.equals(userName)) {
      return getNewPosts(number);
    }
    List<Post> list = new ArrayList<Post>();
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      boolean isAdmin = isAdminRole(userName);
      if (!isAdmin) {
        isAdmin = (new PropertyReader(getUserProfileNode(getUserProfileHome(sProvider), userName)).l(EXO_USER_ROLE, 3) == 0) ? true : false;
      }
      Node categoryHome = getCategoryHome(sProvider);
            
      StringBuffer stringBuffer = new StringBuffer();
      stringBuffer.append(JCR_ROOT).append(categoryHome.getPath()).append("//element(*,").append(EXO_POST).append(")[(");
      if (!isAdmin) stringBuffer.append("(@").append(EXO_IS_APPROVED).append("='true') and (@").append(EXO_IS_HIDDEN).append("='false') and (@")
                                .append(EXO_IS_WAITING).append("='false') and (@").append(EXO_IS_ACTIVE_BY_TOPIC).append("='true') and ");
      stringBuffer.append("(@").append(EXO_USER_PRIVATE).append("='exoUserPri' or @").append(EXO_USER_PRIVATE).append("='").append(userName)
                  .append("'))]").append(" order by @").append(EXO_CREATED_DATE).append(" descending");
      
      QueryManager qm = categoryHome.getSession().getWorkspace().getQueryManager();
      Query query = qm.createQuery(stringBuffer.toString(), Query.XPATH);
      // get posts
      if(query instanceof QueryImpl){
        list = getPostByQuery(categoryHome, (QueryImpl)query, number, userName, isAdmin);
      }
    } catch (Exception e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Failed to get new post.", e);
      }
    }
    return list;
  }

  //the function use to get recent post for everyone.  
  public List<Post> getNewPosts(int number) throws Exception {
    List<Post> list = new ArrayList<Post>();
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      Node categoryHome = getCategoryHome(sProvider);
      QueryManager qm = categoryHome.getSession().getWorkspace().getQueryManager();
      StringBuffer stringBuffer = new StringBuffer();
      stringBuffer.append(JCR_ROOT).append(categoryHome.getPath()).append("//element(*,").append(EXO_POST)
                  .append(") [((@").append(EXO_IS_APPROVED).append("='true') and (@").append(EXO_IS_HIDDEN).append("='false') and (@")
                  .append(EXO_IS_WAITING).append("='false') and (@").append(EXO_IS_ACTIVE_BY_TOPIC).append("='true') and (@")
                  .append(EXO_USER_PRIVATE).append("='exoUserPri'))] order by @").append(EXO_CREATED_DATE).append(" descending");
      Query query = qm.createQuery(stringBuffer.toString(), Query.XPATH);
      // get posts
      if(query instanceof QueryImpl){
        list = getPostByQuery(categoryHome, (QueryImpl)query, number, "", false);
      }
    } catch (Exception e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Failed to get new post.", e);
      }
    }
    return list;
  }

  public boolean deleteUserProfile(String userId) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      Node profileHome = getUserProfileHome(sProvider);
      Node profileDeleted;
      Node profile = profileHome.getNode(userId);
      Session session = profileHome.getSession();
      profile.setProperty(EXO_USER_ROLE, UserProfile.USER_DELETED);
      profile.setProperty(EXO_USER_TITLE, UserProfile.USER_REMOVED);
      profile.setProperty(EXO_MODERATE_CATEGORY, new String[] {});
      profile.setProperty(EXO_MODERATE_FORUMS, new String[] {});
      profile.save();
      StringBuilder id = new StringBuilder(userId).append(Utils.DELETED);
      try {
        profileDeleted = profileHome.getNode(Utils.USER_PROFILE_DELETED);
        long index = profileDeleted.getNodes().getSize();
        if (index > 0){
          id.append(index);
        }
      } catch (Exception e) {
        profileDeleted = profileHome.addNode(Utils.USER_PROFILE_DELETED, EXO_USER_DELETED);
        session.save();
        deletedUserCalculateListener(profileDeleted);
      }
      session.getWorkspace().move(profile.getPath(), new StringBuilder(profileDeleted.getPath()).append(CommonUtils.SLASH).append(id).toString());
      try {
        Node avatarHome = session.getRootNode().getNode(dataLocator.getAvatarsLocation());
        if (avatarHome.hasNode(userId)) {
          avatarHome.getNode(userId).remove();
        }
      } catch (Exception e) {
        LOG.info("User deleted has not avatar !!!");
      }
      session.save();
    } catch (PathNotFoundException e) {
      return false;
    }
    return true;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.forum.service.DataStorage#processEnabledUser(java.lang.String, java.lang.String, boolean)
   */
  @Override
  public void processEnabledUser(String userName, String email, boolean isEnabled) {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    if (!isEnabled && !CommonUtils.isEmpty(userName)) {
      processWatched(sProvider, userName);
    }
    //
    if (!isEnabled && !CommonUtils.isEmpty(email)) {
      processForumNotifyEmail(sProvider, email);
    }
    //
    if (!CommonUtils.isEmpty(userName)) {
      processUserProfile(sProvider, userName, isEnabled);
    }
  }
  
  private void processUserProfile(SessionProvider sProvider, String userName, boolean isEnabled) {
    try {
      Node profileHome = getUserProfileHome(sProvider);
      if (profileHome.hasNode(userName)) {
        Node profileNode = profileHome.getNode(userName);
        if (!profileNode.isNodeType(EXO_DISABLED)) {
          profileNode.addMixin(EXO_DISABLED);
        }
        profileNode.setProperty(EXO_IS_DISABLED, !isEnabled);
        profileHome.getSession().save();
      } else {
        LOG.warn(String.format("Forum's profile(%s) not found!", userName));
      }
      
    } catch (Exception e) {
      logDebug(String.format("Process to to update status disabled/enabled of used %s is unsuccessfully.", userName), e);
    }
  }

  /**
   * Process remove userName and email, that watched by disabled user
   * @param sProvider
   * @param userName The userName of disabled user
   */
  private void processWatched(SessionProvider sProvider, String userName) {
    try {
      Node categoryHome = getCategoryHome(sProvider);
      QueryManager qm = categoryHome.getSession().getWorkspace().getQueryManager();
      StringBuilder sqlQuery = new StringBuilder("SELECT * FROM ").append(EXO_FORUM_WATCHING);
      sqlQuery.append(" WHERE ").append(EXO_USER_WATCHING).append("='").append(userName).append("'");
      Query query = qm.createQuery(sqlQuery.toString(), Query.SQL);
      NodeIterator iter = query.execute().getNodes();
      while (iter.hasNext()) {
        Node watchedNode = iter.nextNode();
        updateWatchedProperty(watchedNode, userName);
      }
      categoryHome.getSession().save();
    } catch (Exception e) {
      logDebug(String.format("Removes UserName %s is unsuccessfully.", userName), e);
    }
  }

  /**
   * Process remove email on forum notify.
   * 
   * @param sProvider
   * @param email The email of disabled user
   */
  private void processForumNotifyEmail(SessionProvider sProvider, String email) {
    try {
      Node categoryHome = getCategoryHome(sProvider);
      QueryManager qm = categoryHome.getSession().getWorkspace().getQueryManager();
      //
      StringBuilder sqlQuery = new StringBuilder("SELECT * FROM ").append(EXO_FORUM);
      sqlQuery.append(" WHERE (").append(EXO_NOTIFY_WHEN_ADD_TOPIC).append("='").append(email).append("' OR ")
              .append(EXO_NOTIFY_WHEN_ADD_POST).append("='").append(email).append("')");
      Query query = qm.createQuery(sqlQuery.toString(), Query.SQL);
      NodeIterator iter = query.execute().getNodes();
      //
      while (iter.hasNext()) {
        Node forumNode = iter.nextNode();
        updateForumNotifyEmail(forumNode, email);
      }

      categoryHome.getSession().save();
    } catch (Exception e) {
      logDebug(String.format("Removes email %s is unsuccessfully.", email), e);
    }
  }

  /**
   * Update two properties of Forum, that contain email of disabled user 
   * 
   * @param forumNode
   * @param email
   */
  private void updateForumNotifyEmail(Node forumNode, String email) {
    try {
      removeValueProperty(forumNode, EXO_NOTIFY_WHEN_ADD_TOPIC, email);
      removeValueProperty(forumNode, EXO_NOTIFY_WHEN_ADD_POST, email);
    } catch (Exception e) {
      logDebug(String.format("Updated forum notify of email %s is unsuccessfully.", email), e);
    }
  }

  /**
   * Remove value of property
   * 
   * @param node The node want to update
   * @param property The property change
   * @param removeValue The value will remove
   * @return
   * @throws Exception
   */
  private int removeValueProperty(Node node, String property, String removeValue) throws Exception {
    String[] values = new PropertyReader(node).strings(property, new String[] {});
    int index = ArrayUtils.indexOf(values, removeValue);
    if (index >= 0) {
      node.setProperty(property, (String[]) ArrayUtils.remove(values, index));
    }
    return index;
  }

  /**
   * Update watched properties of categories/forums/topics, that disabled user watched.
   * 
   * @param watchedNode
   * @param userName
   */
  private void updateWatchedProperty(Node watchedNode, String userName) {
    try {
      int index = removeValueProperty(watchedNode, EXO_USER_WATCHING, userName);
      if (index >= 0) {
        String[] emails = new PropertyReader(watchedNode).strings(EXO_EMAIL_WATCHING, new String[] {});
        watchedNode.setProperty(EXO_EMAIL_WATCHING, (String[]) ArrayUtils.remove(emails, index));
      }
    } catch (Exception e) {
      logDebug(String.format("Updated watched user %s is unsuccessfully.", userName), e);
    }
  }

  public void calculateDeletedUser(String userName) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    String tempUserName = userName;
    userName = tempUserName.substring(0, tempUserName.indexOf(Utils.DELETED));

    try {
      Node categoryHome = getCategoryHome(sProvider);
      QueryManager qm = categoryHome.getSession().getWorkspace().getQueryManager();
      String[] strs = new String[] { EXO_OWNER, EXO_MODIFIED_BY, EXO_LAST_POST_BY, EXO_USER_PRIVATE, EXO_MODERATORS, EXO_CREATE_TOPIC_ROLE,
                                     EXO_POSTER, EXO_VIEWER, EXO_CAN_POST, EXO_CAN_VIEW, EXO_USER_WATCHING, EXO_RSS_WATCHING };
      StringBuilder builder = new StringBuilder();
      for (int i = 0; i < strs.length; i++) {
        if (i > 0)
          builder.append(" or ");
        builder.append("(@").append(strs[i]).append("='").append(JCRQueryUtils.escapeSimpleQuoteCharacter(userName)).append("')");
      }

      StringBuilder pathQuery = new StringBuilder();
      pathQuery.append(JCR_ROOT).append(categoryHome.getPath()).append("//*[").append(builder).append("]");
      Query query = qm.createQuery(pathQuery.toString(), Query.XPATH);
      QueryResult result = query.execute();
      NodeIterator iter = result.getNodes();

      while (iter.hasNext()) {
        Node node = iter.nextNode();
        if (node.isNodeType(EXO_FORUM_CATEGORY) || node.isNodeType(EXO_FORUM) || node.isNodeType(EXO_TOPIC) || node.isNodeType(EXO_POST)) {
          for (int i = 0; i < strs.length; i++) {
            if (i < 3) {
              if (new PropertyReader(node).string(strs[i], "").equals(userName)) {
                node.setProperty(strs[i], tempUserName);
              }
            } else {
              if (strs[i].equals(EXO_USER_WATCHING)) {
                updateWatchedProperty(node, userName);
              } else {
                removeValueProperty(node, strs[i], userName);
              }
            }
          }
        }
      }
      if (categoryHome.isNew()) {
        categoryHome.getSession().save();
      } else {
        categoryHome.save();
      }
    } catch (Exception e) {
      LOG.error("Failed to calculate deleting user.", e);
    }
  }

  public void calculateDeletedGroup(String groupId, String groupName) throws Exception {
    try {
      // remove forum in space
      Node forumSpaceNode = getNodeById(CommonUtils.createSystemProvider(), Utils.FORUM_SPACE_ID_PREFIX + groupName, Utils.FORUM);
      if (forumSpaceNode != null) {
        LOG.info("\nINFO: Delete forum in space: " + forumSpaceNode.getName());
        removeForum(forumSpaceNode.getParent().getName(), forumSpaceNode.getName());
      }
      // remove group storage in categories/forums/topics
      SessionProvider sProvider = CommonUtils.createSystemProvider();
      Node categoryHome = getCategoryHome(sProvider);
      QueryManager qm = categoryHome.getSession().getWorkspace().getQueryManager();
      String[] strs = new String[] { EXO_USER_PRIVATE, EXO_CREATE_TOPIC_ROLE, EXO_POSTER, EXO_VIEWER, EXO_CAN_POST, EXO_CAN_VIEW, EXO_MODERATORS };
      StringBuilder pathQuery = new StringBuilder(JCR_ROOT).append(categoryHome.getPath()).append("//*[");
      for (int i = 0; i < strs.length; i++) {
        if (i > 0) {
          pathQuery.append(" or ");
        }
        pathQuery.append("(@").append(strs[i]).append("='").append(groupId).append("') or (jcr:contains(@").append(strs[i]).append(", '").append(groupId).append("'))");
      }
      pathQuery.append("]");
      Query query = qm.createQuery(pathQuery.toString(), Query.XPATH);
      QueryResult result = query.execute();
      NodeIterator iter = result.getNodes();
      List<String> list;
      PropertyReader reader;
      boolean isSave;
      while (iter.hasNext()) {
        Node node = iter.nextNode();
        if (node.isNodeType(EXO_FORUM_CATEGORY) || node.isNodeType(EXO_FORUM) || node.isNodeType(EXO_TOPIC)) {
          isSave = false;
          reader = new PropertyReader(node);
          for (int i = 0; i < strs.length; i++) {
            list = reader.list(strs[i], new ArrayList<String>());
            if (!list.isEmpty()) {
              int oldSize = list.size();
              list = containsGroup(list, groupId);
              if(oldSize > list.size() || list.get(0).equals(CommonUtils.EMPTY_STR)) {
                if(strs[i].equals(EXO_MODERATORS)) {
                  // calculate moderator for users
                  node.setProperty(EXO_TEMP_MODERATORS, reader.strings(EXO_MODERATORS, new String[]{CommonUtils.EMPTY_STR}));
                  node.save();
                }
                node.setProperty(strs[i], list.toArray(new String[list.size()]));
                isSave = true;
              }
            }
          }
          if(isSave){
            node.save();
          }
        }
      }
    } catch (Exception e) {
      logDebug("Failed to calculate deleted Group.", e);
    }
  }
  
  public static List<String> containsGroup(List<String> list, String groupId) {
    List<String> ls = new ArrayList<String>();
    for (String string : list) {
      if(string.indexOf(groupId) >= 0) {
        continue;
      }
      ls.add(string);
    }
    if(ls.isEmpty()) {
      ls.add(CommonUtils.EMPTY_STR);
    }
    return ls;
  }
  
  public List<InitializeForumPlugin> getDefaultPlugins() {
    return defaultPlugins;
  }

  public List<RoleRulesPlugin> getRulesPlugins() {
    return rulesPlugins;
  }

  public Map<String, String> getServerConfig() {
    return serverConfig;
  }

  public KSDataLocation getDataLocation() {
    return dataLocator;
  }

  public void setDataLocator(KSDataLocation dataLocator) {
    this.dataLocator = dataLocator;
    sessionManager = dataLocator.getSessionManager();
    repository = dataLocator.getRepository();
    workspace = dataLocator.getWorkspace();
    LOG.info("JCR Data Storage for forum initialized to " + dataLocator);
  }

  private static void logDebug(String message, Throwable e) {
    LOG.warn(message);
    if (LOG.isDebugEnabled() && e != null) {
      LOG.debug(e.getMessage(), e);
    }
  }

  private static void logDebug(String message) {
    logDebug(message, null);
  }

  private Calendar getGreenwichMeanTime() {
    return CommonUtils.getGreenwichMeanTime();
  }

  @Override
  public void saveActivityIdForOwner(String ownerId, String type, String activityId) {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    final ReentrantLock localLock = lock;
    try {
      localLock.lock();
      Node ownerNode = getNodeById(sProvider, ownerId, type);
      ActivityTypeUtils.attachActivityId(ownerNode, activityId);
      ownerNode.save();
    } catch (Exception e) {
      logDebug(String.format("Failed to attach activityId %s for node %s ", activityId, ownerId), e);
    } finally {
      localLock.unlock();
    }
  }

  @Override
  public void saveActivityIdForOwner(String ownerPath, String activityId) {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    final ReentrantLock localLock = lock;
    try {
      localLock.lock();
      Node ownerNode = getNodeAt(sProvider, ownerPath);
      if(ownerNode != null) {
        ActivityTypeUtils.attachActivityId(ownerNode, activityId);
        ownerNode.save();
      }
    } catch (Exception e) {
      logDebug(String.format("Failed to save attach activityId %s for node %s ", activityId, ownerPath), e);
    } finally {
      localLock.unlock();
    }
  }

  @Override
  public String getActivityIdForOwner(String ownerId, String type) {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      Node ownerNode = getNodeById(sProvider, ownerId, type);
      if(ownerNode != null) {
        return ActivityTypeUtils.getActivityId(ownerNode);
      }
    } catch (Exception e) {
      LOG.error("Failed to get activityId for {}: {}", type, ownerId, e);
    }
    return null;
  }

  @Override
  public String getActivityIdForOwner(String ownerPath) {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      Node ownerNode = getNodeAt(sProvider, ownerPath);
      if(ownerNode != null) {
        return ActivityTypeUtils.getActivityId(ownerNode);
      }
    } catch (Exception e) {
      LOG.error(String.format("Failed to get attach activityId for %s ", ownerPath), e);
    }
    return null;
  }
  
  private StringBuilder jcrPathLikeAndNotLike(String nodeType, String fullPath) {
    StringBuilder sqlQuery = new StringBuilder("SELECT * FROM ").append(nodeType)
        .append(" WHERE (").append("jcr:path LIKE '").append(fullPath)
        .append("/%' AND NOT jcr:path LIKE '").append(fullPath).append("/%/%'").append(")");
    return sqlQuery;
  }

  @Override
  public List<UserProfile> searchUserProfileByFilter(UserProfileFilter userProfileFilter, int offset, int limit) throws Exception {

    SessionProvider sProvider = CommonUtils.createSystemProvider();
    List<UserProfile> list = new ArrayList<UserProfile>();
    try {
      //
      DataStorage storage = getCachedDataStorage();
      Node userProfileNode = getUserProfileHome(sProvider);
      QueryManager qm = userProfileNode.getSession().getWorkspace().getQueryManager();

      QueryImpl query = (QueryImpl) qm.createQuery(buildSearchUserProfileQuery(userProfileFilter.getSearchKey()), Query.SQL);
      query.setOffset(offset);
      query.setLimit(limit);

      QueryResult result = query.execute();
      NodeIterator iter = result.getNodes();
      while (iter.hasNext()) {
        Node node = iter.nextNode();
        list.add(storage.getQuickProfile(node.getName()));
      }
    } catch (Exception e) {
      LOG.warn("Get UserProfile by filter failed.", e);
    }

    return list;
  }
  
  @Override
  public int getUserProfileByFilterCount(UserProfileFilter userProfileFilter) throws Exception {
    SessionProvider sProvider = CommonUtils.createSystemProvider();
    try {
      //
      Node userProfileNode = getUserProfileHome(sProvider);
      QueryManager qm = userProfileNode.getSession().getWorkspace().getQueryManager();
      Query query = qm.createQuery(buildSearchUserProfileQuery(userProfileFilter.getSearchKey()), Query.SQL);
      QueryResult result = query.execute();
      NodeIterator iterator = result.getNodes();

      return (int) iterator.getSize();
    } catch (Exception e) {
      LOG.warn("Get count of UserProfile by filter failed.", e);
    }
    return 0;
  }

  private String buildSearchUserProfileQuery(String searchKey) {
    String fullPath = "/" + dataLocator.getUserProfilesLocation();

    StringBuilder sqlQuery = jcrPathLikeAndNotLike(Utils.USER_PROFILES_TYPE, fullPath);
    if (Utils.isEmpty(searchKey) == false) {
      searchKey = searchKey.replace("*", "%");
      sqlQuery.append(" AND (")
      .append(EXO_USER_ID).append(" LIKE '%").append(searchKey).append("%' OR ")
      .append(EXO_FULL_NAME).append(" LIKE '%").append(searchKey).append("%' OR ")
      .append(EXO_SCREEN_NAME).append(" LIKE '%").append(searchKey).append("%'")
      .append(")");
    }
    sqlQuery.append(" AND (").append(EXO_IS_DISABLED).append(" IS NULL OR ")
            .append(EXO_IS_DISABLED).append("<>'true')");
    return sqlQuery.toString();
  }
  
  @Override
  public void removeCacheUserProfile(String userName) {
  }
  
  private NodeIterator getNodeIteratorBySQLQuery(SessionProvider sProvider, String sqlQuery, int offset, int limit, boolean caseInsensitiveOrder) throws Exception {
    QueryManager qm = sessionManager.getSession(sProvider).getWorkspace().getQueryManager();
    QueryImpl query = (QueryImpl) qm.createQuery(sqlQuery, Query.SQL);
    if (limit > 0) {
      query.setOffset(offset);
      query.setLimit(limit);
    }
    if (caseInsensitiveOrder) {
      query.setCaseInsensitiveOrder(true);
    }
    QueryResult result = query.execute();
    return result.getNodes();
  }
  
}
