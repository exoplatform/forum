/*
 * Copyright (C) 2012 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.forum.common.webui;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.forum.common.UserHelper;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.webui.container.UIContainer;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.UIPortletApplication;
import org.exoplatform.webui.core.UITree;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIFormStringInput;

/**
 * @author <a href="mailto:haithanh0809@gmail.com">Hai Thanh Nguyen</a>
 * @version $Id$
 *
 */
@ComponentConfigs({
   @ComponentConfig(
      template = "classpath:groovy/forum/common/UIPermissionPanel.gtmpl",
      events = { 
         @EventConfig(listeners = UIPermissionPanel.OpenUserPopupActionListener.class),
         @EventConfig(listeners = UIPermissionPanel.OpenRoleAndGroupPopupActionListener.class),
         @EventConfig(listeners = UIPermissionPanel.AddPermissionActionListener.class)
      }
   ),
   
   @ComponentConfig(
      id = "UIPermissionPopupWindow",
      type = UIPopupWindow.class,
      template = "system:/groovy/webui/core/UIPopupWindow.gtmpl",
      events = {
        @EventConfig(listeners = UIPermissionPanel.ClosePopupActionListener.class, name = "ClosePopup"),
        @EventConfig(listeners = UIPermissionPanel.AddActionListener.class, name = "Add", phase = Phase.DECODE),
        @EventConfig(listeners = UIPermissionPanel.ClosePopupActionListener.class, name = "Close")
      }
   )
})
public class UIPermissionPanel extends UIContainer implements UISelector {

  private static final String PERMISSION_INPUT = "permissionInput";

  private static final String PERMISSION_GRID  = "UIPermissionGrid";

  private static final String POPUP_WINDOW_ID  = "UIPermissionPopupWindow";
  
  private static final String selectOwner      = "UIPermissionPanel.label.SelectOwner";
  
  private String              spaceGroupId     = null;

  public UIPermissionPanel() throws Exception {
    setId("UIPermissionPanel");
    UIFormStringInput stringInput = new UIFormStringInput(PERMISSION_INPUT, PERMISSION_INPUT, null);
    UIPermissionGrid grid = createUIComponent(UIPermissionGrid.class, null, PERMISSION_GRID);
    addChild(stringInput);
    addChild(grid);
  }
  
  protected void initPlaceholder() throws Exception {
    UIFormStringInput stringInput = (UIFormStringInput) getChildById(PERMISSION_INPUT);
    stringInput.setHTMLAttribute("placeholder", WebUIUtils.getLabel(null, selectOwner));
    stringInput.setHTMLAttribute("style", "vertical-align: top; margin:0px; padding-left:3px");
  }

  public String getSpaceGroupId() {
    return spaceGroupId;
  }

  public void setSpaceGroupId(String spaceGroupId) {
    this.spaceGroupId = spaceGroupId;
  }

  public void setPermission(String spaceGroupId, String... permissions) {
    setSpaceGroupId(spaceGroupId);
    //
    UIPermissionGrid grid = getChildById(PERMISSION_GRID);
    grid.setPermissions(permissions);
  }

  public String getOwnersByPermission(String permission) {
    UIPermissionGrid grid = getChildById(PERMISSION_GRID);
    return grid.getOwnersByPermission(permission);
  }

  public void addPermissionForOwners(String permission, String[] owners) {
    UIPermissionGrid grid = getChildById(PERMISSION_GRID);
    for (String owner : owners) {
      if (owner.length() == 0)
        continue;
      grid.addPermission(owner, permission);
    }
  }

  public static String[] splitValues(String str) {
    if (CommonUtils.isEmpty(str) == false) {
      str = StringUtils.remove(str, CommonUtils.SPACE);
      if (str.contains(CommonUtils.COMMA)) {
        str = str.replaceAll(CommonUtils.SEMICOLON, CommonUtils.COMMA);
        return str.trim().split(CommonUtils.COMMA);
      } else {
        str = str.replaceAll(CommonUtils.COMMA, CommonUtils.SEMICOLON);
        return str.trim().split(CommonUtils.SEMICOLON);
      }
    } else
      return new String[] { CommonUtils.EMPTY_STR };
  }

  private static void closePopup(UIPopupWindow popupWindow) {
    popupWindow.setUIComponent(null);
    popupWindow.setShow(false);
    popupWindow.setRendered(false);
    WebuiRequestContext context = RequestContext.getCurrentInstance();
    context.addUIComponentToUpdateByAjax(popupWindow.getParent());
  }

  public static class AddPermissionActionListener extends EventListener<UIPermissionPanel> {
    @Override
    public void execute(Event<UIPermissionPanel> event) throws Exception {
      UIPermissionPanel panel = event.getSource();
      UIPermissionGrid grid = panel.getChildById(PERMISSION_GRID);
      String value = event.getRequestContext().getRequestParameter(OBJECTID);
      String errorUser = UserHelper.checkValueUser(value);
      if (CommonUtils.isEmpty(errorUser) == false) {
        ((UIFormStringInput)panel.getChildById(PERMISSION_INPUT)).setValue("");       
        WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
        ApplicationMessage message = new ApplicationMessage("UserNameValidator.msg.error-input", 
                                                            new String[] { String.format("\"%s\"", errorUser) }, 
                                                            ApplicationMessage.WARNING);
        context.getUIApplication().addMessage(message);
        ((PortalRequestContext) context.getParentAppRequestContext()).ignoreAJAXUpdateOnPortlets(true);
        return;
      }
      grid.setOwners(splitValues(value));
      ((UIFormStringInput)panel.getChildById(PERMISSION_INPUT)).setValue("");
      event.getRequestContext().addUIComponentToUpdateByAjax(panel);
    }
  }

  public static class OpenUserPopupActionListener extends EventListener<UIPermissionPanel> {
    @Override
    public void execute(Event<UIPermissionPanel> event) throws Exception {
      UIPermissionPanel permissionPanel = event.getSource();

      UIPopupContainer uiPopupContainer = permissionPanel.getAncestorOfType(UIPopupContainer.class);
      UIGroupSelector uiGroupSelector = uiPopupContainer.findFirstComponentOfType(UIGroupSelector.class);
      if (uiGroupSelector != null) {
        UIPopupWindow popupWindow = uiGroupSelector.getAncestorOfType(UIPopupWindow.class);
        closePopup(popupWindow);
      }

      UIPopupWindow uiPopupWindow = uiPopupContainer.getChildById(POPUP_WINDOW_ID);
      if (uiPopupWindow == null){
        uiPopupWindow = uiPopupContainer.addChild(UIPopupWindow.class, POPUP_WINDOW_ID, POPUP_WINDOW_ID);
      }

      //
      UIUserSelect uiUserSelector = uiPopupContainer.createUIComponent(UIUserSelect.class, null, "UIUserSelector");
      uiUserSelector.setShowSearch(true);
      uiUserSelector.setShowSearchUser(true);
      uiPopupWindow.setUIComponent(uiUserSelector);
      uiPopupWindow.setShow(true);
      uiPopupWindow.setWindowSize(740, 400);
      uiPopupWindow.setRendered(true);
      uiPopupContainer.setRendered(true);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupContainer);
    }
  }

  public static class OpenRoleAndGroupPopupActionListener extends EventListener<UIPermissionPanel> {
    @Override
    public void execute(Event<UIPermissionPanel> event) throws Exception {
      UIPermissionPanel permissionPanel = event.getSource();

      UIPopupContainer uiPopupContainer = event.getSource().getAncestorOfType(UIPopupContainer.class);
      UIUserSelect user = uiPopupContainer.findFirstComponentOfType(UIUserSelect.class);
      if (user != null) {
        UIPopupWindow popupWindow = user.getAncestorOfType(UIPopupWindow.class);
        closePopup(popupWindow);
      }

      UIPopupAction popupAction = uiPopupContainer.getChild(UIPopupAction.class);
      if (popupAction == null) {
        popupAction = uiPopupContainer.addChild(UIPopupAction.class, POPUP_WINDOW_ID, POPUP_WINDOW_ID);
      }

      UIPopupWindow uiPopupWindow = popupAction.getChild(UIPopupWindow.class);
      //
      String type = event.getRequestContext().getRequestParameter(OBJECTID);
      String componentId = "UIMemberShipSelector";
      if ("2".equals(type)) {
        componentId = "UIGroupSelector";
      }
      UIGroupSelector group = uiPopupContainer.createUIComponent(UIGroupSelector.class, null, componentId);
      group.setType(type);
      group.setSpaceGroupId(permissionPanel.getSpaceGroupId());
      group.setComponent(event.getSource(), new String[] { PERMISSION_INPUT });
      group.getChild(UITree.class).setId(UIGroupSelector.TREE_GROUP_ID);
      group.getChild(org.exoplatform.webui.core.UIBreadcumbs.class).setId(UIGroupSelector.BREADCUMB_GROUP_ID);

      uiPopupWindow.setUIComponent(group);
      uiPopupWindow.setShow(true);
      uiPopupWindow.setWindowSize(600, 0);
      uiPopupWindow.setRendered(true);
      uiPopupContainer.setRendered(true);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupContainer);
    }
  }

  public static class AddActionListener extends EventListener<UIUserSelect> {
    @Override
    public void execute(Event<UIUserSelect> event) throws Exception {
      UIUserSelect uiUserSelector = event.getSource();
      String values = uiUserSelector.getSelectedUsers();
      UIPermissionPanel uiPermission = uiUserSelector.getAncestorOfType(UIPortletApplication.class)
          .findFirstComponentOfType(UIPermissionPanel.class);
      uiPermission.addValue(values);

      closePopup((UIPopupWindow) uiUserSelector.getParent());
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPermission);
    }
  }

  public static class ClosePopupActionListener extends EventListener<UIPopupWindow> {
    public void execute(Event<UIPopupWindow> event) throws Exception {
      UIPopupWindow popupWindow = event.getSource();
      closePopup(popupWindow);
    }
  }

  @Override
  public void updateSelect(String selectField, String value) throws Exception {
    addValue(value);
  }

  private void addValue(String value) throws Exception {
    UIFormStringInput input = getChildById(PERMISSION_INPUT);
    if (CommonUtils.isEmpty(input.getValue())) {
      input.setValue(value);
    } else {
      input.setValue(input.getValue() + CommonUtils.COMMA + value);
    }
  }
}
