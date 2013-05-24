(function($, utils, eventManager, window, document) {

  var DragDrop = {
    dragObject : null,
    targetClass : [],
    component : null,

    init : function(compid) {
      DragDrop.component = $.fn.findId(compid);
      DragDrop.component.find('li.faqCategory')
              .on('mousedown', DragDrop.attach);

      var leftColumn = DragDrop.component.parents('#FAQViewCategoriesColumn');
      DragDrop.disableSelection(leftColumn);
    },

    attach : function(evt) {
      var dragObject = this.cloneNode(true);
      $(dragObject).attr('class', 'faqDnDCategory');
      $('body').append(dragObject);
      $(dragObject).css('width', $(this).width());
      DragDrop.rootNode = this;

      DragDrop.mousePos = {
        x : evt.clientX,
        y : evt.clientY
      };
      DragDrop.setup(dragObject, [ "faqCategory", "faqBack", "faqTmpCategory" ]);

      DragDrop.dropCallback = function(dragObj, target) {
        $(dragObj).remove();
        if (DragDrop.lastTarget)
          $(DragDrop.lastTarget).css('border', 'none');
        if (target && DragDrop.isMoved) {
          var action = DragDrop.getAction(DragDrop.dragObject, target);
          if (!action) {
            DragDrop.showElement();
            return;
          }
          $.globalEval(action);
        } else {
          DragDrop.showElement();
        }
      }

      DragDrop.dragCallback = function(dragObj, target) {
        if (DragDrop.lastTarget) {
          $(DragDrop.lastTarget).css('border', '');
          if ($(DragDrop.lastTarget).hasClass('faqHighlightCategory')) {
            $(DragDrop.lastTarget).removeClass('faqHighlightCategory');
          }
        }

        if (!target)
          return;
        DragDrop.lastTarget = target;

        if ($(target).hasClass('faqBack')) {
          $(target).trigger('click');
        }
        if ($(target).hasClass('faqTmpCategory')) {
          $(DragDrop.lastTarget).addClass('faqHighlightCategory');
        }
        $(target).css('border', 'dotted 1px #cfcfcf');
        if (DragDrop.hided === false)
          DragDrop.hideElement(DragDrop.rootNode);
      }
    },

    setup : function(dragObject, targetClass) {
      DragDrop.dragObject = dragObject;
      DragDrop.targetClass = targetClass;
      $(document).on('mousemove', DragDrop.onDrag);
      $(document).on('mouseup', DragDrop.onDrop);
    },

    onDrag : function(evt) {
      var dragObject = DragDrop.dragObject;
      $(dragObject).css('left', evt.pageX + 2);
      $(dragObject).css('top', evt.pageY + 2);
      if (DragDrop.dragCallback) {
        var target = DragDrop.findTarget(evt);
        DragDrop.dragCallback(dragObject, target);
      }
    },

    onDrop : function(evt) {
      evt = evt || window.event;
      DragDrop.isMoved = true;
      if (DragDrop.mousePos.x == evt.clientX && DragDrop.mousePos.y == evt.clientY) {
        DragDrop.isMoved = false;
      }
      if (DragDrop.dropCallback) {
        var target = DragDrop.findTarget(evt);
        DragDrop.dropCallback(DragDrop.dragObject, target);
      }
      delete DragDrop.dragObject;
      delete DragDrop.targetClass;
      delete DragDrop.dragCallback;
      delete DragDrop.hided;
      delete DragDrop.rootNode;
      $(document).off('mousemove', DragDrop.onDrag);
      $(document).off('mouseup', DragDrop.onDrop);
    },

    findTarget : function(evt) {
      var targetClass = DragDrop.targetClass;
      if (targetClass) {
        var i = targetClass.length;
        while (i--) {
          var target = eventManager.getEventTargetByClass(evt, targetClass[i]);
          if (target)
            return target;
        }
      }
    },

    disableSelection : function(jelm) {
      jelm.attr('unselectable', 'on').css('user-select', 'none').on('selectstart', function(){return false;});
    },

    hideElement : function(obj) {
      $(obj).prev('li').css('display', 'none');
      $(obj).css('display', 'none');
      this.hided = true;
    },

    showElement : function() {
      if (!DragDrop.rootNode)
        return;
      var preElement = $(DragDrop.rootNode).prev('li');
      if (preElement.exists())
        preElement.css('display', '');
      $(DragDrop.rootNode).css('display', '');
      if (DragDrop.lastTarget) {
        $(DragDrop.lastTarget).css('border', '');
        if ($(DragDrop.lastTarget).hasClass('faqHighlightCategory'))
          $(DragDrop.lastTarget).removeClass('faqHighlightCategory');
      }
    },

    getAction : function(obj, target) {
      var info = $(obj).find('input.infoCategory:first');
      if ($(target).hasClass('faqTmpCategory')) {
        var preElement = $(target).prev('li');
        var top = ' ';
        if (!preElement.exists()) {
          preElement = $(target).next('li');
          top = 'top';
        }
        var preElementInfo = preElement.find('input.infoCategory:first');
        if (info.attr('id') == preElementInfo.attr('id'))
          return false;
        var actionLink = info.attr('value');
        actionLink = actionLink.replace("=objectId", ("=" + info.attr('id') + "," + preElementInfo.attr('id') + "," + top));
      } else if ($(target).hasClass('faqCategory')) {
        var actionLink = info.attr('value');
        var targetInfo = $(target).find('input.infoCategory:first');
        actionLink = actionLink.replace("=objectId", "=" + info.attr('id') + "," + targetInfo.attr('id'));
        actionLink = actionLink.replace("ChangeIndex", "MoveCategoryInto");
      }
      return actionLink;
    }
  };

  return DragDrop;
})(gj, forumUtils, forumEventManager, window, document);