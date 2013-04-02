/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
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
 */
package org.exoplatform.faq.service;

import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.faq.service.impl.AnswerEventListener;
import org.exoplatform.forum.common.NotifyInfo;

import com.arjuna.ats.internal.jdbc.drivers.modifiers.list;

public interface FAQService extends FAQServiceLegacy {

  /**
   * Adds the plugin.
   * 
   * @param plugin the plugin
   * 
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public void addPlugin(ComponentPlugin plugin) throws Exception;

  /**
   * Adds plugin to initialize default FAQ data.
   * 
   * @param plugin
   * @throws Exception
   * @LevelAPI Platform
   */
  public void addInitialDataPlugin(InitialDataPlugin plugin) throws Exception;

  /**
   * This method should check exists category or NOT to create new or update exists category
   * <p>
   * This function is used to add new or edit category in list. User will input information of fields need
   * in form add category, so user save then category will persistent in data
   * 
   * @param    parentId is address id of the category parent where user want add sub category
   * when paretId = null so this category is parent category else sub category  
   * @param    cat is properties that user input to interface will save on data
   * @param    isAddNew is true when add new category else update category
   * @return  List parent category or list sub category
   * @see     list category
   * @LevelAPI Platform
   */
  public void saveCategory(String parentId, Category cat, boolean isAddNew);
  
  
  /**
   * Builds category tree of provided id.
   * 
   * @param categoryId
   * @return the CategoryTree
   * @throws Exception
   * @LevelAPI Platform
   */
  CategoryTree buildCategoryTree(String categoryId) throws Exception;

  /**
   * Changes status of view of category.
   * 
   * @param   listCateIds is address ids of the category need to change 
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public void changeStatusCategoryView(List<String> listCateIds) throws Exception;

  /**
   * Removes specific category by provided id.
   * 
   * @param    categoryId is address id of the category need remove 
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public void removeCategory(String categoryId) throws Exception;

  /**
   * Gets category by id.
   * 
   * @param    categoryId is address id of the category so you want get
   * @return  category is id = categoryId
   * @see     current category
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public Category getCategoryById(String categoryId) throws Exception;

  /**
   * Gets all categories.
   * 
   * @return Category list
   * 
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public List<Category> getAllCategories() throws Exception;

  /**
   * Get all categories of specific provided user.
   * 
   * @param user      the name of user
   * 
   * @return Category list
   * 
   * @throws Exception if can't found user
   * @LevelAPI Platform
   */
  public List<String> getListCateIdByModerator(String user) throws Exception;

  /**
   * Gets all sub-categories of a category.
   * 
   * @param categoryId the category id
   * @param faqSetting
   * @param isGetAll
   * @param userView the list of users to view categories.
   * @return Category list
   * 
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public List<Category> getSubCategories(String categoryId, FAQSetting faqSetting, boolean isGetAll, List<String> userView) throws Exception;

  /**
   * Moves a category to another one.
   * 
   * @param categoryId the category id should move
   * @param destCategoryId : category is moved
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public void moveCategory(String categoryId, String destCategoryId) throws Exception;

  /**
   * Saves question after create new question or edit infor of quesiton which is existed.
   * If param <code>isAddNew</code> is <code>true</code> then create new question node
   * and set properties of question object to this node else reset infor for
   * this question node
   * 
   * @param question  the question
   * @param isAddNew  is <code>true</code> if create new question node
   *                  and is <code>false</code> if edit question node
   * @param faqSetting Setting FAQ information
   * @return the question node
   * 
   * @throws Exception if path of question nod not found
   * @LevelAPI Platform
   */
  public Node saveQuestion(Question question, boolean isAddNew, FAQSetting faqSetting) throws Exception;

  /**
   * Deletes question by question's id. Check question if it's existed then remove it
   * 
   * @param questionId  the id of question is deleted
   * 
   * @throws Exception  if question not found
   * @LevelAPI Platform
   */
  public void removeQuestion(String questionId) throws Exception;

  /**
   * Gets a question by id.
   * 
   * @param questionId the question id
   * 
   * @return Question
   * 
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public Question getQuestionById(String questionId) throws Exception;

  /**
   * Gets all questions
   * 
   * 
   * @return List of question
   * 
   * @throws Exception  if attachment not foune
   * @LevelAPI Platform
   */
  public QuestionPageList getAllQuestions() throws Exception;

  /**
   * Gets all questions not yet answered, the first get all questions 
   * which have property response is null (have not yet answer) then
   * convert to list of question object
   * 
   * @param categoryId the id of category
   * @param isApproved Question approved or not
   * @return List of question
   * 
   * @throws Exception  if lost attachment
   * @LevelAPI Platform
   */
  public QuestionPageList getQuestionsNotYetAnswer(String categoryId, boolean isApproved) throws Exception;

  /**
   * Gets questions are activated and approved in the category.
   * The first get category from id which is specified by param <code>categoryId</code>
   * then lookup questions of this category, only question is activated and approved  
   * via category identify, the last convert to list of question object
   * 
   * @param categoryId  the category id
   * @param faqSetting FAQ setting information
   * @return Question list
   * 
   * @throws Exception  if can't found category
   * @LevelAPI Platform
   */
  public QuestionPageList getQuestionsByCatetory(String categoryId, FAQSetting faqSetting) throws Exception;

  /**
   * Gets all questions of the category.
   * The first get category from id which is specified by param <code>categoryId</code>
   * then get all questions of this category and put them into an question page list object
   * 
   * @param categoryId    the id of category
   * @param faqSetting FAQ setting information
   * @return Question page list
   * 
   * @throws Exception    when category not found
   * @LevelAPI Platform
   */
  public QuestionPageList getAllQuestionsByCatetory(String categoryId, FAQSetting faqSetting) throws Exception;

  /**
   * Gets some informations of category: Lookup category node by category's id
   * and count sub-categories and questions are contained in this category.
   * 
   * @param categoryId the category id
   * @param setting FAQ setting information
   * @return              number of sub-categories
   * number of questions
   * number of questions is not approved
   * number of question is have not yet answered
   * 
   * @throws  Exception   if not found category by id
   * if not found question or lose file attach
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public long[] getCategoryInfo(String categoryId, FAQSetting setting) throws Exception;

  /**
   * Gets questions in list categories.
   * <p>
   * With each category in the list categories, if <code>isNotYetAnswer</code>
   * is <code>false</code> get all questoin in this catgory else get questions 
   * which is not yet answered, and put them in to a QuestionPageList object
   * 
   * @param listCategoryId  the list category id
   * @param isNotYetAnswer  is <code>true</code> if get qeustions not yet answered
   *                        is <code>false</code> if want get all questions in list categories
   * 
   * @return Question page list
   * 
   * @throws Exception      the exception
   * @LevelAPI Platform
   */
  public QuestionPageList getQuestionsByListCatetory(List<String> listCategoryId, boolean isNotYetAnswer) throws Exception;

  /**
   * Gets path of question. For example question A is included in category C and C is child of
   * category B, then, this function will be return C > B
   * 
   * @param categoryId  id of category is contain question
   * @return  name of categories
   * @throws Exception
   * @LevelAPI Platform
   */
  public String getCategoryPathOfQuestion(String categoryId) throws Exception;

  /**
   * Gets all language nodes of question node which have id is specified,
   * with each language node get properties of them and set into 
   * QuestionLanguage object. One QuestionLanguage object have properties: 
   * <ul>
   * <li> Language: the name of language
   * <li> Question: content of questions is written by Language
   * <li> Response: content of response is written by Language
   * </ul>
   * 
   * @param questionId  the id of question
   * 
   * @return list languages are support by the question
   * @LevelAPI Platform
   */
  public List<QuestionLanguage> getQuestionLanguages(String questionId);

  /**
   * Moves all of questions to category which have id is  specified.
   * 
   * @param questions the questions
   * @param destCategoryId the dest category id
   * @param questionLink the question link
   * @param faqSetting the FAQSetting
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public void moveQuestions(List<String> questions, String destCategoryId, String questionLink, FAQSetting faqSetting) throws Exception;

  /**
   * Updates FAQ setting information.
   * 
   * @param faqSetting FAQ setting information
   * @param userName name of user who setting
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public void saveFAQSetting(FAQSetting faqSetting, String userName) throws Exception;

  /**
   * Adds watch a category. You have to register your email for whenever there is new question is inserted 
   * in the category or new category then there will  a notification sent to you.
   * 
   * @param    id of category with user want add watch on that category 
   * @param    watch added watch object
   * @throws Exception the exception
   * @LevelAPI Platform
   *  
   */
  public void addWatchCategory(String id, Watch watch) throws Exception;

  /**
   * Deletes watch in one category. 
   * 
   * @param   categoryId is id of current category
   * @param   user user want delete watch 
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public void deleteCategoryWatch(String categoryId, String user) throws Exception;

  /**
   * Un-watches in one category. 
   * 
   * @param   categoryId is id of current category
   * @param   userCurrent is user current then you unwatch
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public void unWatchCategory(String categoryId, String userCurrent) throws Exception;

  /**
   * Un-watches one question. 
   * 
   * @param   questionID is id of current category
   * @param   userCurrent is user current then you un watch
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public void unWatchQuestion(String questionID, String userCurrent) throws Exception;

  /**
   * Searches FAQs by event query.
   * 
   * @param eventQuery
   * @return
   * @throws Exception
   * @LevelAPI Platform
   */
  public List<ObjectSearchResult> getSearchResults(FAQEventQuery eventQuery) throws Exception;

  /**
   * Gets path of category by category id.
   * 
   * @param  categoryId id of category
   * @return list category name is sort(path of this category)
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public List<String> getCategoryPath(String categoryId) throws Exception;

  /**
   * Gets messages to send notify.
   * 
   * @LevelAPI Platform
   */
  public Iterator<NotifyInfo> getPendingMessages();

  /**
   * Adds language for question node, this function only use for Question node, 
   * and language node is added not default.
   * <p>
   * the first, get this language node if it's existed, opposite add new language node.
   * Then set properties for this node: node's name, question's content and
   * response's content.
   * 
   * @param questionNode  add language node for this question node
   * @param language      The QuestionLanguage object which is add for the question node,
   *                      language object have some properties: name, question's content
   *                      and response's content. Property <code>response</code> may be don't need
   *                      setting value if question not yet answered
   * 
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public void addLanguage(Node questionNode, QuestionLanguage language) throws Exception;

  /**
   * Gets setting of user to view data (categories and questions). At first time user come, 
   * system will create setting for user (automatically) base on setting of admin 
   * (Default setting of FAQ system). After that, when user login again, his setting is getted.
   * 
   * @param userName  the name of user
   * @param faqSetting  the setting of user
   * @throws Exception  when can't find user or faqSetting
   * @LevelAPI Platform
   */
  public void getUserSetting(String userName, FAQSetting faqSetting) throws Exception;

  /**
   * Gets informations about message.
   * 
   * @param name  key of message
   * @return informations contain message and email addresses. 
   * @throws Exception
   * @LevelAPI Platform
   */
  public NotifyInfo getMessageInfo(String name) throws Exception;

  /**
   * Checks permission of user.
   * 
   * @param userName  id or user name of user who is checked permission
   * @return  return <code>true</code> if user is admin. The current user is implied if userName is null.
   * @throws Exception
   * @LevelAPI Platform
   */
  public boolean isAdminRole(String userName) throws Exception;

  /**
   * Gets all user is administration.
   * 
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public List<String> getAllFAQAdmin() throws Exception;

  /**
   * Adds the plugin.
   * 
   * @param plugin the plugin
   * 
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public void addRolePlugin(ComponentPlugin plugin) throws Exception;

  /**
   * Adds watch for a question.
   * 
   * @param questionId id of question
   * @param watch contains information of users
   * @param isNew add new or edit 
   * @throws Exception
   * @LevelAPI Platform
   */
  public void addWatchQuestion(String questionId, Watch watch, boolean isNew) throws Exception;

  /** 
   * Saves topic.
   * 
   * @param questionId Question to discuss
   * @param pathDiscuss path to discussion 
   * @throws Exception
   * @LevelAPI Platform
   */
  public void saveTopicIdDiscussQuestion(String questionId, String pathDiscuss) throws Exception;

  /**
   * Gets list of questions that user watches.
   * 
   * @param faqSetting setting of user
   * @param currentUser username
   * @return List of questions 
   * @throws Exception
   * @LevelAPI Platform
   */
  public QuestionPageList getListQuestionsWatch(FAQSetting faqSetting, String currentUser) throws Exception;

  /**
   * Gets category node.
   * 
   * @param categoryId id of category
   * @return node of category 
   * @throws Exception
   * @LevelAPI Platform
   */
  public Node getCategoryNodeById(String categoryId) throws Exception;

  /**
   * Imports data to category.
   * 
   * @param categoryId the id of category
   * @param inputStream the data
   * @param isZip data is zip or not
   * @return informations contain message and email addresses. 
   * @throws Exception
   * @LevelAPI Platform
   */
  public boolean importData(String categoryId, InputStream inputStream, boolean isZip) throws Exception;

  /**
   * Swaps two categories.
   * 
   * @param cateId1 id of category 1
   * @param cateId2 id of category 2 
   * @throws Exception
   * @LevelAPI Platform
   */
  public void swapCategories(String cateId1, String cateId2) throws Exception;

  /**
   * Gets max index of categories.
   * 
   * @param parentId id of parent
   * @return index 
   * @throws Exception
   * @LevelAPI Platform
   */
  public long getMaxindexCategory(String parentId) throws Exception;

  /**
   * Removes an answer.
   * 
   * @param questionId id of question
   * @param answerId id of answer 
   * @throws Exception
   * @LevelAPI Platform
   */
  public void deleteAnswer(String questionId, String answerId) throws Exception;

  /**
   * Removes comment of question.
   * 
   * @param questionId id of question
   * @param commentId id of comment 
   * @throws Exception
   * @LevelAPI Platform
   */
  public void deleteComment(String questionId, String commentId) throws Exception;

  /**
   * Saves an answer.
   * 
   * @param questionId id of question
   * @param answer saved answer
   * @param isNew save new or edit 
   * @throws Exception
   * @LevelAPI Platform
   */
  public void saveAnswer(String questionId, Answer answer, boolean isNew) throws Exception;

  /**
   * Saves comment of question.
   * 
   * @param questionId id of question
   * @param comment saved comment
   * @param isNew save new or edit 
   * @throws Exception
   * @LevelAPI Platform
   */
  public void saveComment(String questionId, Comment comment, boolean isNew) throws Exception;

  /**
   * Gets comment of question.
   * 
   * @param questionId id of question
   * @param commentId id of comment
   * @return comment 
   * @throws Exception
   * @LevelAPI Platform
   */
  public Comment getCommentById(String questionId, String commentId) throws Exception;

  /**
   * Gets answer of question.
   * 
   * @param questionId id of question
   * @param answerid id of answer
   * @return an Answer 
   * @throws Exception
   * @LevelAPI Platform
   */
  public Answer getAnswerById(String questionId, String answerid) throws Exception;

  /**
   * Saves an answer.
   * 
   * @param questionId id of question
   * @param answers saved answers 
   * @throws Exception
   * @LevelAPI Platform
   */
  public void saveAnswer(String questionId, Answer[] answers) throws Exception;

  /**
   * Gets comments of question.
   * 
   * @param questionId id of question
   * @return comment page list 
   * @throws Exception
   * @LevelAPI Platform
   */
  public JCRPageList getPageListComment(String questionId) throws Exception;

  /**
   * Gets answers of question.
   * 
   * @param questionId id of question
   * @param isSortByVote sort by vote
   * @return answers page list 
   * @throws Exception
   * @LevelAPI Platform
   */
  public JCRPageList getPageListAnswer(String questionId, boolean isSortByVote) throws Exception;

  /**
   * Get list questions that user watches
   * @param userId username 
   * @return question page list 
   * @throws Exception
   * @LevelAPI Platform
   */
  public QuestionPageList getWatchedCategoryByUser(String userId) throws Exception;

  /**
   * Gets avatar of user.
   * 
   * @param userName username
   * @return avatar of user 
   * @throws Exception
   * @LevelAPI Platform
   */
  public FileAttachment getUserAvatar(String userName) throws Exception;

  /**
   * Saves avatar of an user.
   * 
   * @param userId username
   * @param fileAttachment avatar of user 
   * @throws Exception
   * @LevelAPI Platform
   */
  public void saveUserAvatar(String userId, FileAttachment fileAttachment) throws Exception;

  /**
   * Checks that user is watching a category.
   * 
   * @param userId username
   * @param cateId id of category
   * @return true if user is watching and false if isn't 
   * @throws Exception
   * @LevelAPI Platform
   */
  public boolean isUserWatched(String userId, String cateId);

  /**
   * Sets default avatar for an user.
   * 
   * @param userName username 
   * @throws Exception
   * @LevelAPI Platform
   */
  public void setDefaultAvatar(String userName) throws Exception;

  /**
   * Gets list pending questions in a category.
   * 
   * @param categoryId id of category
   * @param faqSetting settings
   * @return question page list 
   * @throws Exception
   * @LevelAPI Platform
   */
  public QuestionPageList getPendingQuestionsByCategory(String categoryId, FAQSetting faqSetting) throws Exception;

  /**
   * Exports category to stream.
   * 
   * @param categoryId id of category
   * @param createZipFile create zip file or not
   * @return input stream of category 
   * @throws Exception
   * @LevelAPI Platform
   */
  public InputStream exportData(String categoryId, boolean createZipFile) throws Exception;

  /**
   * Checks a path exist or not.
   * 
   * @param path path need check
   * @return exist or not 
   * @throws Exception
   * @LevelAPI Platform
   */
  public boolean isExisting(String path) throws Exception;

  /**
   * Gets path of Category by id.
   * 
   * @param id category id 
   * @return path of category
   * @throws Exception
   * @LevelAPI Platform
   */
  public String getCategoryPathOf(String id) throws Exception;

  /**
   * Gets titles of questions active.
   * 
   * @param paths  path of questions
   * @return list titles of questions 
   * @throws Exception
   * @LevelAPI Platform
   */
  public Map<String, String> getRelationQuestion(List<String> paths) throws Exception;
  
  /**
   * Gets titles of questions.
   * 
   * @param paths  path of questions
   * @return list titles of questions 
   * @throws Exception
   * @LevelAPI Platform
   */
  public List<String> getQuestionContents(List<String> paths) throws Exception;

  /**
   * Checks moderate answer or not.
   * 
   * @param id id of category
   * @return answer or not 
   * @throws Exception
   * @LevelAPI Platform
   */
  public boolean isModerateAnswer(String id) throws Exception;

  /**
   * Gets question node.
   * 
   * @param path id of question
   * @return question node 
   * @throws Exception
   * @LevelAPI Platform
   */
  public Node getQuestionNodeById(String path) throws Exception;

  /**
   * Gets name of parent category.
   * 
   * @param path id of category
   * @return name of parent category 
   * @throws Exception
   * @LevelAPI Platform
   */
  public String getParentCategoriesName(String path) throws Exception;

  /**
   * Gets email addresses that watch in a category.
   * 
   * @param categoryId id of category
   * @return question page list 
   * @throws Exception
   * @LevelAPI Platform
   */
  public QuestionPageList getListMailInWatch(String categoryId) throws Exception;

  /**
   * Checks user is moderator or not.
   * 
   * @param categoryId id of category
   * @param user username
   * @return user is moderator or not. The current user is implied if user is null.
   * @throws Exception
   * @LevelAPI Platform
   */
  public boolean isCategoryModerator(String categoryId, String user) throws Exception;

  /**
   * Adds language to a question.
   * 
   * @param questionPath patch of question
   * @param language question language 
   * @throws Exception
   * @LevelAPI Platform
   */
  public void addLanguage(String questionPath, QuestionLanguage language) throws Exception;

  /**
   * Deletes language in a answer.
   * 
   * @param questionPath path of question
   * @param answerId id of answer
   * @param language deleted language 
   * @throws Exception
   * @LevelAPI Platform
   */
  public void deleteAnswerQuestionLang(String questionPath, String answerId, String language) throws Exception;

  /**
   * Deletes language in a comment.
   * 
   * @param questionPath path of question
   * @param commentId id of comment
   * @param language deleted language 
   * @param isPromoted true in case a comment is promoted to an answer
   * @throws Exception
   * @LevelAPI Platform
   */
  public void deleteCommentQuestionLang(String questionPath, String commentId, String language, boolean isPromoted) throws Exception;

  /**
   * Gets language of question.
   * 
   * @param questionPath path of question
   * @param language got language
   * @return Language of question 
   * @throws Exception
   * @LevelAPI Platform
   */
  public QuestionLanguage getQuestionLanguageByLanguage(String questionPath, String language) throws Exception;

  /**
   * Gets Comment of question.
   * 
   * @param questionPath path of question
   * @param commentId id of comment
   * @param language
   * @return comment of question 
   * @throws Exception
   * @LevelAPI Platform
   */
  public Comment getCommentById(String questionPath, String commentId, String language) throws Exception;

  /**
   * Gets answer object.
   * 
   * @param questionPath path of question
   * @param answerid id of answer
   * @param language  
   * @return answer has inputed id  
   * @throws Exception
   * @LevelAPI Platform
   */
  public Answer getAnswerById(String questionPath, String answerid, String language) throws Exception;

  /**
   * Saves an answer of question.
   * 
   * @param questionPath path of question
   * @param answer object answer want to save
   * @param languge language of answer 
   * @throws Exception
   * @LevelAPI Platform
   */
  public void saveAnswer(String questionPath, Answer answer, String languge) throws Exception;

  /**
   * Saves an answer of question.
   * 
   * @param questionPath path of question
   * @param questionLanguage language of answer 
   * @throws Exception
   * @LevelAPI Platform
   */
  public void saveAnswer(String questionPath, QuestionLanguage questionLanguage) throws Exception;

  /**
   * Saves comment of a question.
   * 
   * @param questionPath path of question
   * @param comment comment want to save
   * @param languge language of comment 
   * @throws Exception
   * @LevelAPI Platform
   */
  public void saveComment(String questionPath, Comment comment, String languge) throws Exception;

  /**
   * Removes languages from question.
   * 
   * @param questionPath path of question
   * @param listLanguage removed languages 
   * @throws Exception
   * @LevelAPI Platform
   */
  public void removeLanguage(String questionPath, List<String> listLanguage);

  /**
   * Votes for an answer.
   * 
   * @param answerPath path of answer
   * @param userName username of user vote for answer
   * @param isUp up or not 
   * @throws Exception
   * @LevelAPI Platform
   */
  public void voteAnswer(String answerPath, String userName, boolean isUp) throws Exception;

  /**
   * Votes for a question.
   * 
   * @param questionPath path of question
   * @param userName username of user vote for question
   * @param number value user vote 
   * @throws Exception
   * @LevelAPI Platform
   */
  public void voteQuestion(String questionPath, String userName, int number) throws Exception;

  /**
   * Gets moderators of question or category.
   * 
   * @param path path of question or category
   * @return array users are moderator 
   * @throws Exception
   * @LevelAPI Platform
   */
  public String[] getModeratorsOf(String path) throws Exception;

  /**
   * Removes vote for question.
   * 
   * @param questionPath path of question
   * @param userName username remove vote 
   * @throws Exception
   * @LevelAPI Platform
   */
  public void unVoteQuestion(String questionPath, String userName) throws Exception;

  /**
   * Checks view author information or not.
   * 
   * @param id id of question
   * @return is view author information or not
   * @LevelAPI Platform
   */
  public boolean isViewAuthorInfo(String id);

  /**
   * Gets number of categories.
   * 
   * @return number of categories 
   * @throws Exception
   * @LevelAPI Platform
   */
  public long existingCategories() throws Exception;

  /**
   * Gets name of category.
   * 
   * @param categoryPath path of category
   * @return name of category
   * @throws Exception
   * @LevelAPI Platform
   */
  public String getCategoryNameOf(String categoryPath) throws Exception;

  /**
   * Gets quick questions.
   * 
   * @param listCategoryId id of some categories
   * @param isNotYetAnswer is answer or not
   * @return list of questions 
   * @throws Exception
   * @LevelAPI Platform
   */
  public List<Question> getQuickQuestionsByListCatetory(List<String> listCategoryId, boolean isNotYetAnswer) throws Exception;

  /**
   * Gets list of categories.
   * 
   * @return list of categories 
   * @throws Exception
   * @LevelAPI Platform
   */
  public List<Cate> listingCategoryTree() throws Exception;

  /**
   * Gets list of watches.
   * 
   * @param categoryId id of category
   * @return list of watches in a category 
   * @throws Exception
   * @LevelAPI Platform
   */
  public List<Watch> getWatchByCategory(String categoryId) throws Exception;

  /**
   * Gets information has watch or not.
   * 
   * @param categoryPath path of category
   * @return has watch or has not 
   * @throws Exception
   * @LevelAPI Platform
   */
  public boolean hasWatch(String categoryPath);

  /**
   * Gets informations about category.
   * 
   * @param categoryPath path of category
   * @param categoryIdScoped list sub of category
   * @return informations in CategoryInfo object 
   * @throws Exception
   * @LevelAPI Platform
   */
  public CategoryInfo getCategoryInfo(String categoryPath, List<String> categoryIdScoped) throws Exception;

  /**
   * Gets template of questions.
   * 
   * @return template 
   * @throws Exception
   * @LevelAPI Platform
   */
  public byte[] getTemplate() throws Exception;

  /**
   * Saves a template.
   * 
   * @param str template 
   * @throws Exception
   * @LevelAPI Platform
   */
  public void saveTemplate(String str) throws Exception;

  /**
   * Checks category is exist or not.
   * 
   * @param name name of category
   * @param path path of category
   * @return is exist or not 
   * @throws Exception
   * @LevelAPI Platform
   */
  public boolean isCategoryExist(String name, String path);

  /**
   * Updates relatives for a question.
   * 
   * @param questionPath path of question
   * @param relatives input relatives 
   * @throws Exception
   * @LevelAPI Platform
   */
  public void updateQuestionRelatives(String questionPath, String[] relatives) throws Exception;

  /**
   * Checks question has moderator or not.
   * 
   * @param id id of question
   * @return is moderate or not 
   * @throws Exception
   * @LevelAPI Platform
   */
  public boolean isModerateQuestion(String id) throws Exception;

  /**
   * Creates RSS for answer.
   * 
   * @param cateId id of category
   * @return stream of answer rss 
   * @throws Exception
   * @LevelAPI Platform
   */
  public InputStream createAnswerRSS(String cateId) throws Exception;

  /**
   * Saves last active information of question.
   *  
   * @param absPathOfItem path of question 
   * @throws Exception
   * @LevelAPI Platform
   */
  public void reCalculateLastActivityOfQuestion(String absPathOfItem) throws Exception;

  /**
   * Adds listener for answer.
   *  
   * @param listener answer event listener 
   * @throws Exception
   * @LevelAPI Platform
   */
  public void addListenerPlugin(AnswerEventListener listener) throws Exception;
  
  /**
   * Removes listener for answer.
   *  
   * @param listener answer event listener 
   * @throws Exception
   * @LevelAPI Platform
   */
  public void removeListenerPlugin(AnswerEventListener listener) throws Exception;

  /**
   * Gets comments of a question.
   *  
   * @param questionId id of question
   * @return comments of question 
   * @throws Exception
   * @LevelAPI Platform
   */
  public Comment[] getComments(String questionId) throws Exception;
  
  /**
   * Calculates deleted user.
   * 
   * @param userName
   * @throws Exception
   */
  public void calculateDeletedUser(String userName) throws Exception;
  
  /**
   * Reads  property of the category by its name.
   * 
   * @param categoryId id of the category
   * @param propertyName name of the property
   * @param returnType expected return-type. The supported class are String[], String, Long, Boolean, Double and Date .  
   * @return 
   * @throws Exception
   * @LevelAPI Platform
   */
  public Object readCategoryProperty(String categoryId, String propertyName, Class returnType) throws Exception;
  
  /**
   * Reads property of the question by its name.
   * 
   * @param questionId id of the question
   * @param propertyName name of the property
   * @param returnType expected return-type. The supported class are String[], String, Long, Boolean, Double and Date.
   * @return
   * @throws Exception
   * @LevelAPI Platform
   */
  public Object readQuestionProperty(String questionId, String propertyName, Class returnType) throws Exception;
  
  /**
   * Defines Mixin type exo:activityInfo for question node that means to add exo:activityId property 
   * into Node what is owner created activity via patch
   * 
   * @param ownerPath - the Path's Node what is owner created activity
   * @param activityId - the Id's activity created. 
   * @LevelAPI Platform
   * @since 4.0
   */
  public void saveActivityIdForQuestion(String ownerPath, String activityId);

  /**
   * Gets value of exo:activityId property in question node via path. 
   * If property is not existing then return null.
   * 
   * @param ownerPath - the Path's Node what is owner created activity
   * @return String - the Id's activity created.
   * @LevelAPI Platform 
   * @since 4.0
   */
  public String getActivityIdForQuestion(String ownerPath);
  
  /**
   * Defines Mixin type exo:activityInfo for answer node that means to add exo:activityId property 
   * into Node what is owner created activity via patch
   * 
   * @param ownerPath - the Path's Node what is owner created activity
   * @param answer
   * @param activityId - the Id's activity created. 
   * @LevelAPI Platform
   * @since 4.0
   */
  public void saveActivityIdForAnswer(String ownerPath, Answer answer, String activityId);

  /**
   * Gets value of exo:activityId property in answer node via path. 
   * If property is not existing then return null.
   * 
   * @param ownerPath - the Path's Node what is owner created activity
   * @param answer
   * @return String - the Id's activity created. 
   * @LevelAPI Platform
   * @since 4.0
   */
  public String getActivityIdForAnswer(String ownerPath, Answer answer);
  
  /**
   * Defines Mixin type exo:activityInfo for comment node that means to add exo:activityId property 
   * into Node what is owner created activity base on provided path.
   * 
   * @param ownerPath - the Path's Node what is owner created activity
   * @param commentId - the id of comment posted for the question
   * @param language - the language of comment
   * @param activityId - the Id's activity created. 
   * @LevelAPI Platform
   * @since 4.0
   */
  public void saveActivityIdForComment(String ownerPath, String commentId, String language, String activityId);

  /**
   * Gets value of exo:activityId property in comment node base on provided path. 
   * If property is not existing then return null.
   * 
   * @param ownerPath - the Path's Node what is owner created activity
   * @param commentId - the id of comment posted for the question
   * @param language - the language of comment
   * @return String - the Id's activity created. 
   * @LevelAPI Platform
   * @since 4.0
   */
  public String getActivityIdForComment(String ownerPath, String commentId, String language);
}