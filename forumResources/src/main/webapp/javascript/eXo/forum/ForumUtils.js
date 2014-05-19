(function (gj) {
  var ForumUtils = {
    onResizeCallback : {},
    hideElementList : new Array(),
    currWidth : 0,
    cancelEvent : function(evt) {
      var event = evt || window.event;
      event.cancelBubble = true;
      gj.event.fix(event).stopPropagation();
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
      var object = gj(elm);
      if (!ForumUtils.hideElementList.contains(object)) {
        ForumUtils.hideElementList.push(object);
      }
    },

    onResize : function(callback) {
      if (callback && String(typeof callback) == "function") {
        var name = String(callback.name + new Date().getTime());
        ForumUtils.onResizeCallback[name] = callback;
        eXo.core.Browser.addOnResizeCallback(name, callback);
      }
    },

    // mask layer of uiForm popup
    setMaskLayer : function(id) {
      var portlet = gj('div#' + id);

      if (portlet.exists()) {
        var jmaskLayer = gj('div.KSMaskLayer');
        jmaskLayer.css('width', 'auto').css('height', 'auto');
        var jpopupAction = portlet.find('span.UIKSPopupAction');
        var jpopupWindow = jpopupAction.find('.UIPopupWindow');
        if (jpopupWindow.exists()) {
          if (jpopupWindow.css('display') == 'block') {
            jmaskLayer.css('width', (portlet.outerWidth() - 3) + 'px').css('height', (portlet.outerHeight() - 3) + 'px');
          }
          var closeButton = jpopupAction.find('.uiIconClose');
          if (closeButton.exists()) {
            var newDiv = closeButton.find('div.ClosePopup');
            if (!newDiv.exists()) {
              newDiv = gj('<div><span></span></div>');
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
    showUserMenu : function(evt) {
      var evt = evt || window.event;
      var event = gj.event.fix(evt); 
      var jobj = gj(this);
      var jPopup = jobj.find(".uiUserMenuInfo");
      if (!jPopup.exists()) {
        return;
      }
      ForumUtils.hideElements();
      jPopup.on('click', ForumUtils.cancelEvent);
      jPopup.css('visibility', 'inherit').css('display', 'inline');
      if (ForumUtils.isChrome()) {
        jPopup.css('float', 'right');
      }
      var Browser = eXo.core.Browser;
      var X = Browser.findMouseRelativeX(jobj, event, false);
      var Y = Browser.findMouseRelativeY(jobj, event);
      jPopup.css('left', (X - 37) + 'px');
      jPopup.css('top', (Y + 5) + 'px');
      ForumUtils.addhideElement(jPopup);
      ForumUtils.cancelEvent(evt);
      jPopup.find('li').on('click', ForumUtils.hideElements);
    },
    
    initTooltip : function(id) {
      if(id != null) {
      gj('#'+id).find('[rel=tooltip]').tooltip();
      }
    },
    setCookies : function(name, value, expiredays) {
      var exdate = new Date();
      exdate.setDate(exdate.getDate() + expiredays);
      expiredays = ((expiredays == null) ? "" : ";expires=" + exdate.toGMTString());
      var path = ';path=/portal';
      document.cookie = name + "=" + escape(value) + expiredays + path;
    },
    getCookie : function(name) {
      return eXo.core.Browser.getCookie(name);
    },

    submitFCKEditorForm : function(elm) {
      if (eXo.core.Browser.isIE()) {
        var fck = gj(elm).parents('.UIForm:first').find('iframe:first');
        if (fck.exists()) {
          var elmId = fck.attr('id').replace('___Frame', '');
          if(window.FCKeditorAPI.Instances && window.FCKeditorAPI.Instances[elmId]) {
            window.FCKeditorAPI.Instances[elmId].UpdateLinkedField();
          } else if(window.forumFCK) {
            window.forumFCK.UpdateLinkedField();
          }
        }
      }
      //
      var action = gj(elm).attr('data-link').replace('javascript:', '');
      gj.globalEval(action);
    }
    
  };

  window.Array.prototype.clear = function() {
    this.length = 0;
  };

  window.Array.prototype.pushAll = function(array) {
    if (array != null) {
      for ( var i = 0; i < array.length; i++) {
        this.push(array[i]);
      }
    }
  };

  window.Array.prototype.contains = function (element) {
    for (var i = 0; i < this.length; i++) {
      if (this[i] == element) {
        return true ;
      }
    }
    return false ;
  } ;

  gj(window).resize(function(evt) {
    eXo.core.Browser.managerResize();
    if (ForumUtils.currWidth != document.documentElement.clientWidth) {
      try{
        var callback = ForumUtils.onResizeCallback ;
        for(var name in callback) {
          var method = callback[name];
          if (typeof(method) == "function") method(evt) ;
        }
      }catch(e){};
    }
    ForumUtils.currWidth = document.documentElement.clientWidth;
  });
  gj('body').click(ForumUtils.hideElements);

  window.eXo = window.eXo || {};
  window.eXo.forum = window.eXo.forum || {};
  window.eXo.forum.ForumUtils = window.eXo.forum.ForumUtils || {};
  window.eXo.forum.ForumUtils.submitFCKEditorForm= ForumUtils.submitFCKEditorForm;
 return ForumUtils;
})(gj);
