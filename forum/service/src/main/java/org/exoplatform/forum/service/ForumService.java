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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 ***************************************************************************/
package org.exoplatform.forum.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.NodeIterator;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.forum.service.filter.model.CategoryFilter;
import org.exoplatform.forum.service.filter.model.ForumFilter;
import org.exoplatform.forum.service.impl.model.PostFilter;
import org.exoplatform.forum.service.impl.model.TopicFilter;
import org.exoplatform.forum.service.impl.model.UserProfileFilter;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.user.UserStateService;

/**
 * Manages Forums and all its related objects (categories, topics and posts).
 *
 */
public interface ForumService extends ForumServiceLegacy {

  /**
   * Adds a component plugin which keeps email configuration for Forums. 
   * 
   * @param plugin The plugin to be added.
   * 
   * @throws Exception the exception
   * @LevelAPI Platform
   * @since 1.0.x
   */
  void addPlugin(ComponentPlugin plugin) throws Exception;

  /**
   * Adds a role plugin which defines role rules in Forums.
   * 
   * @param plugin The plugin to be added.
   * @throws Exception the exception
   * @LevelAPI Platform
   * @since 1.0.x
   */
  void addRolePlugin(ComponentPlugin plugin) throws Exception;

  /**
   * Adds a plugin which initializes data when Forums is started at first time.
   *  
   * @param plugin The plugin to be added.
   * @throws Exception the exception
   * @LevelAPI Platform
   * @since 1.0.x
   */
  void addInitialDataPlugin(ComponentPlugin plugin) throws Exception;

  /**
   * Adds a plugin which initializes the default data when Forums is started at first time.
   * 
   * @param plugin The plugin to be added.
   * @throws Exception
   * @LevelAPI Platform
   */
  void addInitialDefaultDataPlugin(ComponentPlugin plugin) throws Exception;

  /**
   * Gets categories in Forums.
   * 
   * @return Categories.
   * @LevelAPI Platform
   */
  List<Category> getCategories();

  /**
   * 
   * Gets a category by its provided Id.
   * 
   * @param categoryId Id of the category.
   * @return A category.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  Category getCategory(String categoryId);

  /**
   * Gets a category which contains the forum spaces. 
   * 
   * @return A category.
   * @throws Exception
   * @LevelAPI Platform
   */
  Category getCategoryIncludedSpace();

  /**
   * Gets users or groups who have permission to edit topics in a category.
   * 
   * @param categoryId Id of the category.
   * @param type Type of the category.
   * @throws Exception the exception
   * @LevelAPI Platform
   * @since 1.2.x
   */
  String[] getPermissionTopicByCategory(String categoryId, String type) throws Exception;

  /**
   * Saves a category which is added or updated.
   * 
   * @param category The category to be saved.
   * @param isNew If "true", the new category is added. If "false", the category is updated.
   * @throws Exception the exception
   * @LevelAPI Platform
   * @since 1.0.x
   */
  void saveCategory(Category category, boolean isNew) throws Exception;

  /**
   * Checks information about a moderator of a category.
   * 
   * @param categoryPath Path to the category.
   * @param isNew If "true", the new information is added. If "false", the information is updated.
   * @throws Exception the exception
   * @LevelAPI Platform
   * @since 1.2.x
   */
  void calculateModerator(String categoryPath, boolean isNew) throws Exception;

  /**
   * Sets a moderator for a category.
   *
   * @param moderatorCate The moderator.
   * @param userId Id of the moderator.
   * @param isAdd If "true", the moderator is added. If "false", the moderator is removed.
   * @throws Exception the exception
   * @LevelAPI Platform
   * @since 1.2.x
   */
  void saveModOfCategory(List<String> moderatorCate, String userId, boolean isAdd);

  /**
   * Removes a category by its provided Id.
   * 
   * @param categoryId Id of the category to be removed.
   * @return The removed category.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  Category removeCategory(String categoryId) throws Exception;

  /**
   * Gets a list of forums by a specific category Id and a query condition.
   * 
   * @param categoryId Id of the category.
   * @param strQuery The query condition.
   * @return Forums.
   * @throws Exception the exception
   * @LevelAPI Platform
   *
   * @deprecated use {@link #getForums(ForumFilter)}
   */
  List<Forum> getForums(String categoryId, String strQuery) throws Exception;

  /**
   * Gets summaries of Forums.
   * 
   * @param categoryId Id of the category.
   * @param strQuery The statement to query forums.
   * @return The list of forums.
   * @throws Exception the exception
   * @LevelAPI Platform
   * 
   * @deprecated use {@link #getForums(ForumFilter)}
   */
  List<Forum> getForumSummaries(String categoryId, String strQuery) throws Exception;

  /**
   * Gets a list forums by filter.
   * 
   * @param userName Name of the user.
   * @param filter The condition to get forums.
   * @return The forums.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  List<Forum> getForums(ForumFilter filter);

  /**
   * Gets a list of category filters by a forum name and userName.
   * 
   * @param filterKey The key to search for a forum.
   * @param userName Name of the user.
   * @param maxSize The maximum number of category filters which are returned.
   * @return Category filters.
   * @throws Exception
   * @LevelAPI Platform
   */
  List<CategoryFilter> filterForumByName(String filterKey, String userName, int maxSize) throws Exception;

  /**
   * Gets a forum by a specific category Id and forum Id.
   * 
   * @param categoryId Id of the category.
   * @param forumId Id of the forum.
   * @return The forum.
   * @LevelAPI Platform
   */
  Forum getForum(String categoryId, String forumId);

  /**
   * Modifies an existing forum with various types.
   * <ul>
   *  <li>1. {@link Utils#CLOSE}: Closes a given forum.</li>
   *  <li>2. {@link Utils#LOCK}: Locks a given forum.</li>
   *  <li>3. {@link Utils#APPROVE}: Approves a given forum.</li>
   *  <li>4. {@link Utils#STICKY}: Sticks a given forum.</li>
   *  <li>5. {@link Utils#ACTIVE}: Activates a given forum.</li>
   *  <li>6. {@link Utils#WAITING}: Waits for a given forum.</li>
   *  <li>7. {@link Utils#HIDDEN}: Hides a given forum.</li>
   * </ul>
   * 
   * @param forum The forum to be modified.
   * @param type A type to modify the forum.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void modifyForum(Forum forum, int type) throws Exception;

  /**
   * Saves a forum into a specific category.
   * 
   * @param categoryId Id of the category.
   * @param forum The forum to be saved.
   * @param isNew If "true", the new forum is added. If "false", the forum is updated.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void saveForum(String categoryId, Forum forum, boolean isNew) throws Exception;

  /**
   * Saves or removes a moderator to/from a list of forums.
   * 
   * @param forumPaths Paths to the forums.
   * @param userName Name of the moderator.
   * @param isDelete If "true", the modertator is removed. If "false", the moderator is added.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void saveModerateOfForums(List<String> forumPaths, String userName, boolean isDelete) throws Exception;

  /**
   * Removes a forum by its category Id and forum Id.
   * 
   * @param categoryId Id of the category.
   * @param forumId Id of the forum to be removed.
   * @return The forum
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  Forum removeForum(String categoryId, String forumId) throws Exception;

  /**
   * Moves a list of forums to a specific category.
   * 
   * @param forums Forums to be moved.
   * @param destCategoryPath Path to the target category.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void moveForum(List<Forum> forums, String destCategoryPath) throws Exception;

  /**
   * Gets a list of topics by a specific category Id, forum Id and query condition.
   * 
   * @param categoryId Id of the category.
   * @param forumId Id of the forum.
   * @param strQuery The query condition.
   * @param strOrderBy The returned topics are shown by the ascending or descending order.
   * @return Topics.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  JCRPageList getPageTopic(String categoryId, String forumId, String strQuery, String strOrderBy) throws Exception;

  /**
   * Gets a list of topics by a given username.
   * 
   * @param userName Name of the user.
   * @param isMod If "true", only topics moderated by the user is returned.
   * @param strOrderBy The order type for topics (ascending or descending).
   * @return Topics.
   * @throws Exception the exception
   * @LevelAPI Platform
   * 
   * @deprecated used {@link #getPageTopicByUser(TopicFilter)}
   */
  JCRPageList getPageTopicByUser(String userName, boolean isMod, String strOrderBy) throws Exception;
  
  /**
   * Gets a list access of topics by a given userName returned as ListAccess.
   * 
   * @param userName Name of the user.
   * @param filter The condition to get posts.
   * @return The topics.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public ListAccess<Topic> getPageTopicByUser(TopicFilter filter) throws Exception;

  /**
   * Gets a list of topics which were created before a given date.
   * 
   * @param date The given date.
   * @param forumPatch Path to the forum which contains topics.
   * @return Topics.
   * @throws Exception the exception
   * @LevelAPI Platform
   * 
   * @deprecated used {@link #getTopicsByDate(long, String)}
   */
  JCRPageList getPageTopicOld(long date, String forumPatch) throws Exception;
  
  /**
   * Gets a list access of topics which were created before a given date.
   * 
   * @param date The given date.
   * @param forumPatch Path to the forum which contains topics.
   * @return Topics.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  ListAccess<Topic> getTopicsByDate(long date, String forumPath) throws Exception;

  /**
   * Gets a list of topics which were created before a given date.
   * 
   * @param date The given date.
   * @param forumPatch Path to the forum which contains topics.
   * @return Topics.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  List<Topic> getAllTopicsOld(long date, String forumPatch) throws Exception;

  /**
   * Gets the total number of topics which were created before a given date.
   * 
   * @param date The given date.
   * @param forumPatch Path to the forum which contains topics.
   * @return The total number of topics.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  long getTotalTopicOld(long date, String forumPatch);

  /**
   * Gets a list of topics by category Id and forum Id.
   * 
   * @param categoryId Id of the category.
   * @param forumId Id of the forum.
   * @return A lis of topics.
   * @throws Exception the exception
   * @LevelAPI Platform
   * 
   */
  List<Topic> getTopics(String categoryId, String forumId) throws Exception;

  /**
   * Gets a topic by category Id, forum Id and topic Id.
   * 
   * @param categoryId Id of the category.
   * @param forumId Id of the forum.
   * @param topicId Id of the topic.
   * @param userRead The user who views the topic.
   * @return The topic.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  Topic getTopic(String categoryId, String forumId, String topicId, String userRead) throws Exception;
  
  /**
   * Gets posts which are returned as ListAccess.
   * @param filter The condition to get posts.
   * @return The posts.
   * @throws Exception
   * @since 2.2.11
   */
  ListAccess<Topic> getTopics(TopicFilter filter) throws Exception;
  
  /**
   * Counts the number of topic viewers.
   * 
   * @param path Path to the topic.
   * @param userRead The user who views the topic.
   * @LevelAPI Platform
   */
  void setViewCountTopic(String path, String userRead);
  
  /**
   * Saves the number of topic viewers.
   * 
   * @since 2.2.11
   */
  void writeViews();
  
  /**
   * Gets a topic by its path and post condition.
   * 
   * @param topicPath Path to the topic.
   * @param isLastPost If "true", the returned topic is one which has the last post.
   * @return The topic.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  Topic getTopicByPath(String topicPath, boolean isLastPost) throws Exception;

  /**
   * Gets the last post of a forum.
   * 
   * @param lastTopicPath Path to the topic which contains the last post.
   * @return The topic which contains the last post.
   * @throws Exception
   * @LevelAPI Platform
   */
  Topic getLastPostOfForum(String lastTopicPath) throws Exception;
  
  /**
   * Gets a topic by its path.
   * 
   * @param topicPath Path to the topic.
   * @return The topic.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  Topic getTopicSummary(String topicPath) throws Exception;

  /**
   * Gets more information about a given topic.
   * 
   * @param topic The topic which is required to have more information.
   * @param isSummary If "true", the summary information about the topic is included.
   * @return The topic.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  Topic getTopicUpdate(Topic topic, boolean isSummary) throws Exception;

  /**
   * Modifies a list of topics with various types.
   * <ul>
   *  <li> 1. {@link Utils#CLOSE}: Closes a given topic.</li>
   *  <li> 2. {@link Utils#LOCK}: Locks a given topic.</li>
   *  <li> 3. {@link Utils#APPROVE}: Approves a given topic.</li>
   *  <li> 4. {@link Utils#STICKY}: Sticks a given topic.</li>
   *  <li> 5. {@link Utils#ACTIVE}: Activates a given topic.</li>
   *  <li> 6. {@link Utils#WAITING}: Waits for a given topic.</li>
   *  <li> 7. {@link Utils#HIDDEN}: Hides a given topic.</li>
   * </ul>
   * 
   * @param topics Topics to be modified.
   * @param type The selected modification type.
   * @LevelAPI Platform
   */
  void modifyTopic(List<Topic> topics, int type);
  
  /**
   * Modifies a list of merged topics with various types.
   * <ul>
   *  <li> 1. {@link Utils#CLOSE}: Closes the given list of merged topics.</li>
   *  <li> 2. {@link Utils#LOCK}: Locks the given list of merged topics.</li>
   *  <li> 3. {@link Utils#APPROVE}: Approves the given list of merged topics.</li>
   *  <li> 4. {@link Utils#STICKY}: Sticks the given list of merged topics.</li>
   *  <li> 5. {@link Utils#ACTIVE}: Activates the given list of merged topics.</li>
   *  <li> 6. {@link Utils#WAITING}: Waits for the given list of merged topics.</li>
   *  <li> 7. {@link Utils#HIDDEN}: Hides the given list of merged topics.</li>
   * </ul>
   * 
   * @param topics Merged topics.
   * @param type The selected modification type.
   * @LevelAPI Platform
   */
  void modifyMergedTopic(List<Topic> topics, int type);

  /**
   * Saves or updates a topic by category Id and forum Id.
   * 
   * @param categoryId Id of the category.
   * @param forumId Id of the forum.
   * @param topic The topic to be saved or updated.
   * @param isNew If "true", the new topic is added. If "false", the topic is updated.
   * @param isMove If "true", the topic is moved.
   * @param messageBuilder Builds an email message.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void saveTopic(String categoryId, String forumId, Topic topic, boolean isNew, boolean isMove, MessageBuilder messageBuilder) throws Exception;

  /**
   * Removes a topic.
   * 
   * @param categoryId Id of the category.
   * @param forumId Id of the forum.
   * @param topicId Id of the topic.
   * @return The topic.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  Topic removeTopic(String categoryId, String forumId, String topicId) throws Exception;

  /**
   * Moves a list of topics to a forum.
   * 
   * @param topics Topics to be moved.
   * @param destForumPath Path to the target forum.
   * @param mailContent Content of the email notification.
   * @param link Link to a specific topic.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void moveTopic(List<Topic> topics, String destForumPath, String mailContent, String link) throws Exception;

  /**
   * Merges two topics into one.
   * 
   * @param srcTopicPath Path to the source topic.
   * @param destTopicPath Path to the target topic.
   * @param mailContent Content of the email notification.
   * @param link Link to a specific topic.
   * @param topicMergeTitle Title of new topic which has been merged.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void mergeTopic(String srcTopicPath, String destTopicPath, String mailContent, String link, String topicMergeTitle) throws Exception;

  /**
   * Splits a topic into 2 ones.
   * 
   * @param newTopic The new topic which is created after being split.
   * @param firstPost The fist post of the new topic.
   * @param postPathMove Path to the post which is moved to the topic.
   * @param mailContent Content of the email notification.
   * @param link Link to the new topic.
   * @throws Exception
   * @LevelAPI Platform
   * @since 4.0
   */
  void splitTopic(Topic newTopic, Post firstPost, List<String> postPathMove, String mailContent, String link) throws Exception;

  /**
   * Gets a list of posts in a given forum that match with conditions, including "Approved", "Hidden" and query statement.
   * 
   * @param categoryId Id of the category.
   * @param forumId Id of the forum.
   * @param topicId Id of the topic.
   * @param isApproved If "true", only approved posts are returned.
   * @param isHidden If "true", only hidden posts are returned.
   * @param strQuery The query statement.
   * @param userLogin Name of the user who logs in.
   * @return Posts.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  JCRPageList getPosts(String categoryId, String forumId, String topicId, String isApproved, String isHidden, String strQuery, String userLogin) throws Exception;

  /**
   * Gets a list of posts in a given topic.
   * 
   * @param topicPath Path to the topic.
   * @return Posts.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  JCRPageList getPostForSplitTopic(String topicPath) throws Exception;

  /**
   * Gets posts which are returned as ListAccess.
   * @param filter The condition to get posts.
   * @return The posts.
   * @throws Exception
   * @since 2.2.11
   */
  ListAccess<Post> getPosts(PostFilter filter) throws Exception;

  /**
   * Gets the number of available posts which match with conditions, including "Approved", "Hidden" and query statement.
   * 
   * @param categoryId Id of the category.
   * @param forumId Id of the forum.
   * @param topicId Id of the topic.
   * @param isApproved If "true", only approved posts are returned.
   * @param isHidden If "true", only hidden posts are returned.
   * @param userLogin Name of the user who logs in.
   * @return The number of available posts.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  long getAvailablePost(String categoryId, String forumId, String topicId, String isApproved, String isHidden, String userLogin) throws Exception;

  /**
   * Gets an index of the last read post in a specific topic.
   * 
   * @param path Path to the post.
   * @param isApproved If "true", the last read post must be an approved one.
   * @param isHidden If "true", the last read post must be a hidden one.
   * @param userLogin Name of the user who logs in.
   * @return The post index.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  long getLastReadIndex(String path, String isApproved, String isHidden, String userLogin) throws Exception;

  /**
   * Gets a list of posts by a specific user.
   * 
   * @param userName Name of the current viewer.
   * @param userId Id of the poster.
   * @param isMod If "true", the poster is moderator.
   * @param strOrderBy The order type for posts (ascending or descending).
   * @return Posts.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  JCRPageList getPagePostByUser(String userName, String userId, boolean isMod, String strOrderBy) throws Exception;

  /**
   * Gets a post based on category Id, forum Id, topic Id and post Id.
   * 
   * @param categoryId Id of the category.
   * @param forumId Id of the forum.
   * @param topicId Id of the topic.
   * @param postId Id of the post.
   * @return The post.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  Post getPost(String categoryId, String forumId, String topicId, String postId) throws Exception;

  /**
   * Saves or updates a post. 
   * 
   * @param categoryId Id of the category.
   * @param forumId Id of the forum.
   * @param topicId Id of the topic.
   * @param post The post.
   * @param isNew If "true", the new post is added. If "false", the post is updated.
   * @param messageBuilder Builds an email message.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void savePost(String categoryId, String forumId, String topicId, Post post, boolean isNew, MessageBuilder messageBuilder) throws Exception;

  /**
   * Modifies a list of posts with various types.
   * <ul>
   *  <li>1. {@link Utils#CLOSE}: Closes the given post.</li>
   *  <li>2. {@link Utils#LOCK}: Locks the given post.</li>
   *  <li>3. {@link Utils#APPROVE}: Approves the given post.</li>
   *  <li>4. {@link Utils#STICKY}: Sticks the given post.</li>
   *  <li>5. {@link Utils#ACTIVE}: Activates the given post.</li>
   *  <li>6. {@link Utils#WAITING}: Waits for the given post.</li>
   *  <li>7. {@link Utils#HIDDEN}: Hides the given post.</li>
   * </ul>
   * 
   * @param posts The posts.
   * @param type The modification type.
   * @LevelAPI Platform
   */
  void modifyPost(List<Post> posts, int type);

  /**
   * Removes a post.
   * 
   * @param categoryId Id of the category.
   * @param forumId Id of the forum.
   * @param topicId Id of the topic.
   * @param postId Id of the post.
   * @return The post.
   * @LevelAPI Platform
   */
  Post removePost(String categoryId, String forumId, String topicId, String postId);

  /**
   * Moves a list of posts to a given topic.
   * 
   * @param postPaths Paths to the moved posts.
   * @param destTopicPath Path to the destination topic.
   * @param isCreatNewTopic If "true", the new topic is created. If "false", the topic is updated.
   * @param mailContent Content of the email notification.
   * @param link Link to the topic.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void movePost(String[] postPaths, String destTopicPath, boolean isCreatNewTopic, String mailContent, String link) throws Exception;

  /**
   * Gets the object name by its path.
   * 
   * @param path Path to the object name.
   * @return The object name.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  Object getObjectNameByPath(String path) throws Exception;

  /**
   * Gets the object name by its path.
   * 
   * @param id Identity to get the object name.
   * @param type Type of the object.
   * @return The object name.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  Object getObjectNameById(String id, String type) throws Exception;

  /**
   * Gets all links of a given forum.
   * 
   * @param strQueryCate The query condition of the category.
   * @param strQueryForum The query condition of the forum.
   * @return All links.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  List<ForumLinkData> getAllLink(String strQueryCate, String strQueryForum) throws Exception;

  /**
   * Gets the home path of a given forum.
   * 
   * @return The home path.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  String getForumHomePath() throws Exception;

  /**
   * Adds a list of tags to a specific topic.
   * 
   * @param tags Tags to be added.
   * @param userName Name of the current viewer.
   * @param topicPath Path to the topic.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void addTag(List<Tag> tags, String userName, String topicPath) throws Exception;

  /**
   * Removes a tag from a given topic.
   * 
   * @param tagId Id of the tag to be removed.
   * @param userName Name of the current viewer.
   * @param topicPath Path to the topic.
   * @LevelAPI Platform
   */
  void unTag(String tagId, String userName, String topicPath);

  /**
   * Gets a tag by its Id.
   * 
   * @param tagId Id of the tag.
   * @return The tag.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  Tag getTag(String tagId) throws Exception;

  /**
   * Gets all tag names by query statement and topic Id.
   * 
   * @param strQuery The query statement.
   * @param userAndTopicId The 'userId,topicId' pattern.
   * @return Tag names.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  List<String> getAllTagName(String strQuery, String userAndTopicId) throws Exception;

  /**
   * Gets all tag names in a given topic.
   * 
   * @param userAndTopicId The 'userId,topicId' pattern.
   * @return The tag names.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  List<String> getTagNameInTopic(String userAndTopicId) throws Exception;

  /**
   * Gets all tags.
   * 
   * @return The tags.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  List<Tag> getAllTags() throws Exception;

  /**
   * Gets a list of tags by their Ids.
   * 
   * @param tagIds Ids of the tags.
   * @return The tags.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  List<Tag> getMyTagInTopic(String[] tagIds) throws Exception;

  /**
   * Gets topics by a given tag.
   * 
   * @param userIdAndtagId The 'userId:tagId' pattern.
   * @param strOrderBy The order type for topics (ascending or descending).
   * @return The topics.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  JCRPageList getTopicByMyTag(String userIdAndtagId, String strOrderBy) throws Exception;

  /**
   * Saves a new tag.
   * 
   * @param newTag The tag to be saved.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void saveTag(Tag newTag) throws Exception;

  /**
   * Saves the user profile.
   * 
   * @param userProfile The user profile to be saved.
   * @param isOption If "true", only some basic information is saved. If "false", all information is saved.
   * @param isBan If "true", the profile of the banned user is still saved.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void saveUserProfile(UserProfile userProfile, boolean isOption, boolean isBan) throws Exception;

  /**
   * Saves the user profile.
   * 
   * @param user The user who wants to update his profile.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void updateUserProfile(User user) throws Exception;

  /**
   * Sets the moderator role to a user in a forum or a category.
   * 
   * @param userName Name of the user.
   * @param ids Ids in the forum or category.
   * @param isModeCate If "true", the moderator role of the category is set to the user. If "false", the moderator role of the forum is set to set to the user.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void saveUserModerator(String userName, List<String> ids, boolean isModeCate) throws Exception;

  /**
   * Searches for a user profile.
   * 
   * @param userSearch The user who wants to search.
   * @return A list of user profiles.
   * @throws Exception the exception
   * @LevelAPI Platform
   * 
   * @deprecated Use {@link #searchUserProfileByFilter(UserProfileFilter)} instead of.
   */
  JCRPageList searchUserProfile(String userSearch) throws Exception;
  
  /**
   * Searches for user profiles return as ListAccess.
   * 
   * @param userProfileFilter The filter object
   * @return A ListAccess of user profiles.
   * @throws Exception the exception
   * @LevelAPI Platform
   * @since 4.0.5
   */
  ListAccess<UserProfile> searchUserProfileByFilter(UserProfileFilter userProfileFilter) throws Exception;

  /**
   * Gets information about a user.
   * 
   * @param userName Name of the user.
   * @return The user information.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  UserProfile getUserInfo(String userName) throws Exception;

  /**
   * Gets Ids of forums or categories where the user is moderator.
   * 
   * @param userName Name of the user.
   * @param isModeCate If "true", Ids of categories are returned. If "false", Ids of forums are returned.
   * @return Ids.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  List<String> getUserModerator(String userName, boolean isModeCate) throws Exception;

  /**
   * Saves a user's bookmark.
   * 
   * @param userName Name of the user who bookmarks.
   * @param bookMark The bookmark to be saved.
   * @param isNew If "true", the new bookmark is added. If "false", the bookmark is updated.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void saveUserBookmark(String userName, String bookMark, boolean isNew) throws Exception;

  /**
   * Saves Ids of the last read posts into a topic or forum.
   * 
   * @param userId Name of the current user.
   * @param lastReadPostOfForum The number of the last read posts in a forum.
   * @param lastReadPostOfTopic The number of the last read posts in a topic.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void saveLastPostIdRead(String userId, String[] lastReadPostOfForum, String[] lastReadPostOfTopic) throws Exception;

  /**
   * Saves a collapsed category.
   * 
   * @param userName Name of the current user.
   * @param categoryId Id of the collapsed category.
   * @param isAdd If "true", the new collapsed category is added. If "false", the collapsed category is updated.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void saveCollapsedCategories(String userName, String categoryId, boolean isAdd) throws Exception;

  /**
   * Gets all user profiles.
   * 
   * @return A list of user profiles.
   * @throws Exception the exception.
   * @LevelAPI Platform
   * 
   * @deprecated Use {@link #searchUserProfileByFilter(UserProfileFilter)} instead of.
   */
  JCRPageList getPageListUserProfile() throws Exception;

  /**
   * Gets a quick search.
   * 
   * @param textQuery The text query.
   * @param type Type of object to search (forum, topic or post).
   * @param pathQuery The path by which the quick search is performed.
   * @param userId The user who performs the quick search.
   * @param listCateIds A list of categories where the quick search is performed.
   * @param listForumIds A list of forums where the quick search is performed.
   * @param forumIdsOfModerator Ids of the forums where the user is moderator.   
   * @return A list of search results.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  List<ForumSearchResult> getQuickSearch(String textQuery, String type, String pathQuery, String userId, List<String> listCateIds, List<String> listForumIds, List<String> forumIdsOfModerator) throws Exception;

  /**
   * Gets a screen name.
   * 
   * @param userName Name of the user.
   * @return The screen name.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  String getScreenName(String userName) throws Exception;

  /**
   * Gets an advanced search.
   * 
   * @param eventQuery The search condition.
   * @param listCateIds A list of categories where the advanced search is performed.
   * @param listForumIds A list of forums where the advanced search is performed.
   * @return A list of search results.
   * @LevelAPI Platform
   */
  List<ForumSearchResult> getAdvancedSearch(ForumEventQuery eventQuery, List<String> listCateIds, List<String> listForumIds);

  /**
   * Saves statistics of Forums.
   * 
   * @param forumStatistic The Forums statistics to be saved.
   * @throws Exception the exception.
   * @LevelAPI Platform
   */
  void saveForumStatistic(ForumStatistic forumStatistic) throws Exception;

  /**
   * Gets statistics of Forums.
   * 
   * @return The Forums statistics.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  ForumStatistic getForumStatistic() throws Exception;

  /**
   * Saves the administration settings of Forums.
   * 
   * @param forumAdministration The Forums administration settings to be saved.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void saveForumAdministration(ForumAdministration forumAdministration) throws Exception;

  /**
   * Gets the administration settings of Forums.
   * 
   * @return Information about the Forum administration settings.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  ForumAdministration getForumAdministration() throws Exception;

  /**
   * Updates information about topics and posts.
   * 
   * @param topicCoutn The number of topics.
   * @param postCount The number of posts.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void updateStatisticCounts(long topicCoutn, long postCount) throws Exception;

  /**
   * Sets the login information of a user.
   * 
   * @param userId Id of the current user.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void userLogin(String userId) throws Exception;
  void userLogin(String repoName, String userId) throws Exception;

  /**
   * Sets the logout information of a user.
   * 
   * @param userId Id of the user.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void userLogout(String userId) throws Exception;

  /**
   * Checks if a user is online or not.
   * 
   * @param userId Id of the user.
   * @return If "true", the checked user is online.
   * @throws Exception the exception
   * @LevelAPI Platform
   * @deprecated  use {@link UserStateService#isOnline(String)}
   */
  boolean isOnline(String userId) throws Exception;

  /**
   * Gets users who are online.
   * 
   * @return The online users.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  List<String> getOnlineUsers() throws Exception;

  /**
   * Gets the last login.
   * 
   * @return Id of the user who did the last login.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  String getLastLogin() throws Exception;

  /**
   * Gets private messages of a user.
   * 
   * @param userName Name of the user.
   * @param type Two message types: Sent or Received.
   * @return Private messages.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  JCRPageList getPrivateMessage(String userName, String type) throws Exception;

  /**
   * Gets the count of new private messages.
   * 
   * @param userName Name of the user.
   * @return The count of new private messages.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  long getNewPrivateMessage(String userName) throws Exception;

  /**
   * Saves a private message.
   * 
   * @param privateMessage The private message to be saved.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void savePrivateMessage(ForumPrivateMessage privateMessage) throws Exception;

  /**
   * Saves a read message.
   * 
   * @param messageId Id of the message to be saved.
   * @param userName Name of the current user.
   * @param type Two message types: Sent or Received.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void saveReadMessage(String messageId, String userName, String type) throws Exception;

  /**
   * Removes a private message.
   * 
   * @param messageId Id of the message.
   * @param userName Name of the current user.
   * @param type Two message types: Sent or Received.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void removePrivateMessage(String messageId, String userName, String type) throws Exception;

  /**
   * Gets information subscribed by a user.
   * 
   * @param userId Id of the current user.
   * @return The subscription information.
   * @LevelAPI Platform
   */
  ForumSubscription getForumSubscription(String userId);

  /**
   * Saves a subscription of a user.
   * 
   * @param forumSubscription The subscription information to be saved.
   * @param userId Id of the current user.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void saveForumSubscription(ForumSubscription forumSubscription, String userId) throws Exception;

  /**
   * Adds a watch.
   * 
   * @param watchType Type of watch (RSS or Email).
   * @param path Path to the watched object.
   * @param values Content of the email notification.
   * @param currentUser Information about the current user.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void addWatch(int watchType, String path, List<String> values, String currentUser) throws Exception;

  /**
   * Removes a watch.
   * 
   * @param watchType Type of watch (RSS or Email).
   * @param path Path to the unwatched object. 
   * @param values The value to remove.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void removeWatch(int watchType, String path, String values) throws Exception;

  /**
   * Gets a waiting job which needs to be moderated by the current user.
   * 
   * @param paths Paths in Forums where the user is moderator.
   * @return Topics and posts which are waiting for the moderator to validate.
   * @LevelAPI Platform
   */
  List<ForumSearchResult> getJobWattingForModerator(String[] paths);

  /**
   * Gets the number of waiting jobs which need to be moderated by the current user.
   * 
   * @param userId Id of the user.
   * @return The number of waiting jobs.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  int getJobWattingForModeratorByUser(String userId) throws Exception;

  /**
   * Gets information to build a message.
   * 
   * @param name Name of the message.
   * @return The message information.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  SendMessageInfo getMessageInfo(String name) throws Exception;

  /**
   * Gets messages which are pending (not sent).
   * 
   * @return The pending messages.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  Iterator<SendMessageInfo> getPendingMessages() throws Exception;

  /**
   * Checks if a user is administrator or not.
   * 
   * @param userName Id of the checked user.
   * @return The returned value is "true" if the user is administrator.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  boolean isAdminRole(String userName) throws Exception;
  
  /**
   * Checks if a user is administrator or not in the XML configuration.
   * 
   * @param userName Id of the checked user.
   * @return The returned value is "true" if the user is administrator.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  boolean isAdminRoleConfig(String userName) throws Exception;

  /**
   * Gets a list of new public posts with a given limit.
   * 
   * @param number The limit size.
   * @return New public posts.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  List<Post> getNewPosts(int number) throws Exception;
  
  /**
   * Gets a list of recent posts by a user with a given limit.
   * 
   * @param userName Id of the user.
   * @param number The limit size.
   * @return The recent posts.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  List<Post> getRecentPostsForUser(String userName, int number) throws Exception;

  /**
   * Searches by a query statement.
   * 
   * @param queryString The query statement.
   * @return The nodes.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  NodeIterator search(String queryString) throws Exception;

  /**
   * Evaluates a list of active users by a query statement.
   * 
   * @param query The query statement.
   * @LevelAPI Platform
   */
  void evaluateActiveUsers(String query);

  /**
   * Creates a new user profile.
   * 
   * @param user The user whose profile is created.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void createUserProfile(User user) throws Exception;

  /**
   * Updates the access information to a topic of a user. 
   * 
   * @param userId Id of the user.
   * @param topicId Id of the topic.
   * @LevelAPI Platform
   */
  void updateTopicAccess(String userId, String topicId);

  /**
   * Updates the access information to a forum of a user.
   * 
   * @param userId Id of the user.
   * @param forumId Id of the forum.
   * @LevelAPI Platform
   */
  void updateForumAccess(String userId, String forumId);

  /**
   * Marks topics which have been read, then saves them into the user profile.
   * 
   * @since 2.2.11
   */
  void writeReads();

  /**
   * Exports data of a category or a forum to an XML file. 
   * 
   * @param categoryId Id of the category.
   * @param forumId Id of the forum.
   * @param objectIds Id of the category or forum.
   * @param nodePath Path to the category or forum which is exported.
   * @param bos The input byte stream. If this value is null, the output is a zip-typed file.
   * @param isExportAll If "true", all is exported.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  Object exportXML(String categoryId, String forumId, List<String> objectIds, String nodePath, ByteArrayOutputStream bos, boolean isExportAll) throws Exception;

  /**
   * Imports data into a category or a forum. 
   *  
   * @param nodePath Path to the data which is imported.
   * @param bis The input stream to import.
   * @param typeImport Type of import. Refer to {@link ImportUUIDBehavior}.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void importXML(String nodePath, ByteArrayInputStream bis, int typeImport) throws Exception;

  /**
   * Gets basic profiles of users.
   * 
   * @param userList Users.
   * @return The profiles.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  List<UserProfile> getQuickProfiles(List<String> userList) throws Exception;

  /**
   * Gets basic profiles of a user.
   * 
   * @param userName Id of the user.
   * @return Profile information of the user.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  UserProfile getQuickProfile(String userName) throws Exception;

  /**
   * Gets detailed profile information of a user.
   * 
   * @param userProfile The profile information of the user.
   * @return The detailed profile information of the user.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  UserProfile getUserInformations(UserProfile userProfile) throws Exception;

  /**
   * Gets the default profile information of a user.
   * 
   * @param userName Id of the user.
   * @param ip Ip of the user.
   * @return The default profile information.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  UserProfile getDefaultUserProfile(String userName, String ip) throws Exception;

  /**
   * Updates settings of the user profile.
   * 
   * @param userProfile The input user profile.
   * @return The updated settings of the user profile.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  UserProfile updateUserProfileSetting(UserProfile userProfile) throws Exception;

  /**
   * Gets bookmarks of a user.
   * 
   * @param userName Id of the user.
   * @return Bookmarks.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  List<String> getBookmarks(String userName) throws Exception;

  /**
   * Gets the profile settings by a user Id.
   * 
   * @param userName Id of the user.
   * @return Profile information of the user.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  UserProfile getUserSettingProfile(String userName) throws Exception;

  /**
   * Gets the management information of a user profile.
   * 
   * @param userName Id of the user.
   * @return Profile information of the user.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  UserProfile getUserProfileManagement(String userName) throws Exception;

  /**
   * Saves the settings information of a user profile.
   * 
   * @param userProfile The user profile information to be saved.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void saveUserSettingProfile(UserProfile userProfile) throws Exception;

  /**
   * Updates information of a forum.
   * 
   * @param path Path to the forum.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void updateForum(String path) throws Exception;

  /**
   * Gets a list of banned Ips.
   * 
   * @return The banned Ips.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  List<String> getBanList() throws Exception;

  /**
   * Adds an Ip to the list of banned Ips.
   * 
   * @param ip Ip to be added.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  boolean addBanIP(String ip) throws Exception;

  /**
   * Removes a banned Ip.
   * 
   * @param ip Ip to be removed.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void removeBan(String ip) throws Exception;

  /**
   * Gets a list of banned Ips in a forum.
   * 
   * @param forumId Id of the forum.
   * @return The banned Ips in the forum.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  List<String> getForumBanList(String forumId) throws Exception;

  /**
   * Adds an Ip to the list of banned Ips in a forum.
   * 
   * @param ip Ip to be added.
   * @param forumId Id of the forum.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  boolean addBanIPForum(String ip, String forumId) throws Exception;

  /**
   * Removes an Ip from the list of banned Ips in a forum.
   * 
   * @param ip Ip to be removed.
   * @param forumId Id of the forum.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void removeBanIPForum(String ip, String forumId) throws Exception;

  /**
   * Gets a list of posts by a given Ip.
   * 
   * @param ip Ip to get the list of posts.
   * @param strOrderBy The returned posts are shown in the ascending or descending order.
   * @return The posts.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  JCRPageList getListPostsByIP(String ip, String strOrderBy) throws Exception;

  /**
   * Gets an avatar by a given username.
   * 
   * @param userName Id of the user.
   * @return The user avatar.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  ForumAttachment getUserAvatar(String userName) throws Exception;

  /**
   * Saves an avatar for a user.
   * 
   * @param userId Id of the user.
   * @param fileAttachment The avatar data to be saved.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void saveUserAvatar(String userId, ForumAttachment fileAttachment) throws Exception;

  /**
   * Sets a default avatar.
   * 
   * @param userName Id of the user.
   * @LevelAPI Platform
   */
  void setDefaultAvatar(String userName);

  /**
   * Gets a list of watches by a user.
   * 
   * @param userId Id of the user.
   * @return The watches.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  List<Watch> getWatchByUser(String userId) throws Exception;

  /**
   * Updates an email address of a watch for a user.
   * 
   * @param listNodeId A list of Ids of the watched nodes.
   * @param newEmailAdd The new email address to be updated.
   * @param userId Id of the user.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void updateEmailWatch(List<String> listNodeId, String newEmailAdd, String userId) throws Exception;

  /**
   * Gets settings of all prunes.
   * 
   * @return Settings of all prunes.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  List<PruneSetting> getAllPruneSetting() throws Exception;

  /**
   * Gets settings of a prune by a forum path.
   * 
   * @param forumPath Path to the forum.
   * @return Settings information of the prune.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  PruneSetting getPruneSetting(String forumPath) throws Exception;

  /**
   * Saves settings of a prune.
   * 
   * @param pruneSetting The prune settings to be saved.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void savePruneSetting(PruneSetting pruneSetting) throws Exception;

  /**
   * Runs settings of a prune.
   * 
   * @param pSetting The prune setting to be run.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void runPrune(PruneSetting pSetting) throws Exception;

  /**
   * Runs settings of a prune by a forum path.
   * 
   * @param forumPath Path to the forum.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void runPrune(String forumPath) throws Exception;

  /**
   * Checks if settings of a prune is applied to topics or not.
   * 
   * @param pSetting The prune settings to be checked.
   * @return The number of topics to be pruned.  
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  long checkPrune(PruneSetting pSetting) throws Exception;

  /**
   * Gets a list of topics.
   * 
   * @param categoryId Id of the category containing the topics.
   * @param forumId Id of the forum.
   * @param string The input condition.
   * @param strOrderBy The returned topics are shown in the ascending or descending order.
   * @param pageSize The limit size to get topics.
   * @return Topics.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  LazyPageList<Topic> getTopicList(String categoryId, String forumId, String string, String strOrderBy, int pageSize) throws Exception;

  /**
   * Updates the user profile information.
   * 
   * @param name Id of the user.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void updateUserProfileInfo(String name) throws Exception;

  /**
   * Adds a new member to a forum. The forum profile is created and its statistics is updated.
   * 
   * @param user The user to be added.
   * @param profileTemplate The user profile template to be used for default settings.
   * @throws Exception
   * @LevelAPI Platform
   */
  void addMember(User user, UserProfile profileTemplate) throws Exception;

  /**
   * Removes an existing member from a forum. The forum profile is deleted and its statistics is updated.
   * 
   * @param user The user who is removed from the forum.
   * @throws Exception
   * @LevelAPI Platform
   */
  void removeMember(User user) throws Exception;

  /**
   * Updates information of logged-in users.
   * 
   * @throws Exception
   * @LevelAPI Platform
   */
  public void updateLoggedinUsers() throws Exception;
  
  public void updateLoggedinUsers(String repoName) throws Exception;

  /**
   * Updates statistics when a user is deleted.
   * 
   * @param userName Id of the removed user.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public void calculateDeletedUser(String userName) throws Exception;

  /**
   * Updates statistics when a group is deleted.
   * 
   * @param groupId Id of the deleted group.
   * @param groupName Name of the deleted group.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public void calculateDeletedGroup(String groupId, String groupName) throws Exception;

  /**
   * Creates an RSS of Forums.
   * 
   * @param objectId Id of any object which needs to create RSS (forum, topic).
   * @param link The link to get RSS.
   * @return RSS data information.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public InputStream createForumRss(String objectId, String link) throws Exception;

  /**
   * Creates an RSS of a user.
   * 
   * @param userId Id of the user.
   * @param link The link to get the RSS.
   * @return RSS data information.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public InputStream createUserRss(String userId, String link) throws Exception;

  /**
   * Adds a listener plugin.
   * 
   * @param listener The forum listener to be added.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public void addListenerPlugin(ForumEventListener listener) throws Exception;
  
  /**
   * Removes data of user profile from cache of a user.
   * 
   * @param userName Id of the user.
   * @throws Exception 
   * @LevelAPI Platform
   */
  public void removeCacheUserProfile(String userName) throws Exception;
  
  /**
   * Saves information of a forum activity by the owner Id that is used for processing the activity streams.
   * 
   * @param ownerId Id of the user who has created the forum activity.
   * @param activityId Id of the forum activity.
   * @LevelAPI Platform
   * @since 4.0
   */
  public void saveActivityIdForOwnerId(String ownerId, String activityId);

  /**
   * Saves information of a forum activity by the owner path that is used for processing the activity streams.
   * 
   * @param ownerPath Path to the user who has created the forum activity.
   * @param activityId Id of the forum activity. 
   * @LevelAPI Platform
   * @since 4.0
   */
  public void saveActivityIdForOwnerPath(String ownerPath, String activityId);

  /**
   * Gets information of a forum activity by the owner Id that is used for processing the activity streams.
   * 
   * @param ownerId Id of the user who has created the forum activity.
   * @return Id of the forum activity.
   * @LevelAPI Platform
   * @since 4.0
   */
  public String getActivityIdForOwnerId(String ownerId);

  /**
   * Gets information of a forum activity by the owner path that is used for processing the activity streams.
   * 
   * @param ownerPath Path to the user who has created the forum activity.
   * @return Id of the forum activity.
   * @LevelAPI Platform
   * @since 4.0
   */
  public String getActivityIdForOwnerPath(String ownerPath);
  
  /**
   * Saves information of a comment by the owner Id.
   * 
   * @param ownerId Id of the user who has commented.
   * @param commentId Id of the comment.
   * @LevelAPI Platform
   * @since 4.0
   */
  public void saveCommentIdForOwnerId(String ownerId, String commentId);

  /**
   * Saves information of a comment by the owner path.
   * 
   * @param ownerPath Path to the user who has commented.
   * @param commentId Id of the comment. 
   * @LevelAPI Platform
   * @since 4.0
   */
  public void saveCommentIdForOwnerPath(String ownerPath, String commentId);

  /**
   * Gets information of a comment by the owner Id.
   * 
   * @param ownerId Id of the user who has commented.
   * @return Id of the comment.
   * @LevelAPI Platform
   * @since 4.0
   */
  public String getCommentIdForOwnerId(String ownerId);

  /**
   * Gets information of a comment by the owner path.
   * 
   * @param ownerPath Path to the user who has commented.
   * @return Id of the comment.
   * @LevelAPI Platform
   * @since 4.0
   */
  public String getCommentIdForOwnerPath(String ownerPath);

}
