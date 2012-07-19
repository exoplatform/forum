;(function($, window, document) {
  function EventManager() {
  }
  
  EventManager.prototype.getMouseButton = function(evt) {
    var evt = evt || window.event;
    return evt.button;
  };
  
  EventManager.prototype.getEventTarget = function(evt) {
    var evt = evt || window.event;
    var target = evt.target || evt.srcElement;
    if (target.nodeType == 3) { // check textNode
      target = target.parentNode;
    }
    return target;
  };
  
  EventManager.prototype.getEventTargetByClass = function(evt, className) {
    var target = this.getEventTarget(evt);
    if ($(target).hasClass(className)) {
      return target;
    } else {
      return $(target).parents('.' + className + ':first')[0];
    }
  };
  
  EventManager.prototype.getEventTargetByTagName = function(evt, tagName) {
    var target = this.getEventTarget(evt);
    if (target.tagName.toLowerCase() == tagName.trim())
      return target;
    else
      return $(target).parents(tagName).eq(0)[0];
  };
  
  window.eXo.forum = window.eXo.forum || {};
  window.eXo.forum.EventManager = new EventManager();
})(gj, window, document);