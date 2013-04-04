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

  ForumSendNotification.initParam = function(notification, message, post, titeName, from, briefContent, GoDirectly) {
    ForumSendNotification.notification = notification;
    ForumSendNotification.message = message;
    ForumSendNotification.post = post;
    ForumSendNotification.titeName = titeName;
    ForumSendNotification.from = from;
    ForumSendNotification.briefContent = briefContent;
    ForumSendNotification.GoDirectly = GoDirectly;
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
    console.log(message);
    var msgBox = jPortlet.find('.uiNotification:first').clone();
    if(msgBox.exists()) {
      var name = msgBox.find('.name:first');
      name.html(name.html().replace('Message', ForumSendNotification.notification));
      msgBox.find('.closePopup:first').on('click', ForumSendNotification.closeBox);
      var link = String(ForumSendNotification.GoDirectly);
      if(message.type === 'PrivatePost') {
        var titeName = ForumSendNotification.titeName.replace('TYPE', ForumSendNotification.post);
        msgBox.find('.nameMessage:first').html('<strong>' + titeName + '</strong>');
        link = link.replace('TYPE', ForumSendNotification.post);
        link = link.replace(' LINK', '<a style="color:#204AA0" href="' + String(message.id) + '">') + '</a>';
        msgBox.find('.link:first').html(link);
      } else {
        var titeName = ForumSendNotification.titeName.replace('TYPE', ForumSendNotification.message);
        msgBox.find('.nameMessage:first').html('<strong>' + titeName + '</strong>');
        link = link.replace('TYPE', ForumSendNotification.message);
        var alink = $('#privateMessageLink');
        if (alink.exists()) {
          link = link.replace(' LINK', '<a style="color:#204AA0" href="' + alink.attr('href') + '">') + '</a>';
          msgBox.find('.link:first').html(link);
        }
      }
      msgBox.find('.from:first').html('<strong>' + ForumSendNotification.from + ':</strong> ' + message.from);
      msgBox.find('.title:first').html('<strong>' + ForumSendNotification.title + ':</strong> ' + String(message.name).replace(/Reply:/g, ''));
      var cont = $('<span></span>').html(message.message).text();
      msgBox.find('.content:first').html('<strong>' + ForumSendNotification.briefContent + ':</strong> ' + cont);
      msgBox.find('.link:first').find('a').on('mouseup', ForumSendNotification.closeBox);
      var actionBar = jPortlet.find('.uiForumActionBar:first');
      var top = actionBar.offset().top + actionBar.height() + 10;
      msgBox.css({'width': '300px', 'top': top + 'px', 'z-index' : 1000});

      msgBox.show(100);
      var container = $('<div class="uiForumPortlet" id="forumBox"></div>')
      container.append(msgBox);
      ForumSendNotification.closeBox();
      $(document.body).append(container);
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

