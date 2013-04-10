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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.portal.webui.container.UIContainer;
import org.exoplatform.services.organization.GroupHandler;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;

/**
 * @author <a href="mailto:haithanh0809@gmail.com">Hai Thanh Nguyen</a>
 * @version $Id$
 *
 */
@ComponentConfig(
   template = "classpath:groovy/forum/common/UIPermissionGrid.gtmpl",
   events = { 
      @EventConfig(listeners = UIPermissionGrid.TogglePermissionActionListener.class),
      @EventConfig(listeners = UIPermissionGrid.RemoveOwnerActionListener.class)
   }
)
public class UIPermissionGrid extends UIContainer {
  private Set<String>              permissions;

  private Map<String, Set<String>> owners;

  public UIPermissionGrid() {
    permissions = new LinkedHashSet<String>();
    owners = new HashMap<String, Set<String>>();
    setId("UIPermissionGrid");
  }

  public static class RemoveOwnerActionListener extends EventListener<UIPermissionGrid> {
    @Override
    public void execute(Event<UIPermissionGrid> event) throws Exception {
      String owner = event.getRequestContext().getRequestParameter(OBJECTID);
      UIPermissionGrid source = event.getSource();
      source.removeOwner(owner);
      event.getRequestContext().addUIComponentToUpdateByAjax(source);
    }
  }

  public static class TogglePermissionActionListener extends EventListener<UIPermissionGrid> {
    @Override
    public void execute(Event<UIPermissionGrid> event) throws Exception {
      String requestParam = event.getRequestContext().getRequestParameter(OBJECTID);
      UIPermissionGrid grid = event.getSource();
      String owner = requestParam.substring(0, requestParam.indexOf(CommonUtils.COMMA));
      String permission = requestParam.substring(owner.length() + 1);
      if (grid.hasPermission(owner, permission)) {
        grid.removePermission(owner, permission);
      } else {
        grid.addPermission(owner, permission);
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(event.getSource());
    }
  }

  void setPermissions(String... permissions) {
    this.permissions.addAll(Arrays.asList(permissions));
  }

  protected Set<String> getPermissions() {
    return permissions;
  }

  protected String getLabelPermission(String id) throws Exception {
    BaseUIForm uiForm = getAncestorOfType(BaseUIForm.class);
    return uiForm.getLabel(id);
  }

  void setOwners(String... values) {
    for (String value : values) {
      if (value.length() == 0)
        continue;

      if (owners.containsKey(value)) {
        continue;
      }
      owners.put(value, new HashSet<String>(permissions));
    }
  }

  Set<String> getOwners() {
    return owners.keySet();
  }

  void removeOwner(String owner) {
    owners.remove(owner);
  }

  boolean hasPermission(String owner, String permission) {
    if (owners.containsKey(owner)) {
      Set<String> permissions = owners.get(owner);
      return permissions.contains(permission);
    }
    return false;
  }

  boolean removePermission(String owner, String permission) {
    if (owners.containsKey(owner)) {
      Set<String> permissions = owners.get(owner);
      return permissions.remove(permission);
    }
    return true;
  }

  boolean addPermission(String owner, String permission) {
    if (owners.containsKey(owner)) {
      Set<String> permissions = owners.get(owner);
      return permissions.add(permission);
    }
    owners.put(owner, new HashSet<String>(Arrays.asList(permission)));
    return true;
  }

  String getOwnersByPermission(String permission) {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, Set<String>> entry : owners.entrySet()) {
      Set<String> permissions = entry.getValue();
      if (permissions.contains(permission)) {
        sb.append(entry.getKey());
        sb.append(',');
      }
    }
    return sb.length() == 0 ? CommonUtils.EMPTY_STR : sb.substring(0, sb.length() - 1);
  }

  String getDisplayNameOfOwner(String owner) throws Exception {
    if (CommonUtils.isEmpty(owner) == true) {
      return CommonUtils.EMPTY_STR;
    }

    OrganizationService service = (OrganizationService) PortalContainer.getInstance().getComponentInstanceOfType(OrganizationService.class);
    UserHandler userHandler = service.getUserHandler();
    GroupHandler groupHandler = service.getGroupHandler();
    if (owner.contains(CommonUtils.SLASH)) {
      if (owner.contains(CommonUtils.COLON)) {
        String membership = owner.substring(0, owner.indexOf(CommonUtils.COLON));
        String groupId = owner.substring(membership.length() + 1);
        String groupName = groupHandler.findGroupById(groupId).getGroupName();
        return membership + " in " + groupName;
      }
      return groupHandler.findGroupById(owner).getGroupName();
    } else {
      User user = userHandler.findUserByName(owner);
      String displayName = user.getDisplayName();
      if (CommonUtils.isEmpty(displayName) || owner.equals(displayName)) {
        displayName = user.getFirstName() + CommonUtils.SPACE + user.getLastName();
      }
      return displayName;
    }
  }
}
