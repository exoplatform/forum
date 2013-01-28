(function(utils, gj, eventManager) {
  var UIContextMenu = {
    container : new Array(),
    menus : [],
    classNames : new Array(),
    setup : function() {
      var i = UIContextMenu.container.length;
      while (i--) {
        var container = gj(UIContextMenu.container[i]);
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
      if (!element || !gj(element).exists())
        return;
      var menuId = String(element.attr('id')).replace("Context", "");
      var jcont = element.parents('.PORTLET-FRAGMENT');
      var jmenu = jcont.findId(menuId);
      if (!jmenu.exists())
        return;
      return jmenu;
    },

    getMenuElement : function(evt) {
      var target = eventManager.getEventTarget(evt);
      for (i = 0; i < UIContextMenu.classNames.length; i++) {
        var parent = gj(target).parents('.' + UIContextMenu.classNames[i]);
        if (parent.exists()) {
          return parent;
        }
      }
      return null;
    },

    setPosition : function(context, jobj, evt) {
      evt = evt || window.event;
      var event = gj.event.fix(evt);
      var Browser = eXo.core.Browser;
      var X = Browser.findMouseRelativeX(context, event, false) || 2;
      var Y = Browser.findMouseRelativeY(context, event) + 6 || 2;
      jobj.css('position', 'absolute').show();
      jobj.css('left', X + 'px');
      jobj.css('top', Y + 'px');
    },

    show : function(evt) {
      utils.hideElements();
      var context = gj(this);
      var jmenu = UIContextMenu.getMenu(context, evt);
      if (jmenu && jmenu.exists()) {
        var parent = context.parents('.PORTLET-FRAGMENT');
        if (parent.exists()) {
          parent.css('position', 'relative');
        } else {
          parent = context;
          parent.parent.css('position', 'relative');
        }
        UIContextMenu.setPosition(parent, jmenu, evt);
        utils.addhideElement(jmenu);
        utils.cancelEvent(evt);
        gj.event.fix(evt).preventDefault();
      }
    }
  };
  return UIContextMenu;
})(utils, gj, eventManager);
