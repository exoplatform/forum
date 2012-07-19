
;(function($, window, document) {
  
  var ForumUtils = {
    cancelEvent : function(evt) {
      var _e = evt || window.event;
      _e.cancelBubble = true;
      return;
    },
    returnFalse : function() {
      return false;
    },
    isChrome : function() {
      var str = String(navigator.userAgent).toLowerCase();
      return (str.indexOf('chrome') >= 0 && window.chrome != null && window.chrome != undefined);
    },
    getKeynum : function(event) {
      var keynum = -1;
      if (window.event) { /* IE */
        event = window.event;
        keynum = event.keyCode;
      } else if (event.which) { /* Netscape/Firefox/Opera */
        keynum = event.which;
      }
      if (keynum == 0) {
        keynum = event.keyCode;
      }
      return keynum;
    },
    
    // hide all popup menu.
    hideElementList : new Array(),
    hideElements : function() {
      var l = ForumUtils.hideElementList.length;
      for ( var i = 0; l > 0 && i < l; i++) {
        ForumUtils.hideElementList[i].hide();
      }
      ForumUtils.hideElementList.clear();
    },
    addhideElement : function(elm) {
      var object = $(elm);
      if (!ForumUtils.hideElementList.contains(object)) {
        ForumUtils.hideElementList.push(object);
      }
    },
    
    // mask layer of uiForm popup
    setMaskLayer : function(id) {
      var portlet = $('div#' + id);
      
      if (portlet.exists()) {
        var jmaskLayer = $('div.KSMaskLayer');
        jmaskLayer.css('width', 'auto').css('height', 'auto');
        var jpopupAction = portlet.find('span.UIKSPopupAction');
        var jpopupWindow = jpopupAction.find('.UIPopupWindow');
        if (jpopupWindow.exists()) {
          if (jpopupWindow.css('display') == 'block') {
            jmaskLayer.css('width', (portlet.outerWidth() - 3) + 'px').css('height', (portlet.outerHeight() - 3) + 'px');
          }
          var closeButton = jpopupAction.find('.CloseButton');
          if (closeButton.exists()) {
            var newDiv = closeButton.find('div.ClosePopup');
            if (!newDiv.exists()) {
              newDiv = $('<div><span></span></div>');
              newDiv.addClass('ClosePopup');
              closeButton.append(newDiv);
            }
            var w = closeButton.outerWidth();
            var h = closeButton.outerHeight();
            newDiv.css('width', ((w > 0) ? w : 22) + 'px');
            newDiv.css('height', ((h > 0) ? h : 16) + 'px');
            newDiv.on('click', function(event) {
              jmaskLayer.css('width', 'auto').css('height', 'auto');
            });
          }
        }
        jmaskLayer.on('selectstart', this.returnFalse);
        jmaskLayer.on('dragstart', this.returnFalse);
        jmaskLayer[0].unselectable = "no";
      }
    },
    
    // show users menu
    showUserMenu : function(obj, event) {
      var event = event || window.event;
      var jobj = $(obj);
      var jPopup = jobj.find(".UIPopupInfoMenu");
      if (!jPopup.exists()) {
        return;
      }
      var uiPopup = jPopup.find(".UIPopupInfoContent");
      uiPopup.on('click', ForumUtils.cancelEvent);
      ForumUtils.hideElements();
      jPopup.css('visibility', 'inherit').css('display', 'inline');
      if (ForumUtils.isChrome()) {
        jPopup.css('float', 'right');
      }
      var Browser = eXo.core.Browser;
      var X = Browser.findMouseRelativeX(jPopup, event, false);
      var Y = Browser.findMouseRelativeY(jPopup, event);
      ForumUtils.cancelEvent(event);
      uiPopup.css('left', (X - 37) + 'px');
      uiPopup.css('top', (Y + 5) + 'px');
      ForumUtils.addhideElement(jPopup);
    }
  };
  
  $('body').click(ForumUtils.hideElements);
  window.eXo = window.eXo || {};
  window.eXo.forum = window.eXo.forum || {};
  window.eXo.forum.ForumUtils = ForumUtils;
})(gj, window, document);
