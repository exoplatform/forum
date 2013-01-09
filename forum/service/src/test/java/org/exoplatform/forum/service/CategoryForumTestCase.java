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

/**
 * Created by The eXo Platform SARL
 * Author : Vu Duy Tu
 *          tuvd@exoplatform.com
 * July 3, 2007  
 */

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


  public void testForum() throws Exception {
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
    forumService_.saveModerateOfForums(list, "demo", false);
    forum = forumService_.getForum(catId, forumId);
    list.clear();
    list.addAll(Arrays.asList(forum.getModerators()));
    assertEquals(list.contains("demo"), true);

    // test moderator of category.
    list.clear();
    list.add(catId);
    forumService_.saveModOfCategory(list, USER_JOHN, true);
    forum = forumService_.getForum(catId, forumId);
    list.clear();
    list.addAll(Arrays.asList(forum.getModerators()));
    assertEquals("Forum in category can not content moderatort user admin", list.contains(USER_JOHN), true);

    // test moveForum, Move list Forum from Category 'cat' to Category 'cate'

    // create new Category
    Category cate = createCategory(getId(Utils.CATEGORY));
    forumService_.saveCategory(cate, true);
    Category cateNew = forumService_.getCategory(cate.getId());

    // move forum
    forumService_.moveForum(forums, cateNew.getPath());

    // get forum in new category
    forum = forumService_.getForum(cate.getId(), forumId);
    assertNotNull(forum);

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
    // create categories
    List<String> categories = new ArrayList<String>();
    for(int i = 0; i < 10; ++i) {
      String catId = getId(Utils.CATEGORY);
      categories.add(catId);
      Category cat = createCategory(catId);
      forumService_.saveCategory(cat, true);
    }

    // create forums
    String prefix = "search ";
    for(String cateId : categories) {
      for(int i =0; i < 5; ++i) {
        Forum forum = createdForum();
        String t = String.valueOf(Character.toChars(103-i)[0]);
        forum.setForumName(prefix + t + " test key foo bar ");
        forumService_.saveForum(cateId, forum, true);
      }
    }
    
    // search with key random
    List<CategoryFilter> categoryFilters = forumService_.filterForumByName("jobl", null);
    // result have 0 categories. 
    assertEquals(0, categoryFilters.size());
    
    // search with key: foo
    categoryFilters = forumService_.filterForumByName("foo", null);
    // result have 10 category. 
    assertEquals(10, categoryFilters.size());
    
    // search with key: search1
    categoryFilters = forumService_.filterForumByName(prefix+"c", null);
    // result have 10 categories and each one category has one forum. 
    assertEquals(10, categoryFilters.size());
    assertEquals(1, categoryFilters.get(0).getForumFilters().size());

    // search with key: search
    categoryFilters = forumService_.filterForumByName(prefix, null);
    // result have 10 categories and each one category has 5 forums. 
    assertEquals(10, categoryFilters.size());
    assertEquals(2, categoryFilters.get(0).getForumFilters().size());
    

    
  }
}
