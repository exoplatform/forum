(function(Cometd, $, document, window) {
  
  var ForumSendNotification = {
      portletId : "UIForumPortlet",
      notification : "Notification",
      message : "message",
      post : "post",
      titeName : "You have received a new private TYPE",
      from : "From",
      briefContent : "Brief content",
      GoDirectly : "Go directly to the TYPE: LINK Click here.",
      title : "Title",
      currentUser : ""
  };

  ForumSendNotification.initParam = function(notification, message, post, titeName, from, briefContent, GoDirectly, Title) {
    ForumSendNotification.notification = notification;
    ForumSendNotification.messageLabel = message;
    ForumSendNotification.postLabel = post;
    ForumSendNotification.titeName = titeName;
    ForumSendNotification.from = from;
    ForumSendNotification.briefContent = briefContent;
    ForumSendNotification.GoDirectly = GoDirectly;
    ForumSendNotification.title = Title || ForumSendNotification.title;
    ForumSendNotification.currentUser = $.trim(eXo.core.Browser.getCookie('forumCurrentUserId') || '');
  };

  ForumSendNotification.init = function(portId, eXoUser, eXoToken, contextName) {
    ForumSendNotification.portletId = portId;
    if (String(eXoToken) != '') {
      if (Cometd.isConnected() === false) {
        if (ForumSendNotification.currentUser !== eXoUser || ForumSendNotification.currentUser === '') {
          ForumSendNotification.currentUser = eXoUser;
          document.cookie = 'forumCurrentUserId=' + escape(eXoUser) + ';path=/portal';
          Cometd._connecting = false;
          Cometd.currentTransport = null;
          Cometd.clientId = null;
        }
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
      ForumSendNotification.createMessage(JSON.parse(eventObj.data))
    });
  };
  
  ForumSendNotification.getPlainText = function(str) {
    return $.trim($('<span></span>').html(str).text().replace(/</gi, '&lt;').replace(/>/gi, '&gt;'));
  };
  
  ForumSendNotification.buildLink = function(link, type, alink) {
    link = link.replace('TYPE', type);
    var key = " LINK.";
    if(link.indexOf(key) < 0) {
      key = " LINK";
    }
    link = link.replace(key, '<a style="color:#204AA0" href="' + alink + '">') + '</a>';
    return link;
  };

  ForumSendNotification.createMessage = function(message) {
    var component = ForumSendNotification;
    var jPortlet = $.fn.findId(component.portletId);
    var msgBox = jPortlet.find('.uiNotification:first').clone();
    if(msgBox.exists()) {
      var name = msgBox.find('.name:first');
      name.html(name.html().replace('Message', component.notification));
      msgBox.find('.closePopup:first').on('click', component.closeBox);
      //
      var link = String(component.GoDirectly);
      if(message.type === 'PrivatePost') {
        var titeName = component.titeName.replace('TYPE', component.postLabel);
        msgBox.find('.nameMessage:first').html('<strong>' + titeName + '</strong>');
        msgBox.find('.link:first').html(component.buildLink(link, component.postLabel, String(message.id)));
      } else {
        var titeName = component.titeName.replace('TYPE', component.messageLabel);
        msgBox.find('.nameMessage:first').html('<strong>' + titeName + '</strong>');
        var alink = $('#privateMessageLink');
        if (alink.exists()) {
          msgBox.find('.link:first').html(component.buildLink(link, component.messageLabel, alink.attr('href')));
        }
      }
      //
      msgBox.find('.from:first').html('<strong>' + component.from + ':</strong> ' + message.from);
      msgBox.find('.title:first').html('<strong>' + component.title + ':</strong> ' + component.getPlainText(message.name).replace(/Reply:/g, ''));
      //
      var cont = component.getPlainText(message.message);
      msgBox.find('.content:first').html('<strong>' + component.briefContent + ':</strong> ' + cont);
      msgBox.find('.link:first').find('a').on('mouseup', component.closeBox);
      //
      var info = component.getInfo();
      msgBox.css({'width': '300px', 'top': info.top + 'px', 'right' : ((info.left > 5) ? info.left : 10) + 'px', 'z-index' : 1000});
      var container = $('<div class="uiForumPortlet forumBoxNotification"></div>')
      container.css({'width': '1px', 'height' : '1px', 'z-index' : 10})
      container.attr('data-time', new Date().getTime());
      container.append(msgBox);
      $(document.body).append(container);
      msgBox.show('linear');
      //
      setTimeout(component.closeTimeBox, 15001);
      var reloadLink = $('#Reload');
      eval(String(reloadLink.attr('href')).replace('javascript:', '')); 
    }
  };

  ForumSendNotification.getInfo = function() {
    var info = {top:0, left:0};
    var jPortlet = $.fn.findId(ForumSendNotification.portletId);
    var actionBar = jPortlet.find('.uiForumActionBar:first');
    var left = $(window).width() - (jPortlet.offset().left + jPortlet.outerWidth() - 20);
    var top = actionBar.offset().top + actionBar.height() + 14;
    var containers = $(document.body).find('.forumBoxNotification');
    if(containers.exists()) {
      $.each(containers, function(index, item) {
        top += $(item).find('.uiNotification:first').outerHeight();
      });
    }
    info.left = left;
    info.top = top;
    return info;
  };

  ForumSendNotification.closeBox = function(e) {
    if(e && e.type) {
      e.stopPropagation();
    }
    ForumSendNotification.hideBox($(this).parents('.forumBoxNotification:first'));
  };

  ForumSendNotification.hideBox = function(container) {
    if(container.exists()) {
      container.find('.uiNotification').css('overflow', 'hidden')
      .animate({height: '0px'}, 300, function() {container.remove();});
    }
  };
  
  ForumSendNotification.closeTimeBox = function() {
    var containers = $(document.body).find('.forumBoxNotification');
    if(containers.exists()) {
      var time = new Date().getTime();
      $.each(containers, function(index, item) {
         var dataTime = $(item).data('time')*1 + 15000;
         if(dataTime <= time) {
           ForumSendNotification.hideBox($(item));
         }
      });
    }
  };
  return ForumSendNotification;

})(cometd, gj, document, window);
