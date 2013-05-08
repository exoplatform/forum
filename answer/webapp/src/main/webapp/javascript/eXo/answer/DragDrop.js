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
      var dnd = eXo.answer.DragDrop;
      var dragObject = this.cloneNode(true);
      $(dragObject).attr('class', 'faqDnDCategory');
      $('body').append(dragObject);
      $(dragObject).css('width', $(this).width());
      dnd.rootNode = this;

      dnd.mousePos = {
        x : evt.clientX,
        y : evt.clientY
      };
      dnd.setup(dragObject, [ "faqCategory", "faqBack", "faqTmpCategory" ]);

      dnd.dropCallback = function(dragObj, target) {
        $(dragObj).remove();
        if (this.lastTarget)
          $(this.lastTarget).css('border', '');
        if (target && dnd.isMoved) {
          var action = this.getAction(this.dragObject, target);
          if (!action) {
            this.showElement();
            return;
          }
          $.globalEval(action);
        } else {
          this.showElement();
        }
      }
      dnd.dragCallback = function(dragObj, target) {
        if (dnd.lastTarget) {
          $(dnd.lastTarget).css('border', '');
          if ($(dnd.lastTarget).hasClass('faqHighlightCategory')) {
            $(dnd.lastTarget).removeClass('faqHighlightCategory');
          }
        }

        if (!target)
          return;
        dnd.lastTarget = target;

        if ($(target).hasClass('faqBack'))
          $(target).click();
        if ($(target).hasClass('faqTmpCategory')) {
          $(dnd.lastTarget).addClass('faqHighlightCategory');
        }
        $(target).css('border', 'dotted 1px #cfcfcf');
        if (!dnd.hided)
          dnd.hideElement(dnd.rootNode);
      }
    },

    setup : function(dragObject, targetClass) {
      DragDrop.dragObject = dragObject;
      DragDrop.targetClass = targetClass;
      $(document).on('mousemove', eXo.answer.DragDrop.onDrag);
      $(document).on('mouseup', eXo.answer.DragDrop.onDrop);
    },

    onDrag : function(evt) {
      var dnd = eXo.answer.DragDrop;
      var dragObject = dnd.dragObject;
      $(dragObject).css('left', evt.pageX + 2);
      $(dragObject).css('top', evt.pageY + 2);
      if (dnd.dragCallback) {
        var target = dnd.findTarget(evt);
        dnd.dragCallback(dragObject, target);
      }
    },

    onDrop : function(evt) {
      evt = evt || window.event;
      var dnd = eXo.answer.DragDrop;
      dnd.isMoved = true;
      if (dnd.mousePos.x == evt.clientX && dnd.mousePos.y == evt.clientY) {
        dnd.isMoved = false;
      }
      if (dnd.dropCallback) {
        var target = dnd.findTarget(evt);
        dnd.dropCallback(dnd.dragObject, target);
      }
      delete dnd.dragObject;
      delete dnd.targetClass;
      delete dnd.dragCallback;
      delete dnd.hided;
      delete dnd.rootNode;
      $(document).off('mousemove', eXo.answer.DragDrop.onDrag);
      $(document).off('mouseup', eXo.answer.DragDrop.onDrop);
    },

    findTarget : function(evt) {
      var targetClass = eXo.answer.DragDrop.targetClass;
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
      jelm.attr('unselectable', 'on').css('user-select', 'none').on('selectstart', false);
    },

    hideElement : function(obj) {
      $(obj).prev('li').css('display', 'none');
      $(obj).css('display', 'none');
      this.hided = true;
    },

    showElement : function() {
      var dnd = eXo.answer.DragDrop;
      if (!dnd.rootNode)
        return;
      var preElement = $(dnd.rootNode).prev('li');
      if (preElement.exists())
        preElement.css('display', '');
      $(dnd.rootNode).css('display', '');
      if (dnd.lastTarget) {
        $(dnd.lastTarget).css('border', '');
        if ($(dnd.lastTarget).hasClass('faqHighlightCategory'))
          $(dnd.lastTarget).removeClass('faqHighlightCategory');
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

  // Expose
  window.eXo = eXo || {};
  window.eXo.answer = eXo.answer || {};
  window.eXo.answer.DragDrop = DragDrop;

  return DragDrop;
})(gj, forumUtils, forumEventManager, window, document);
