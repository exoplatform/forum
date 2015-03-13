/***************************************************************************
* Copyright 2001-2006 The eXo Platform SARL         All rights reservd.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.forum.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.exoplatform.forum.base.BaseForumServiceTestCase;
import org.exoplatform.forum.service.filter.model.CategoryFilter;

public class CategoryForumTestCase extends BaseForumServiceTestCase {

  @Override
  public void setUp() throws Exception {
    super.setUp();

  }
  
  @Override
  public void tearDown() throws Exception {
    //
    super.tearDown();
  }


  public void testCategory() throws Exception {
    String[] catIds = new String[] { getId(Utils.CATEGORY), getId(Utils.CATEGORY), getId(Utils.CATEGORY) };

    // test case failed by KS-4422
    String catId = getId(Utils.CATEGORY);
    Category cat_ = createCategory(catId);
    // check category existing
    assertNull(String.format("The category has ID is %s existed.", catId), forumService_.getCategory(catId));
    // save new category
    forumService_.saveCategory(cat_, true);
    // check again category existing
    assertNotNull(String.format("The category has ID is %s not existing.", catId), forumService_.getCategory(catId));

    // test case failed by KS-4427
    // get category new created it same category cat_
    Category catTest = forumService_.getCategory(catId);
    assertEquals(Utils.isEmpty(catTest.getUserPrivate()), true);

    // test save/update moderators of category
    List<String> categoriesId = new ArrayList<String>();
    categoriesId.add(catId);
    forumService_.saveModOfCategory(categoriesId, USER_DEMO, true);
    catTest = forumService_.getCategory(catId);
    assertEquals("The moderators of category not contanins user demo.", catTest.getModerators()[0], USER_DEMO);

    // add category
    forumService_.saveCategory(createCategory(catIds[0]), true);
    forumService_.saveCategory(createCategory(catIds[1]), true);
    forumService_.saveCategory(createCategory(catIds[2]), true);
    Category category = forumService_.getCategory(catIds[0]);
    assertNotNull("Category is null", category);
    // get categories
    List<Category> categories = forumService_.getCategories();
    assertEquals(categories.size(), 4);
    // update category
    category.setCategoryName("ReName Category");
    forumService_.saveCategory(category, false);
    Category updatedCat = forumService_.getCategory(catIds[0]);
    assertEquals("Category name is not change", "ReName Category", updatedCat.getCategoryName());

    // test removeCategory
    for (int i = 0; i < 3; ++i) {
      forumService_.removeCategory(catIds[i]);
    }
    forumService_.removeCategory(catId);
    categories = forumService_.getCategories();
    assertEquals("Size categories can not equals 0", categories.size(), 0);
  }

  public void testGetCategoryIncludedSpace() throws Exception {
    assertNull(forumService_.getCategoryIncludedSpace());
    Category cat = createCategory(getId(Utils.CATEGORY_SPACE_ID_PREFIX));
    cat.setIncludedSpace(true);
    forumService_.saveCategory(cat, true);
    assertNotNull(forumService_.getCategoryIncludedSpace());
  }

  public void testForum() throws Exception {
    forumService_.saveUserProfile(createdUserProfile(USER_DEMO), false, false);
    forumService_.saveUserProfile(createdUserProfile(USER_JOHN), false, false);
    //
    String catId = getId(Utils.CATEGORY);
    Category cat = createCategory(catId);
    // create new category
    forumService_.saveCategory(cat, true);

    // create new forum
    Forum forum = createdForum();
    String forumId = forum.getId();

    // add forum
    forumService_.saveForum(catId, forum, true);

    // getForum
    forum = forumService_.getForum(catId, forumId);
    assertNotNull("Forum is null", forum);

    // getList Forum
    // Created 5 new forum, we have total 6 forum.
    List<Forum> forums = new ArrayList<Forum>();
    for (int i = 0; i < 5; i++) {
      forumService_.saveForum(cat.getId(), createdForum(), true);
    }
    forums.addAll(forumService_.getForums(catId, ""));

    // check size of list forum
    assertEquals("List forums size not equals 6", forums.size(), 6);

    // update Forum
    forum.setForumName("Forum update");
    forumService_.saveForum(catId, forum, false);
    assertEquals(forum.getForumName(), forumService_.getForum(catId, forumId).getForumName());

    // modifyForum
    forum.setIsLock(true);
    forumService_.modifyForum(forum, 2);
    forum = forumService_.getForum(catId, forumId);
    assertEquals(forum.getIsLock(), true);

    // saveModerateOfForum
    List<String> list = new ArrayList<String>();
    list.add(catId + "/" + forum.getId());
    forumService_.saveModerateOfForums(list, USER_DEMO, false);
    forum = forumService_.getForum(catId, forumId);
    list.clear();
    list.addAll(Arrays.asList(forum.getModerators()));
    assertEquals(list.contains(USER_DEMO), true);

    // test moderator of category.
    list.clear();
    list.add(catId);
    forumService_.saveModOfCategory(list, USER_JOHN, true);
    forum = forumService_.getForum(catId, forumId);
    list.clear();
    list.addAll(Arrays.asList(forum.getModerators()));
    assertEquals("Forum in category can not content moderatort user admin", list.contains(USER_JOHN), true);

    // test moveForum, Move list Forum from Category 'cat' to Category 'cate'
    
    // create new topic on forums
    Topic topic = createdTopic(USER_ROOT);
    forumService_.saveTopic(catId, forumId, topic, true , false, new MessageBuilder());

    // create new Category
    Category cate = createCategory(getId(Utils.CATEGORY));
    forumService_.saveCategory(cate, true);
    Category cateNew = forumService_.getCategory(cate.getId());
    
    // move forum
    forumService_.moveForum(forums, cateNew.getPath());

    // get forum in new category
    forum = forumService_.getForum(cateNew.getId(), forumId);
    assertNotNull(forum);
    
    // get Topic in forum
    topic = (Topic) forumService_.getObjectNameById(topic.getId(), Utils.TOPIC);
    assertNotNull(topic);
    assertEquals(forumId, topic.getForumId());
    assertEquals(cateNew.getId(), topic.getCategoryId());
    //
    topic = (Topic) forumService_.getTopic(cateNew.getId(), forumId, topic.getId(), USER_DEMO);
    assertNotNull(topic);
    assertEquals(cateNew.getId(), topic.getCategoryId());

    // remove Forum and return this Forum
    for (Forum forum2 : forums) {
      forumService_.removeForum(cate.getId(), forum2.getId());
    }

    // check remove
    forums = forumService_.getForumSummaries(catId, "");
    assertEquals("List forums can not equals 0", forums.size(), 0);
  }
  
  public void testFilterForumByName() throws Exception {
    loginUser(USER_ROOT);
    // create category
    categoryId = getId(Utils.CATEGORY);
    forumService_.saveCategory(createCategory(categoryId), true);

    // create forums
    String prefix = "search";
    for (int i = 0; i < 5; ++i) {
      Forum forum = createdForum();
      String t = String.valueOf(Character.toChars(103 - i)[0]);
      forum.setForumName(prefix + " " + t + " test key foo bar ");
      forumService_.saveForum(categoryId, forum, true);
    }
    
    Forum forum = createdForum();
    forum.setForumName("abc xy");
    forumService_.saveForum(categoryId, forum, true);
    
    // search with key random
    List<CategoryFilter> categoryFilters = forumService_.filterForumByName("jobl", null, 0);
    // result have 0 categories.
    assertEquals(0, categoryFilters.size());

    // search with key: foo
    categoryFilters = forumService_.filterForumByName("foo", null, 0);
    // result have 1 category and has 5 forums.
    assertEquals(1, categoryFilters.size());
    assertEquals(5, categoryFilters.get(0).getForumFilters().size());

    // search with key: search1
    categoryFilters = forumService_.filterForumByName(prefix + " c", null, 0);
    // result have 1 category and has one forum.
    assertEquals(1, categoryFilters.size());
    assertEquals(1, categoryFilters.get(0).getForumFilters().size());

    // search with key: search
    categoryFilters = forumService_.filterForumByName(prefix, null, 0);
    // result have 1 categories and has 5 forums.
    assertEquals(1, categoryFilters.size());
    assertEquals(5, categoryFilters.get(0).getForumFilters().size());

    // search with key: x
    categoryFilters = forumService_.filterForumByName("x", null, 0);
    // result have 1 category and has one forum.
    assertEquals(1, categoryFilters.size());
    assertEquals(1, categoryFilters.get(0).getForumFilters().size());

    // search with key: est
    categoryFilters = forumService_.filterForumByName("est", null, 0);
    // result have 0 category.
    assertEquals(0, categoryFilters.size());

    // search with key: tes
    categoryFilters = forumService_.filterForumByName("tes", null, 0);
    // result have 1 categories and has 5 forums.
    assertEquals(1, categoryFilters.size());
    assertEquals(5, categoryFilters.get(0).getForumFilters().size());
  }
  
  
  public void testFilterAllForumByName() throws Exception {
    // create Data
    for (int i = 0; i < 3; i++) {
      Category cate = createCategory(getId(Utils.CATEGORY));
      cate.setCategoryName("cate number " + i);
      cate.setUserPrivate(new String[] { USER_ROOT, USER_DEMO });
      forumService_.saveCategory(cate, true);
      for (int j = 0; j < 5; j++) {
        Forum forum = createdForum();
        forum.setForumName("forum number " + j + " of cate " + i);
        forumService_.saveForum(cate.getId(), forum, true);
      }
    }
    // limit 2 forums
    List<CategoryFilter> result = forumService_.filterForumByName("_", USER_ROOT, 2);
    assertEquals(2, getSizeOfForumFound(result));
    // not limit
    result = forumService_.filterForumByName("_", USER_ROOT, 0);
    assertEquals(3 * 5, getSizeOfForumFound(result));
    // add more category/forums space
    Category cate = createCategory(Utils.CATEGORY_SPACE_ID_PREFIX);
    cate.setCategoryName("spaces");
    forumService_.saveCategory(cate, true);
    for (int j = 0; j < 5; j++) {
      Forum forum = createdForum();
      forum.setForumName("forum number " + j + " of cate space");
      forumService_.saveForum(cate.getId(), forum, true);
    }
    // result not change after add space
    result = forumService_.filterForumByName("_", USER_ROOT, 0);
    assertEquals(3 * 5, getSizeOfForumFound(result));
    // test with FORUM-826
    // create public category have one forum
    cate = createCategory(getId(Utils.CATEGORY));
    forumService_.saveCategory(cate, true);
    Forum forum = createdForum();
    forum.setForumName("a1 forum test");
    forumService_.saveForum(cate.getId(), forum, true);
    // create private category for DEMO have one forum
    cate = createCategory(getId(Utils.CATEGORY));
    cate.setUserPrivate(new String[] { USER_ROOT });
    forumService_.saveCategory(cate, true);
    forum = createdForum();
    forum.setForumName("a2 forum test");
    forumService_.saveForum(cate.getId(), forum, true);
    // limit 2 forums for DEMO, before fix
    // + if category private have 2 forums it will return 0
    // + if category private have 1 forums it will return 1
    result = forumService_.filterForumByName("_", USER_DEMO, 2);
    assertEquals(2, getSizeOfForumFound(result));
  }
  
  public static int getSizeOfForumFound(List<CategoryFilter> result) {
    int found = 0;
    for (CategoryFilter categoryFilter : result) {
      found += categoryFilter.getForumFilters().size();
    }
    return found;
  }
  
}
