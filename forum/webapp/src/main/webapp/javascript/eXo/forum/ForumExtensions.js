(function(extensionRegistry) {
  return {
    init: () => {
      extensionRegistry.registerExtension('activity', 'type', {
        type: 'ks-forum:spaces',
        options: {
          canEdit: () => false,
          supportsThumbnail: true,
          useSameViewForMobile: true,
          thumbnailProperties: {
            height: '90px',
            width: '90px',
            noBorder: true,
          },
          getThumbnail: () => '/forum/images/forumActivity.png',
          getTitle: activity => activity && activity.title || '',
          getSummary: activity => activity && activity.body && Vue.prototype.$utils.htmlToText(activity.body) || '',
          getSourceLink: (activity) => activity.templateParams
                                        && activity.templateParams.TopicId
                                        && `${eXo.env.portal.context}/${eXo.env.portal.portalName}/forum/topic/${activity.templateParams.TopicId}`,
        },
      });
    },
  };
})(extensionRegistry);
