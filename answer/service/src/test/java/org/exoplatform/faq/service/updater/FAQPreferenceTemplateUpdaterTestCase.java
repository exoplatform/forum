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
package org.exoplatform.faq.service.updater;

import java.io.InputStream;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.faq.base.FAQServiceBaseTestCase;

public class FAQPreferenceTemplateUpdaterTestCase extends FAQServiceBaseTestCase {
  private final static String TEMPLATE_PATH = "conf/standalone/FAQViewerPortlet.gtmpl";

  public FAQPreferenceTemplateUpdaterTestCase() throws Exception {
    super();
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();

  }

  @Override
  public void tearDown() throws Exception {
    super.tearDown();
  }

  public void testTemplateUpdater() throws Exception {
    FAQPreferenceTemplateUpdaterPlugin templateUpdaterPlugin = new FAQPreferenceTemplateUpdaterPlugin(new InitParams()) {
      @Override
      public InputStream getTemplateStream() throws Exception {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(TEMPLATE_PATH);
      }
    };
    
    assertNotNull(templateUpdaterPlugin.getTemplateStream());
    templateUpdaterPlugin.processUpgrade("2.2.x", "4.0");
    
    byte[] bytes = faqService_.getTemplate();
    String str = new String(bytes, "UTF-8");
    assertTrue(str.length() > 1000);
    assertTrue(str.indexOf("roundCornerBoxWithTitle") > 0);
    
  }
}