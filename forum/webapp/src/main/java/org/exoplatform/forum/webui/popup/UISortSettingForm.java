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
package org.exoplatform.forum.webui.popup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.exoplatform.forum.ForumUtils;
import org.exoplatform.forum.common.webui.BaseEventListener;
import org.exoplatform.forum.service.ForumAdministration;
import org.exoplatform.forum.service.SortSettings;
import org.exoplatform.forum.webui.BaseForumForm;
import org.exoplatform.forum.webui.UICategory;
import org.exoplatform.forum.webui.UIForumPortlet;
import org.exoplatform.forum.webui.UITopicContainer;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIFormSelectBox;

@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "app:/templates/forum/webui/popup/UIFormForum.gtmpl",
    events = {
      @EventConfig(listeners = UISortSettingForm.SaveActionListener.class),
      @EventConfig(listeners = UISortSettingForm.CloseActionListener.class, phase = Phase.DECODE)
    }
)
public class UISortSettingForm extends BaseForumForm implements UIPopupComponent {
  public static final String  FIELD_FORUMSORTBY_INPUT     = "forumSortBy";

  public static final String  FIELD_FORUMSORTBYTYPE_INPUT = "forumSortByType";

  public static final String  FIELD_TOPICSORTBY_INPUT     = "topicSortBy";

  public static final String  FIELD_TOPICSORTBYTYPE_INPUT = "topicSortByType";

  private ForumAdministration administration;
  private Locale locale = null;

  public UISortSettingForm() {

    addUIFormInput(new UIFormSelectBox(FIELD_FORUMSORTBY_INPUT, FIELD_FORUMSORTBY_INPUT, null));
    addUIFormInput(new UIFormSelectBox(FIELD_FORUMSORTBYTYPE_INPUT, FIELD_FORUMSORTBYTYPE_INPUT, null));
    addUIFormInput(new UIFormSelectBox(FIELD_TOPICSORTBY_INPUT, FIELD_TOPICSORTBY_INPUT, null));
    addUIFormInput(new UIFormSelectBox(FIELD_TOPICSORTBYTYPE_INPUT, FIELD_TOPICSORTBYTYPE_INPUT, null));
  }

  public void setInitForm() throws Exception {
    administration = getForumService().getForumAdministration();
  }
  
  @Override
  public void processRender(WebuiRequestContext context) throws Exception {
    changeLocale();
    super.processRender(context);
  }
  
  private void changeLocale() throws Exception {
    PortalRequestContext portalContext = Util.getPortalRequestContext();
    Locale locale = portalContext.getLocale();
    if (this.locale == null || !locale.getLanguage().equals(this.locale.getLanguage())) {
      initFields();
      this.locale = locale;
    }
  }

  private List<SelectItemOption<String>> getSortDirectionOptions() {
    List<SelectItemOption<String>> ls = new ArrayList<SelectItemOption<String>>();
    ls.add(new SelectItemOption<String>(getLabel(SortSettings.Direction.ASC.toString()), SortSettings.Direction.ASC.toString()));
    ls.add(new SelectItemOption<String>(getLabel(SortSettings.Direction.DESC.toString()), SortSettings.Direction.DESC.toString()));
    return ls;
  }

  private void initFields() {
    UIFormSelectBox topicSortByType = getUIFormSelectBox(FIELD_TOPICSORTBYTYPE_INPUT).setOptions(getSortDirectionOptions());
    topicSortByType.setValue(administration.getTopicSortByType());
    
    List<SelectItemOption<String>> ls = new ArrayList<SelectItemOption<String>>();
    List<String> topicSorts = SortSettings.getTopicSortBys();
    for (String sortBy : topicSorts) {
      ls.add(new SelectItemOption<String>(getLabel(sortBy), sortBy));
    }
    UIFormSelectBox topicSortBy = getUIFormSelectBox(FIELD_TOPICSORTBY_INPUT).setOptions(ls);
    topicSortBy.setValue(administration.getTopicSortBy());
    
    //
    UIFormSelectBox forumSortByType = getUIFormSelectBox(FIELD_FORUMSORTBYTYPE_INPUT).setOptions(getSortDirectionOptions());
    forumSortByType.setValue(administration.getForumSortByType());
    
    ls = new ArrayList<SelectItemOption<String>>();
    List<String> forumSorts = SortSettings.getForumSortBys();
    for (String sortBy : forumSorts) {
      ls.add(new SelectItemOption<String>(this.getLabel(sortBy), sortBy));
    }
    UIFormSelectBox forumSortBy = getUIFormSelectBox(FIELD_FORUMSORTBY_INPUT).setOptions(ls);
    forumSortBy.setValue(administration.getForumSortBy());
  }

  public void activate() {
  }

  public void deActivate() {
  }

  static public class SaveActionListener extends BaseEventListener<UISortSettingForm> {
    public void onEvent(Event<UISortSettingForm> event, UISortSettingForm uiForm, String objId) throws Exception {
      UIForumPortlet forumPortlet = uiForm.getAncestorOfType(UIForumPortlet.class);
      String forumSortBy = uiForm.getUIFormSelectBox(FIELD_FORUMSORTBY_INPUT).getValue();
      String forumSortByType = uiForm.getUIFormSelectBox(FIELD_FORUMSORTBYTYPE_INPUT).getValue();
      String topicSortBy = uiForm.getUIFormSelectBox(FIELD_TOPICSORTBY_INPUT).getValue();
      String topicSortByType = uiForm.getUIFormSelectBox(FIELD_TOPICSORTBYTYPE_INPUT).getValue();

      if (!forumSortBy.equals(uiForm.administration.getForumSortBy()) || !forumSortByType.equals(uiForm.administration.getForumSortByType())) {
        forumPortlet.findFirstComponentOfType(UICategory.class).setIsEditForum(true);
      }

      ForumAdministration forumAdministration = uiForm.administration;
      forumAdministration.setForumSortBy(forumSortBy);
      forumAdministration.setForumSortByType(forumSortByType);
      forumAdministration.setTopicSortBy(topicSortBy);
      forumAdministration.setTopicSortByType(topicSortByType);
      try {
        uiForm.getForumService().saveForumAdministration(forumAdministration);
        forumPortlet.findFirstComponentOfType(UITopicContainer.class).setOrderBy(ForumUtils.EMPTY_STR);
      } catch (Exception e) {
        uiForm.log.error("failed to save forum administration", e);
      }
      forumPortlet.cancelAction();
      event.getRequestContext().addUIComponentToUpdateByAjax(forumPortlet);
    }
  }

  static public class CloseActionListener extends BaseEventListener<UISortSettingForm> {
    public void onEvent(Event<UISortSettingForm> event, UISortSettingForm uiForm, String objId) throws Exception {
      UIForumPortlet forumPortlet = uiForm.getAncestorOfType(UIForumPortlet.class);
      forumPortlet.cancelAction();
    }
  }
}
