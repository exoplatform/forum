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
package org.exoplatform.answer.webui.popup;

import org.exoplatform.answer.webui.UIAnswersPortlet;
import org.exoplatform.faq.service.FAQSetting;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

@ComponentConfig(
    template = "app:/templates/answer/webui/popup/UIQuestionManagerForm.gtmpl", 
    events = {
        @EventConfig(listeners = UIQuestionManagerForm.CancelActionListener.class) 
    }
)
public class UIQuestionManagerForm extends UIContainer {
  public static final String UI_QUESTION_INFO       = "QuestionInfo";

  public static final String UI_QUESTION_FORM       = "UIQuestionForm";

  public static final String UI_RESPONSE_FORM       = "UIResponseForm";

  public boolean             isEditQuestion         = false;

  public boolean             isResponseQuestion     = false;

  public boolean             isViewEditQuestion     = true;

  public boolean             isViewResponseQuestion = false;

  public UIQuestionManagerForm() throws Exception {
    isEditQuestion = false;
    isResponseQuestion = false;
    isViewEditQuestion = false;
    isViewResponseQuestion = false;
    addChild(UIQuestionsInfo.class, null, UI_QUESTION_INFO);
    addChild(UIQuestionForm.class, null, UI_QUESTION_FORM);
    addChild(UIResponseForm.class, null, UI_RESPONSE_FORM);
  }

  public void setFAQSetting(FAQSetting setting) throws Exception {
    UIQuestionsInfo questionsInfo = this.getChildById(UI_QUESTION_INFO);
    questionsInfo.setFAQSetting(setting);
  }

  protected boolean getIsEdit() {
    return this.isEditQuestion;
  }

  protected boolean getIsViewEdit() {
    return this.isViewEditQuestion;
  }

  protected boolean getIsResponse() {
    return this.isResponseQuestion;
  }

  protected boolean getIsVewResponse() {
    return this.isViewResponseQuestion;
  }

  static public class CancelActionListener extends EventListener<UIQuestionManagerForm> {
    public void execute(Event<UIQuestionManagerForm> event) throws Exception {
      UIAnswersPortlet portlet = event.getSource().getAncestorOfType(UIAnswersPortlet.class);
      portlet.cancelAction();
    }
  }
}
