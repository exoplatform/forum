/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
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
package org.exoplatform.faq.service.search;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import org.exoplatform.commons.api.search.data.SearchContext;
import org.exoplatform.commons.api.search.data.SearchResult;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.faq.base.FAQServiceBaseTestCase;
import org.exoplatform.faq.service.Answer;
import org.exoplatform.faq.service.Category;
import org.exoplatform.faq.service.Comment;
import org.exoplatform.faq.service.DataStorage;
import org.exoplatform.faq.service.FAQSetting;
import org.exoplatform.faq.service.Question;
import org.exoplatform.faq.service.Utils;
import org.exoplatform.web.controller.metadata.ControllerDescriptor;
import org.exoplatform.web.controller.metadata.DescriptorBuilder;
import org.exoplatform.web.controller.router.Router;
import org.exoplatform.web.controller.router.RouterConfigException;

public class AnswerSearchConnectorTestCase extends FAQServiceBaseTestCase {
  private final static String CONTROLLER_PATH = "conf/standalone/controller.xml";
  private SearchContext context;
  private Router router;

  public AnswerSearchConnectorTestCase() throws Exception {
    super();
  }

  private AnswerSearchConnector answerSearchConnector;
  private Question questionTest;
  @Override
  public void setUp() throws Exception {
    super.setUp();
    loadController();
    removeData();

    //
    Category cat = createCategory("Category A", 0);
    faqService_.saveCategory(Utils.CATEGORY_HOME, cat, true);

    questionTest = createQuestion(Utils.CATEGORY_HOME + "/" + cat.getId());
    questionTest.setQuestion("Questiontest kool 1");
    questionTest.setDetail("foo detail");
    faqService_.saveQuestion(questionTest, true, new FAQSetting());
    
    Question quest = createQuestion(Utils.CATEGORY_HOME + "/" + cat.getId());
    quest.setQuestion("Question with Close word");
    quest.setDetail("foo detail");
    faqService_.saveQuestion(quest, true, new FAQSetting());

    Question question = createQuestion(Utils.CATEGORY_HOME + "/" + cat.getId());;
    question.setQuestion("Questiontest B");
    question.setDetail("foo ");
    faqService_.saveQuestion(question, true, new FAQSetting());

    question = createQuestion(Utils.CATEGORY_HOME + "/" + cat.getId());;
    question.setQuestion("Questiontest C");
    question.setDetail("bar");
    faqService_.saveQuestion(question, true, new FAQSetting());

    question = faqService_.getQuestionById(question.getId());
    Answer answer = createAnswer(USER_ROOT, "new reponses 1");
    answer.setNew(true);

    //the same answer in the same question.
    faqService_.saveAnswer(question.getPath(), answer, true);
    
    answer = createAnswer(USER_ROOT, "new reponses 2");
    answer.setNew(true);

    faqService_.saveAnswer(question.getPath(), answer, true);

    
    Comment comment = createComment(USER_ROOT, "comment test");
    comment.setNew(true);

    faqService_.saveComment(question.getPath(), comment, true);
    
    DataStorage dataStorage = (DataStorage) getService(DataStorage.class);
    
    //
    InitParams params = new InitParams();
    params.put("constructor.params", new PropertiesParam());
    answerSearchConnector = new AnswerSearchConnector(params, dataStorage);

  }

  @Override
  public void tearDown() throws Exception {
    context = null;
    router = null;
    super.tearDown();
  }

  public void testFilter() throws Exception {
    assertEquals(3, answerSearchConnector.search(context, " Questiontest~", Collections.EMPTY_LIST, 0, 10, "relevancy", "ASC").size());
    
    //offset =1 && limit =10
    assertEquals(2, answerSearchConnector.search(context, " Questiontest~", Collections.EMPTY_LIST, 1, 10, "relevancy", "ASC").size());
    
    //offset =2 && limit =10
    assertEquals(1, answerSearchConnector.search(context, " Questiontest~", Collections.EMPTY_LIST, 2, 10, "relevancy", "ASC").size());
    
    assertEquals(3, answerSearchConnector.search(context, " foo~", Collections.EMPTY_LIST, 0, 10, "relevancy", "ASC").size());
    
    //return 2 result for 2 answer.
    assertEquals(2, answerSearchConnector.search(context, " reponses~", Collections.EMPTY_LIST, 0, 10, "relevancy", "ASC").size());
    assertEquals(1, answerSearchConnector.search(context, " comment~", Collections.EMPTY_LIST, 0, 10, "relevancy", "ASC").size());
    
    //test for special characters in Unified Search
    assertEquals(2, answerSearchConnector.search(context, " repon~", Collections.EMPTY_LIST, 0, 10, "relevancy", "ASC").size());
    assertEquals(4, answerSearchConnector.search(context, " ques~", Collections.EMPTY_LIST, 0, 10, "relevancy", "ASC").size());
    assertEquals(1, answerSearchConnector.search(context, " clo~", Collections.EMPTY_LIST, 0, 10, "relevancy", "ASC").size());
    assertEquals(1, answerSearchConnector.search(context, " clo*~", Collections.EMPTY_LIST, 0, 10, "relevancy", "ASC").size());

  }

  public void testData() throws Exception {
    List<SearchResult> aResults = (List<SearchResult>) answerSearchConnector.search(context, " kool~", Collections.EMPTY_LIST, 0, 10, "relevancy", "ASC");
    questionTest = faqService_.getQuestionById(questionTest.getId());
    SearchResult aResult = aResults.get(0);
    assertEquals(questionTest.getQuestion(), aResult.getTitle());
    String url = aResult.getUrl();
    assertTrue(url.indexOf("/portal/classic/answers/?&questionId") >= 0);
    // content is question
    assertEquals(questionTest.getCreatedDate().getTime(), aResult.getDate());
    assertEquals(questionTest.getDetail(), ((UnifiedSearchResult)aResult).getContent());
    //get excerpt field
    assertTrue(aResult.getExcerpt().indexOf("kool") >= 0);
    
    // content is answer
    aResults = (List<SearchResult>)answerSearchConnector.search(context, " reponses~", Collections.EMPTY_LIST, 0, 10, "title", "ASC");
    aResult = aResults.get(0);
    assertEquals("new reponses 1", ((UnifiedSearchResult)aResult).getContent());
    aResult = aResults.get(1);
    assertEquals("new reponses 2", ((UnifiedSearchResult)aResult).getContent());
    //get excerpt field
    assertTrue(aResult.getExcerpt().indexOf("reponses") >= 0);
    
    // content is comment
    aResults = (List<SearchResult>)answerSearchConnector.search(context, " comment~", Collections.EMPTY_LIST, 0, 10, "title", "ASC");
    aResult = aResults.get(0);
    assertEquals("comment test", ((UnifiedSearchResult)aResult).getContent());
    //get excerpt field
    assertTrue(aResult.getExcerpt().indexOf("comment") >= 0);

  }

  public void testOrder() throws Exception {
    List<SearchResult> rTitleAsc = (List<SearchResult>) answerSearchConnector.search(context, " Questiontest~", Collections.EMPTY_LIST, 0, 10, "title", "ASC");
    assertEquals("Questiontest B", rTitleAsc.get(0).getTitle());
    assertEquals("Questiontest C", rTitleAsc.get(1).getTitle());

    List<SearchResult> rTitleDesc = (List<SearchResult>) answerSearchConnector.search(context, " Questiontest~", Collections.EMPTY_LIST, 0, 10, "title", "DESC");
    assertEquals("Questiontest kool 1", rTitleDesc.get(0).getTitle());
    assertEquals("Questiontest C", rTitleDesc.get(1).getTitle());

    List<SearchResult> rDateAsc = (List<SearchResult>) answerSearchConnector.search(context, " Questiontest~", Collections.EMPTY_LIST, 0, 10, "date", "ASC");
    assertEquals("Questiontest kool 1", rDateAsc.get(0).getTitle());
    assertEquals("Questiontest B", rDateAsc.get(1).getTitle());

    List<SearchResult> rDateDesc = (List<SearchResult>) answerSearchConnector.search(context, " Questiontest~", Collections.EMPTY_LIST, 0, 10, "date", "DESC");
    assertEquals("Questiontest C", rDateDesc.get(0).getTitle());
    assertEquals("Questiontest B", rDateDesc.get(1).getTitle());
  }
  
  private void loadController() throws Exception {
    ClassLoader loader = getClass().getClassLoader();
    InputStream in = loader.getResourceAsStream(CONTROLLER_PATH);
    try {
      ControllerDescriptor routerDesc = new DescriptorBuilder().build(in);
      router = new Router(routerDesc);
      context = new SearchContext(router, "");
    } catch (RouterConfigException e) {
      log.info(e.getMessage());
    } finally {
      in.close();
    }
  }

}