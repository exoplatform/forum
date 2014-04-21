/***************************************************************************
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
 ***************************************************************************/
package org.exoplatform.forum.webui.popup;

import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.exoplatform.forum.ForumUtils;
import org.exoplatform.forum.common.webui.WebUIUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormDateTimeInput;
import org.exoplatform.webui.form.UIFormInputBase;
import org.exoplatform.webui.form.UIFormInputSet;
import org.exoplatform.webui.form.UIFormRadioBoxInput;
import org.exoplatform.webui.form.input.UICheckBoxInput;

public class UIForumInputWithActions extends UIFormInputSet {
  public static Log             log             = ExoLogger.getLogger(UIForumInputWithActions.class);

  Map<String, List<ActionData>> actionField     = new HashMap<String, List<ActionData>>();

  private String                actionAddItem;

  private String                labelActionAddItem;

  private String                actionIdAddItem = ForumUtils.EMPTY_STR;

  private Map<String, String>   mapLabelInfo    = new HashMap<String, String>();

  public UIForumInputWithActions(String id) {
    super.setId(id);
  }

  public Map<String, String> getMapLabelInfo() {
    return mapLabelInfo;
  }

  public void setMapLabelInfo(String itemId, String valueInfo) {
    this.mapLabelInfo.put(itemId, valueInfo);
  }

  public String getActionAddItem() {
    return actionAddItem;
  }

  public void setActionAddItem(String actionAddItem) {
    this.actionAddItem = actionAddItem;
  }

  public String getLabelActionAddItem() {
    return labelActionAddItem;
  }

  public void setLabelActionAddItem(String labelActionAddItem) {
    this.labelActionAddItem = labelActionAddItem;
  }

  public String getActionIdAddItem() {
    return actionIdAddItem;
  }

  public void setActionIdAddItem(String actionIdAddItem) {
    this.actionIdAddItem = actionIdAddItem;
  }

  public void setActionField(String fieldName, List<ActionData> actions) throws Exception {
    actionField.put(fieldName, actions);
  }

  public void processRender(WebuiRequestContext context) throws Exception {
    if (getComponentConfig() != null) {
      super.processRender(context);
      return;
    }
    UIForm uiForm = getAncestorOfType(UIForm.class);
    Writer w = context.getWriter();
    w.write("<div id=\"" + getId() + "\" class=\"UIFormInputSet " + getId() + "\">");
    w.write("<div class=\"form-horizontal\">");
    ResourceBundle res = context.getApplicationResourceBundle();

    for (UIComponent inputEntry : getChildren()) {
      if (inputEntry.isRendered() == false) {
        continue;
      }
      String label;
      try {
        label = uiForm.getLabel(res, inputEntry.getId());
        if (inputEntry instanceof UIFormInputBase) {
          ((UIFormInputBase) inputEntry).setLabel(label);
        }
      } catch (MissingResourceException ex) {
        label = inputEntry.getId();
      }
      if (!mapLabelInfo.isEmpty() && mapLabelInfo.containsKey(inputEntry.getId())) {
        w.write("<div class=\"control-group\">");
        w.write("  <div class=\"InfoMessageIcon\" id=\"" + inputEntry.getId() + "Info\">" + mapLabelInfo.get(inputEntry.getId()) + "</div>");
        w.write("</div>");
      }
      
      w.write("<div class=\"control-group\">");
      w.write("<label class=\"control-label\" for=\"" + inputEntry.getId() + "\">");
      w.write(label);
      w.write("</label>");
      w.write("<div class=\"controls\">");
      renderUIComponent(inputEntry);
      List<ActionData> actions = actionField.get(inputEntry.getName());
      if (actions != null) {
        for (ActionData action : actions) {
          String actionLink, actionLabel;
          if (action.getActionParameter() != null) {
            actionLink = ((UIComponent) getParent()).event(action.getActionListener(), action.getActionParameter());
          } else {
            actionLink = ((UIComponent) getParent()).event(action.getActionListener());
          }

          if (action.getActionType() == ActionData.TYPE_ATT) {
            actionLabel = action.getActionName();
            w.write("<span title=\"" + actionLabel + "\" rel=\"tooltip\" data-placement=\"bottom\">");
            w.write("<i class=\"" + action.getCssIconClass() + "\"></i>");
            if (action.isShowLabel) {
              String size = ForumUtils.EMPTY_STR;

              if (actionLabel.lastIndexOf("(") > 0) {
                size = actionLabel.substring(actionLabel.lastIndexOf("("));
                actionLabel = actionLabel.substring(0, actionLabel.lastIndexOf("("));
              }
              String type = ForumUtils.EMPTY_STR;
              int dot = actionLabel.lastIndexOf(".");
              if (dot > 0) {
                type = actionLabel.substring(dot);
                actionLabel = actionLabel.substring(0, dot);
              }

              actionLabel = ForumUtils.getSubString(actionLabel, 30);
              //
              w.write(new StringBuilder(actionLabel).append(type).append(size).toString());
            }
            w.write("</span>");

          } else {
            actionLabel = WebUIUtils.getLabel(null, action.getActionName());

            w.write("<a title=\"" + actionLabel + "\" class=\"actionIcon\" rel=\"tooltip\" data-placement=\"bottom\" href=\"" + actionLink + "\">");
            actionLabel = ForumUtils.getSubString(actionLabel, 30);
            if (action.getActionType() == ActionData.TYPE_ICON) {
              w.write("<i alt=\"" + actionLabel + "\" class=\"" + action.getCssIconClass() + "\"></i>");
              if (action.isShowLabel) {
                w.write(actionLabel);
              }
            } else if (action.getActionType() == ActionData.TYPE_LINK) {
              w.write(actionLabel);
            }
            w.write("</a>");
          }
          if (action.isBreakLine()) {
            w.write("<br/>");
          } else {
            w.write("&nbsp;");
          }
        }
      }
      if (inputEntry.getId().equals(actionIdAddItem)) {
    	  w.write("<button class=\"btn\" type=\"button\" onclick=\"" + ((UIComponent) getParent()).event(actionAddItem) + "\">" +
          "<i class=\"uiIconAttach uiIconLightGray\"></i> " + labelActionAddItem + "</button>");
      }
      w.write("</div>");
      w.write("</div>");
    }
    w.write("</div>");
    w.write("</div>");
  }

  public UIFormDateTimeInput getUIFormDateTimeInput(String name) {
    return (UIFormDateTimeInput) findComponentById(name);
  }

  public UIFormRadioBoxInput getUIFormRadioBoxInput(String name) {
    return (UIFormRadioBoxInput) findComponentById(name);
  }

  public UICheckBoxInput getUICheckBoxInput(String name) {
    return (UICheckBoxInput) findComponentById(name);
  }

  static public class ActionData {
    final public static int TYPE_ICON       = 0;

    final public static int TYPE_LINK       = 1;

    final public static int TYPE_ATT        = 2;

    private int             actionType      = 0;

    private String          actionName;

    private String          actionListener;

    private String          actionParameter = null;

    private String          cssIconClass    = "AddNewNodeIcon";

    private boolean         isShowLabel     = false;

    private boolean         isBreakLine     = false;

    public void setActionType(int actionType) {
      this.actionType = actionType;
    }

    public int getActionType() {
      return actionType;
    }

    public void setActionName(String actionName) {
      this.actionName = actionName;
    }

    public String getActionName() {
      return actionName;
    }

    public void setActionListener(String actionListener) {
      this.actionListener = actionListener;
    }

    public String getActionListener() {
      return actionListener;
    }

    public void setActionParameter(String actionParameter) {
      this.actionParameter = actionParameter;
    }

    public String getActionParameter() {
      return actionParameter;
    }

    public void setCssIconClass(String cssIconClass) {
      this.cssIconClass = cssIconClass;
    }

    public String getCssIconClass() {
      return cssIconClass;
    }

    public void setShowLabel(boolean isShowLabel) {
      this.isShowLabel = isShowLabel;
    }

    public boolean isShowLabel() {
      return isShowLabel;
    }

    public void setBreakLine(boolean isBreakLine) {
      this.isBreakLine = isBreakLine;
    }

    public boolean isBreakLine() {
      return isBreakLine;
    }
  }
}
