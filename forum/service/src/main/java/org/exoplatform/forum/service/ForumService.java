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

import javax.jcr.NodeIterator;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.forum.service.filter.model.CategoryFilter;
import org.exoplatform.forum.service.impl.model.PostFilter;
import org.exoplatform.services.organization.User;

/**
 * APIs for Forum and all other related objects (Categories, Topics and Posts) operations.
 *
 */
public interface ForumService extends ForumServiceLegacy {

  /**
   * Adds component plug-in what keeps email configuration for Forum. 
   * 
   * @param plugin the provided email configuration plug-in
   * 
   * @throws Exception the exception
   * @LevelAPI Platform
   * @since 1.0.x
   */
  void addPlugin(ComponentPlugin plugin) throws Exception;

  /**
   * Adds the role plug-in what define role rule in forum.
   * 
   * @param plugin the provided role rules plug-in
   * @throws Exception the exception
   * @LevelAPI Platform
   * @since 1.0.x
   */
  void addRolePlugin(ComponentPlugin plugin) throws Exception;

  /**
   * Adds the initialization data when runs forum first time.
   *  
   * @param plugin the provided initialization data plug-in
   * @throws Exception the exception
   * @LevelAPI Platform
   * @since 1.0.x
   */
  void addInitialDataPlugin(ComponentPlugin plugin) throws Exception;

  /**
   * Adds the initialization default data when runs forum first time.
   * 
   * @param plugin
   * @throws Exception
   * @LevelAPI Platform
   */
  void addInitialDefaultDataPlugin(ComponentPlugin plugin) throws Exception;

  /**
   * Returns {@link Category} list contained in forum.
   * 
   * @return the list category
   * @LevelAPI Platform
   */
  List<Category> getCategories();

  /**
   * 
   * Returns the {@link Category} to which the specified key is stored, 
   * or null if forum does not contain any {@link Category} for the key.
   * 
   * @param categoryId is the id of category.
   * @return the category
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  Category getCategory(String categoryId);

  /**
   * Returns the {@link Category} to which keeps {@link Forum} under Space context. 
   * 
   * @return the category
   * @throws Exception
   * @LevelAPI Platform
   */
  Category getCategoryIncludedSpace();

  /**
   * Gets user and group have edit permission in the {@link Category}
   * 
   * @param categoryId id of category
   * @param type type of category
   * @throws Exception the exception
   * @LevelAPI Platform
   * @since 1.2.x
   */
  String[] getPermissionTopicByCategory(String categoryId, String type) throws Exception;

  /**
   * Saves {@link Category}, also checks exists category or not.
   * If not to create new else update exists category
   * 
   * @param category is the category
   * @param isNew optional add new or not.
   * @throws Exception the exception
   * @LevelAPI Platform
   * @since 1.0.x
   */
  void saveCategory(Category category, boolean isNew) throws Exception;

  /**
   * Calculates moderator information for category
   * 
   * @param categoryPath path of category
   * @param isNew is calculate new or not
   * @throws Exception the exception
   * @LevelAPI Platform
   * @since 1.2.x
   */
  void calculateModerator(String categoryPath, boolean isNew) throws Exception;

  /**
   * Saves moderator to the {@link Category} list for specified userId
   *
   * @param moderatorCate the category list
   * @param userId the userId
   * @param isAdd optional value add new or not
   * @throws Exception the exception
   * @LevelAPI Platform
   * @since 1.2.x
   */
  void saveModOfCategory(List<String> moderatorCate, String userId, boolean isAdd);

  /**
   * Removes the {@link Category} the specified key from the forum if present.
   * 
   * @param categoryId the removed categoryId.
   * @return the category removed
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  Category removeCategory(String categoryId) throws Exception;

  /**
   * Returns the {@link Forum} list which contains 
   * the specified {@link Category}'s Id and match query condition.
   * 
   * @param categoryId is the id of category have list forum
   * @param strQuery condition to get
   * @return the list forum
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  List<Forum> getForums(String categoryId, String strQuery) throws Exception;

  /**
   * Returns the {@link CategoryFilter} list which match forumName and userName specification.
   * 
   * @param filterKey - the key to search forum.
   * @param userName - the identify of user.
   * @param maxSize - limit of forum result
   * @return - list of {@link CategoryFilter}.
   * @throws Exception
   * @LevelAPI Platform
   */
  List<CategoryFilter> filterForumByName(String filterKey, String userName, int maxSize) throws Exception;

  /**
   * Returns the {@link Forum} which contains 
   * the specified {@link Category}'s Id and {@link Forum}'s Id.
   * 
   * @param categoryId is the id of category identify.
   * @param forumId is the id of forum identify.
   * @return the forum
   * @LevelAPI Platform
   */
  Forum getForum(String categoryId, String forumId);

  /**
   * Modifies the existing {@link Forum} base on Type of updating.
   * <ul>
   *  <li> 1. {@link Utils#CLOSE} : close specified {@link Forum}</li>
   *  <li> 2. {@link Utils#LOCK} : lock specified {@link Forum}</li>
   *  <li> 3. {@link Utils#APPROVE} : approve specified {@link Forum}</li>
   *  <li> 4. {@link Utils#STICKY} : sticky specified {@link Forum}</li>
   *  <li> 5. {@link Utils#ACTIVE} : active specified {@link Forum}</li>
   *  <li> 6. {@link Utils#WAITING} : wait specified {@link Forum}</li>
   *  <li> 7. {@link Utils#HIDDEN} : hide specified {@link Forum}</li>
   * </ul>
   * 
   * @param forum is the object forum that should be modified
   * @param type is choose when modify this forum.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void modifyForum(Forum forum, int type) throws Exception;

  /**
   * Creates new or update Forum.
   * 
   * @param categoryId is the id of category identify.
   * @param forum the forum.
   * @param isNew true is new forum, else update forum
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void saveForum(String categoryId, Forum forum, boolean isNew) throws Exception;

  /**
   * Saves or remove user is moderator of list forum
   * 
   * @param forumPaths {@link Forum} path list will be updated moderator.
   * @param userName the userName
   * @param isDelete is false when you want to add userId into list moderator of
   *          forums isDelete is true when you want to remove userId from list
   *          moderator of forums.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void saveModerateOfForums(List<String> forumPaths, String userName, boolean isDelete) throws Exception;

  /**
   * Removes the forum base on {@link Category}'s Id and {@link Forum}'s Id.
   * 
   * @param categoryId is the id of category.
   * @param forumId is the id of forum need remove.
   * @return the forum
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  Forum removeForum(String categoryId, String forumId) throws Exception;

  /**
   * Moves {@link Forum} list to destination {@link Category}
   * 
   * @param forums the forum list
   * @param destCategoryPath the target {@link Category}
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void moveForum(List<Forum> forums, String destCategoryPath) throws Exception;

  /**
   * Gets the topic list specified {@link Forum}
   * 
   * @param categoryId the Category's Id
   * @param forumId the Forum's Id
   * @param strQuery Condition to get
   * @param strOrderBy the query statement
   * @return the topic list keeps in {@link JCRPageList}
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  JCRPageList getPageTopic(String categoryId, String forumId, String strQuery, String strOrderBy) throws Exception;

  /**
   * Gets the {@link Topic} list which match userName
   * 
   * @param userName the owner {@link Topic}
   * @param isMod the viewer is moderator or not
   * @param strOrderBy is a string. It's content have command to set 'order by' of Query. This function will return page topic has 'order by'
   *        by strOrderby. 
   * @return the topic list keeps in {@link JCRPageList}
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  JCRPageList getPageTopicByUser(String userName, boolean isMod, String strOrderBy) throws Exception;

  /**
   * Gets the {@link Topic} list which match created date.
   * 
   * @param date the created date
   * @param forumPatch the {@link Forum} path
   * @return the {@link Topic} list keeps in {@link JCRPageList}
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  JCRPageList getPageTopicOld(long date, String forumPatch) throws Exception;

  /**
   * Gets the {@link Topic} list which match created date.
   * 
   * @param date the date
   * @param forumPatch path of forum
   * @return the {@link Topic} list
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  List<Topic> getAllTopicsOld(long date, String forumPatch) throws Exception;

  /**
   * Count number for these topic which match created date.
   * 
   * @param date the created date
   * @param forumPatch the {@link Forum} path
   * @return the count number
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  long getTotalTopicOld(long date, String forumPatch);

  /**
   * Gets the topic list specified {@link Forum}
   * 
   * @param categoryId the Category's Id
   * @param forumId the Forum's Id
   * @return the {@link Topic} list
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  List<Topic> getTopics(String categoryId, String forumId) throws Exception;

  /**
   * Gets the {@link Post} specified {@link Topic}
   * 
   * @param categoryId the Category's Id
   * @param forumId the Forum's Id
   * @param topicId the topic id
   * @param userRead the viewer
   * @return the {@link Post}
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  Topic getTopic(String categoryId, String forumId, String topicId, String userRead) throws Exception;

  /**
   * Updates topic viewers 
   * 
   * @param path path of topic
   * @param userRead the viewer
   * @LevelAPI Platform
   */
  void setViewCountTopic(String path, String userRead);
  
  /**
   * Write the number of topic viewers.
   * 
   * @since 2.2.11
   */
  void writeViews();
  
  /**
   * Gets newest {@link Topic}
   * 
   * @param topicPath the topic path
   * @param isLastPost is the last post
   * @return the topic
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  Topic getTopicByPath(String topicPath, boolean isLastPost) throws Exception;

  /**
   * Gets newest {@link Post}
   * 
   * @param lastTopicPath
   * @return the topic contain last post of forum.
   * @throws Exception
   * @LevelAPI Platform
   */
  Topic getLastPostOfForum(String lastTopicPath) throws Exception;
  
  /**
   * Returns Topic's summary information
   * 
   * @param topicPath the topic path
   * @return the {@link Topic}
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  Topic getTopicSummary(String topicPath) throws Exception;

  /**
   * Gets more information for specified {@link Topic}
   * 
   * @param topic the topic object
   * @param isSummary included summary information or not.
   * @return the topic
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  Topic getTopicUpdate(Topic topic, boolean isSummary) throws Exception;

  /**
   * Modify the existing {@link Topic} list base on updating type.
   * <ul>
   *  <li> 1. {@link Utils#CLOSE} : close specified {@link Forum}</li>
   *  <li> 2. {@link Utils#LOCK} : lock specified {@link Forum}</li>
   *  <li> 3. {@link Utils#APPROVE} : approve specified {@link Forum}</li>
   *  <li> 4. {@link Utils#STICKY} : sticky specified {@link Forum}</li>
   *  <li> 5. {@link Utils#ACTIVE} : active specified {@link Forum}</li>
   *  <li> 6. {@link Utils#WAITING} : wait specified {@link Forum}</li>
   *  <li> 7. {@link Utils#HIDDEN} : hide specified {@link Forum}</li>
   * </ul>
   * 
   * @param topics the topic list will be updated
   * @param type specified action type
   * @LevelAPI Platform
   */
  void modifyTopic(List<Topic> topics, int type);
  
  /**
   * Modify the merged {@link Topic} list base on updating type.
   * <ul>
   *  <li> 1. {@link Utils#CLOSE} : close specified {@link Forum}</li>
   *  <li> 2. {@link Utils#LOCK} : lock specified {@link Forum}</li>
   *  <li> 3. {@link Utils#APPROVE} : approve specified {@link Forum}</li>
   *  <li> 4. {@link Utils#STICKY} : sticky specified {@link Forum}</li>
   *  <li> 5. {@link Utils#ACTIVE} : active specified {@link Forum}</li>
   *  <li> 6. {@link Utils#WAITING} : wait specified {@link Forum}</li>
   *  <li> 7. {@link Utils#HIDDEN} : hide specified {@link Forum}</li>
   * </ul>
   * 
   * @param topics the topics
   * @param type the type
   * @LevelAPI Platform
   */
  void modifyMergedTopic(List<Topic> topics, int type);

  /**
   * Save or update {@link Topic}.
   * 
   * @param categoryId the category id
   * @param forumId the forum id
   * @param topic the topic
   * @param isNew is the new
   * @param isMove is the move
   * @param messageBuilder Message builder
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void saveTopic(String categoryId, String forumId, Topic topic, boolean isNew, boolean isMove, MessageBuilder messageBuilder) throws Exception;

  /**
   * Removes the topic.
   * 
   * @param categoryId the category id
   * @param forumId the forum id
   * @param topicId the topic id
   * @return the topic
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  Topic removeTopic(String categoryId, String forumId, String topicId) throws Exception;

  /**
   * Move topic list to target {@link Forum}
   * 
   * @param topics the topics
   * @param destForumPath the target of forum path
   * @param mailContent mail to send notification
   * @param link to topic
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void moveTopic(List<Topic> topics, String destForumPath, String mailContent, String link) throws Exception;

  /**
   * Merge two topics to be new one.
   * 
   * @param srcTopicPath path of moved topic
   * @param destTopicPath the target of topic
   * @param mailContent mail to send notification
   * @param link to topic
   * @param topicMergeTitle new topic's name
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void mergeTopic(String srcTopicPath, String destTopicPath, String mailContent, String link, String topicMergeTitle) throws Exception;

  /**
   * Splits specified {@link Topic} to two one.
   * 
   * @param newTopic - the new topic create when split topic.
   * @param firstPost - the fist post of new topic.
   * @param postPathMove - the list path's posts move to new topic
   * @param mailContent - the mail content to send notification
   * @param link to topic
   * @throws Exception
   * @LevelAPI Platform
   * @since 4.0
   */
  void splitTopic(Topic newTopic, Post firstPost, List<String> postPathMove, String mailContent, String link) throws Exception;

  /**
   * Gets the post list in specified {@link Forum} 
   * which match conditions such as isApproved, isHidden and query statement.
   * 
   * @param categoryId the category id
   * @param forumId the forum id
   * @param topicId the topic id
   * @param isApproved is the approved
   * @param isHidden is the hidden
   * @param strQuery the str query
   * @param userLogin the user login
   * @return the {@link Post} list
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  JCRPageList getPosts(String categoryId, String forumId, String topicId, String isApproved, String isHidden, String strQuery, String userLogin) throws Exception;

  /**
   * Gets the post list in specified {@link Topic} 
   * 
   * @param topicPath path of topic
   * @return the {@link Post} list
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  JCRPageList getPostForSplitTopic(String topicPath) throws Exception;

  /**
   * Gets Posts and return ListAccess
   * @param filter
   * @return
   * @throws Exception
   * @since 2.2.11
   */
  ListAccess<Post> getPosts(PostFilter filter) throws Exception;

  /**
   * Count number of {@link Post} which match conditions such as isApproved, isHidden and query statement.
   * 
   * @param categoryId the category id
   * @param forumId the forum id
   * @param topicId the topic id
   * @param isApproved is the approved
   * @param isHidden is the hidden
   * @param userLogin the user login
   * @return the count number
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  long getAvailablePost(String categoryId, String forumId, String topicId, String isApproved, String isHidden, String userLogin) throws Exception;

  /**
   * Gets specified {@link Post} position number in the {@link Topic}.
   * 
   * @param path path of post
   * @param isApproved is the approved
   * @param isHidden is the hidden
   * @param userLogin the user login
   * @return index of the post
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  long getLastReadIndex(String path, String isApproved, String isHidden, String userLogin) throws Exception;

  /**
   * Gets the {@link Post} list by poster.
   * 
   * @param userName the user name
   * @param userId the poster
   * @param isMod the role of poster
   * @param strOrderBy is a string. It's content have command Query. This function
   *        will return page post suitable to content of that strQuery
   * @return the {@link Post} list
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  JCRPageList getPagePostByUser(String userName, String userId, boolean isMod, String strOrderBy) throws Exception;

  /**
   * Return {@link Post} base on {@link Category}'s Id, {@link Forum}'s Id, {@link Topic}'s Id and {@link Post}'s Id
   * 
   * @param categoryId the category id
   * @param forumId the forum id
   * @param topicId the topic id
   * @param postId the post id
   * @return the post
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  Post getPost(String categoryId, String forumId, String topicId, String postId) throws Exception;

  /**
   * Saves or updates {@link Post} base on provided isNew. 
   * 
   * @param categoryId the category id
   * @param forumId the forum id
   * @param topicId the topic id
   * @param post the post
   * @param isNew is the new
   * @param messageBuilder Message builder
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void savePost(String categoryId, String forumId, String topicId, Post post, boolean isNew, MessageBuilder messageBuilder) throws Exception;

  /**
   * Modifies the {@link Post} base on updating type.
   * <ul>
   *  <li> 1. {@link Utils#CLOSE} : close specified {@link Forum}</li>
   *  <li> 2. {@link Utils#LOCK} : lock specified {@link Forum}</li>
   *  <li> 3. {@link Utils#APPROVE} : approve specified {@link Forum}</li>
   *  <li> 4. {@link Utils#STICKY} : sticky specified {@link Forum}</li>
   *  <li> 5. {@link Utils#ACTIVE} : active specified {@link Forum}</li>
   *  <li> 6. {@link Utils#WAITING} : wait specified {@link Forum}</li>
   *  <li> 7. {@link Utils#HIDDEN} : hide specified {@link Forum}</li>
   * </ul>
   * 
   * @param posts the posts
   * @param type type of post
   * @LevelAPI Platform
   */
  void modifyPost(List<Post> posts, int type);

  /**
   * Removes the {@link Post}.
   * 
   * @param categoryId the category id
   * @param forumId the forum id
   * @param topicId the topic id
   * @param postId the post id
   * @return the post
   * @LevelAPI Platform
   */
  Post removePost(String categoryId, String forumId, String topicId, String postId);

  /**
   * Move post to the the target {@link Topic} 
   * 
   * @param postPaths Paths to move post
   * @param destTopicPath the destination topic path
   * @param isCreatNewTopic New topic or update
   * @param mailContent Content of mail
   * @param link Link to move
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void movePost(String[] postPaths, String destTopicPath, boolean isCreatNewTopic, String mailContent, String link) throws Exception;

  /**
   * Gets the object name by path.
   * 
   * @param path the path
   * @return the object name by path
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  Object getObjectNameByPath(String path) throws Exception;

  /**
   * Gets the object name by path.
   * 
   * @param id Identity to get
   * @param type  The type to get
   * @return the object name by path
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  Object getObjectNameById(String id, String type) throws Exception;

  /**
   * Gets the all link.
   * 
   * @param strQueryCate is a string. It's content have command Query. This function
   *        will return page category suitable to content of that strQueryCate
   * @param strQueryForum is a string. It's content have command Query. This function
   *        will return page forum suitable to content of that strQueryForum
   * @return the all link
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  List<ForumLinkData> getAllLink(String strQueryCate, String strQueryForum) throws Exception;

  /**
   * Gets the forum home path.
   * 
   * @return the forum home path
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  String getForumHomePath() throws Exception;

  /**
   * Puts the tags into specified {@link Topic}
   * 
   * @param tags the list tag is add
   * @param userName UserName
   * @param topicPath the topic path
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void addTag(List<Tag> tags, String userName, String topicPath) throws Exception;

  /**
   * Removes the Tag out the {@link Topic}}
   * 
   * @param tagId the tag id
   * @param userName the user id
   * @param topicPath the topic path
   * @LevelAPI Platform
   */
  void unTag(String tagId, String userName, String topicPath);

  /**
   * Gets the {@link Tag} base on {@link Tag}'s Id
   * 
   * @param tagId the tag id
   * @return the tag
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  Tag getTag(String tagId) throws Exception;

  /**
   * Gets all the {@link Tag} list.
   * 
   * @param strQuery query to get tags
   * @param userAndTopicId input id
   * @return the list names of tags
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  List<String> getAllTagName(String strQuery, String userAndTopicId) throws Exception;

  /**
   * Gets all the tag name in topic.
   * 
   * @param userAndTopicId 'userId,topicId' pattern
   * @return the list names of tags
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  List<String> getTagNameInTopic(String userAndTopicId) throws Exception;

  /**
   * Gets all the {@link Tag} list.
   * 
   * @return the tags
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  List<Tag> getAllTags() throws Exception;

  /**
   * Gets the tags by user.
   * 
   * @param tagIds the list tag id of user tag in topic.
   * @return the tags by user add in topic
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  List<Tag> getMyTagInTopic(String[] tagIds) throws Exception;

  /**
   * Gets the topics by tag.
   * 
   * @param userIdAndtagId the user id and tag id (userId:tagId)
   * @param strOrderBy the topic order by
   * @return the topics by tag of user tag
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  JCRPageList getTopicByMyTag(String userIdAndtagId, String strOrderBy) throws Exception;

  /**
   * Creates new {@link Tag}
   * 
   * @param newTag the new tag
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void saveTag(Tag newTag) throws Exception;

  /**
   * Save user profile.
   * 
   * @param userProfile the user profile
   * @param isOption is the option
   * @param isBan is the ban
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void saveUserProfile(UserProfile userProfile, boolean isOption, boolean isBan) throws Exception;

  /**
   * Save user profile.
   * 
   * @param user user want to update
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void updateUserProfile(User user) throws Exception;

  /**
   * Save user moderator.
   * 
   * @param userName the username of a user
   * @param ids optional categoryId list or forumId list
   * @param isModeCate true: update Category otherwise update Forum 
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void saveUserModerator(String userName, List<String> ids, boolean isModeCate) throws Exception;

  /**
   * Search for user profile
   * 
   * @param userSearch user want to search
   * @return forum page list
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  JCRPageList searchUserProfile(String userSearch) throws Exception;

  /**
   * Gets the user info.
   * 
   * @param userName the user name
   * @return the user info
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  UserProfile getUserInfo(String userName) throws Exception;

  /**
   * Gets all moderators.
   * 
   * @param userName the user name
   * @param isModeCate is category or not
   * @return list of users
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  List<String> getUserModerator(String userName, boolean isModeCate) throws Exception;

  /**
   * Save user bookmark.
   * 
   * @param userName the user name
   * @param bookMark the book mark
   * @param isNew is the new
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void saveUserBookmark(String userName, String bookMark, boolean isNew) throws Exception;

  /**
   * Save to post if this post has an user read
   * 
   * @param userId the user name
   * @param lastReadPostOfForum last post was read
   * @param lastReadPostOfTopic last post was read
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void saveLastPostIdRead(String userId, String[] lastReadPostOfForum, String[] lastReadPostOfTopic) throws Exception;

  /**
   * Save user collap Categories.
   * 
   * @param userName the user name
   * @param categoryId the book mark
   * @param isAdd is the new
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void saveCollapsedCategories(String userName, String categoryId, boolean isAdd) throws Exception;

  /**
   * Gets the page list user profile.
   * 
   * @return the page list user profile
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  JCRPageList getPageListUserProfile() throws Exception;

  /**
   * Gets the quick search.
   * 
   * @param textQuery the text query
   * @param type is type user and type object(forum, topic and post)
   * @param pathQuery the path query
   * @param userId Id of user
   * @param listCateIds category ids
   * @param listForumIds Forum ids
   * @param forumIdsOfModerator the list of forumId witch user searching has role is 'moderator'.   
   * @return the quick search
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  List<ForumSearch> getQuickSearch(String textQuery, String type, String pathQuery, String userId, List<String> listCateIds, List<String> listForumIds, List<String> forumIdsOfModerator) throws Exception;

  /**
   * Gets screen name
   * 
   * @param userName username of user
   * @return screen name
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  String getScreenName(String userName) throws Exception;

  /**
   * Gets the advanced search.
   * 
   * @param eventQuery the event query
   * @param listCateIds List of category
   * @param listForumIds List of forum id
   * @return the advanced search
   * @LevelAPI Platform
   */
  List<ForumSearch> getAdvancedSearch(ForumEventQuery eventQuery, List<String> listCateIds, List<String> listForumIds);

  /**
   * Saves forum statistic.
   * 
   * @param forumStatistic the forum statistic
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void saveForumStatistic(ForumStatistic forumStatistic) throws Exception;

  /**
   * Gets the forum statistic.
   * 
   * @return the forum statistic
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  ForumStatistic getForumStatistic() throws Exception;

  /**
   * Saves forum administration.
   * 
   * @param forumAdministration the forum administration
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void saveForumAdministration(ForumAdministration forumAdministration) throws Exception;

  /**
   * Gets the forum administration.
   * 
   * @return the forum administration
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  ForumAdministration getForumAdministration() throws Exception;

  /**
   * Update informations for topics and posts
   * 
   * @param topicCoutn number of  topics
   * @param postCount number of posts
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void updateStatisticCounts(long topicCoutn, long postCount) throws Exception;

  /**
   * User login.
   * 
   * @param userId the user id
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void userLogin(String userId) throws Exception;

  /**
   * User logout.
   * 
   * @param userId the user id
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void userLogout(String userId) throws Exception;

  /**
   * Checks if is online.
   * 
   * @param userId the user id
   * @return true, if is online
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  boolean isOnline(String userId) throws Exception;

  /**
   * Gets the online users.
   * 
   * @return the online users
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  List<String> getOnlineUsers() throws Exception;

  /**
   * Gets the last login.
   * 
   * @return the last login
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  String getLastLogin() throws Exception;

  /**
   * Gets the private message.
   * 
   * @param userName the user name
   * @param type the type
   * @return the private message
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  JCRPageList getPrivateMessage(String userName, String type) throws Exception;

  /**
   * Gets new private message.
   * 
   * @param userName the user name
   * @return number of private messages
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  long getNewPrivateMessage(String userName) throws Exception;

  /**
   * Saves private message.
   * 
   * @param privateMessage the private message
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void savePrivateMessage(ForumPrivateMessage privateMessage) throws Exception;

  /**
   * Saves read message.
   * 
   * @param messageId the message id
   * @param userName the user name
   * @param type the type
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void saveReadMessage(String messageId, String userName, String type) throws Exception;

  /**
   * Removes the private message.
   * 
   * @param messageId the message id
   * @param userName the user name
   * @param type the type
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void removePrivateMessage(String messageId, String userName, String type) throws Exception;

  /**
   * Get descriptions of forum
   * 
   * @param userId username of an user
   * @return subscription of forum
   * @LevelAPI Platform
   */
  ForumSubscription getForumSubscription(String userId);

  /**
   * Save descriptions of forum
   * 
   * @param forumSubscription informations want to save
   * @param userId username of an user
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void saveForumSubscription(ForumSubscription forumSubscription, String userId) throws Exception;

  /**
   * Adds the watch.
   * 
   * @param watchType the watch type
   * @param path the path
   * @param values the values
   * @param currentUser Current user information
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void addWatch(int watchType, String path, List<String> values, String currentUser) throws Exception;

  /**
   * Remove the watch.
   * 
   * @param watchType the watch type
   * @param path the path
   * @param values the values
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void removeWatch(int watchType, String path, String values) throws Exception;

  /**
   * Get job waiting for moderator.
   * 
   * @param paths the paths
   * @return list of forum
   * @LevelAPI Platform
   */
  List<ForumSearch> getJobWattingForModerator(String[] paths);

  /**
   * Get number of jobs are waiting for moderator.
   * 
   * @param userId username of an user
   * @return number of jobs
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  int getJobWattingForModeratorByUser(String userId) throws Exception;

  /**
   * Get information of message
   * 
   * @param name name of message
   * @return message information
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  SendMessageInfo getMessageInfo(String name) throws Exception;

  /**
   * Get messages are pending
   * 
   * @return pending messages
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  Iterator<SendMessageInfo> getPendingMessages() throws Exception;

  /**
   * Check admin role
   * 
   * @param userName username of an user
   * @return is admin or not
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  boolean isAdminRole(String userName) throws Exception;
  
  /**
   * Check admin role by checking xml config
   * 
   * @param userName username of an user
   * @return is admin or not
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  boolean isAdminRoleConfig(String userName) throws Exception;

  /**
   * Gets recent public posts limited by number post.
   * 
   * @param number is number of post
   * @return the list recent public post.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  List<Post> getNewPosts(int number) throws Exception;
  
  /**
   * Gets recent posts for user and limited by number post.
   * 
   * @param userName is userId for check permission.
   * @param number is number of post
   * @return the list recent post for user.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  List<Post> getRecentPostsForUser(String userName, int number) throws Exception;

  /**
   * Searches  node
   * 
   * @param queryString query
   * @return iterator of nodes
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  NodeIterator search(String queryString) throws Exception;

  /**
   * Evaluates active of users 
   * 
   * @param query input a query
   * @LevelAPI Platform
   */
  void evaluateActiveUsers(String query);

  /**
   * Creates a user profile
   * 
   * @param user saved user
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void createUserProfile(User user) throws Exception;

  /**
   * Updates user access a topic 
   * 
   * @param userId username of an user
   * @param topicId id of a topic
   * @LevelAPI Platform
   */
  void updateTopicAccess(String userId, String topicId);

  /**
   * Updates user access a forum 
   * 
   * @param userId username of an user
   * @param forumId id of a forum
   * @LevelAPI Platform
   */
  void updateForumAccess(String userId, String forumId);

  /**
   * write user access a topic
   * 
   * @since 2.2.11
   */
  void writeReads();

  /**
   * Exports to xml object 
   * 
   * @param categoryId id of category
   * @param forumId id of forum
   * @param objectIds ids
   * @param nodePath path of node
   * @param bos byte array output stream
   * @param isExportAll is export all or not
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  Object exportXML(String categoryId, String forumId, List<String> objectIds, String nodePath, ByteArrayOutputStream bos, boolean isExportAll) throws Exception;

  /**
   * Imports a stream 
   * 
   * @param nodePath path of node
   * @param bis byte array input stream
   * @param typeImport type of import
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void importXML(String nodePath, ByteArrayInputStream bis, int typeImport) throws Exception;

  /**
   * Gets profiles of users
   * 
   * @param userList list of users
   * @return list of profiles
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  List<UserProfile> getQuickProfiles(List<String> userList) throws Exception;

  /**
   * Gets profile of an user
   * 
   * @param userName username
   * @return object user profile
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  UserProfile getQuickProfile(String userName) throws Exception;

  /**
   * Gets more informations of user 
   * 
   * @param userProfile profile of user
   * @return user profile
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  UserProfile getUserInformations(UserProfile userProfile) throws Exception;

  /**
   * Gets default user profile
   * 
   * @param userName username of a user
   * @param ip current ip
   * @return user profile
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  UserProfile getDefaultUserProfile(String userName, String ip) throws Exception;

  /**
   * Updates user profile
   * 
   * @param userProfile input user profile
   * @return user profile
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  UserProfile updateUserProfileSetting(UserProfile userProfile) throws Exception;

  /**
   * Gets bookmarks of user
   * 
   * @param userName username of a user
   * @return bookmarks
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  List<String> getBookmarks(String userName) throws Exception;

  /**
   * Gets user profile
   * 
   * @param userName username of a user
   * @return user profile
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  UserProfile getUserSettingProfile(String userName) throws Exception;

  /**
   * Gets user profile manager
   * 
   * @param userName username of a user
   * @return user profile
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  UserProfile getUserProfileManagement(String userName) throws Exception;

  /**
   * Saves user profile
   * 
   * @param userProfile saved user profile
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void saveUserSettingProfile(UserProfile userProfile) throws Exception;

  /**
   * Updates forum
   * 
   * @param path path to forum
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void updateForum(String path) throws Exception;

  /**
   * Gets list of banded ips
   * 
   * @return list banded ips
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  List<String> getBanList() throws Exception;

  /**
   * Adds ip to ban
   * 
   * @param ip add ip
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  boolean addBanIP(String ip) throws Exception;

  /**
   * Removes banded ip
   * 
   * @param ip removed banded ip
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void removeBan(String ip) throws Exception;

  /**
   * Gets list of band ips in forum
   * 
   * @param forumId id of forum
   * @return list band ips
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  List<String> getForumBanList(String forumId) throws Exception;

  /**
   * Adds ip to list of band ips in forum
   * 
   * @param ip add ip
   * @param forumId id of forum
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  boolean addBanIPForum(String ip, String forumId) throws Exception;

  /**
   * Removes ip from list of band ips in forum
   * 
   * @param ip removed ip
   * @param forumId id of forum
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void removeBanIPForum(String ip, String forumId) throws Exception;

  /**
   * Gets list of posts
   * 
   * @param ip input ip
   * @param strOrderBy input order
   * @return list of posts
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  JCRPageList getListPostsByIP(String ip, String strOrderBy) throws Exception;

  /**
   * Registers listener
   * 
   * @param categoryId id of category
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void registerListenerForCategory(String categoryId) throws Exception;

  /**
   * Removes listener
   * 
   * @param path path of category
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void unRegisterListenerForCategory(String path) throws Exception;

  /**
   * Gets avatar
   * 
   * @param userName username
   * @return avatar
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  ForumAttachment getUserAvatar(String userName) throws Exception;

  /**
   * Saves avatar for user
   * 
   * @param userId username
   * @param fileAttachment avatar
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void saveUserAvatar(String userId, ForumAttachment fileAttachment) throws Exception;

  /**
   * Sets default avatar
   * 
   * @param userName username
   * @LevelAPI Platform
   */
  void setDefaultAvatar(String userName);

  /**
   * Gets watches
   * 
   * @param userId username
   * @return watches by user
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  List<Watch> getWatchByUser(String userId) throws Exception;

  /**
   * Updates watch email addresss for user 
   * 
   * @param listNodeId watch node
   * @param newEmailAdd watch email address
   * @param userId username
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void updateEmailWatch(List<String> listNodeId, String newEmailAdd, String userId) throws Exception;

  /**
   * Gets prune settings
   * @return list of prune settings
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  List<PruneSetting> getAllPruneSetting() throws Exception;

  /**
   * Gets prune setting
   * 
   * @param forumPath path of forum
   * @return prune setting
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  PruneSetting getPruneSetting(String forumPath) throws Exception;

  /**
   * Saves a prune setting
   * 
   * @param pruneSetting input prune setting
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void savePruneSetting(PruneSetting pruneSetting) throws Exception;

  /**
   * Runs prune setting
   * 
   * @param pSetting input prune setting
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void runPrune(PruneSetting pSetting) throws Exception;

  /**
   * Runs prune setting
   * 
   * @param forumPath path of forum
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void runPrune(String forumPath) throws Exception;

  /**
   * Checks prune setting
   * 
   * @param pSetting input prune setting
   * @return prune setting
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  long checkPrune(PruneSetting pSetting) throws Exception;

  /**
   * Gets list of topics
   * 
   * @param categoryId id of category
   * @param forumId id of forum
   * @param string condition
   * @param strOrderBy input order
   * @return topic page list
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  LazyPageList<Topic> getTopicList(String categoryId, String forumId, String string, String strOrderBy, int pageSize) throws Exception;

  /**
   * Gets summaries
   * 
   * @param categoryId id of category
   * @param strQuery query
   * @return page list of forums
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  List<Forum> getForumSummaries(String categoryId, String strQuery) throws Exception;

  /**
   * Updates user profile
   * 
   * @param name username
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  void updateUserProfileInfo(String name) throws Exception;

  /**
   * <p>
   * Adds a new member to the forum. The forum profile is created and statistics
   * updated
   * </p>
   * 
   * @param user user that becomes a new forum member
   * @param profileTemplate user profile template to be used for default settings
   * @throws Exception
   * @LevelAPI Platform
   */
  void addMember(User user, UserProfile profileTemplate) throws Exception;

  /**
   * <p>
   * Removes an existing member from the forum. The forum profile is deleted and
   * statistics updated
   * </p>
   * 
   * @param user user that leaves forum
   * @throws Exception
   * @LevelAPI Platform
   */
  void removeMember(User user) throws Exception;

  /**
   * <p>
   * Updates information of logged in users that records in a queue to statistic and profile
   * </p>
   * 
   * @throws Exception
   * @LevelAPI Platform
   */
  public void updateLoggedinUsers() throws Exception;

  /**
   * Updates when delete an user
   * 
   * @param userName username
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public void calculateDeletedUser(String userName) throws Exception;

  /**
   * Updates data when delete a group
   * 
   * @param groupId the identity of group.
   * @param groupName the name of group
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public void calculateDeletedGroup(String groupId, String groupName) throws Exception;

  /**
   * Creates RSS
   * 
   * @param objectId id of forum
   * @param link the link to get rss
   * @return input stream
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public InputStream createForumRss(String objectId, String link) throws Exception;

  /**
   * Creates RSS of user
   * 
   * @param userId username
   * @param link link of RSS
   * @return input stream
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public InputStream createUserRss(String userId, String link) throws Exception;

  /**
   * Adds listener
   * 
   * @param listener add listener
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public void addListenerPlugin(ForumEventListener listener) throws Exception;
  
  /**
   * Removes user-profile of user login cache in service
   * 
   * @param userName 
   * @throws Exception 
   * @LevelAPI Platform
   */
  public void removeCacheUserProfile(String userName) throws Exception;
  
  /**
   * Defines Mixin type exo:activityInfo for node that means to add exo:activityId property 
   * into Node what is owner created activity via Id
   * 
   * @param ownerId - the Id's Node what is owner created activity
   * @param activityId - the Id's activity created. 
   * @LevelAPI Platform
   * @since 4.0
   */
  public void saveActivityIdForOwnerId(String ownerId, String activityId);

  /**
   * Defines Mixin type exo:activityInfo for node that means to add exo:activityId property 
   * into Node what is owner created activity via patch
   * 
   * @param ownerPath - the Path's Node what is owner created activity
   * @param activityId - the Id's activity created. 
   * @LevelAPI Platform
   * @since 4.0
   */
  public void saveActivityIdForOwnerPath(String ownerPath, String activityId);

  /**
   * Gets value of exo:activityId property in specified node via Id. 
   * If property is not existing then return null.
   * 
   * @param ownerId - the Id's Node what is owner created activity
   * @return String - the Id's activity created. 
   * @LevelAPI Platform
   * @since 4.0
   */
  public String getActivityIdForOwnerId(String ownerId);

  /**
   * Gets value of exo:activityId property in specified node via path. 
   * If property is not existing then return null.
   * 
   * @param ownerPath - the Path's Node what is owner created activity
   * @return String - the Id's activity created. 
   * @LevelAPI Platform
   * @since 4.0
   */
  public String getActivityIdForOwnerPath(String ownerPath);
  
  /**
   * Defines Mixin type exo:activityInfo for node that means to add exo:activityId property 
   * into post's Node what is owner created activity via Id
   * 
   * @param ownerId - the Id's Node what is owner created activity
   * @param commentId - the Id's activity created. 
   * @LevelAPI Platform
   * @since 4.0
   */
  public void saveCommentIdForOwnerId(String ownerId, String commentId);

  /**
   * Defines Mixin type exo:activityInfo for node that means to add exo:activityId property 
   * into post's Node what is owner created activity via patch
   * 
   * @param ownerPath - the Path's Node what is owner created activity
   * @param commentId - the Id's activity created. 
   * @LevelAPI Platform
   * @since 4.0
   */
  public void saveCommentIdForOwnerPath(String ownerPath, String commentId);

  /**
   * Get value of exo:activityId property in specified post's node via Id. 
   * If property is not existing then return null.
   * 
   * @param ownerId - the Id's Node what is owner created activity
   * @return String - the Id's activity created. 
   * @LevelAPI Platform
   * @since 4.0
   */
  public String getCommentIdForOwnerId(String ownerId);

  /**
   * Get value of exo:activityId property in specified post's node via path. 
   * If property is not existing then return null.
   * 
   * @param ownerPath - the Path's Node what is owner created activity
   * @return String - the Id's activity created. 
   * @LevelAPI Platform
   * @since 4.0
   */
  public String getCommentIdForOwnerPath(String ownerPath);
  
}
