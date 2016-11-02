function initUserProfilePopup(uicomponentId, labels) {
require(['SHARED/jquery','SHARED/userPopupPlugin','SHARED/socialUtil'],function($,userPopup,socialUtil){
	var UIProfile = {
	  labels: {},
	  KEYS : {
	    ENTER : 13
	  } 
	};
    UIProfile.labels = $.extend(true, {}, UIProfile.labels, labels);
    $.each(UIProfile.labels, function(key) {
      UIProfile.labels[key] =  window.decodeURIComponent(UIProfile.labels[key]);
    });
    // User Profile Popup initialize
    var portal = eXo.env.portal;
    var restUrl = '//' + window.location.host + portal.context + '/' + portal.rest + '/social/people' + '/getPeopleInfo/{0}.json';
    var userLinks = $('.' + uicomponentId).find('div[href]');
    $.each(userLinks, function (idx, el) {
        var userUrl = $(el).attr('href');
        var userId = userUrl.substring(userUrl.lastIndexOf('/') + 1);
        
        $(el).userPopup({
          restURL: restUrl,
          labels: UIProfile.labels,
          content: false,
          defaultPosition: "left",
          keepAlive: true,
          maxWidth: "240px"
        });
    });
  });
}
