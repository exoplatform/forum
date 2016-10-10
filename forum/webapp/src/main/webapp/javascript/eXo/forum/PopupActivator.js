
 function initUserProfilePopup(globalLabels) {
      require(['SHARED/jquery','SHARED/userPopupPlugin','SHARED/socialUtil'],function(jQuery,userPopup,socialUtil){
          var labels = {};
          var profileLabels = jQuery.extend(true, {}, labels, globalLabels);
          jQuery.each(profileLabels, function(key) {
            profileLabels[key] =  window.decodeURIComponent(profileLabels[key]);
          });

          jQuery("[data^=forumUserPopup]").each(function() {
			  var val = jQuery(this).attr('data');
			  val = val.split("-")[1];
		          jQuery(".forumUserPopup-"+val).userPopup({
		              restURL: '//' + window.location.host + eXo.social.portal.context + '/' + eXo.social.portal.rest + '/social/people' + '/getPeopleInfo/'+val+'.json',
		              labels: profileLabels,
		              content: false,
		              defaultPosition: "left",
		              keepAlive: true,
		              maxWidth: "240px"
		          });
		  });
      });
   }