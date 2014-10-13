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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.forum.rendering.core.SupportedSyntaxes;
import org.exoplatform.forum.rendering.spi.MarkupRenderDelegate;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.core.UIApplication;

public class BuildRendering {
  private static final Log LOG = ExoLogger.getLogger(BuildRendering.class);
  private static Map<String, Set<String>> codeHighlighterMap = new HashMap<String, Set<String>>();
  private static Map<String, List<String>> supportedLangs = new HashMap<String, List<String>>();
  private static final Pattern codeHighlighterPattern = Pattern.compile("(\\[code=.*?\\]|brush:.*?;)", Pattern.CASE_INSENSITIVE);
  private static final String[] dataCodes = new String [] {
    "applescript|AppleScript",
    "actionscript3 as3|AS3",
    "bash shell sh|Bash",
    "coldfusion cf|ColdFusion",
    "cpp c|Cpp",
    "c# c-sharp csharp|CSharp",
    "css less|Css",
    "delphi pascal pas|Delphi",
    "diff patch|Diff",
    "erl erlang|Erlang",
    "groovy|Groovy",
    "haxe hx|Haxe",
    "java|Java",
    "jfx javafx|JavaFX",
    "js jscript javascript|JScript",
    "perl Perl pl|Perl",
    "php|Php",
    "text plain|Plain",
    "powershell ps posh|PowerShell",
    "py python|Python",
    "ruby rails ror rb|Ruby",
    "sass scss|Sass",
    "scala|Scala",
    "sql|Sql",
    "ts typescript|TypeScript",
    "vb vbnet|Vb",
    "xml xhtml xslt html|Xml"
  };
  
  static {
    for (int i = 0; i < dataCodes.length; i++) {
      String []langValue = dataCodes[i].split("\\|");
      supportedLangs.put(langValue[1], Arrays.asList(langValue[0].split(" ")));
    }
  }

  public static Map<String, List<String>> getCodeSupportedLangs() {
    return supportedLangs;
  }
  
  public static abstract class AbstractRenderDelegate<T> implements MarkupRenderDelegate<T> {
    public String getMarkup(T target) {
      return processCodeHighlighter(CommonUtils.decodeSpecialCharToHTMLnumberIgnore(getMessage(target)));
    }
    public String getSyntax(T target) {
      return SupportedSyntaxes.bbcode.name();
    }
    public abstract String getMessage(T target);
  }
  
  protected static String processCodeHighlighter(String postContent) {
    //Not support BBCode with CODE<option> tag 
    WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
    Set<String> codeHighlighters = codeHighlighterMap.get(context.getUIApplication().getId());
    if (codeHighlighters == null) {
      return postContent;
    }
    Matcher m = codeHighlighterPattern.matcher(postContent);
    String g;
    Map<String, List<String>> supportedLangs = getCodeSupportedLangs();
    while (m.find()) {
      g = m.group().toLowerCase();
      g = g.replace("[code=", "").replace("]", "").replace("brush:", "").replace(";", "");
      for (String lang : supportedLangs.keySet()) {
        List<String> supportType = supportedLangs.get(lang);
        if (supportType.contains(g)) {
          codeHighlighters.add(lang);
        }
      }
    }
    return postContent;
  }

  public static void startRender(WebuiRequestContext context) {
    UIApplication uiApplication = context.getUIApplication();
    codeHighlighterMap.put(uiApplication.getId(), new HashSet<String>());
    UIScriptBBCodeContainer scriptContainer = uiApplication.getChild(UIScriptBBCodeContainer.class);
    if(scriptContainer == null) {
      try {
        uiApplication.addChild(UIScriptBBCodeContainer.class, null, null).setRendered(true);
      } catch (Exception e) {
        LOG.error("Failed to add component : " + e.getMessage(), e);  
      }
    }
    context.addUIComponentToUpdateByAjax(scriptContainer);
  }

  public static Collection<String> getCodeHighlighters(WebuiRequestContext context) {
    return codeHighlighterMap.get(context.getUIApplication().getId());
  }

  public static void endRender(WebuiRequestContext context) {
    codeHighlighterMap.remove(context.getUIApplication().getId());
  }
}
