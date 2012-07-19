;(function($, window, document) {

  function CheckBoxManager() {
  }
  
  CheckBoxManager.prototype.init = function(cont) {
    if (typeof (cont) == "string"){
      cont = findId(cont);
    } else {
      cont = $(cont);
    }
    var checkboxes = cont.find('input.checkbox');
    if (!checkboxes.exists())
      return;
    checkboxes.on('click', this.check);
    checkboxes.eq(0).on('click', this.checkAll);
  };
  
  CheckBoxManager.prototype.checkAll = function() {
    eXo.forum.CheckBoxManager.checkAllItem(this);
  };
  
  CheckBoxManager.prototype.getItems = function(obj) {
    var table = $(obj).parents('table');
    return table.find('input.checkbox');
  };
  
  CheckBoxManager.prototype.check = function() {
    eXo.forum.CheckBoxManager.checkItem(this);
  };
  
  CheckBoxManager.prototype.checkAllItem = function(obj) {
    var checked = $(obj).val();
    var items = eXo.forum.CheckBoxManager.getItems(obj);
    for ( var i = 1; i < items.length; i++) {
      items.eq(i).val(checked);
    }
  };
  
  CheckBoxManager.prototype.checkItem = function(obj) {
    var checkboxes = eXo.forum.CheckBoxManager.getItems(obj);
    var len = checkboxes.length;
    var state = true;
    if (!$(obj).val()) {
      checkboxes.eq(0).val(false);
    } else {
      for ( var i = 1; i < len; i++) {
        state = state && checkboxes.eq(i).val();
      }
      checkboxes.eq(0).val(state);
    }
  };
  window.eXo.forum = window.eXo.forum || {};
  window.eXo.forum.CheckBoxManager = new CheckBoxManager();
})(gj, window, document);