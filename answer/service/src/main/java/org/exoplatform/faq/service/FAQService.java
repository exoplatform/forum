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

/**
 * APIs for FAQ and all operations of other related objects (Categories, Questions and Answers).
 */
public interface FAQService extends FAQServiceLegacy {

  /**
   * Adds a plugin to the list of FAQ plugins.
   * 
   * @param plugin The plugin to be added.
   * 
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public void addPlugin(ComponentPlugin plugin) throws Exception;

  /**
   * Adds a plugin which initializes the default FAQ data.
   * 
   * @param plugin The plugin to be added.
   * @throws Exception
   * @LevelAPI Platform
   */
  public void addInitialDataPlugin(InitialDataPlugin plugin) throws Exception;

  /**
   * Checks if a category exists or not. If "true", the new category is created. If "false", the category will be updated.
   * <p>
   * This function is used to add new or update the category. The user will input information into required fields 
   * of the Add Category form, then save the category into database.
   * 
   * @param    parentId The address Id of the parent category where the user wants to add a sub-category.
   * When paretId is "null", this category does not contain any sub-categories.
   * @param    cat The category information provided by the user.
   * @param    isAddNew If "true", the new category is added. If "false", the category is updated.
   * @see     list category
   * @LevelAPI Platform
   */
  public void saveCategory(String parentId, Category cat, boolean isAddNew);
  
  
  /**
   * Builds a category tree by its provided Id.
   * 
   * @param categoryId Id of the category.
   * @return The category tree.
   * @throws Exception
   * @LevelAPI Platform
   */
  CategoryTree buildCategoryTree(String categoryId) throws Exception;

  /**
   * Changes status of a category to "hidden" or "shown".
   * 
   * @param   listCateIds Ids of a category which is changed.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public void changeStatusCategoryView(List<String> listCateIds) throws Exception;

  /**
   * Removes a specific category by its provided Id.
   * 
   * @param categoryId Id of the category which is removed. 
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public void removeCategory(String categoryId) throws Exception;

  /**
   * Gets a category by its Id.
   * 
   * @param categoryId Id of the category.
   * @return  The category.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public Category getCategoryById(String categoryId) throws Exception;

  /**
   * Gets all categories.
   * 
   * @return Categories.
   * 
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public List<Category> getAllCategories() throws Exception;

  /**
   * Get all categories of a given user.
   * 
   * @param user Name of the user.
   * 
   * @return Categories.
   * 
   * @throws Exception if the given user is not found.
   * @LevelAPI Platform
   */
  public List<String> getListCateIdByModerator(String user) throws Exception;

  /**
   * Gets all sub-categories of a category.
   * 
   * @param categoryId Id of the category.
   * @param faqSetting Settings of FAQ.
   * @param isGetAll Deprecated. 
   * @param userView Users who have the view permission on sub-categories of a category.
   * @return Sub-categories.
   * 
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public List<Category> getSubCategories(String categoryId, FAQSetting faqSetting, boolean isGetAll, List<String> userView) throws Exception;

  /**
   * Moves a category to another one.
   * 
   * @param categoryId Id of the category which is moved.
   * @param destCategoryId Id of the destination category.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public void moveCategory(String categoryId, String destCategoryId) throws Exception;

  /**
   * Saves information for a question which is added or updated.
   * 
   * @param question The question to be added or updated.
   * @param isAddNew If "true", the new question node is added. If "false", the question is updated.
   * @param faqSetting Settings of FAQ.
   * @return The question node.
   * 
   * @throws Exception if the question node's path is not found.
   * @LevelAPI Platform
   */
  public Node saveQuestion(Question question, boolean isAddNew, FAQSetting faqSetting) throws Exception;

  /**
   * Deletes a question by its Id.
   * 
   * @param questionId Id of the question which is deleted.
   * 
   * @throws Exception if the question is not found.
   * @LevelAPI Platform
   */
  public void removeQuestion(String questionId) throws Exception;

  /**
   * Gets a question by its  Id.
   * 
   * @param questionId Id of the question
   * 
   * @return The question
   * 
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public Question getQuestionById(String questionId) throws Exception;

  /**
   * Gets all questions.
   * 
   * 
   * @return Questions.
   * 
   * @throws Exception if the attachment is not found.
   * @LevelAPI Platform
   */
  public QuestionPageList getAllQuestions() throws Exception;

  /**
   * Gets all questions which are not yet answered.
   * 
   * @param categoryId Id of the category which contains unanswered questions.
   * @param isApproved If "true", only approved questions are got.
   * @return Questions.
   * 
   * @throws Exception if the attachment is lost.
   * @LevelAPI Platform
   */
  public QuestionPageList getQuestionsNotYetAnswer(String categoryId, boolean isApproved) throws Exception;

  /**
   * Gets activated and approved questions of a category by its Id.
   * 
   * @param categoryId  Id of the category.
   * @param faqSetting Settings of FAQ.
   * @return Questions.
   * 
   * @throws Exception if the category is not found.
   * @LevelAPI Platform
   */
  public QuestionPageList getQuestionsByCatetory(String categoryId, FAQSetting faqSetting) throws Exception;

  /**
   * Gets all questions of a category by its Id.
   * 
   * @param categoryId Id of the category.
   * @param faqSetting Settings of FAQ.
   * @return Questions.
   * 
   * @throws Exception when the category is not found.
   * @LevelAPI Platform
   */
  public QuestionPageList getAllQuestionsByCatetory(String categoryId, FAQSetting faqSetting) throws Exception;

  /**
   * Gets some information of a category by its Id, 
   * including the number of its sub-categories, total of questions,
   * the number of questions which are unapproved and not yet answered.
   * 
   * @param categoryId Id of the category.
   * @param setting Settings of FAQ.
   * @return The number of sub-categories; 
   * The number of questions; 
   * The number of unapproved questions;
   * The number of questions which are not yet answered;
   * 
   * @throws Exception if the category or question is not found by its Id, or the attached file is lost.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public long[] getCategoryInfo(String categoryId, FAQSetting setting) throws Exception;

  /**
   * Gets all questions from a list of categories by its Id.
   * 
   * @param listCategoryId  Id of the categories list.
   * @param isNotYetAnswer  If "true", all questions which are not yet answered are got. 
   * If "false", all questions are got.
   * 
   * @return Questions at the page list.
   * 
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public QuestionPageList getQuestionsByListCatetory(List<String> listCategoryId, boolean isNotYetAnswer) throws Exception;

  /**
   * Gets a question path.
   * 
   * @param categoryId Id of the category containing the question.
   * @return The category path.
   * @throws Exception
   * @LevelAPI Platform
   */
  public String getCategoryPathOfQuestion(String categoryId) throws Exception;

  /**
   * Gets all languages of a question by its Id.
   * 
   * @param questionId Id of the question.
   * 
   * @return Languages.
   * @LevelAPI Platform
   */
  public List<QuestionLanguage> getQuestionLanguages(String questionId);

  /**
   * Moves questions to a category.
   * 
   * @param questions The questions to be moved.
   * @param destCategoryId Id of the destination category.
   * @param questionLink URL of the question.
   * @param faqSetting Settings of FAQ.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public void moveQuestions(List<String> questions, String destCategoryId, String questionLink, FAQSetting faqSetting) throws Exception;

  /**
   * Updates the settings information of FAQ.
   * 
   * @param faqSetting Settings of FAQ.
   * @param userName Name of the user for whom the FAQ settings are updated.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public void saveFAQSetting(FAQSetting faqSetting, String userName) throws Exception;

  /**
   * Adds a watch to a category. The notifications will be sent to your registered email when any new questions are added to the watched category.
   * 
   * @param id Id of the category which is watched.
   * @param watch The user who watches the category.
   * @throws Exception the exception
   * @LevelAPI Platform
   *  
   */
  public void addWatchCategory(String id, Watch watch) throws Exception;

  /**
   * Deletes a watch from a category. 
   * 
   * @param categoryId Id of the category.
   * @param user The user whose watch is removed by himself or someone else.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public void deleteCategoryWatch(String categoryId, String user) throws Exception;

  /**
   * Unwatches a category. 
   * 
   * @param categoryId Id of the category which is unwatched.
   * @param userCurrent The user whose watch is removed by himself or someone else.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public void unWatchCategory(String categoryId, String userCurrent) throws Exception;

  /**
   * Unwatches a question. 
   * 
   * @param questionID Id of the question which is unwatched.
   * @param userCurrent The user whose watch is removed by himself or someone else.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public void unWatchQuestion(String questionID, String userCurrent) throws Exception;

  /**
   * Searches for information in FAQ.
   * 
   * @param eventQuery The Search condition.
   * @return The FAQ information matching with the search condition.
   * @throws Exception
   * @LevelAPI Platform
   */
  public List<ObjectSearchResult> getSearchResults(FAQEventQuery eventQuery) throws Exception;

  /**
   * Gets paths of a category by its Id.
   * 
   * @param  categoryId Id of the category.
   * @return Paths of the category and of its sub-categories.
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public List<String> getCategoryPath(String categoryId) throws Exception;

  /**
   * Gets messages which are not yet sent.
   * 
   * @LevelAPI Platform
   */
  public Iterator<NotifyInfo> getPendingMessages();

  /**
   * Adds a language to a question. The added language is never set as default.
   * <p>
   * This method checks if a language node exists or not. 
   * If the language node does not exist, it will be added.
   * Next, this method sets properties for the language node: name, content of question and response.
   * 
   * @param questionNode The question to which a language is added.
   * @param language Information of the added language.
   * 
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public void addLanguage(Node questionNode, QuestionLanguage language) throws Exception;

  /**
   * Gets settings of a user that are used for displaying categories and questions. When the user signs up, 
   * the system will automatically create settings based on settings of administrator.
   * 
   * @param userName Name of the user.
   * @param faqSetting Settings of FAQ.
   * @throws Exception when the user or FAQ settings is/are not found.
   * @LevelAPI Platform
   */
  public void getUserSetting(String userName, FAQSetting faqSetting) throws Exception;

  /**
   * Gets information of a message.
   * 
   * @param name Key to get the message.
   * @return Information which contains message and email addresses. 
   * @throws Exception
   * @LevelAPI Platform
   */
  public NotifyInfo getMessageInfo(String name) throws Exception;

  /**
   * Checks if a user is administrator or not.
   * 
   * @param userName Name of the checked user.
   * @return The "true" value is returned if the user is admin.
   * @throws Exception
   * @LevelAPI Platform
   */
  public boolean isAdminRole(String userName) throws Exception;

  /**
   * Gets all users who are administrators of FAQ.
   * 
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public List<String> getAllFAQAdmin() throws Exception;

  /**
   * Adds a plugin which defines the "administrator" role.
   * 
   * @param plugin The plugin to be added.
   * 
   * @throws Exception the exception
   * @LevelAPI Platform
   */
  public void addRolePlugin(ComponentPlugin plugin) throws Exception;

  /**
   * Adds a watch to a question.
   * 
   * @param questionId Id of the question.
   * @param watch Information of the user who watches the question.
   * @param isNew If "true", a new watch is added. If "false", the watch is edited.
   * @throws Exception
   * @LevelAPI Platform
   */
  public void addWatchQuestion(String questionId, Watch watch, boolean isNew) throws Exception;

  /** 
   * Saves a topic.
   * 
   * @param questionId Id of the question which is discussed in the topic.
   * @param pathDiscuss Path to the discussion.
   * @throws Exception
   * @LevelAPI Platform
   */
  public void saveTopicIdDiscussQuestion(String questionId, String pathDiscuss) throws Exception;

  /**
   * Gets a list of questions watched by a user.
   * 
   * @param faqSetting Settings of FAQ.
   * @param currentUser Name of the user who watches questions.
   * @return Questions.
   * @throws Exception
   * @LevelAPI Platform
   */
  public QuestionPageList getListQuestionsWatch(FAQSetting faqSetting, String currentUser) throws Exception;

  /**
   * Gets a category by its Id.
   * 
   * @param categoryId Id of the category.
   * @return The category.
   * @throws Exception
   * @LevelAPI Platform
   */
  public Node getCategoryNodeById(String categoryId) throws Exception;

  /**
   * Imports data to a category.
   * 
   * @param categoryId Id of the category.
   * @param inputStream The data.
   * @param isZip Data is zip-typed or not.
   * @return If the import is successful, the returned value is "true". If not, the returned value is "false".
   * @throws Exception
   * @LevelAPI Platform
   */
  public boolean importData(String categoryId, InputStream inputStream, boolean isZip) throws Exception;

  /**
   * Swaps two categories.
   * 
   * @param cateId1 Id of the Category 1.
   * @param cateId2 Id of the Category 2.
   * @throws Exception
   * @LevelAPI Platform
   */
  public void swapCategories(String cateId1, String cateId2) throws Exception;

  /**
   * Gets the maximum index of categories.
   * 
   * @param parentId Id of the parent category.
   * @return An index value.
   * @throws Exception
   * @LevelAPI Platform
   */
  public long getMaxindexCategory(String parentId) throws Exception;

  /**
   * Removes an answer.
   * 
   * @param questionId Id of the question whose answer is removed.
   * @param answerId Id of the answer.
   * @throws Exception
   * @LevelAPI Platform
   */
  public void deleteAnswer(String questionId, String answerId) throws Exception;

  /**
   * Removes a question's comment.
   * 
   * @param questionId Id of the question whose comment is removed.
   * @param commentId Id of the comment.
   * @throws Exception
   * @LevelAPI Platform
   */
  public void deleteComment(String questionId, String commentId) throws Exception;

  /**
   * Saves an answer.
   * 
   * @param questionId Id of the question.
   * @param answer The answer to be saved.
   * @param isNew If "true", the new answer is added. If "false", the answer is updated.
   * @throws Exception
   * @LevelAPI Platform
   */
  public void saveAnswer(String questionId, Answer answer, boolean isNew) throws Exception;

  /**
   * Saves a question's comment.
   * 
   * @param questionId Id of the question.
   * @param comment The saved comment.
   * @param isNew If "true", the new answer is added. If "false", the answer is updated.
   * @throws Exception
   * @LevelAPI Platform
   */
  public void saveComment(String questionId, Comment comment, boolean isNew) throws Exception;

  /**
   * Gets a question's comment.
   * 
   * @param questionId Id of the question.
   * @param commentId Id of the comment.
   * @return A comment.
   * @throws Exception
   * @LevelAPI Platform
   */
  public Comment getCommentById(String questionId, String commentId) throws Exception;

  /**
   * Gets a question's answer.
   * 
   * @param questionId Id of the question.
   * @param answerid Id of the answer.
   * @return The answer.
   * @throws Exception
   * @LevelAPI Platform
   */
  public Answer getAnswerById(String questionId, String answerid) throws Exception;

  /**
   * Saves answers.
   * 
   * @param questionId Id of the question which contains the saved answers.
   * @param answers The saved answers.
   * @throws Exception
   * @LevelAPI Platform
   */
  public void saveAnswer(String questionId, Answer[] answers) throws Exception;

  /**
   * Gets comments of a question.
   * 
   * @param questionId Id of the question.
   * @return Comments.
   * @throws Exception
   * @LevelAPI Platform
   */
  public JCRPageList getPageListComment(String questionId) throws Exception;

  /**
   * Gets answers of a question.
   * 
   * @param questionId Id of the question.
   * @param isSortByVote If "true", the returned answers will be sorted by vote.
   * @return Answers.
   * @throws Exception
   * @LevelAPI Platform
   */
  public JCRPageList getPageListAnswer(String questionId, boolean isSortByVote) throws Exception;

  /**
   * Gets a list of questions watched by a user.
   * @param userId Name of the user who watches questions.
   * @return Questions.
   * @throws Exception
   * @LevelAPI Platform
   */
  public QuestionPageList getWatchedCategoryByUser(String userId) throws Exception;

  /**
   * Gets a user's avatar.
   * 
   * @param userName Name of the user.
   * @return The avatar
   * @throws Exception
   * @LevelAPI Platform
   */
  public FileAttachment getUserAvatar(String userName) throws Exception;

  /**
   * Saves an avatar of the user.
   * 
   * @param userId Name of the user.
   * @param fileAttachment Avatar of the user. 
   * @throws Exception
   * @LevelAPI Platform
   */
  public void saveUserAvatar(String userId, FileAttachment fileAttachment) throws Exception;

  /**
   * Checks if a user is watching a category or not.
   * 
   * @param userId Name of the user.
   * @param cateId Id of the category.
   * @return The returned value is "true" if the user is watching the category. 
   * @throws Exception
   * @LevelAPI Platform
   */
  public boolean isUserWatched(String userId, String cateId);

  /**
   * Sets a default avatar for a user.
   * 
   * @param userName Name of the user.
   * @throws Exception
   * @LevelAPI Platform
   */
  public void setDefaultAvatar(String userName) throws Exception;

  /**
   * Gets a list of pending questions in a category.
   * 
   * @param categoryId Id of the category.
   * @param faqSetting Settings of FAQ.
   * @return Pending questions.
   * @throws Exception
   * @LevelAPI Platform
   */
  public QuestionPageList getPendingQuestionsByCategory(String categoryId, FAQSetting faqSetting) throws Exception;

  /**
   * Exports a category.
   * 
   * @param categoryId Id of the exported category.
   * @param createZipFile The category is exported into a zip file or not.
   * @return The exported data.
   * @throws Exception
   * @LevelAPI Platform
   */
  public InputStream exportData(String categoryId, boolean createZipFile) throws Exception;

  /**
   * Checks if a path exists or not.
   * 
   * @param path The path to be checked.
   * @return The returned value is "true" if the path has already existed.
   * @throws Exception
   * @LevelAPI Platform
   */
  public boolean isExisting(String path) throws Exception;

  /**
   * Gets a category path by its Id.
   * 
   * @param id Id of the category. 
   * @return The category path.
   * @throws Exception
   * @LevelAPI Platform
   */
  public String getCategoryPathOf(String id) throws Exception;

  /**
   * Gets titles of questions, then maps them to their paths.
   * 
   * @param paths Paths to the questions.
   * @return Titles of questions which are mapped to their paths.
   * @throws Exception
   * @LevelAPI Platform
   */
  public Map<String, String> getRelationQuestion(List<String> paths) throws Exception;
  
  /**
   * Gets titles of questions.
   * 
   * @param paths Paths to the questions.
   * @return The titles of questions.
   * @throws Exception
   * @LevelAPI Platform
   */
  public List<String> getQuestionContents(List<String> paths) throws Exception;

  /**
   * Checks if moderation is applied to answers of a category or not.
   * 
   * @param id Id of the category.
   * @return The returned value is "true" if moderation is enabled.
   * @throws Exception
   * @LevelAPI Platform
   */
  public boolean isModerateAnswer(String id) throws Exception;

  /**
   * Gets a question by its Id.
   * 
   * @param path Path to the question.
   * @return The question. 
   * @throws Exception
   * @LevelAPI Platform
   */
  public Node getQuestionNodeById(String path) throws Exception;

  /**
   * Gets name of a parent category.
   * 
   * @param path Path to the parent category.
   * @return Name of the parent category.
   * @throws Exception
   * @LevelAPI Platform
   */
  public String getParentCategoriesName(String path) throws Exception;

  /**
   * Gets email addresses that are watched in a category.
   * 
   * @param categoryId Id of the category.
   * @return The email addresses.
   * @throws Exception
   * @LevelAPI Platform
   */
  public QuestionPageList getListMailInWatch(String categoryId) throws Exception;

  /**
   * Checks if a user is moderator of a category or not.
   * 
   * @param categoryId Id of the category.
   * @param user Name of the user.
   * @return The returned value is "true" if the user is moderator of the category.
   * @throws Exception
   * @LevelAPI Platform
   */
  public boolean isCategoryModerator(String categoryId, String user) throws Exception;

  /**
   * Adds a language to a question.
   * 
   * @param questionPath Path to the question.
   * @param language Information of the added language.
   * @throws Exception
   * @LevelAPI Platform
   */
  public void addLanguage(String questionPath, QuestionLanguage language) throws Exception;

  /**
   * Deletes an answer's language.
   * 
   * @param questionPath Path to the question.
   * @param answerId Id of the answer.
   * @param language The language to be deleted. 
   * @throws Exception
   * @LevelAPI Platform
   */
  public void deleteAnswerQuestionLang(String questionPath, String answerId, String language) throws Exception;

  /**
   * Deletes a comment's language.
   * 
   * @param questionPath Path to the question.
   * @param commentId Id of the comment.
   * @param language The language to be deleted.
   * @param isPromoted If "true", the comment is promoted to an answer.
   * @throws Exception
   * @LevelAPI Platform
   */
  public void deleteCommentQuestionLang(String questionPath, String commentId, String language, boolean isPromoted) throws Exception;

  /**
   * Gets a question's language.
   * 
   * @param questionPath Path to the question.
   * @param language Type of the language.
   * @return Language of the question.
   * @throws Exception
   * @LevelAPI Platform
   */
  public QuestionLanguage getQuestionLanguageByLanguage(String questionPath, String language) throws Exception;

  /**
   * Gets a question's comment by the question path and comment Id.
   * 
   * @param questionPath Path to the question.
   * @param commentId Id of the comment.
   * @param language Type of the language.
   * @return A question's comment. 
   * @throws Exception
   * @LevelAPI Platform
   */
  public Comment getCommentById(String questionPath, String commentId, String language) throws Exception;

  /**
   * Gets an answer by its Id.
   * 
   * @param questionPath Path to the question.
   * @param answerid Id of the answer.
   * @param language Type of the language.
   * @return The answer.
   * @throws Exception
   * @LevelAPI Platform
   */
  public Answer getAnswerById(String questionPath, String answerid, String language) throws Exception;

  /**
   * Saves an answer.
   * 
   * @param questionPath Path to the question.
   * @param answer The answer to be saved.
   * @param languge The language type of the saved answer.
   * @throws Exception
   * @LevelAPI Platform
   */
  public void saveAnswer(String questionPath, Answer answer, String languge) throws Exception;

  /**
   * Saves an answer.
   * 
   * @param questionPath Path to the question.
   * @param questionLanguage The language type of the saved answer.
   * @throws Exception
   * @LevelAPI Platform
   */
  public void saveAnswer(String questionPath, QuestionLanguage questionLanguage) throws Exception;

  /**
   * Saves a question's comment.
   * 
   * @param questionPath Path to the question.
   * @param comment The comment to be saved.
   * @param languge The language type of comment.
   * @throws Exception
   * @LevelAPI Platform
   */
  public void saveComment(String questionPath, Comment comment, String languge) throws Exception;

  /**
   * Removes languages from a question.
   * 
   * @param questionPath Path to the question.
   * @param listLanguage A list of removed languages.
   * @throws Exception
   * @LevelAPI Platform
   */
  public void removeLanguage(String questionPath, List<String> listLanguage);

  /**
   * Votes for an answer.
   * 
   * @param answerPath Path of the answer.
   * @param userName Name of the user who has voted for the answer.
   * @param isUp If this value is "true", the answer is voted up. If "false", the answer is voted down. 
   * @throws Exception
   * @LevelAPI Platform
   */
  public void voteAnswer(String answerPath, String userName, boolean isUp) throws Exception;

  /**
   * Votes for a question.
   * 
   * @param questionPath Path to the question.
   * @param userName Name of the user who has voted for the question.
   * @param number The value of ratings (from 0 to 5 points).
   * @throws Exception
   * @LevelAPI Platform
   */
  public void voteQuestion(String questionPath, String userName, int number) throws Exception;

  /**
   * Gets moderators of a question or a category.
   * 
   * @param path Path to the question or category.
   * @return Moderators.
   * @throws Exception
   * @LevelAPI Platform
   */
  public String[] getModeratorsOf(String path) throws Exception;

  /**
   * Removes a vote from a question.
   * 
   * @param questionPath Path to the question.
   * @param userName Name of the user who unvotes the question.
   * @throws Exception
   * @LevelAPI Platform
   */
  public void unVoteQuestion(String questionPath, String userName) throws Exception;

  /**
   * Checks if the author information is displayed or not.
   * 
   * @param id Id of the question.
   * @return The returned value is "true" if the author information is displayed.
   * @LevelAPI Platform
   */
  public boolean isViewAuthorInfo(String id);

  /**
   * Gets the number of existing categories.
   * 
   * @return Categories.
   * @throws Exception 
   * @LevelAPI Platform
   */
  public long existingCategories() throws Exception;

  /**
   * Gets a category name.
   * 
   * @param categoryPath Path to the category.
   * @return The category name.
   * @throws Exception
   * @LevelAPI Platform
   */
  public String getCategoryNameOf(String categoryPath) throws Exception;

  /**
   * Gets quick questions from a list of categories.
   * 
   * @param listCategoryId Id of the categories.
   * @param isNotYetAnswer If this value is "true", only quick questions which are not yet answered are got.
   * @return Quick questions.
   * @throws Exception
   * @LevelAPI Platform
   */
  public List<Question> getQuickQuestionsByListCatetory(List<String> listCategoryId, boolean isNotYetAnswer) throws Exception;

  /**
   * Gets a list of categories.
   * 
   * @return Categories.
   * @throws Exception
   * @LevelAPI Platform
   */
  public List<Cate> listingCategoryTree() throws Exception;

  /**
   * Gets a list of watches in a category.
   * 
   * @param categoryId Id of the category.
   * @return Watches.
   * @throws Exception
   * @LevelAPI Platform
   */
  public List<Watch> getWatchByCategory(String categoryId) throws Exception;

  /**
   * Checks if a category has been watched or not.
   * 
   * @param categoryPath Path to the category.
   * @return The returned value is "true" if the category has been watched. 
   * @throws Exception
   * @LevelAPI Platform
   */
  public boolean hasWatch(String categoryPath);

  /**
   * Gets information of a category.
   * 
   * @param categoryPath Path to the category.
   * @param categoryIdScoped A list of sub-categories.
   * @return The category information.
   * @throws Exception
   * @LevelAPI Platform
   */
  public CategoryInfo getCategoryInfo(String categoryPath, List<String> categoryIdScoped) throws Exception;

  /**
   * Gets a question's template.
   * 
   * @return The template.
   * @throws Exception
   * @LevelAPI Platform
   */
  public byte[] getTemplate() throws Exception;

  /**
   * Saves a template.
   * 
   * @param str The template to be saved.
   * @throws Exception
   * @LevelAPI Platform
   */
  public void saveTemplate(String str) throws Exception;

  /**
   * Checks if a category exists or not.
   * 
   * @param name Name of the category.
   * @param path Path to the category.
   * @return The returned value is "true" if the category has already existed.
   * @throws Exception
   * @LevelAPI Platform
   */
  public boolean isCategoryExist(String name, String path);

  /**
   * Updates relatives of a question.
   * 
   * @param questionPath Path to the question.
   * @param relatives Relative paths of the question.
   * @throws Exception
   * @LevelAPI Platform
   */
  public void updateQuestionRelatives(String questionPath, String[] relatives) throws Exception;

  /**
   * Checks if moderation is applied to a question or not.
   * 
   * @param id Id of the question.
   * @return The returned value is "true" if moderation is enabled.
   * @throws Exception
   * @LevelAPI Platform
   */
  public boolean isModerateQuestion(String id) throws Exception;

  /**
   * Creates an RSS for a set of answers under a category.
   * 
   * @param cateId Id of the category.
   * @return RSS.
   * @throws Exception
   * @LevelAPI Platform
   */
  public InputStream createAnswerRSS(String cateId) throws Exception;

  /**
   * Updates the last active information of a question.
   *  
   * @param absPathOfItem Path to the question.
   * @throws Exception
   * @LevelAPI Platform
   */
  public void reCalculateLastActivityOfQuestion(String absPathOfItem) throws Exception;

  /**
   * Adds a listener plugin.
   *  
   * @param listener The listener plugin to be added.
   * @throws Exception
   * @LevelAPI Platform
   */
  public void addListenerPlugin(AnswerEventListener listener) throws Exception;
  
  /**
   * Removes a listener plugin.
   *  
   * @param listener The listener plugin to be removed.
   * @throws Exception
   * @LevelAPI Platform
   */
  public void removeListenerPlugin(AnswerEventListener listener) throws Exception;

  /**
   * Gets comments of a question.
   *  
   * @param questionId Id of the question.
   * @return Comments.
   * @throws Exception
   * @LevelAPI Platform
   */
  public Comment[] getComments(String questionId) throws Exception;
  
  /**
   * Updates information of answers related to a deleted user.
   * 
   * @param userName Name of the deleted user.
   * @throws Exception
   */
  public void calculateDeletedUser(String userName) throws Exception;
  
  /**
   * Reads a category property by its name.
   * 
   * @param categoryId Id of the category.
   * @param propertyName Name of the property.
   * @param returnType A returned type of the property. The supported types are String[], String, Long, Boolean, Double and Date.  
   * @return A property.
   * @throws Exception
   * @LevelAPI Platform
   */
  public Object readCategoryProperty(String categoryId, String propertyName, Class returnType) throws Exception;
  
  /**
   * Reads a question property by its name.
   * 
   * @param questionId Id of the question.
   * @param propertyName Name of the property.
   * @param returnType A returned type of the property. The supported types are String[], String, Long, Boolean, Double and Date.
   * @return A property.
   * @throws Exception
   * @LevelAPI Platform
   */
  public Object readQuestionProperty(String questionId, String propertyName, Class returnType) throws Exception;
  
  /**
   * Saves information of a question activity that is used for processing the activity streams.
   * 
   * @param ownerPath Path to the question activity.
   * @param activityId Id of the question activity. 
   * @LevelAPI Platform
   * @since 4.0
   */
  public void saveActivityIdForQuestion(String ownerPath, String activityId);

  /**
   * Gets information of a question activity that is used for processing the activity streams.
   * 
   * @param ownerPath Path to the question activity.
   * @return Id of the question activity.
   * @LevelAPI Platform 
   * @since 4.0
   */
  public String getActivityIdForQuestion(String ownerPath);
  
  /**
   * Saves information of an answer activity that is used for processing the activity streams.
   * 
   * @param ownerPath Path to the answer activity.
   * @param answer Information of the answer which is used for creating or updating the activity.
   * @param activityId Id of the answer activity.
   * @LevelAPI Platform
   * @since 4.0
   */
  public void saveActivityIdForAnswer(String ownerPath, Answer answer, String activityId);

  /**
   * Gets information of an answer activity that is used for processing the activity streams.
   * 
   * @param ownerPath Path to the answer activity.
   * @param answer Information of the answer which is used for getting the activity.
   * @return Id of the answer activity. 
   * @LevelAPI Platform
   * @since 4.0
   */
  public String getActivityIdForAnswer(String ownerPath, Answer answer);
  
  /**
   * Saves information of a comment activity that is used for processing the activity streams.
   * 
   * @param ownerPath Path to the comment activity.
   * @param commentId Id of the comment.
   * @param language Language of the comment.
   * @param activityId Id of the comment activity. 
   * @LevelAPI Platform
   * @since 4.0
   */
  public void saveActivityIdForComment(String ownerPath, String commentId, String language, String activityId);

  /**
   * Get information of a comment activity that is used for processing the activity streams.
   *  
   * @param ownerPath Path to the comment activity.
   * @param commentId Id of the comment.
   * @param language Language of the comment.
   * @return Id of the comment activity.  
   * @LevelAPI Platform
   * @since 4.0
   */
  public String getActivityIdForComment(String ownerPath, String commentId, String language);
}