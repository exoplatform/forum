var EventManager = {

  getMouseButton : function(evt) {
    var evt = evt || window.event;
    return evt.button;
  },

  getEventTarget : function(evt) {
    var evt = evt || window.event;
    var target = evt.target || evt.srcElement;
    if (target.nodeType == 3) { // check textNode
      target = target.parentNode;
    }
    return target;
  },

  getEventTargetByClass : function(evt, className) {
    var target = this.getEventTarget(evt);
    if (gj(target).hasClass(className)) {
      return target;
    } else {
      return gj(target).parents('.' + className + ':first')[0];
    }
  },

  getEventTargetByTagName : function(evt, tagName) {
    var target = this.getEventTarget(evt);
    if (target.tagName.toLowerCase() == tagName.trim())
      return target;
    else
      return gj(target).parents(tagName).eq(0)[0];
  }
};
_module.EventManager = EventManager;
