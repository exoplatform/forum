;(function($, window, document) {
  
  var Tour = {
    buttons : null,
    contents : null,
    init : function() {
      Tour.buttons = $('#UIKSGuidedTour li.Button');
      Tour.contents = $('#UIKSGuidedTour div.ContentTour');
      Tour.buttons.on('click', Tour.click);
      Tour.buttons.on('mouseover', Tour.over);
      Tour.buttons.on('mouseout', Tour.out);
    },
    click : function() {
      var button = $(this);
      Tour.buttons.addClass('NormalButton')
          .removeClass('OverButton').removeClass('HightLineButton');
      button.addClass('HightLineButton').removeClass('NormalButton');
      Tour.contents.hide().eq(button.index()).show();
    },
    over : function() {
      var button = $(this);
      if(!button.hasClass('HightLineButton')) {
        button.addClass('OverButton').removeClass('NormalButton');
      }
    },
    out : function() {
      var button = $(this);
      if(!button.hasClass('HightLineButton')) {
        button.addClass('NormalButton').removeClass('OverButton');
      }
    }
  };
  
  window.eXo = window.eXo || {};
  window.eXo.forum = window.eXo.forum || {};
  window.eXo.forum.Tour = Tour;
})(gj, window, document);
