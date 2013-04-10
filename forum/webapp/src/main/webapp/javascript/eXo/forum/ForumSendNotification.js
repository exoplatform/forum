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
      title : "Title"
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
  };

  ForumSendNotification.init = function(portId, eXoUser, eXoToken, contextName) {
    ForumSendNotification.portletId = portId;
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
      ForumSendNotification.createMessage(JSON.parse(eventObj.data))
    });
  };

  ForumSendNotification.createMessage = function(message) {
    var jPortlet = $.fn.findId(ForumSendNotification.portletId);
    var msgBox = jPortlet.find('.uiNotification:first').clone();
    if(msgBox.exists()) {
      var name = msgBox.find('.name:first');
      name.html(name.html().replace('Message', ForumSendNotification.notification));
      msgBox.find('.closePopup:first').on('click', ForumSendNotification.closeBox);
      var link = String(ForumSendNotification.GoDirectly);
      if(message.type === 'PrivatePost') {
        var titeName = ForumSendNotification.titeName.replace('TYPE', ForumSendNotification.postLabel);
        msgBox.find('.nameMessage:first').html('<strong>' + titeName + '</strong>');
        link = link.replace('TYPE', ForumSendNotification.postLabel);
        link = link.replace(' LINK', '<a style="color:#204AA0" href="' + String(message.id) + '">') + '</a>';
        msgBox.find('.link:first').html(link);
      } else {
        var titeName = ForumSendNotification.titeName.replace('TYPE', ForumSendNotification.messageLabel);
        msgBox.find('.nameMessage:first').html('<strong>' + titeName + '</strong>');
        link = link.replace('TYPE', ForumSendNotification.messageLabel);
        var alink = $('#privateMessageLink');
        if (alink.exists()) {
          link = link.replace(' LINK', '<a style="color:#204AA0" href="' + alink.attr('href') + '">') + '</a>';
          msgBox.find('.link:first').html(link);
        }
      }
      msgBox.find('.from:first').html('<strong>' + ForumSendNotification.from + ':</strong> ' + message.from);
      msgBox.find('.title:first').html('<strong>' + ForumSendNotification.title + ':</strong> ' + String(message.name).replace(/Reply:/g, ''));
      var cont = $.trim($('<span></span>').html(message.message).text());
      msgBox.find('.content:first').html('<strong>' + ForumSendNotification.briefContent + ':</strong> ' + cont);
      msgBox.find('.link:first').find('a').on('mouseup', ForumSendNotification.closeBox);
      var actionBar = jPortlet.find('.uiForumActionBar:first');
      var left = $(window).width() - (jPortlet.offset().left + jPortlet.outerWidth() - 20);
      var top = actionBar.offset().top + actionBar.height() + 14;
      msgBox.css({'width': '300px', 'top': top + 'px', 'right' : ((left > 5) ? left : 10) + 'px', 'z-index' : 1000});
      var container = $('<div class="uiForumPortlet" id="forumBox"></div>')
      container.css({'width': '1px', 'height' : '1px', 'z-index' : 10})
      container.append(msgBox);
      ForumSendNotification.closeBox();
      $(document.body).append(container);
      msgBox.show('linear');
      setTimeout(ForumSendNotification.closeBox, 15000);
      var reloadLink = $('#Reload');
      eval(String(reloadLink.attr('href')).replace('javascript:', '')); 
    }
  };

  ForumSendNotification.closeBox = function(e) {
    var container = $(document.body).find('#forumBox');
    if(container.exists()) {
      container.find('.uiNotification').css('overflow', 'hidden')
        .animate({height: '0px'}, 300, function() {container.remove();});
      if(e) {
        e.stopPropagation();
      }
    }
  };

  return ForumSendNotification;

})(cometd, gj, document, window);

