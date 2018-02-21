package org.exoplatform.forum.service.cache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.jcr.NodeIterator;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.forum.common.cache.ServiceContext;
import org.exoplatform.forum.common.cache.model.CacheType;
import org.exoplatform.forum.common.cache.model.CachedData;
import org.exoplatform.forum.common.cache.model.data.SimpleCacheData;
import org.exoplatform.forum.common.cache.model.key.SimpleCacheKey;
import org.exoplatform.forum.common.cache.model.selector.ScopeCacheSelector;
import org.exoplatform.forum.common.conf.RoleRulesPlugin;
import org.exoplatform.forum.common.jcr.KSDataLocation;
import org.exoplatform.forum.common.lifecycle.LifeCycleCompletionService;
import org.exoplatform.forum.service.Category;
import org.exoplatform.forum.service.DataStorage;
import org.exoplatform.forum.service.Forum;
import org.exoplatform.forum.service.ForumAdministration;
import org.exoplatform.forum.service.ForumAttachment;
import org.exoplatform.forum.service.ForumEventQuery;
import org.exoplatform.forum.service.ForumLinkData;
import org.exoplatform.forum.service.ForumPrivateMessage;
import org.exoplatform.forum.service.ForumSearchResult;
import org.exoplatform.forum.service.ForumStatistic;
import org.exoplatform.forum.service.ForumSubscription;
import org.exoplatform.forum.service.InitializeForumPlugin;
import org.exoplatform.forum.service.JCRPageList;
import org.exoplatform.forum.service.LazyPageList;
import org.exoplatform.forum.service.MessageBuilder;
import org.exoplatform.forum.service.Post;
import org.exoplatform.forum.service.PruneSetting;
import org.exoplatform.forum.service.SendMessageInfo;
import org.exoplatform.forum.service.SortSettings;
import org.exoplatform.forum.service.SortSettings.Direction;
import org.exoplatform.forum.service.SortSettings.SortField;
import org.exoplatform.forum.service.Tag;
import org.exoplatform.forum.service.Topic;
import org.exoplatform.forum.service.UserProfile;
import org.exoplatform.forum.service.Utils;
import org.exoplatform.forum.service.Watch;
import org.exoplatform.forum.service.cache.model.data.CategoryData;
import org.exoplatform.forum.service.cache.model.data.ForumData;
import org.exoplatform.forum.service.cache.model.data.LinkData;
import org.exoplatform.forum.service.cache.model.data.ListCategoryData;
import org.exoplatform.forum.service.cache.model.data.ListForumData;
import org.exoplatform.forum.service.cache.model.data.ListLinkData;
import org.exoplatform.forum.service.cache.model.data.ListPostData;
import org.exoplatform.forum.service.cache.model.data.ListTopicData;
import org.exoplatform.forum.service.cache.model.data.ListUserProfileData;
import org.exoplatform.forum.service.cache.model.data.ListWatchData;
import org.exoplatform.forum.service.cache.model.data.LoginUserProfileData;
import org.exoplatform.forum.service.cache.model.data.PostData;
import org.exoplatform.forum.service.cache.model.data.TagData;
import org.exoplatform.forum.service.cache.model.data.TopicData;
import org.exoplatform.forum.service.cache.model.data.UserProfileData;
import org.exoplatform.forum.service.cache.model.data.WatchData;
import org.exoplatform.forum.service.cache.model.key.CategoryKey;
import org.exoplatform.forum.service.cache.model.key.CategoryListKey;
import org.exoplatform.forum.service.cache.model.key.ForumKey;
import org.exoplatform.forum.service.cache.model.key.ForumListKey;
import org.exoplatform.forum.service.cache.model.key.LinkListKey;
import org.exoplatform.forum.service.cache.model.key.ObjectNameKey;
import org.exoplatform.forum.service.cache.model.key.PostKey;
import org.exoplatform.forum.service.cache.model.key.PostListCountKey;
import org.exoplatform.forum.service.cache.model.key.PostListKey;
import org.exoplatform.forum.service.cache.model.key.TopicKey;
import org.exoplatform.forum.service.cache.model.key.TopicListCountKey;
import org.exoplatform.forum.service.cache.model.key.TopicListKey;
import org.exoplatform.forum.service.cache.model.key.UserProfileKey;
import org.exoplatform.forum.service.cache.model.key.UserProfileListCountKey;
import org.exoplatform.forum.service.cache.model.key.UserProfileListKey;
import org.exoplatform.forum.service.cache.model.selector.CategoryIdSelector;
import org.exoplatform.forum.service.cache.model.selector.ForumPathSelector;
import org.exoplatform.forum.service.cache.model.selector.MiscDataSelector;
import org.exoplatform.forum.service.cache.model.selector.PostListCountSelector;
import org.exoplatform.forum.service.cache.model.selector.TopicListCountSelector;
import org.exoplatform.forum.service.cache.model.selector.TopicListSelector;
import org.exoplatform.forum.service.filter.model.CategoryFilter;
import org.exoplatform.forum.service.filter.model.ForumFilter;
import org.exoplatform.forum.service.impl.JCRDataStorage;
import org.exoplatform.forum.service.impl.model.PostFilter;
import org.exoplatform.forum.service.impl.model.TopicFilter;
import org.exoplatform.forum.service.impl.model.UserProfileFilter;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.cache.future.FutureExoCache;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.User;
import org.picocontainer.Startable;

public class CachedDataStorage implements DataStorage, Startable {

  private static final Log LOG = ExoLogger.getLogger(CachedDataStorage.class);
  private static final String PRIVATE_MESSAGE_COUNT_KEY = "messageCount";
  private static final String SCREEN_NAME_KEY = "screenName";
  private static final String FORUM_CAN_VIEW_KEY = "userCanView";
  private static final String USER_AVATAR_KEY = "userAvatarKey";
  private static final String PROFILE_KEY = "profile";
  private static final String WATCH_TYPE = "watchType";

  private DataStorage storage;
  private CacheService service;
  private LifeCycleCompletionService completionService;

  //
  private ExoCache<CategoryKey, CategoryData> categoryData;
  private ExoCache<CategoryListKey, ListCategoryData> categoryList;
  private ExoCache<ForumKey, ForumData> forumData;
  private ExoCache<ForumListKey, ListForumData> forumList;
  
  private ExoCache<PostKey, PostData> postData;
  private ExoCache<PostListKey, ListPostData> postList;
  private ExoCache<PostListCountKey, SimpleCacheData<Integer>> postListCount;
  
  private ExoCache<TopicKey, TopicData> topicData;
  private ExoCache<TopicListKey, ListTopicData> topicList;
  private ExoCache<TopicListCountKey, SimpleCacheData<Integer>> topicListCount;
  
  private ExoCache<UserProfileKey, UserProfileData> userProfileData;
  private ExoCache<UserProfileListKey, ListUserProfileData> userProfileList;
  private ExoCache<UserProfileListCountKey, SimpleCacheData<Integer>> userProfileListCount;
  
  private ExoCache<UserProfileKey, LoginUserProfileData> loginUserProfile;
  
  private ExoCache<SimpleCacheKey, ListWatchData> watchListData;
  private ExoCache<LinkListKey, ListLinkData> linkListData;
  private ExoCache<ObjectNameKey, CachedData> objectNameData;
  private ExoCache<SimpleCacheKey, SimpleCacheData> miscData;

  //
  private FutureExoCache<CategoryKey, CategoryData, ServiceContext<CategoryData>> categoryDataFuture;
  private FutureExoCache<CategoryListKey, ListCategoryData, ServiceContext<ListCategoryData>> categoryListFuture;
  
  private FutureExoCache<ForumKey, ForumData, ServiceContext<ForumData>> forumDataFuture;
  private FutureExoCache<ForumListKey, ListForumData, ServiceContext<ListForumData>> forumListFuture;

  private FutureExoCache<PostKey, PostData, ServiceContext<PostData>> postDataFuture;
  private FutureExoCache<PostListKey, ListPostData, ServiceContext<ListPostData>> postListFuture;
  private FutureExoCache<PostListCountKey, SimpleCacheData<Integer>, ServiceContext<SimpleCacheData<Integer>>> postListCountFuture;

  private FutureExoCache<TopicKey, TopicData, ServiceContext<TopicData>> topicDataFuture;
  private FutureExoCache<TopicListKey, ListTopicData, ServiceContext<ListTopicData>> topicListFuture;
  private FutureExoCache<TopicListCountKey, SimpleCacheData<Integer>, ServiceContext<SimpleCacheData<Integer>>> topicListCountFuture;
  
  private FutureExoCache<UserProfileKey, UserProfileData, ServiceContext<UserProfileData>> userProfileDataFuture;
  private FutureExoCache<UserProfileListKey, ListUserProfileData, ServiceContext<ListUserProfileData>> userProfileListFuture;
  private FutureExoCache<UserProfileListCountKey, SimpleCacheData<Integer>, ServiceContext<SimpleCacheData<Integer>>> userProfileListCountFuture;
  
  private FutureExoCache<UserProfileKey, LoginUserProfileData, ServiceContext<LoginUserProfileData>> loginUserProfileFuture;

  private FutureExoCache<SimpleCacheKey, ListWatchData, ServiceContext<ListWatchData>> watchListDataFuture;
  private FutureExoCache<LinkListKey, ListLinkData, ServiceContext<ListLinkData>> linkListDataFuture;
  private FutureExoCache<SimpleCacheKey, SimpleCacheData, ServiceContext<SimpleCacheData>> miscDataFuture;

  private ForumStatistic statistic;

  public CachedDataStorage(CacheService service, JCRDataStorage storage) {
    
    this.storage = storage;
    this.service = service;
    this.completionService = CommonsUtils.getService(LifeCycleCompletionService.class);
  }

  private void clearCategoryCache(String id) throws Exception {
    categoryData.remove(new CategoryKey(id));
  }

  private void clearCategoryCache(Category category) throws Exception {
    if (category != null) {
      clearCategoryCache(category.getId());
      clearObjectCache(category, false);
    }
  }

  private void clearForumCache(Forum forum, boolean isPutNewKey) throws Exception {
    if (isPutNewKey) {
      forumData.put(new ForumKey(forum), new ForumData(forum));
    } else {
      forumData.remove(new ForumKey(forum));
    }
    statistic = null;
  }

  private void clearForumCache(String categoryId, String forumId, boolean isPutNewKey) throws Exception {
    Forum forum = getForum(categoryId, forumId);
    if (forum != null) {
      clearForumCache(forum, isPutNewKey);
    }
  }
  
  private void clearForumListCache() throws Exception {
    forumList.select(new ScopeCacheSelector<ForumListKey, ListForumData>());
  }

  private void clearPostListCache() throws Exception {
    postList.select(new ScopeCacheSelector<PostListKey, ListPostData>());
  }

  private void clearPostListCountCache(String topicId) throws Exception {   
    postListCount.select(new PostListCountSelector(topicId));
  }

  private void clearTopicListCache() throws Exception {
    topicList.select(new ScopeCacheSelector<TopicListKey, ListTopicData>());
  }
  
  private void clearTopicListCache(String forumId) throws Exception {
    topicList.select(new TopicListSelector(forumId));
  }
  
  private void clearTopicListCountCache(String forumId) throws Exception {   
    topicListCount.select(new TopicListCountSelector(forumId));
  }

  private void clearLinkListCache() throws Exception {
    linkListData.select(new ScopeCacheSelector<LinkListKey, ListLinkData>());
  }

  private void clearMiscDataCache(String type) throws Exception {
    miscData.select(new MiscDataSelector(type));
  }
  
  private void clearTopicsCache(List<Topic> topics) throws Exception {
    for(Topic t : topics) {
      clearTopicCache(t);
    }
  }
  
  private void clearUserProfileListCache() throws Exception {
    if (userProfileList != null) {
      userProfileList.select(new ScopeCacheSelector<UserProfileListKey, ListUserProfileData>());
    }
  }
  
  private void clearUserProfileListCountCache() throws Exception {
    if (userProfileListCount != null) {
      userProfileListCount.select(new ScopeCacheSelector<UserProfileListCountKey, SimpleCacheData<Integer>>());
    }
  }

  private void clearTopicCache(String topicPath) {
    try {
      Topic topic = getTopicByPath(topicPath, false);
      clearTopicCache(topic);
    } catch (Exception e) {
      LOG.warn("Can not clear topic cache for: " + topicPath);
    }
  }

  private void clearTopicCache(String categoryId, String forumId, String topicId) throws Exception {
    String topicPath = new StringBuffer(categoryId).append("/").append(forumId).append("/").append(topicId).toString();
    clearTopicCache(topicPath);
  }
  
  public void clearTopicCache(Topic topic) throws Exception {
    if (topic != null) {
      String topicPath = Utils.getSubPath(topic.getPath());
      topicData.remove(new TopicKey(topicPath, true));
      topicData.remove(new TopicKey(topicPath, false));
      topicData.remove(new TopicKey(topicPath.toUpperCase(), false));
      objectNameData.remove(new ObjectNameKey(topic.getId(), Utils.TOPIC));
    }
  }

  private void clearPostCache(String categoryId, String forumId, String topicId, String postId) throws Exception {
    postData.remove(new PostKey(categoryId, forumId, topicId, postId));
  }

  private void clearObjectCache(Forum forum, boolean isPutNewKey) throws Exception {
    if (forum != null) {
      ForumData forumData = new ForumData(forum);
      String forumId = forum.getId();
      if (isPutNewKey) {
        objectNameData.put(new ObjectNameKey(forumId, Utils.FORUM), forumData);
      } else {
        objectNameData.remove(new ObjectNameKey(forumId, Utils.FORUM));
      }
    }
  }
  
  private void clearObjectCache(Category category, boolean isNew) throws Exception {
    if (isNew) {
      CategoryData categoryData = new CategoryData(category);
      objectNameData.put(new ObjectNameKey(category.getId(), Utils.CATEGORY), categoryData);
    } else {
      objectNameData.remove(new ObjectNameKey(category.getId(), Utils.CATEGORY));
    }
  }
  
  private void clearObjectCache(String categoryId, String forumId, boolean isPutNewKey) throws Exception {
    clearObjectCache(getForum(categoryId, forumId), isPutNewKey);
  }
  
  private void clearUserProfile(String userName) {
    if (!Utils.isEmpty(userName)) {
      UserProfileKey key = new UserProfileKey(userName);
      userProfileData.remove(key);
      loginUserProfile.remove(key);
    } else {
      //clear all
      userProfileData.clearCache();
      loginUserProfile.clearCache();
    }
  }
  
  /**
   * Refresh user profile caching
   * @param profile
   * @throws Exception
   */
  public void refreshUserProfile(UserProfile profile) throws Exception {
    UserProfileKey key = new UserProfileKey(profile.getUserId());
    userProfileData.put(key, new UserProfileData(profile));
    loginUserProfile.put(key, new LoginUserProfileData(profile));
  }
  
  private void clearWatchingItemCache(String watchingItemPath) throws Exception {
    String categoryId = Utils.getCategoryId(watchingItemPath);
    String forumId = Utils.getForumId(watchingItemPath);
    String topicPath = Utils.getTopicPath(watchingItemPath);

    // Clear watching item data
    if (!Utils.isEmpty(topicPath)) {
      clearTopicCache(topicPath);
    } else if (!Utils.isEmpty(forumId)) {
      clearForumCache(categoryId, forumId, false);
    } else if (!Utils.isEmpty(categoryId)) {
      clearCategoryCache(categoryId);
    }
  }

  public void start() {

    //
    this.categoryData = CacheType.CATEGORY_DATA.getFromService(service);
    this.categoryList = CacheType.CATEGORY_LIST.getFromService(service);
    this.forumData = CacheType.FORUM_DATA.getFromService(service);
    this.forumList = CacheType.FORUM_LIST.getFromService(service);
    this.postData = CacheType.POST_DATA.getFromService(service);
    this.postList = CacheType.POST_LIST.getFromService(service);
    this.postListCount = CacheType.POST_LIST_COUNT.getFromService(service);

    this.topicData = CacheType.TOPIC_DATA.getFromService(service);
    this.topicList = CacheType.TOPIC_LIST.getFromService(service);
    this.topicListCount = CacheType.TOPIC_LIST_COUNT.getFromService(service);
    
    this.userProfileData = CacheType.USER_PROFILE_DATA.getFromService(service);
    this.userProfileList = CacheType.USER_PROFILE_LIST.getFromService(service);
    this.userProfileListCount = CacheType.USER_PROFILE_LIST_COUNT.getFromService(service);
    
    this.loginUserProfile = CacheType.LOGIN_USER_PROFILE.getFromService(service);

    this.objectNameData = CacheType.OBJECT_NAME_DATA.getFromService(service);
    this.miscData = CacheType.MISC_DATA.getFromService(service);
    this.watchListData = CacheType.WATCH_LIST_DATA.getFromService(service);
    this.linkListData = CacheType.LINK_LIST_DATA.getFromService(service);

    //
    this.categoryDataFuture = CacheType.CATEGORY_DATA.createFutureCache(categoryData);
    this.categoryListFuture = CacheType.CATEGORY_LIST.createFutureCache(categoryList);
    
    this.forumDataFuture = CacheType.FORUM_DATA.createFutureCache(forumData);
    this.forumListFuture = CacheType.FORUM_LIST.createFutureCache(forumList);
    
    this.postDataFuture = CacheType.POST_DATA.createFutureCache(postData);
    this.postListFuture = CacheType.POST_LIST.createFutureCache(postList);
    this.postListCountFuture = CacheType.POST_LIST_COUNT.createFutureCache(postListCount);
    
    this.topicDataFuture = CacheType.TOPIC_DATA.createFutureCache(topicData);
    this.topicListFuture = CacheType.TOPIC_LIST.createFutureCache(topicList);
    this.topicListCountFuture = CacheType.TOPIC_LIST_COUNT.createFutureCache(topicListCount);
    
    this.userProfileDataFuture = CacheType.USER_PROFILE_DATA.createFutureCache(userProfileData);
    this.userProfileListFuture = CacheType.USER_PROFILE_LIST.createFutureCache(userProfileList);
    this.userProfileListCountFuture = CacheType.USER_PROFILE_LIST_COUNT.createFutureCache(userProfileListCount);
    
    this.loginUserProfileFuture = CacheType.LOGIN_USER_PROFILE.createFutureCache(loginUserProfile);
    
    this.watchListDataFuture = CacheType.WATCH_LIST_DATA.createFutureCache(watchListData);
    this.linkListDataFuture = CacheType.LINK_LIST_DATA.createFutureCache(linkListData);
    this.miscDataFuture = CacheType.MISC_DATA.createFutureCache(miscData);
    
  }

  public void stop() {
  }

  private ListLinkData buildLinkInput(List<ForumLinkData> links) {
    List<LinkData> data = new ArrayList<LinkData>();
    for (ForumLinkData l : links) {
      data.add(new LinkData(l));
    }
    return new ListLinkData(data);
  }

  private List<ForumLinkData> buildLinkOutput(ListLinkData data) {

    if (data == null) {
      return null;
    }

    List<ForumLinkData> out = new ArrayList<ForumLinkData>();
    for (LinkData d : data.getIds()) {
      out.add(d.build());
    }
    return out;

  }
  
  private ListWatchData buildWatchInput(List<Watch> watches) {
    List<WatchData> data = new ArrayList<WatchData>();
    for (Watch w : watches) {
      data.add(new WatchData(w));
    }
    return new ListWatchData(data);
  }

  private List<Watch> buildWatchOutput(ListWatchData data) {

    if (data == null) {
      return null;
    }

    List<Watch> out = new ArrayList<Watch>();
    for (WatchData d : data.getIds()) {
      out.add(d.build());
    }
    return out;

  }

  private ListPostData buildPostInput(List<Post> posts) {
    List<PostKey> data = new ArrayList<PostKey>();
    PostKey key;
    for (Post p : posts) {
      key = new PostKey(p);
      data.add(key);
    }
    return new ListPostData(data);
  }

  private List<Post> buildPostOutput(ListPostData data) {

    if (data == null) {
      return null;
    }

    List<Post> out = new ArrayList<Post>();
    for (PostKey k : data.getIds()) {
      try {
        out.add(getPost(k.getCategory(), k.getForum(), k.getTopic(), k.getPost()));
      } catch (Exception e) {
        LOG.error(e);
      }
    }
    return out;

  }

  private ListForumData buildForumInput(List<Forum> forums) {
    List<ForumKey> keys = new ArrayList<ForumKey>();
    for (Forum f : forums) {
      keys.add(new ForumKey(f));
    }
    return new ListForumData(keys);
  }

  private List<Forum> buildForumOutput(ListForumData data) {

    if (data == null) {
      return null;
    }

    List<Forum> out = new ArrayList<Forum>();
    for (ForumKey k : data.getIds()) {
      out.add(getForum(k.getCategoryId(), k.getForumId()));
    }
    return out;

  }

  private ListCategoryData buildCategoryInput(List<Category> categories) {
    List<CategoryKey> keys = new ArrayList<CategoryKey>();
    for (Category c : categories) {
      keys.add(new CategoryKey(c));
    }
    return new ListCategoryData(keys);
  }

  private List<Category> buildCategoryOutput(ListCategoryData data) {

    if (data == null) {
      return null;
    }

    List<Category> out = new ArrayList<Category>();
    for (CategoryKey k : data.getIds()) {
      try {
        out.add(getCategory(k.getId()));
      } catch (Exception e) {
        LOG.error(e);
      }
    }
    return out;

  }

  @ManagedDescription("repository for forum storage")
  @Managed
  public String getRepository() throws Exception {
    return storage.getRepository();
  }

  @ManagedDescription("workspace for the forum storage")
  @Managed
  public String getWorkspace() throws Exception {
    return storage.getWorkspace();
  }

  @ManagedDescription("data path for forum storage")
  @Managed
  public String getPath() throws Exception {
    return storage.getPath();
  }

  public void addPlugin(ComponentPlugin plugin) throws Exception {
    storage.addPlugin(plugin);
  }

  public void addRolePlugin(ComponentPlugin plugin) throws Exception {
    storage.addRolePlugin(plugin);
  }

  public void addInitialDataPlugin(ComponentPlugin plugin) throws Exception {
    storage.addInitialDataPlugin(plugin);
  }

  public void addInitialDefaultDataPlugin(ComponentPlugin plugin) throws Exception {
    storage.addInitialDefaultDataPlugin(plugin);
  }

  public void addDeletedUserCalculateListener() throws Exception {
    storage.addDeletedUserCalculateListener();
  }

  public void initCategoryListener() {
    storage.initCategoryListener();
  }

  public boolean isAdminRole(String userName) throws Exception {
    return storage.isAdminRole(userName);
  }
  
  public boolean isAdminRoleConfig(String userName) throws Exception {
    return storage.isAdminRoleConfig(userName);
  }

  public void setDefaultAvatar(String userName) {
    storage.setDefaultAvatar(userName);
    miscData.remove(new SimpleCacheKey(USER_AVATAR_KEY, userName));
  }

  public ForumAttachment getUserAvatar(final String userName) throws Exception {
    SimpleCacheKey key = new SimpleCacheKey(USER_AVATAR_KEY, userName);
    //
    return (ForumAttachment) miscDataFuture.get(new ServiceContext<SimpleCacheData>() {
      public SimpleCacheData<ForumAttachment> execute() {
        try {
          ForumAttachment got = storage.getUserAvatar(userName);
          return new SimpleCacheData<ForumAttachment>(got);
        } catch (Exception e) {
          return new SimpleCacheData<ForumAttachment>(null);
        }
      }
    }, key).build();
  }

  public void saveUserAvatar(String userId, ForumAttachment fileAttachment) throws Exception {
    storage.saveUserAvatar(userId, fileAttachment);
    miscData.remove(new SimpleCacheKey(USER_AVATAR_KEY, userId));
  }

  public void saveForumAdministration(ForumAdministration forumAdministration) throws Exception {
    storage.saveForumAdministration(forumAdministration);
  }

  public ForumAdministration getForumAdministration() throws Exception {
    return storage.getForumAdministration();
  }

  public SortSettings getForumSortSettings() {
    return storage.getForumSortSettings();
  }

  public SortSettings getTopicSortSettings() throws Exception {
    return storage.getTopicSortSettings();
  }

  public List<Category> getCategories() {

    return buildCategoryOutput(
      categoryListFuture.get(
        new ServiceContext<ListCategoryData>() {
          public ListCategoryData execute() {
            return buildCategoryInput(storage.getCategories());
          }
        },
        new CategoryListKey(null)
      )
    );
    
  }

  public Category getCategory(final String categoryId) {

    return categoryDataFuture.get(
      new ServiceContext<CategoryData>() {
        public CategoryData execute() {
          try {
            Category got = storage.getCategory(categoryId);
            if (got != null) {
              return new CategoryData(got);
            }
            else {
              return CategoryData.NULL;
            }
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        }
      },
      new CategoryKey(categoryId)
    ).build();

  }
  
  public Category getCategoryIncludedSpace() {
    return storage.getCategoryIncludedSpace();
  }

  public String[] getPermissionTopicByCategory(String categoryId, String type) throws Exception {
    return storage.getPermissionTopicByCategory(categoryId, type);
  }

  public void saveCategory(Category category, boolean isNew) throws Exception {
    storage.saveCategory(category, isNew);
    categoryList.select(new ScopeCacheSelector<CategoryListKey, ListCategoryData>());
    clearLinkListCache();
    clearObjectCache(category, isNew);
    //
    clearUserProfile(null);
    clearCategoryCache(category);
  }

  public void saveModOfCategory(List<String> moderatorCate, String userId, boolean isAdd) {
    storage.saveModOfCategory(moderatorCate, userId, isAdd);
    try {
      categoryData.select(new CategoryIdSelector(moderatorCate, categoryData));
    } catch (Exception e) {
      LOG.debug("Can not clear list categories in cached.", e);
    }
    //
    clearUserProfile(null);
  }

  public void calculateModerator(String nodePath, boolean isNew) throws Exception {
    storage.calculateModerator(nodePath, isNew);
    clearForumCache(Utils.getCategoryId(nodePath), Utils.getForumId(nodePath), false);
    clearForumListCache();
    //
    clearUserProfile(null);
  }

  public Category removeCategory(String categoryId) throws Exception {
    objectNameData.clearCache();
    categoryData.remove(new CategoryKey(categoryId));
    categoryList.select(new ScopeCacheSelector<CategoryListKey, ListCategoryData>());
    clearLinkListCache();
    //
    clearUserProfile(null);
    return storage.removeCategory(categoryId);  
  }

  @Deprecated
  public List<Forum> getForums(final String categoryId, final String strQuery) throws Exception {
    return getForums(new ForumFilter(categoryId, false).strQuery(strQuery));
  }

  @Deprecated
  public List<Forum> getForumSummaries(final String categoryId, final String strQuery) throws Exception {
    return getForums(new ForumFilter(categoryId, true).strQuery(strQuery));
  }

  public List<Forum> getForums(final ForumFilter filter) {
    SortSettings sort = storage.getForumSortSettings();
    SortField orderBy = sort.getField();
    Direction orderType = sort.getDirection();

    return buildForumOutput(
      forumListFuture.get(
        new ServiceContext<ListForumData>() {
          public ListForumData execute() {
            try {
              return buildForumInput(storage.getForums(filter));
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }
        },
        new ForumListKey(filter, orderBy, orderType)
      )
    );
  }

  public List<CategoryFilter> filterForumByName(String filterKey, String userName, int maxSize) throws Exception {
    return storage.filterForumByName(filterKey, userName, maxSize);
  }

  public Forum getForum(final String categoryId, final String forumId) {

    ForumData data = forumDataFuture.get(
        new ServiceContext<ForumData>() {
          public ForumData execute() {
            Forum got = storage.getForum(categoryId, forumId);
            if (got != null) {
              return new ForumData(got);
            }
            else {
              return null;
            }
          }
        },
        new ForumKey(categoryId, forumId)
    );
    
    return data != null ? data.build() : null;
  }

  public void modifyForum(Forum forum, int type) throws Exception {
    storage.modifyForum(forum, type);
    clearForumCache(forum, true);
    clearForumListCache();
    clearObjectCache(forum, true);
    //
    clearMiscDataCache(FORUM_CAN_VIEW_KEY);
  }

  public void saveForum(String categoryId, Forum forum, boolean isNew) throws Exception {
    storage.saveForum(categoryId, forum, isNew);
    //
    clearForumCache(forum, true);
    clearForumListCache();
    clearLinkListCache();
    clearObjectCache(forum, true);
    clearCategoryCache(categoryId);
    //
    clearTopicListCache(forum.getId());
    clearTopicListCountCache(forum.getId());
    //
    clearMiscDataCache(FORUM_CAN_VIEW_KEY);
    //
    clearUserProfile(null);
  }

  public void saveModerateOfForums(List<String> forumPaths, String userName, boolean isDelete) throws Exception {
    storage.saveModerateOfForums(forumPaths, userName, isDelete);
    forumData.select(new ForumPathSelector(forumPaths.toArray(new String[forumPaths.size()]), forumData));
    clearForumListCache();
    for (String forumPath : forumPaths) {
      clearObjectCache(Utils.getCategoryId(forumPath), Utils.getForumId(forumPath), true);
    }
    //
    clearMiscDataCache(FORUM_CAN_VIEW_KEY);
    //
    clearUserProfile(null);
  }

  public Forum removeForum(String categoryId, String forumId) throws Exception {
    clearForumCache(categoryId, forumId, false);
    clearForumListCache();
    clearLinkListCache();
    clearObjectCache(categoryId, forumId, false);
    //
    clearMiscDataCache(FORUM_CAN_VIEW_KEY);
    return storage.removeForum(categoryId, forumId);
  }

  public void moveForum(List<Forum> forums, String destCategoryPath) throws Exception {
    for (Forum forum : forums) {
      clearForumCache(forum, false);
      clearObjectCache(forum, false);
    }
    clearForumListCache();
    clearLinkListCache();
    //
    clearMiscDataCache(FORUM_CAN_VIEW_KEY);
    //
    clearUserProfile(null);
    storage.moveForum(forums, destCategoryPath);
    //
    topicData.clearCache();
    postData.clearCache();
    objectNameData.clearCache();
    watchListData.clearCache();
    forumData.clearCache();
    clearForumListCache();
  }

  public JCRPageList getPageTopic(String categoryId, String forumId, String strQuery, String strOrderBy) throws Exception {
    return storage.getPageTopic(categoryId, forumId, strQuery, strOrderBy);
  }

  public LazyPageList<Topic> getTopicList(String categoryId, String forumId, String xpathConditions, String strOrderBy, int pageSize) throws Exception {
    return storage.getTopicList(categoryId, forumId, xpathConditions, strOrderBy, pageSize);
  }

  @Override
  public List<Topic> getTopics(final TopicFilter filter, final int offset, final int limit) throws Exception {

    TopicListKey key = new TopicListKey(filter, offset, limit);

    ListTopicData data = topicListFuture.get(new ServiceContext<ListTopicData>() {
      @Override
      public ListTopicData execute() {
        try {
          return buildTopicInput(storage.getTopics(filter, offset, limit));
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    }, key);

    return buildTopicOutput(data);
  }
  
  private ListTopicData buildTopicInput(List<Topic> topics) {
    List<TopicKey> data = new ArrayList<TopicKey>();
    TopicKey key;
    for (Topic p : topics) {
      key = new TopicKey(p);
      data.add(key);
      topicData.put(key, new TopicData(p));
    }
    return new ListTopicData(data);
  }

  private List<Topic> buildTopicOutput(ListTopicData data) {

    if (data == null) {
      return null;
    }

    List<Topic> out = new ArrayList<Topic>();
    for (TopicKey k : data.getIds()) {
      try {
        out.add(getTopicByPath(k.getTopicPath(), false));
      } catch (Exception e) {
        LOG.error(e);
      }
    }
    return out;
  }

  @Override
  public int getTopicsCount(final TopicFilter filter) throws Exception {

    TopicListCountKey key = new TopicListCountKey(filter.toString(), filter.forumId());

    SimpleCacheData<Integer> data = topicListCountFuture.get(new ServiceContext<SimpleCacheData<Integer>>() {
      @Override
      public SimpleCacheData<Integer> execute() {
        try {
          return new SimpleCacheData<Integer>(storage.getTopicsCount(filter));
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }

    }, key);

    return data.build();
  }

  public List<Topic> getTopics(final String categoryId, final String forumId) throws Exception {
    TopicFilter filter = new TopicFilter(categoryId, forumId);
    filter.isAdmin(true);
    //
    TopicListKey key = new TopicListKey(filter, 0, 0);

    ListTopicData data = topicListFuture.get(new ServiceContext<ListTopicData>() {
      @Override
      public ListTopicData execute() {
        try {
          return buildTopicInput(storage.getTopics(categoryId, forumId));
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    }, key);

    return buildTopicOutput(data);
  }

  public Topic getTopic(String categoryId, String forumId, String topicId, String userRead) throws Exception {
    String topicPath = topicId;
    if (!Utils.isEmpty(categoryId)) {
      topicPath = new StringBuilder(categoryId).append("/").append(forumId).append("/").append(topicId).toString();
    }
    //
    return getTopicByPath(topicPath, false);
  }

  public Topic getTopicSummary(final String topicPath) {
    TopicData data = topicData.get(new TopicKey(topicPath, false));
    if (data != null) {
      return getTopicPoll(data.build());
    }
    //
    Topic got = topicDataFuture.get(
        new ServiceContext<TopicData>() {
          public TopicData execute() {
            try {
              Topic got = storage.getTopicSummary(topicPath);
              if (got != null) {
                return new TopicData(got);
              } else {
                return TopicData.NULL;
              }
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }
        },
        new TopicKey(Utils.getSubPath(topicPath.toUpperCase()), false)
    ).build();
    //
    return getTopicPoll(got);
  }

  public Topic getTopicSummary(String topicPath, boolean isLastPost) throws Exception {
    if(isLastPost == false) {
      return getTopicSummary(topicPath);
    }
    return storage.getTopicSummary(topicPath, isLastPost);
  }

  public Topic getTopicByPath(final String topicPath, final boolean isLastPost) throws Exception {
    Topic got = topicDataFuture.get(
        new ServiceContext<TopicData>() {
          public TopicData execute() {
            try {
              Topic got = storage.getTopicByPath(topicPath, isLastPost);
              if (got != null) {
                return new TopicData(got);
              } else {
                return TopicData.NULL;
              }
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }
        },
        new TopicKey(topicPath, isLastPost)
    ).build();
    //
    return getTopicPoll(got);
  }

  public Topic getTopicUpdate(Topic topic, boolean isSummary) throws Exception {
    return storage.getTopicUpdate(topic, isSummary);
  }

  @Override
  public List<Topic> getTopicsByDate(long date, String forumPath, int offset, int limit) throws Exception {
    return storage.getTopicsByDate(date, forumPath, offset, limit);
  }

  public JCRPageList getPageTopicOld(long date, String forumPatch) throws Exception {
    return storage.getPageTopicOld(date, forumPatch);
  }

  public List<Topic> getAllTopicsOld(long date, String forumPatch) throws Exception {
    return storage.getAllTopicsOld(date, forumPatch);
  }

  public long getTotalTopicOld(long date, String forumPatch) {
    return storage.getTotalTopicOld(date, forumPatch);
  }

  public JCRPageList getPageTopicByUser(String userName, boolean isMod, String strOrderBy) throws Exception {
    return storage.getPageTopicByUser(userName, isMod, strOrderBy);
  }
  
  public  List<Topic> getTopicsByUser(final TopicFilter filter, final int offset, final int limit) throws Exception {
    
    TopicListKey key = new TopicListKey(filter, 0, 0);

    ListTopicData data = topicListFuture.get(new ServiceContext<ListTopicData>() {
      @Override
      public ListTopicData execute() {
        try {
          return buildTopicInput(storage.getTopicsByUser(filter, offset, limit));
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    }, key);

    return buildTopicOutput(data);
  }

  public void modifyTopic(List<Topic> topics, int type) {
    storage.modifyTopic(topics, type);

    //
    try {
      clearTopicsCache(topics);
      clearTopicListCache(topics.get(0).getForumId());
      clearTopicListCountCache(topics.get(0).getForumId());
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
    }
    
    //
    if(type == Utils.CLOSE || type == Utils.ACTIVE || type == Utils.WAITING || type == Utils.APPROVE) {
      for(Topic topic : topics) {
        try {
          clearForumCache(topic.getCategoryId(), topic.getForumId(), false);
        } catch (Exception e) {
          LOG.error(e.getMessage(), e);
        }
      }
    }
  }

  public void saveTopic(String categoryId, String forumId, Topic topic, boolean isNew, boolean isMove, MessageBuilder messageBuilder) throws Exception {
    storage.saveTopic(categoryId, forumId, topic, isNew, isMove, messageBuilder);
    clearForumCache(categoryId, forumId, false);
    clearForumListCache();
    clearTopicListCache(forumId);

    if(!isNew) {
      clearPostCache(categoryId, forumId, topic.getId(), topic.getId().replace(Utils.TOPIC, Utils.POST)); 
      clearTopicCache(topic);
    } else {
      clearTopicListCountCache(forumId);
      clearUserProfile(topic.getOwner());
    }
  }

  public Topic removeTopic(String categoryId, String forumId, String topicId) {
    try {
      clearForumCache(categoryId, forumId, false);
      clearForumListCache();
      clearTopicCache(categoryId, forumId, topicId);
      //
      clearTopicListCountCache(forumId);
      clearTopicListCache(forumId);
      clearUserProfile(null);
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
    }
    return storage.removeTopic(categoryId, forumId, topicId);
  }

  public void moveTopic(List<Topic> topics, String destForumPath, String mailContent, String link) throws Exception {
    storage.moveTopic(topics, destForumPath, mailContent, link);
    if (topics != null && topics.size() > 0) {
      forumData.select(new ForumPathSelector(new String[] {Utils.getForumPath(topics.get(0).getPath()), destForumPath}, forumData));
      clearForumListCache();
      clearTopicListCache();
      //
      clearTopicListCountCache(topics.get(0).getForumId());
      clearTopicListCountCache(Utils.getForumId(destForumPath));
      for (Topic topic : topics) {
        clearTopicCache(topic);
      }
      //
      clearUserProfile(null);
    }
  }

  public long getLastReadIndex(String path, String isApproved, String isHidden, String userLogin) throws Exception {
    return storage.getLastReadIndex(path, isApproved, isHidden, userLogin);
  }

  public JCRPageList getPosts(String categoryId, String forumId, String topicId, String isApproved, String isHidden, String strQuery, String userLogin) throws Exception {
    return storage.getPosts(categoryId, forumId, topicId, isApproved, isHidden, strQuery, userLogin);
  }

  public long getAvailablePost(String categoryId, String forumId, String topicId, String isApproved, String isHidden, String userLogin) throws Exception {
    PostFilter filter = new PostFilter(categoryId, forumId, topicId, isApproved, isHidden, isHidden, userLogin);
    return Long.valueOf(storage.getPostsCount(filter));
  }

  public JCRPageList getPagePostByUser(String userName, String userId, boolean isMod, String strOrderBy) throws Exception {
    return storage.getPagePostByUser(userName, userId, isMod, strOrderBy);
  }

  public Post getPost(final String categoryId, final String forumId, final String topicId, final String postId) throws Exception {

    PostKey key = new PostKey(categoryId, forumId, topicId, postId);

    return postDataFuture.get(
        new ServiceContext<PostData>() {

          public PostData execute() {
            try {
              Post got = storage.getPost(categoryId, forumId, topicId, postId);
              if (got == null) {
                return PostData.NULL;
              } else {
                return new PostData(got);
              }
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }
        },
        key
    ).build();

  }
  
  public void putPost(Post post) {
    PostKey key = new PostKey(post.getCategoryId(), post.getForumId(), post.getTopicId(), post.getId());
    postData.put(key, new PostData(post));
  }

  public Post getPostFromCache(final String categoryId, final String forumId, final String topicId, final String postId) {
    PostKey key = new PostKey(categoryId, forumId, topicId, postId);
    PostData postCache = postData.get(key);
    if (postCache != null && postCache != PostData.NULL) {
      return postCache.build();
    }
    return null;
  }

  public JCRPageList getListPostsByIP(String ip, String strOrderBy) throws Exception {
    return storage.getListPostsByIP(ip, strOrderBy);
  }

  public void savePost(String categoryId, String forumId, String topicId, Post post, boolean isNew, MessageBuilder messageBuilder) throws Exception {
    storage.savePost(categoryId, forumId, topicId, post, isNew, messageBuilder);
    clearForumCache(categoryId, forumId, false);
    clearForumListCache();
    clearTopicCache(categoryId, forumId, topicId);
    clearTopicListCache(forumId);
    clearPostListCache();
    //
    if (isNew == false) {
      clearPostCache(categoryId, forumId, topicId, post.getId());
    } else {
      clearUserProfile(post.getOwner());
      clearPostListCountCache(topicId);
    }
    statistic = null;
  }

  public void modifyPost(List<Post> posts, int type) {
    storage.modifyPost(posts, type);
    // clear caching
    try {
      clearPostListCache();
      //
      Post p = posts.get(0);
      String categoryId = p.getCategoryId();
      String forumId = p.getForumId();
      String topicId = p.getTopicId();
      for (Post post : posts) {
        clearPostListCountCache(post.getTopicId());
        clearPostCache(categoryId, forumId, topicId, post.getId());
      }
      //
      clearTopicCache(Utils.getTopicPath(p.getPath()));
      clearTopicListCache(forumId);
      clearTopicListCountCache(forumId);
      clearForumCache(categoryId, forumId, false);
      clearForumListCache();
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
    }
  }

  public Post removePost(String categoryId, String forumId, String topicId, String postId) {
    try {
      Post post = getPost(categoryId, forumId, topicId, postId);
      clearUserProfile(post.getOwner());
      clearForumCache(categoryId, forumId, false);
      clearForumListCache();
      clearTopicCache(categoryId, forumId, topicId);
      clearPostCache(categoryId, forumId, topicId, postId);
      clearPostListCache();
      clearPostListCountCache(topicId);
      //
      statistic = null;
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
    }
    return storage.removePost(categoryId, forumId, topicId, postId);
  }

  public void addTag(List<Tag> tags, String userName, String topicPath) throws Exception {
    storage.addTag(tags, userName, topicPath);
    clearTopicCache(topicPath);
  }

  public void unTag(String tagId, String userName, String topicPath) {
    storage.unTag(tagId, userName, topicPath);
    clearTopicCache(topicPath);
  }

  public Tag getTag(String tagId) throws Exception {
    return storage.getTag(tagId);
  }

  public List<String> getTagNameInTopic(String userAndTopicId) throws Exception {
    return storage.getTagNameInTopic(userAndTopicId);
  }

  public List<String> getAllTagName(String keyValue, String userAndTopicId) throws Exception {
    return storage.getAllTagName(keyValue, userAndTopicId);
  }

  public List<Tag> getAllTags() throws Exception {
    return storage.getAllTags();
  }

  public List<Tag> getMyTagInTopic(String[] tagIds) throws Exception {
    return storage.getMyTagInTopic(tagIds);
  }

  public JCRPageList getTopicByMyTag(String userIdAndtagId, String strOrderBy) throws Exception {
    return storage.getTopicByMyTag(userIdAndtagId, strOrderBy);
  }

  public void saveTag(Tag newTag) throws Exception {
    storage.saveTag(newTag);
  }

  public JCRPageList getPageListUserProfile() throws Exception {
    return storage.getPageListUserProfile();
  }

  public JCRPageList searchUserProfile(String userSearch) throws Exception {
    return storage.searchUserProfile(userSearch);
  }

  public UserProfile getDefaultUserProfile(final String userName, final String ip) throws Exception {
    UserProfileKey key = new UserProfileKey(userName);
    return loginUserProfileFuture.get(
        new ServiceContext<LoginUserProfileData>() {
          public LoginUserProfileData execute() {
            try {
              UserProfile got = storage.getDefaultUserProfile(userName, ip);
              return new LoginUserProfileData(got);
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }
        },
        key
    ).build();
  }

  public UserProfile updateUserProfileSetting(UserProfile userProfile) throws Exception {
    clearUserProfile(userProfile.getUserId());
    return storage.updateUserProfileSetting(userProfile);
  }

  public String getScreenName(final String userName) throws Exception {

    SimpleCacheKey key = new SimpleCacheKey(SCREEN_NAME_KEY, userName);

    return (String) miscDataFuture.get(
        new ServiceContext<SimpleCacheData>() {
          public SimpleCacheData<Comparable> execute() {
            try {
              String got = storage.getScreenName(userName);
              return new SimpleCacheData<Comparable>(got);
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }
        },
        key
    ).build();

  }

  public UserProfile getUserSettingProfile(String userName) throws Exception {
    return storage.getUserSettingProfile(userName);
  }

  public void saveUserSettingProfile(UserProfile userProfile) throws Exception {
    storage.saveUserSettingProfile(userProfile);
    miscData.remove(new SimpleCacheKey(SCREEN_NAME_KEY, userProfile.getUserId()));
    clearUserProfile(userProfile.getUserId());
  }

  public UserProfile getLastPostIdRead(UserProfile userProfile, String isOfForum) throws Exception {
    return storage.getLastPostIdRead(userProfile, isOfForum);
  }

  public void saveLastPostIdRead(String userId, String[] lastReadPostOfForum, String[] lastReadPostOfTopic) throws Exception {
    //
    completionService.addTask(new SaveLastPostIdRead(userId, lastReadPostOfForum, lastReadPostOfTopic));
  }

  class SaveLastPostIdRead implements Callable<Boolean> {
    private String userId;
    private String[] lastReadPostOfForum, lastReadPostOfTopic;

    public SaveLastPostIdRead(String userId, String[] lastReadPostOfForum, String[] lastReadPostOfTopic) {
      this.userId = userId;
      this.lastReadPostOfTopic = lastReadPostOfTopic;
      this.lastReadPostOfForum = lastReadPostOfForum;
    }

    @Override
    public Boolean call() throws Exception {
      storage.saveLastPostIdRead(userId, lastReadPostOfForum, lastReadPostOfTopic);
      return true;
    }
  }
  
  public List<String> getUserModerator(String userName, boolean isModeCate) throws Exception {
    return storage.getUserModerator(userName, isModeCate);
  }

  public void saveUserModerator(String userName, List<String> ids, boolean isModeCate) throws Exception {
    storage.saveUserModerator(userName, ids, isModeCate);
  }

  public UserProfile getUserInfo(String userName) throws Exception {
    return storage.getUserInfo(userName);
  }

  public List<UserProfile> getQuickProfiles(List<String> userList) throws Exception {
    return storage.getQuickProfiles(userList);
  }

  public UserProfile getQuickProfile(final String userName) throws Exception {
    
    UserProfileKey key = new UserProfileKey(userName);

    return userProfileDataFuture.get(
        new ServiceContext<UserProfileData>() {
          public UserProfileData execute() {
            try {
              UserProfile got = storage.getQuickProfile(userName);
              return new UserProfileData(got);
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }
        },
        key
    ).build();
    
  }

  public UserProfile getUserInformations(UserProfile userProfile) throws Exception {
    return storage.getUserInformations(userProfile);
  }

  public void saveUserProfile(UserProfile newUserProfile, boolean isOption, boolean isBan) throws Exception {
    clearUserProfile(newUserProfile.getUserId());
    //
    clearUserProfileListCache();
    clearUserProfileListCountCache();
    
    storage.saveUserProfile(newUserProfile, isOption, isBan);
  }

  public UserProfile getUserProfileManagement(String userName) throws Exception {
    return storage.getUserProfileManagement(userName);
  }

  public void saveUserBookmark(String userName, String bookMark, boolean isNew) throws Exception {
    storage.saveUserBookmark(userName, bookMark, isNew);
    clearUserProfile(userName);
  }

  public void saveCollapsedCategories(String userName, String categoryId, boolean isAdd) throws Exception {
    storage.saveCollapsedCategories(userName, categoryId, isAdd);
  }

  public void saveReadMessage(String messageId, String userName, String type) throws Exception {
    storage.saveReadMessage(messageId, userName, type);
    miscData.remove(new SimpleCacheKey(PRIVATE_MESSAGE_COUNT_KEY, userName));
  }

  public JCRPageList getPrivateMessage(String userName, String type) throws Exception {
    return storage.getPrivateMessage(userName, type);
  }

  public long getNewPrivateMessage(final String userName) throws Exception {

    SimpleCacheKey key = new SimpleCacheKey(PRIVATE_MESSAGE_COUNT_KEY, userName);

    return (Long) miscDataFuture.get(
        new ServiceContext<SimpleCacheData>() {
          public SimpleCacheData<Comparable> execute() {
            try {
              Long got = storage.getNewPrivateMessage(userName);
              return new SimpleCacheData<Comparable>(got);
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }
        },
        key
    ).build();

  }

  public void savePrivateMessage(ForumPrivateMessage privateMessage) throws Exception {
    storage.savePrivateMessage(privateMessage);
    miscData.remove(new SimpleCacheKey(PRIVATE_MESSAGE_COUNT_KEY, privateMessage.getSendTo()));
  }

  public void removePrivateMessage(String messageId, String userName, String type) throws Exception {
    storage.removePrivateMessage(messageId, userName, type);
    miscData.remove(new SimpleCacheKey(PRIVATE_MESSAGE_COUNT_KEY, userName));
  }

  public ForumSubscription getForumSubscription(String userId) {
    return storage.getForumSubscription(userId);
  }

  public void saveForumSubscription(ForumSubscription forumSubscription, String userId) throws Exception {
    storage.saveForumSubscription(forumSubscription, userId);
  }

  public ForumStatistic getForumStatistic() throws Exception {
    if (statistic != null) {
      return statistic;
    } else {
      return statistic = storage.getForumStatistic();
    }
  }

  public void saveForumStatistic(ForumStatistic forumStatistic) throws Exception {
    storage.saveForumStatistic(forumStatistic);
  }

  public Object getObjectNameByPath(final String path) throws Exception {

    String type = Utils.getObjectType(path);
    String id = Utils.getIdByType(path, type);

    ObjectNameKey key = new ObjectNameKey(id, type);
    CachedData<?> data = objectNameData.get(key);
    //
    if (data == null) {
      Object got = storage.getObjectNameByPath(path);
      return getObjectNameByKey(got, key);
    } else {
      // check path
      Object got = data.build();
      String sPath = getPath(got);
      if (path.indexOf(sPath) < 0) {
        return null;
      }
      if (got instanceof Topic) {
        return getTopicPoll((Topic) got);
      }
      return got;
    }
  }

  private Object getObjectNameByKey(Object got, ObjectNameKey key) throws Exception {
    if (got instanceof Post) {
      objectNameData.put(key, new PostData((Post) got));
    } else if (got instanceof Topic) {
      objectNameData.put(key, new TopicData((Topic) got));
    } else if (got instanceof Forum) {
      objectNameData.put(key, new ForumData((Forum) got));
    } else if (got instanceof Category) {
      objectNameData.put(key, new CategoryData((Category) got));
    } else if (got instanceof Tag) {
      objectNameData.put(key, new TagData((Tag) got));
    }
    return got;
  }
  
  private String getPath(Object got) {
    String path = null;
    if (got instanceof Post) {
      path = ((Post) got).getPath();
    } else if (got instanceof Topic) {
      path = ((Topic) got).getPath();
    } else if (got instanceof Forum) {
      path = ((Forum) got).getPath();
    } else if (got instanceof Category) {
      path = ((Category) got).getPath();
    } else if (got instanceof Tag) {
      path = ((Tag) got).getId();
    }

    return Utils.getSubPath(path);
  }

  public Object getObjectNameById(String id, String type) throws Exception {

    ObjectNameKey key = new ObjectNameKey(id, type);
    CachedData<?> data = objectNameData.get(key);
    //
    if (data == null) {
      Object got = storage.getObjectNameById(id, type);
      return getObjectNameByKey(got, key);
    } else {
      Object object = data.build();
      if (object instanceof Topic) {
        return getTopicPoll((Topic) object);
      }
      return object;
    }

  }
  
  private Topic getTopicPoll(Topic topic) {
    if(topic != null) {
      topic.setIsPoll(topicHasPoll(topic.getPath()));
    }
    return topic;
  }

  // TODO : need range
  public List<ForumLinkData> getAllLink(final String strQueryCate, final String strQueryForum) throws Exception {

    LinkListKey key = new LinkListKey(strQueryCate, strQueryForum);

    return buildLinkOutput(linkListDataFuture.get(
        new ServiceContext<ListLinkData>() {
          public ListLinkData execute() {
            try {
              List<ForumLinkData> got = storage.getAllLink(strQueryCate, strQueryForum);
              return buildLinkInput(got);
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }
        },
        key
    ));

  }

  public List<ForumSearchResult> getQuickSearch(String textQuery, String type_, String pathQuery, String userId, List<String> listCateIds, List<String> listForumIds, List<String> forumIdsOfModerator) throws Exception {
    return storage.getQuickSearch(textQuery, type_, pathQuery, userId, listCateIds, listForumIds, forumIdsOfModerator);
  }

  public List<ForumSearchResult> getAdvancedSearch(ForumEventQuery eventQuery, List<String> listCateIds, List<String> listForumIds) {
    return storage.getAdvancedSearch(eventQuery, listCateIds, listForumIds);
  }

  public void addWatch(int watchType, String path, List<String> values, String currentUser) throws Exception {
    storage.addWatch(watchType, path, values, currentUser);
    watchListData.remove(new SimpleCacheKey(WATCH_TYPE, currentUser));
    clearWatchingItemCache(path);
  }

  public void removeWatch(int watchType, String path, String values) throws Exception {
    storage.removeWatch(watchType, path, values);
    watchListData.select(new ScopeCacheSelector<SimpleCacheKey, ListWatchData>());
    clearWatchingItemCache(path);
  }

  public void updateEmailWatch(List<String> listNodeId, String newEmailAdd, String userId) throws Exception {
    storage.updateEmailWatch(listNodeId, newEmailAdd, userId);
    watchListData.remove(new SimpleCacheKey(WATCH_TYPE, userId));
    for (String id : listNodeId) {
      if (id.contains(Utils.CATEGORY)) {
        categoryData.remove(new CategoryKey(id));
      } else if (id.contains(Utils.FORUM)) {
        forumData.remove(new ForumKey((Forum) getObjectNameById(id, Utils.FORUM)));
      } else if (id.contains(Utils.TOPIC)) {
        Topic topic = (Topic) getObjectNameById(id, Utils.TOPIC);
        clearTopicCache(topic);
      }
    }
  }

  // TODO : need range
  public List<Watch> getWatchByUser(final String userId) throws Exception {

    SimpleCacheKey key = new SimpleCacheKey(WATCH_TYPE, userId);

    return buildWatchOutput(watchListDataFuture.get(
      new ServiceContext<ListWatchData>() {
        public ListWatchData execute() {
          try {
            List<Watch> got = storage.getWatchByUser(userId);
            return buildWatchInput(got);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        }
      },
      key
    ));

  }

  public void updateForum(String path) throws Exception {
    storage.updateForum(path);
  }

  public SendMessageInfo getMessageInfo(String name) throws Exception {
    return storage.getMessageInfo(name);
  }

  public Iterator<SendMessageInfo> getPendingMessages() throws Exception {
    return storage.getPendingMessages();
  }

  public List<ForumSearchResult> getJobWattingForModerator(String[] paths) {
    return storage.getJobWattingForModerator(paths);
  }

  public int getJobWattingForModeratorByUser(String userId) throws Exception {
    return storage.getJobWattingForModeratorByUser(userId);
  }

  public NodeIterator search(String queryString) throws Exception {
    return storage.search(queryString);
  }

  public void evaluateActiveUsers(String query) {
    storage.evaluateActiveUsers(query);
  }

  public Object exportXML(String categoryId, String forumId, List<String> objectIds, String nodePath, ByteArrayOutputStream bos, boolean isExportAll) throws Exception {
    return storage.exportXML(categoryId, forumId, objectIds, nodePath, bos, isExportAll);
  }

  public void importXML(String nodePath, ByteArrayInputStream bis, int typeImport) throws Exception {
    storage.importXML(nodePath, bis, typeImport);
    categoryList.select(new ScopeCacheSelector<CategoryListKey, ListCategoryData>());
    forumList.select(new ScopeCacheSelector());
    forumData.select(new ScopeCacheSelector());
  }

  public void updateTopicAccess(String userId, String topicId) {
    storage.updateTopicAccess(userId, topicId);
    try {
      UserProfile userProfile = getDefaultUserProfile(userId, null);
      userProfile.setLastTimeAccessTopic(topicId, CommonUtils.getGreenwichMeanTime().getTimeInMillis());
      refreshUserProfile(userProfile);
    } catch (Exception e) {
      LOG.warn("Can't update last access for user profile '" + userId + "' on topic '" + topicId + "'", e);
    }
  }

  public void updateForumAccess(String userId, String forumId) {
    storage.updateForumAccess(userId, forumId);
    try {
      UserProfile userProfile = getDefaultUserProfile(userId, null);
      userProfile.setLastTimeAccessForum(forumId, CommonUtils.getGreenwichMeanTime().getTimeInMillis());
      refreshUserProfile(userProfile);
    } catch (Exception e) {
      LOG.warn("Can't update last access for user profile '" + userId + "' on forum '" + forumId + "'", e);
    }
  }

  public void writeReads() {
    storage.writeReads();
  }

  public List<String> getBookmarks(String userName) throws Exception {
    return storage.getBookmarks(userName);
  }

  public List<String> getBanList() throws Exception {
    return storage.getBanList();
  }

  public boolean isBanIp(String ip) throws Exception {
    return storage.isBanIp(ip);
  }

  public boolean addBanIP(String ip) throws Exception {
    return storage.addBanIP(ip);
  }

  public void removeBan(String ip) throws Exception {
    storage.removeBan(ip);
  }

  public List<String> getForumBanList(String forumId) throws Exception {
    return storage.getForumBanList(forumId);
  }

  public boolean addBanIPForum(String ip, String forumId) throws Exception {
    Forum forum = getForum(forumId.split("/")[0], forumId.split("/")[1]);
    clearForumCache(forum, false);
    clearForumListCache();
    clearLinkListCache();
    clearObjectCache(forum, false);
    return storage.addBanIPForum(ip, forumId);
  }

  public void removeBanIPForum(String ip, String forumId) throws Exception {
    Forum forum = getForum(forumId.split("/")[0], forumId.split("/")[1]);
    clearForumCache(forum, false);
    clearForumListCache();
    clearLinkListCache();
    clearObjectCache(forum, false);
    storage.removeBanIPForum(ip, forumId);
  }

  public void updateStatisticCounts(long topicCount, long postCount) throws Exception {
    storage.updateStatisticCounts(topicCount, postCount);
  }

  public PruneSetting getPruneSetting(String forumPath) throws Exception {
    return storage.getPruneSetting(forumPath);
  }

  public List<PruneSetting> getAllPruneSetting() throws Exception {
    return storage.getAllPruneSetting();
  }

  public void savePruneSetting(PruneSetting pruneSetting) throws Exception {
    storage.savePruneSetting(pruneSetting);
  }

  public void runPrune(String forumPath) throws Exception {
    storage.runPrune(forumPath);
    //
    clearRunPrune(forumPath);
  }
  
  private void clearRunPrune(String forumPath) throws Exception {
    //
    String forumId = Utils.getForumId(forumPath);
    clearForumCache(Utils.getCategoryId(forumPath), forumId, false);
    //
    clearTopicListCache(forumId);
    clearTopicListCountCache(forumId);
  }

  public void runPrune(PruneSetting pSetting) throws Exception {
    storage.runPrune(pSetting);
    //
    String forumPath = pSetting.getForumPath();
    clearRunPrune(forumPath);
  }

  public long checkPrune(PruneSetting pSetting) throws Exception {
    return storage.checkPrune(pSetting);
  }

  public boolean populateUserProfile(User user, UserProfile profileTemplate, boolean isNew) throws Exception {
    boolean isAdded = storage.populateUserProfile(user, profileTemplate, isNew);
    //
    if (isAdded) {
      //clear list of user profiles
      clearUserProfileListCache();
      clearUserProfileListCountCache();
    }
    return isAdded;
  }

  public boolean deleteUserProfile(String userId) throws Exception {
    clearUserProfile(userId);
    clearUserProfileListCache();
    clearUserProfileListCountCache();
    
    clearAllForumCache();
    
    return storage.deleteUserProfile(userId);
  }
  
  private void clearAllForumCache() {
    postData.clearCache();
    postList.clearCache();
    topicData.clearCache();
    forumData.clearCache();
    forumList.clearCache();
    categoryData.clearCache();
    categoryList.clearCache();
    //
    miscData.clearCache();
    watchListData.clearCache();
  }

  public void processEnabledUser(String userName, String email, boolean isEnabled) {
    storage.processEnabledUser(userName, email, isEnabled);
    //
    clearAllForumCache();
    //
    clearUserProfile(userName);
    try {
      clearUserProfileListCache();
      clearUserProfileListCountCache();
    } catch (Exception e) {
      LOG.warn("Failed to clear user cached.");
    }
  }

  public void calculateDeletedUser(String userName) throws Exception {
    storage.calculateDeletedUser(userName);
    //
    clearAllForumCache();
  }

  public void calculateDeletedGroup(String groupId, String groupName) throws Exception {
    storage.calculateDeletedGroup(groupId, groupName);
    topicData.select(new ScopeCacheSelector<TopicKey, TopicData>());
    topicList.select(new ScopeCacheSelector<TopicListKey, ListTopicData>());
    forumData.select(new ScopeCacheSelector<ForumKey, ForumData>());
    forumList.select(new ScopeCacheSelector<ForumListKey, ListForumData>());
    categoryData.select(new ScopeCacheSelector<CategoryKey, CategoryData>());
    categoryList.select(new ScopeCacheSelector<CategoryListKey, ListCategoryData>());
  }

  public void initDataPlugin() throws Exception {
    storage.initDataPlugin();
  }

  public void initDefaultData() throws Exception {
    storage.initDefaultData();
  }

  public List<RoleRulesPlugin> getRulesPlugins() {
    return storage.getRulesPlugins();
  }

  public List<InitializeForumPlugin> getDefaultPlugins() {
    return storage.getDefaultPlugins();
  }

  public void initAutoPruneSchedules() throws Exception {
    storage.initAutoPruneSchedules();
  }

  public void updateLastLoginDate(String userId) throws Exception {
    storage.updateLastLoginDate(userId);
  }

  public String getLatestUser() throws Exception {
    return storage.getLatestUser();
  }

  public List<Post> getNewPosts(int number) throws Exception {
    return storage.getNewPosts(number);
  }

  public List<Post> getRecentPostsForUser(String userName, int number) throws Exception {
    return storage.getRecentPostsForUser(userName, number);
  }

  public Map<String, String> getServerConfig() {
    return storage.getServerConfig();
  }

  public KSDataLocation getDataLocation() {
    return storage.getDataLocation();
  }

  public void setViewCountTopic(String path, String userRead) {
    storage.setViewCountTopic(path, userRead);
  }

  public void writeViews() {
    storage.writeViews();
  }

  public JCRPageList getPostForSplitTopic(String topicPath) throws Exception {
    return storage.getPostForSplitTopic(topicPath);
  }

  public void movePost(String[] postPaths, String destTopicPath, boolean isCreatNewTopic, String mailContent, String link) throws Exception {
    storage.movePost(postPaths, destTopicPath, isCreatNewTopic, mailContent, link);

    String srcTopicPath = Utils.getTopicPath(postPaths[0]);
    //
    forumData.select(new ForumPathSelector(new String[] {Utils.getForumPath(srcTopicPath), Utils.getForumPath(destTopicPath)}, forumData));
    clearForumListCache();
    //
    clearTopicCache(srcTopicPath);
    clearTopicCache(destTopicPath);
    //
    clearPostListCache();
    clearPostListCountCache(srcTopicPath);
    clearPostListCountCache(destTopicPath);
    //
    clearUserProfile(null);
  }

  public void mergeTopic(String srcTopicPath, String destTopicPath, String mailContent, String link) throws Exception {
    clearTopicCache(Utils.getCategoryId(srcTopicPath), Utils.getForumId(srcTopicPath), Utils.getTopicId(srcTopicPath));
    storage.mergeTopic(srcTopicPath, destTopicPath, mailContent, link);
    clearPostListCache();
    clearPostListCountCache(Utils.getTopicId(destTopicPath));
    clearTopicCache(destTopicPath);
    clearForumCache(Utils.getCategoryId(destTopicPath), Utils.getForumId(destTopicPath), false);
    clearTopicListCache();
    clearTopicListCountCache(Utils.getForumId(destTopicPath));
    //
    clearUserProfile(null);
  }

  public void splitTopic(Topic newTopic, Post fistPost, List<String> postPathMove, String mailContent, String link) throws Exception {
    storage.splitTopic(newTopic, fistPost, postPathMove, mailContent, link);
    String oldTopicPath = Utils.getTopicPath(postPathMove.get(0));
    clearPostListCache();
    clearPostListCountCache(Utils.getTopicId(oldTopicPath));
    clearTopicCache(oldTopicPath);
    clearForumCache(Utils.getCategoryId(oldTopicPath), Utils.getForumId(oldTopicPath), false);
    clearTopicListCache(Utils.getForumId(oldTopicPath));
    clearTopicListCountCache(Utils.getForumId(oldTopicPath));
  }

  public void updateUserProfileInfo(String name) throws Exception {
    clearUserProfile(name);
    storage.updateUserProfileInfo(name);
  }

  public InputStream createForumRss(String objectId, String link) throws Exception {
    return storage.createForumRss(objectId, link);
  }

  public InputStream createUserRss(String userId, String link) throws Exception {
    return storage.createUserRss(userId, link);
  }
  
  public List<Post> getPosts(final PostFilter filter, final int offset, final int limit) throws Exception {

    PostListKey key = new PostListKey(filter, offset, limit);

    return buildPostOutput(postListFuture.get(
        new ServiceContext<ListPostData>() {
          public ListPostData execute() {
            try {
              List<Post> got = storage.getPosts(filter, offset, limit);
              return buildPostInput(got);
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }
        },
        key
    ));

  }

  public int getPostsCount(final PostFilter filter) throws Exception {
    PostListCountKey key = new PostListCountKey("postsCount", filter.toString(), filter.getTopicId());

    SimpleCacheData<Integer> data = postListCountFuture.get(new ServiceContext<SimpleCacheData<Integer>>() {
      public SimpleCacheData<Integer> execute() {
        try {
          return new SimpleCacheData<Integer>(storage.getPostsCount(filter));
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    }, key);

    return data.build();
  }

  public void saveActivityIdForOwner(String ownerId,  String type, String activityId) {
    storage.saveActivityIdForOwner(ownerId, type, activityId);
  }

  public void saveActivityIdForOwner(String ownerPath, String activityId) {
    storage.saveActivityIdForOwner(ownerPath, activityId);
  }

  public String getActivityIdForOwner(String ownerId, String type) {
    return storage.getActivityIdForOwner(ownerId, type);
  }

  public String getActivityIdForOwner(String ownerPath) {
    return storage.getActivityIdForOwner(ownerPath);
  }

  @Override
  public boolean topicHasPoll(String topicPath) {
    return storage.topicHasPoll(topicPath);
  }

  @Override
  public List<ForumSearchResult> getUnifiedSearch(String textQuery,
                                                  String userId,
                                                  Integer offset,
                                                  Integer limit,
                                                  String sort,
                                                  String order) throws Exception {
    return storage.getUnifiedSearch(textQuery, userId, offset, limit, sort, order);
  }

  @Override
  public List<String> getForumUserCanView(final List<String> listOfUser, final List<String> listForumIds) throws Exception {
    String key = UserProfile.USER_GUEST;
    if (listOfUser != null && listOfUser.isEmpty() == false) {
      key = listOfUser.toString();
    }
    if (listForumIds != null && listForumIds.isEmpty() == false) {
      key += listForumIds.toString();
    }
    SimpleCacheKey canViewKey = new SimpleCacheKey(FORUM_CAN_VIEW_KEY, key);

    return (List<String>) miscDataFuture.get(
        new ServiceContext<SimpleCacheData>() {
          public SimpleCacheData<List<String>> execute() {
            try {
              List<String> got = storage.getForumUserCanView(listOfUser, listForumIds);
              return new SimpleCacheData<List<String>>(got);
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }
        },
        canViewKey
    ).build();
  }

  @Override
  public List<UserProfile> searchUserProfileByFilter(final UserProfileFilter userProfileFilter,
                                                     final int offset,
                                                     final int limit) throws Exception {
    
    UserProfileListKey key = new UserProfileListKey(userProfileFilter, offset, limit);

    ListUserProfileData data = userProfileListFuture.get(new ServiceContext<ListUserProfileData>() {
      @Override
      public ListUserProfileData execute() {
        try {
          return buildUserProfileInput(storage.searchUserProfileByFilter(userProfileFilter, offset, limit));
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    }, key);

    return buildUserProfileOutput(data);
  }

  @Override
  public int getUserProfileByFilterCount(final UserProfileFilter userProfileFilter) throws Exception {
    
    UserProfileListCountKey key = new UserProfileListCountKey(userProfileFilter);

    SimpleCacheData<Integer> data = userProfileListCountFuture.get(new ServiceContext<SimpleCacheData<Integer>>() {
      @Override
      public SimpleCacheData<Integer> execute() {
        try {
          return new SimpleCacheData<Integer>(storage.getUserProfileByFilterCount(userProfileFilter));
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }

    }, key);

    return data.build();
  }

  private ListUserProfileData buildUserProfileInput(List<UserProfile> userProfiles) {
    List<UserProfileKey> listKeys = new ArrayList<UserProfileKey>();
    for (UserProfile p : userProfiles) {
      UserProfileKey key = new UserProfileKey(p.getUserId());
      listKeys.add(key);
    }
    return new ListUserProfileData(listKeys);
  }
  
  private List<UserProfile> buildUserProfileOutput(ListUserProfileData data) {

    if (data == null) {
      return null;
    }

    List<UserProfile> out = new ArrayList<UserProfile>();
    for (UserProfileKey k : data.getIds()) {
      try {
        out.add(getQuickProfile(k.getUserId()));
      } catch (Exception e) {
        LOG.error(e);
      }
    }
    return out;
  }

  @Override
  public void removeCacheUserProfile(String userName) {
    clearUserProfile(userName);
  }

  @Override
  public void saveUserPrivateOfCategory(String categoryId, String priInfo) {
    storage.saveUserPrivateOfCategory(categoryId, priInfo);
  }
}
