/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
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
package org.exoplatform.forum.common.webui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.exoplatform.commons.serialization.api.annotations.Serialized;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.lifecycle.UIContainerLifecycle;

@ComponentConfig(lifecycle = UIContainerLifecycle.class)
@Serialized
public class UIScriptBBCodeContainer extends UIContainer {
  public UIScriptBBCodeContainer() {
  }
  @Override
  public void processRender(WebuiRequestContext context) throws Exception {
    Collection<String> c = BuildRendering.getCodeHighlighters(context);
    List<String> codeHighlighters = (c != null) ? new ArrayList<String>(c) : null;
    context.getWriter().append("<span id=\"").append(getId()).append("\">")
           .append(WebUIUtils.attachJSSyntaxHighlighter(codeHighlighters));
    context.getWriter().append("</span>");
  }
}