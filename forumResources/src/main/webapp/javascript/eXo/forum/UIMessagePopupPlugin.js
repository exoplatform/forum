/**
 * The plugin to replace system confirm and alert by message-popup.
 *  - Default message-popup is confirm-popup
 *  - If the value of settings.isAlert or value of action execute is empty or null, the message-popup is alert popup.
 * 
 * Using: jQuery, UIMaskLayer, UIPopupWindow
 * 
 * Syntax:
 *  - In html code:
 *  + option 1: <a class="confirm" href="javascript:if(confirm('message...')) { ... action ...}"></a>
 *  + option 2: <a class="confirm" href="javascript:void(0);" onclick="javascript:if(confirm('')) { ... action ...}"></a>
 *  + option 3: <div class="confirm" onclick="javascript:if(confirm('message...')) { ... action ...}"></div>
 *  + option 4: <div class="confirm" data-confirm="message..." onclick="action"></div>
 *  + option 5: <div class="confirm" data-confirm="message..." data-action="action"></div>
 *
 *  + option 6: <div class="confirmManual"></div>
 *  
 *  - Call by js:
 *  + for option from 1 to 5: $('.confirm').confirmation({});
 *  + for option 6: $('.confirmManual').confirmation({action: '...action...', message: 'message....'});
 * 
 */

(function(utils, $, uiMaskLayer, popupWindow, document, window) {
  var defaultSettings = {
    action : '',
    message: '',
    title  : '',
    isMulti: false,
    messages : {},
    isClose : false,
    isAlert : false,
    idStatic : 'ForumPopupConfirmation'
  };

  function log(v) {
    if(window.console && window.console.log) {
      window.console.log(v);
    }
  }

  function isEmptyAction(action) {
    if(isEmpty(action) === true || action.indexOf('javascript:void') >= 0) {
      return true;
    }
    return false;
  }

  function isEmpty(str) {
    if(typeof str === 'undefined' || $.trim(str).length === 0) {
      return true;
    }
    return false;
  }

  function ExecuteCurrentMessagePopup() {
    var currentConfirm = utils.getCookie('forumCurrentConfirm');
    if (currentConfirm && String(currentConfirm).length > 0) {
      var jcurrentConfirm = $('#' + currentConfirm);
      if (jcurrentConfirm.length > 0) {
        if(currentConfirm === defaultSettings.idStatic) {
          jcurrentConfirm.removeAttr('id');
        }
        jcurrentConfirm.trigger('click');
      }
    }
  }
  
  var confirmation = function(settings) {
    settings = $.extend(true, {}, defaultSettings, settings);
    
    var jelm, isClose = settings.isClose, isAlert = settings.isAlert;

    function onExecuteConfirm(evt) {
      isAlert = settings.isAlert
      var thizz = $(this);
      if (thizz.hasClass('disabled') === false && thizz.parent().hasClass('disabled') === false) {
        var id = thizz.attr('id');
        if (typeof id === 'undefined' || id.length === 0) {
          $('#' + defaultSettings.idStatic).removeAttr('id');
          id = defaultSettings.idStatic;
          thizz.attr('id', id);
        }

        var actionOK = settings.action;
        var message = '';
        if (settings.isMulti === true) {
          var number = jelm.attr('data-number') * 1;
          if (number === 0) {
            message = jelm.attr('data-confirm-not');
            actionOK = '';
            isClose = true;
            isAlert = true;
          } else if (number === 1) {
            message = jelm.attr('data-confirm-one');
          } else if (number > 1) {
            message = jelm.attr('data-confirm-number');
            message = message.replace('{0}', number);
          }
        } else {
          message = settings.message;
        }
        if(message.indexOf('?') > 0) {
          message = message.replace("?", "") + " ?";
        }

        var title = settings.title;
        //
        confirmPopup(id, actionOK, message, title);
        
        //
        utils.cancelEvent(evt);
        evt.preventDefault();
      }
    }

    function activeConfirm() {
      jelm.off('click', onExecuteConfirm);
      jelm.on('click', onExecuteConfirm);
    }

    function makeTemplate() {
      $('#UIForumPopupConfirmation').remove();
      var popup = $('.UIPopupConfirmation:first').clone();
      popup.attr('id', 'UIForumPopupConfirmation');
      return popup;
    }
  
    function confirmPopup(id, actionOK, message, title) {
      utils.setCookies('forumCurrentConfirm', id, 300);
      var popup = makeTemplate();
      if(isAlert === true && isEmpty(title) === true) {
        title =  popup.find('.Warning:first').text();
      }
      if(typeof title === 'string' && title.length > 0) {
        popup.find('.popupTitle:first').html(title);
      }

      //
      var content = popup.find('.contentMessage:first');
      content.html(message);

      //
      if(isAlert === true) {
        popup.find('.actionOK:first').remove();
        content.removeClass('confirmationIcon').addClass('warningIcon');
      } else {
        addActionOK(popup, actionOK);
      }
      
      if(isClose === true) {
        popup.find('.actionCancel:first').remove();
      } else {
        popup.find('.actionClose:first').remove();
      }

      popup.find('.uiIconClose:first').on('click', hiden);
      popup.find('.btn').on('click', hiden);
      //
      show(popup);
      
      popup.attr('data-elmid', id);
    }
  
    function addActionOK(popup, actionOK) {
      var btnOK = popup.find('.actionOK:first');
      if (typeof actionOK === 'function') {
        btnOK.on('mouseup', actionOK);
      } else if (isEmpty(actionOK) === false) {
        btnOK.attr('onclick', actionOK)
      } else {
        btnOK.remove();
      }
    }
  
    function show(popup) {
      $('#UIPortalApplication').append(popup);
      popup.css({
        height : 'auto',
        width : '400px',
        visibility : 'hidden',
        display : 'block'
      });
      var pHeight = popup.height();
      var top = ($(window).height() - pHeight) / 2 - 30;
      top = ((top > 10) ? top : 10) + $(window).scrollTop();
      var left = ($(window).width() - popup.width()) / 2;
      popup.css({
        'top' : top + 'px',
        'left' : left + 'px',
        'visibility' : 'visible',
        'overflow' : 'hidden'
      });
      popup.animate({ height : pHeight + 'px' }, 500, function() { });
      uiMaskLayer.createMask(popup[0].parentNode, popup[0], 1);
      popupWindow.initDND(popup.find('.popupTitle')[0], popup[0]);
    }
  
    function hiden(e) {
      var thiz = $(this);
      var popup = thiz.parents('#UIForumPopupConfirmation')
      if (popup.length > 0) {
        //
        var id = popup.attr('data-elmid');
        if(id === defaultSettings.idStatic) {
          $('#'+id).removeAttr('id');
        }
        //
        uiMaskLayer.removeMask(popup[0].previousSibling);
        //
        popup.animate({
          height : '0px'
        }, 300, function() {
          $(this).remove();
        });
      }
      utils.setCookies('forumCurrentConfirm', '', -300);
    }
    
    function processAction() {
      var fullAction = jelm.attr('onclick');
      
      if(isEmptyAction(fullAction)) {
        if(jelm.is('a')) {
          fullAction = jelm.attr('href');
          jelm.attr('href', 'javascript:void(0);');
        }
      } else {
        jelm.removeAttr('onclick');
      }

      if(isEmptyAction(fullAction) === false) {
        //
        if(fullAction.indexOf('confirm')) {
          getVlaueConfirm(fullAction);
        }
        
        //
        if(isEmpty(settings.action)) {
          settings.action = fullAction;
        }
        jelm.attr('data-action', settings.action);
        if(typeof jelm.attr('data-confirm') === 'undefined') {
          jelm.attr('data-confirm', settings.message);
        } else {
          settings.message = jelm.attr('data-confirm');
        }
      } else if(isEmpty(settings.action)) {
        settings.action = jelm.attr('data-action');
        settings.message = jelm.attr('data-confirm');
      }
      
    }
    
    function getVlaueConfirm(val) {
      if(isAlert === false) {
        val = val.replace('confirm (', 'confirm(');
        val = val.replace(') )', '))');
        val = val.substring(val.indexOf('confirm(') + 8);
        var message = val.substring(0, val.indexOf('))'));
        message = $.trim(message.substring(0, message.length - 1).substring(1));
        settings.message = message;
        
        var action = val.substring(val.indexOf('))') + 2);
        action = action.replace('{', '').replace('}', '');
        action = action.replace('javascript:', '');
        settings.action = action;
      } else {
        val = val.replace('alert (', 'alert(');
        val = val.substring(val.indexOf('alert(') + 6);
        var message = val.substring(0, val.indexOf(')'));
        message = $.trim(message.substring(0, message.length - 1).substring(1));
        settings.message = message;
      }

    }

    return {
      init : function(action) {
        jelm = $(action);
        
        if(jelm.length > 0) {
          //
          processAction();
          
          //
          activeConfirm();
        }
      }
    };
  };
  
  $.fn.confirmation = function(method, settings) {

    var outerArguments = arguments;

    if (typeof method === 'object' || !method) {
      settings = method;
    }
    
    if(typeof settings === 'undefined') {
      settings = {};
    }

    return this.each(function() {
      var instance = $.data(this, 'confirmation') || $.data(this, 'confirmation', new confirmation(settings));
      if ($.isFunction(instance[method])) {
        return instance[method].apply(this, Array.prototype.slice.call(outerArguments, 1));
      } else if (typeof method === 'object' || !method) {
        return instance.init.call(this, this);
      } else {
        $.error('Method ' + method + ' does not exist');
      }
    });
  };
  
  window.executeCurrentMessagePopup = ExecuteCurrentMessagePopup;
  
  setTimeout(window.executeCurrentMessagePopup, 220);
  
})(forumUtils, gj, uiMaskLayer, popupWindow, document, window);
