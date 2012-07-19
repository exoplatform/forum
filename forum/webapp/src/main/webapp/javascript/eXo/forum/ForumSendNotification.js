;(function($, window, document) {
  
  function ForumSendNotification() {
    this.notification = "Notification";
    this.message = "message";
    this.post = "post";
    this.titeName = "You have received a new private TYPE";
    this.from = "From";
    this.briefContent = "Brief content";
    this.GoDirectly = "Go directly to the TYPE: LINK Click here.";
  }
  
  ForumSendNotification.prototype.initParam = function(notification, message, post, titeName, from, briefContent, GoDirectly) {
    this.notification = notification;
    this.message = message;
    this.post = post;
    this.titeName = titeName;
    this.from = from;
    this.briefContent = briefContent;
    this.GoDirectly = GoDirectly;
  };
  
  ForumSendNotification.prototype.init = function(eXoUser, eXoToken, contextName) {
    if (String(eXoToken) != '') {
      if (!eXo.core.Cometd.isConnected()) {
        eXo.core.Cometd.url = '/' + contextName + '/cometd';
        eXo.core.Cometd.exoId = eXoUser;
        eXo.core.Cometd.exoToken = eXoToken;
        eXo.core.Cometd.addOnConnectionReadyCallback(this.subcribeCometdSendNotification);
        eXo.core.Cometd.init();
      } else {
        this.subcribeCometdSendNotification();
      }
    }
  };

  ForumSendNotification.prototype.subcribeCometdSendNotification = function() {
    eXo.core.Cometd.subscribe('/eXo/Application/Forum/NotificationMessage', function(eventObj) {
      eXo.forum.ForumSendNotification.alarm(eventObj);
    });
  };

  ForumSendNotification.prototype.alarm = function(eventObj) {
    var message = eXo.core.JSON.parse(eventObj.data); // message
    var parent = $(this.createMessage(message));
    parent.height('auto');
    var popup = parent.find('div.UIPopupNotification');
    popup.show();
    eXo.webui.Box.config(popup[0], popup.outerHeight(), 5, this.openCallback, this.closeBox);
    window.focus();
    return;
  };

  ForumSendNotification.prototype.openCallback = function(obj) {
    $(obj).on('click', function() {
      $(this).hide();
    });
  };

  ForumSendNotification.prototype.closeBox = function(obj) {
    $(obj).hide();
    $(obj).parent().height('0px');
  };

  ForumSendNotification.prototype.createMessage = function(message) {
    var msgBox = $('#msgBox');
    if (msgBox.exists()) {
      var directChildNode = msgBox.find('div.UIPopupNotification:first');
      if (directChildNode.css('visibility') == 'hidden') {
        msgBox.html(this.generateHTML(message));
      } else {
        var contentBox = msgBox.find('div.MCPopupNotification:first');
        var content = contentBox.find('div.contentBox:first');
        content.html(content.html() + '<div style="border-top:1px dashed black;">' + this.getContentHTML(message) + '</div>');
      }
    } else {
      msgBox = $('<div id="msgBox" class="UINotification"></div>').html(this.generateHTML(message));
      $('body').append(msgBox);
    }
    return msgBox;
  };

  ForumSendNotification.prototype.getContentHTML = function(message) {
    var link = '';
    var type = this.message;
    if (message.type == 'PrivatePost') {
      type = this.post;
      link = String(this.GoDirectly).replace('TYPE', type);
      link = link.replace(' LINK', '<a style="color:#204AA0" href="' + String(message.id) + '">') + '</a>';
    } else {
      link = String(this.GoDirectly).replace('TYPE', type);
      var alink = $('#privateMessageLink');
      if (alink.exists()) {
        link = link.replace(' LINK', '<a style="color:#204AA0" href="' + alink.attr('href') + '">') + '</a>';
      }
    }
    var msg = String(message.message).replace(/<\/?[^>]+(>|$)/g, "");
    if (msg.length > 100) {
      msg = msg.substring(0, 100);
    }
    var content = '<div style="padding:7px 0px 7px 5px">' + '<strong>' + String(this.titeName).replace('TYPE', type) + ':</strong> <br/>' + message.name + '<br/>' + '<strong>'
        + this.from + ':</strong> ' + message.from + '<br/>' + '<strong>' + this.briefContent + ':</strong><br/>' + msg + '<br/>' + link + '</div>';
    return content;
  };

  ForumSendNotification.prototype.generateHTML = function(message) {
    var html = '<div class="UIPopupNotification">' + '  <div class="TLPopupNotification">' + '    <div class="TRPopupNotification">'
        + '     <div class="TCPopupNotification"><span></span></div>' + '   </div>' + ' </div>' + ' <div class="MLPopupNotification">' + '    <div class="MRPopupNotification">'
        + '     <div class="MCPopupNotification">' + '        <div class="TitleNotification">' + '          <a class="ItemTitle" href="#">' + this.notification + '</a>'
        + '         <a class="Close" href="#"><span></span></a>' + '        </div>' + '       <div class="contentBox">' + this.getContentHTML(message) + '</div>' + '     </div>' + '   </div>'
        + ' </div>' + ' <div class="BLPopupNotification">' + '    <div class="BRPopupNotification">' + '      <div class="BCPopupNotification"><span></span></div>' + '   </div>'
        + ' </div>' + '</div>';
    return html;
  };

  // Box effect
  function Box() {
    this.speed = 4;
    this.tmpHeight = 0;
    this.autoClose = true;
    this.closeInterval = 30;
  };

  Box.prototype.config = function(obj, height, speed, openCallback, closeCallback) {
    this.object = obj;
    this.maxHeight = height;
    if (speed)
      this.speed = speed;
    this.open();
    if (openCallback)
      this.openCallback = openCallback;
    if (closeCallback)
      this.closeCallback = closeCallback;
  };

  Box.prototype.open = function() {
    var Box = eXo.webui.Box;
    Box.object.parentNode.style.top = Box.calculateY() + "px";
    if (Box.tmpHeight < Box.maxHeight) {
      Box.object.style.overflow = "hidden";
      Box.object.style.visibility = "visible";
      Box.object.style.height = Box.tmpHeight + "px";
      Box.tmpHeight += Box.speed;
      Box.timer = window.setTimeout(Box.open, 10);
    } else {
      Box.floatingBox("msgBox", 0);
      Box.object.style.overflow = "visible";
      Box.tmpHeight = Box.maxHeight;
      if (Box.timer)
        window.clearTimeout(Box.timer);
      if (Box.closeTimer)
        window.clearInterval(Box.closeTimer);
      if (Box.autoClose)
        Box.closeTimer = window.setInterval(Box.close, Box.closeInterval * 1000);
      Box.openCallback(Box.object);
      return;
    }
  };

  Box.prototype.close = function() {
    var Box = eXo.webui.Box;
    if (Box.tmpHeight >= 0) {
      Box.object.style.overflow = "hidden";
      Box.object.style.height = Box.tmpHeight + "px";
      Box.tmpHeight -= Box.speed;
      Box.timer = window.setTimeout(Box.close, 10);
    } else {
      Box.object.style.overflow = "visible";
      Box.object.style.visibility = "hidden";
      Box.tmpHeight = 0;
      Box.object.style.height = Box.tmpHeight + "px";
      if (Box.timer)
        window.clearTimeout(Box.timer);
      if (Box.closeTimer)
        window.clearInterval(Box.closeTimer);
      Box.closeCallback(Box.object);
      return;
    }
  };

  Box.prototype.calculateY = function() {
    var posY = 0;
    if (document.documentElement && document.documentElement.scrollTop) {
      posY = document.documentElement.scrollTop;
    } else if (document.body && document.body.scrollTop) {
      posY = document.body.scrollTop;
    } else if (window.pageYOffset) {
      posY = window.pageYOffset;
    } else if (window.scrollY) {
      posY = window.scrollY;
    }
    return posY;
  };

  Box.prototype.floatingBox = function(objID, posTop) {
    var obj = document.getElementById(objID);
    var currentTop = this.calculateY();
    obj.style.top = (currentTop < posTop) ? posTop + "px" : currentTop + "px";
    window.setTimeout('eXo.webui.Box.floatingBox("' + objID + '",' + posTop + ')', 100);
  };
  
  // Expose
  window.eXo = eXo || {};
  window.eXo.forum = eXo.forum || {} ;
  window.eXo.forum.ForumSendNotification = new ForumSendNotification();
  window.eXo.webui.Box = new Box();
})(gj, window, document);
