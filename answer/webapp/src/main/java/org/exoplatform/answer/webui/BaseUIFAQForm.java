/***************************************************************************
 * Copyright (C) 2003-2010 eXo Platform SAS.
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
package org.exoplatform.answer.webui;

import org.exoplatform.faq.service.FAQService;
import org.exoplatform.forum.common.webui.BaseUIForm;
import org.exoplatform.forum.common.webui.WebUIUtils;
import org.exoplatform.webui.application.WebuiRequestContext;

public class BaseUIFAQForm extends BaseUIForm {
  private FAQService faqService;

  /**
   * Get a reference to the faq service
   * 
   * @return
   */
  protected FAQService getFAQService() {
    if (faqService == null) {
      faqService = getApplicationComponent(FAQService.class);
    }
    return faqService;
  }

  /**
   * Set faq service (used by unit tests)
   * 
   * @param faqService
   */
  protected void setFAQService(FAQService faqService) {
    this.faqService = faqService;
  }
  
  @Override
  public void processRender(WebuiRequestContext context) throws Exception {
    WebUIUtils.addScripts("ForumUtils", "forumUtils", "forumUtils.initTooltip('" + getId() + "');");
    super.processRender(context);
  }

}
