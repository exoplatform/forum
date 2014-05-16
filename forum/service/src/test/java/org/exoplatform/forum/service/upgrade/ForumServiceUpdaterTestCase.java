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
package org.exoplatform.forum.service.upgrade;

import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.forum.base.BaseForumServiceTestCase;
import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.forum.common.jcr.KSDataLocation;
import org.exoplatform.forum.service.Category;
import org.exoplatform.forum.service.DataStorage;
import org.exoplatform.forum.service.ForumNodeTypes;
import org.exoplatform.forum.service.Utils;
import org.exoplatform.forum.service.impl.JCRDataStorage;
import org.exoplatform.forum.service.updater.ForumServiceUpdaterPlugin;
import org.exoplatform.services.jcr.ext.common.SessionProvider;

public class ForumServiceUpdaterTestCase extends BaseForumServiceTestCase {

  private KSDataLocation            dataLocator;

  private DataStorage               dataStorage;

  private ForumServiceUpdaterPlugin serviceUpdaterPlugin;

  public ForumServiceUpdaterTestCase() {
  }

  public void setUp() throws Exception {
    super.setUp();
    dataLocator = CommonUtils.getComponent(KSDataLocation.class);
    dataStorage = CommonUtils.getComponent(JCRDataStorage.class);

    // make old data
    createOldCategoriesData();
    
    //
    InitParams initParams = new InitParams();
    ValueParam param = new ValueParam();
    param.setName("product.group.id");
    param.setValue("org.exoplatform.forum");
    initParams.put("product.group.id", param);
    serviceUpdaterPlugin = new ForumServiceUpdaterPlugin(initParams);
  }

  @Override
  public void tearDown() throws Exception {
    super.tearDown();
  }

  public void testUpgradeCategorySpace() throws Exception {
    // check before upgrade
    Category cateSpace = dataStorage.getCategory(Utils.CATEGORY_SPACE_ID_PREFIX);
    assertNotNull(cateSpace);
    assertFalse(cateSpace.isIncludedSpace());

    // get by DataStorage#getCategoryIncludedSpace
    Category cateSpace2 = dataStorage.getCategoryIncludedSpace();
    assertNotNull(cateSpace2);

    // check with normal category
    Category category = dataStorage.getCategory(Utils.CATEGORY + "0");
    assertNotNull(category);
    assertFalse(hasProperty(category.getPath(), ForumNodeTypes.EXO_INCLUDED_SPACE));

    // run upgrade
    serviceUpdaterPlugin.processUpgrade("3.5.x", "4.0.x");

    // check after upgrade
    cateSpace = dataStorage.getCategory(Utils.CATEGORY_SPACE_ID_PREFIX);
    assertNotNull(cateSpace);
    assertTrue(cateSpace.isIncludedSpace());

    // get by DataStorage#getCategoryIncludedSpace
    cateSpace2 = dataStorage.getCategoryIncludedSpace();
    assertNotNull(cateSpace2);
    assertEquals(cateSpace, cateSpace2);
    
    // check with old normal category
    Category oldCategory = dataStorage.getCategory(Utils.CATEGORY + "0");
    assertNotNull(category);
    assertTrue(hasProperty(oldCategory.getPath(), ForumNodeTypes.EXO_INCLUDED_SPACE));
    assertFalse(oldCategory.isIncludedSpace());

    // check with create new normal category
    initDefaultData();
    //
    category = dataStorage.getCategory(categoryId);
    assertNotNull(category);
    assertTrue(hasProperty(category.getPath(), ForumNodeTypes.EXO_INCLUDED_SPACE));
    assertFalse(category.isIncludedSpace());
  }

  private boolean hasProperty(String path, String property) throws Exception {
    SessionProvider sProvider = SessionProvider.createSystemProvider();
    try {
      Session session = dataLocator.getSessionManager().getSession(sProvider);
      Node cateNode = (Node) session.getItem(path);
      return cateNode.hasProperty(property);
    } catch (Exception e) {
      return false;
    } finally {
      sProvider.close();
    }
  }

  private void createOldCategoriesData() throws Exception {
    SessionProvider sProvider = SessionProvider.createSystemProvider();
    try {
      Session session = dataLocator.getSessionManager().getSession(sProvider);
      Node cateHome = session.getRootNode().getNode(dataLocator.getForumCategoriesLocation());
      // normal category
      for (int i = 0; i < 5; i++) {
        Category category = createCategory(Utils.CATEGORY + "" + i);
        createOldCategoriesData(cateHome, category);
      }
      // category space
      Category cateSpace = createCategory(Utils.CATEGORY_SPACE_ID_PREFIX);
      createOldCategoriesData(cateHome, cateSpace);
    } finally {
      sProvider.close();
    }
  }

  private void createOldCategoriesData(Node cateHome, Category category) throws Exception {
    Node catNode = cateHome.addNode(category.getId(), ForumNodeTypes.EXO_FORUM_CATEGORY);
    catNode.setProperty(ForumNodeTypes.EXO_ID, category.getId());
    catNode.setProperty(ForumNodeTypes.EXO_OWNER, category.getOwner());
    catNode.setProperty(ForumNodeTypes.EXO_CREATED_DATE, Calendar.getInstance());
    catNode.setProperty(ForumNodeTypes.EXO_NAME, category.getCategoryName());
    catNode.setProperty(ForumNodeTypes.EXO_CATEGORY_ORDER, category.getCategoryOrder());
    catNode.setProperty(ForumNodeTypes.EXO_DESCRIPTION, category.getDescription());
    catNode.setProperty(ForumNodeTypes.EXO_MODIFIED_BY, category.getModifiedBy());
    catNode.setProperty(ForumNodeTypes.EXO_MODIFIED_DATE, Calendar.getInstance());
    catNode.setProperty(ForumNodeTypes.EXO_USER_PRIVATE, category.getUserPrivate());
    catNode.setProperty(ForumNodeTypes.EXO_CREATE_TOPIC_ROLE, category.getCreateTopicRole());
    catNode.setProperty(ForumNodeTypes.EXO_POSTER, category.getPoster());
    catNode.setProperty(ForumNodeTypes.EXO_VIEWER, category.getViewer());
    category.setPath(catNode.getPath());
    //
    cateHome.getSession().save();
  }

}
