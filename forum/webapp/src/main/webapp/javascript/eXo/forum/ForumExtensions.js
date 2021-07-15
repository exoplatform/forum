(function(extensionRegistry) {
  return {
    init: () => {
      extensionRegistry.registerExtension('activity', 'type', {
        type: 'ks-forum:spaces',
        options: {
          canEdit: () => false,
          canShare: () => true,
          supportsThumbnail: true,
          useSameViewForMobile: true,
          thumbnailProperties: {
            height: '90px',
            width: '90px',
            noBorder: true,
          },
          getThumbnail: activityOrComment => !activityOrComment.activityId && '/forum/images/forumActivity.png' || '',
          getTitle: activityOrComment => !activityOrComment.activityId && activityOrComment.title || '',
          getSummary: activityOrComment => !activityOrComment.activityId && activityOrComment.body || '',
          getBody: activityOrComment => activityOrComment.activityId && activityOrComment.body || '',
          getSourceLink: activityOrComment => !activityOrComment.activityId
                                          && activityOrComment.templateParams
                                          && activityOrComment.templateParams.TopicLink,
        },
      });
    },
  };
})(extensionRegistry);
