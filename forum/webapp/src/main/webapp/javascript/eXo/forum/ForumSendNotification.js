(function(Cometd, $, document, window) {
  
  var ForumSendNotification = {
      notification : "Notification",
      message : "message",
      post : "post",
      titeName : "You have received a new private TYPE",
      from : "From",
      briefContent : "Brief content",
      GoDirectly : "Go directly to the TYPE: LINK Click here."
  };

  ForumSendNotification.initParam = function(notification, message, post, titeName, from, briefContent, GoDirectly) {
    ForumSendNotification.notification = notification;
    ForumSendNotification.message = message;
    ForumSendNotification.post = post;
    ForumSendNotification.titeName = titeName;
    ForumSendNotification.from = from;
    ForumSendNotification.briefContent = briefContent;
    ForumSendNotification.GoDirectly = GoDirectly;
  };

  ForumSendNotification.init = function(eXoUser, eXoToken, contextName) {
    if (String(eXoToken) != '') {
      if (!Cometd.isConnected()) {
        Cometd.url = '/' + contextName + '/cometd';
        Cometd.exoId = eXoUser;
        Cometd.exoToken = eXoToken;
        Cometd.addOnConnectionReadyCallback(ForumSendNotification.subcribeCometdSendNotification);
        Cometd.init(Cometd.url);
      } else {
        ForumSendNotification.subcribeCometdSendNotification();
      }
    }
  };

  ForumSendNotification.subcribeCometdSendNotification = function() {
    Cometd.subscribe('/eXo/Application/Forum/NotificationMessage', function(eventObj) {
      ForumSendNotification.alarm(eventObj);
    });
  };

  ForumSendNotification.alarm = function(eventObj) {
    var message = JSON.parse(eventObj.data); // message
    var parent = $(ForumSendNotification.createMessage(message));
    parent.height('auto');
    var popup = parent.find('div.UIPopupNotification');
    popup.show();
    Box.config(popup[0], popup.outerHeight(), 5, ForumSendNotification.openCallback, ForumSendNotification.closeBox);
    window.focus();
    return;
  };

  ForumSendNotification.openCallback = function(obj) {
    $(obj).on('click', function() {
      $(this).hide();
    });
  };

  ForumSendNotification.closeBox = function(obj) {
    $(obj).hide();
    $(obj).parent().height('0px');
  };

  ForumSendNotification.createMessage = function(message) {
    var msgBox = $('#msgBox');
    if (msgBox.exists()) {
      var directChildNode = msgBox.find('div.UIPopupNotification:first');
      if (directChildNode.css('visibility') == 'hidden') {
        msgBox.html(ForumSendNotification.generateHTML(message));
      } else {
        var contentBox = msgBox.find('div.MCPopupNotification:first');
        var content = contentBox.find('div.contentBox:first');
        content.html(content.html() + '<div style="border-top:1px dashed black;">' + ForumSendNotification.getContentHTML(message) + '</div>');
      }
    } else {
      msgBox = $('<div id="msgBox" class="UINotification"></div>').html(ForumSendNotification.generateHTML(message));
      $('body').append(msgBox);
    }
    return msgBox;
  };

  ForumSendNotification.getContentHTML = function(message) {
    var link = '';
    var type = ForumSendNotification.message;
    if (message.type == 'PrivatePost') {
      type = ForumSendNotification.post;
      link = String(ForumSendNotification.GoDirectly).replace('TYPE', type);
      link = link.replace(' LINK', '<a style="color:#204AA0" href="' + String(message.id) + '">') + '</a>';
    } else {
      link = String(ForumSendNotification.GoDirectly).replace('TYPE', type);
      var alink = $('#privateMessageLink');
      if (alink.exists()) {
        link = link.replace(' LINK', '<a style="color:#204AA0" href="' + alink.attr('href') + '">') + '</a>';
      }
    }
    var msg = String(message.message).replace(/<\/?[^>]+(>|$)/g, "");
    if (msg.length > 100) {
      msg = msg.substring(0, 100);
    }
    var content = '<div style="padding:7px 0px 7px 5px">' + '<strong>' + String(ForumSendNotification.titeName).replace('TYPE', type) + ':</strong> <br/>' + message.name + '<br/>'
        + '<strong>' + ForumSendNotification.from + ':</strong> ' + message.from + '<br/>' + '<strong>' + ForumSendNotification.briefContent + ':</strong><br/>' + msg + '<br/>'
        + link + '</div>';
    return content;
  };

  ForumSendNotification.generateHTML = function(message) {
    var html = '<div class="UIPopupNotification">' + '  <div class="TLPopupNotification">' + '    <div class="TRPopupNotification">'
        + '     <div class="TCPopupNotification"><span></span></div>' + '   </div>' + ' </div>' + ' <div class="MLPopupNotification">' + '    <div class="MRPopupNotification">'
        + '     <div class="MCPopupNotification">' + '        <div class="TitleNotification">' + '          <a class="ItemTitle" href="#">' + ForumSendNotification.notification
        + '</a>' + '         <a class="Close" href="#"><span></span></a>' + '        </div>' + '       <div class="contentBox">' + ForumSendNotification.getContentHTML(message)
        + '</div>' + '     </div>' + '   </div>' + ' </div>' + ' <div class="BLPopupNotification">' + '    <div class="BRPopupNotification">'
        + '      <div class="BCPopupNotification"><span></span></div>' + '   </div>' + ' </div>' + '</div>';
    return html;
  };

  // Box effect
  var Box = {
    speed : 4,
    tmpHeight : 0,
    autoClose : true,
    closeInterval : 30
  };

  Box.config = function(obj, height, speed, openCallback, closeCallback) {
    Box.object = obj;
    Box.maxHeight = height;
    if (speed)
      Box.speed = speed;
    Box.open();
    if (openCallback)
      Box.openCallback = openCallback;
    if (closeCallback)
      Box.closeCallback = closeCallback;
  };

  Box.open = function() {
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

  Box.close = function() {
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

  Box.calculateY = function() {
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
  
  Box.floatingBox = function(objID, posTop) {
    var obj = document.getElementById(objID);
    var currentTop = Box.calculateY();
    obj.style.top = (currentTop < posTop) ? posTop + "px" : currentTop + "px";
    window.setTimeout('Box.floatingBox("' + objID + '",' + posTop + ')', 100);
  };

  window.Box = Box;
  return ForumSendNotification;

})(cometd, gj, document, window);

