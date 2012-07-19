;(function($, window, document) {
  var UIContextMenu = {
    container : new Array(),
    menus : [],
    classNames : new Array(),
    setup : function() {
      var i = UIContextMenu.container.length;
      while (i--) {
        var container = $(UIContextMenu.container[i]);
        if (container.exists()) {
          for ( var j = 0; j < UIContextMenu.classNames.length; j++) {
            var menu = container.find('.' + UIContextMenu.classNames[j]);
            if (menu.exists()) {
              menu.on('contextmenu', UIContextMenu.show);
            }
          }
        }
      }
    },
    setContainer : function(obj) {
      UIContextMenu.container.push(obj);
    },
    getMenu : function(elm, event) {
      var evt = event || window.event;
      var element = elm || UIContextMenu.getMenuElement(evt);
      if (!element || !$(element).exists())
        return;
      var menuId = String(element.attr('id')).replace("Context", "");
      var jcont = element.parents('.PORTLET-FRAGMENT');
      var jmenu = jcont.findId(menuId);
      if (!jmenu.exists())
        return;
      return jmenu;
    },
    getMenuElement : function(evt) {
      var target = eXo.forum.EventManager.getEventTarget(evt);
      for (i = 0; i < UIContextMenu.classNames.length; i++) {
        var parent = $(target).parents('.' + UIContextMenu.classNames[i]);
        if (parent.exists()) {
          return parent;
        }
      }
      return null;
    },
    setPosition : function(context, jobj, evt) {
      var evt = evt || window.event;
      var Browser = eXo.core.Browser;
      var X = Browser.findMouseRelativeX(context, evt, false) || 2;
      var Y = Browser.findMouseRelativeY(context, evt) + 6 || 2;
      jobj.css('position', 'absolute').show();
      jobj.css('left', X + 'px');
      jobj.css('top', Y + 'px');
    },
    show : function(evt) {
      eXo.forum.ForumUtils.hideElements();
      var context = $(this);
      var jmenu = UIContextMenu.getMenu(context, evt);
      if (!jmenu.exists()) {
        return;
      }
      context.parent().css('position', 'relative');
      UIContextMenu.setPosition(context, jmenu, evt);
      eXo.forum.ForumUtils.addhideElement(jmenu);
      eXo.forum.ForumUtils.cancelEvent(evt);
      return false;
    }
  };
  
  window.eXo = window.eXo || {};
  window.eXo.forum = window.eXo.forum || {};
  window.eXo.forum.UIContextMenu = UIContextMenu;
})(gj, window, document);
