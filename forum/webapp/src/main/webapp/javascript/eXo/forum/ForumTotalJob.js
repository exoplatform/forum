(function(Cometd, $, window, document) {
  var ForumTotalJob = {
    init : function(eXoUser, eXoToken, contextName) {
      if (String(eXoToken)) {
        if (!Cometd.isConnected()) {
          Cometd.url = '/' + contextName + '/cometd';
          Cometd.exoId = eXoUser;
          Cometd.exoToken = eXoToken;
          Cometd.addOnConnectionReadyCallback(ForumTotalJob.subcribeCometdTopics);
          Cometd.init(Cometd.url);
        } else {
          ForumTotalJob.subcribeCometdTopics();
        }
      }
    },
    subcribeCometdTopics : function() {
      Cometd.subscribe('/eXo/Application/Forum/messages', function(eventObj) {
        ForumTotalJob.alarm(eventObj);
      });
    },
    alarm : function(eventObj) {
      var a = JSON.parse(eventObj.data);
      var pr = $('#PendingJob');
      if (pr.exists()) {
        var span = pr.find('span');
        span.html(a.categoryName);
        if(parseInt(a.categoryName) > 0) {
          span.css('text-decoration', 'blink');
        } else {
          span.css('text-decoration', 'none');
        }
      }
      return;
    }
  };

  return ForumTotalJob;

})(cometd, gj, window, document);
