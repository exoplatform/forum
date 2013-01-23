(function(utils, gj) {
  var CheckBoxManager = {
    init : function(cont) {
      if (typeof (cont) == "string") {
        cont = findId(cont);
      } else {
        cont = gj(cont);
      }
      var checkboxes = cont.find('input.checkbox');
      if (!checkboxes.exists())
        return;
      checkboxes.on('click', this.check);
      checkboxes.eq(0).off('click').on('click', this.checkAll);
    },

    checkAll : function() {
      CheckBoxManager.checkAllItem(this);
    },

    getItems : function(obj) {
      var table = gj(obj).parents('table');
      return table.find('input.checkbox');
    },

    check : function() {
      CheckBoxManager.checkItem(this);
    },

    checkAllItem : function(obj) {
      var checked = gj(obj).val();
      var items = CheckBoxManager.getItems(obj);
      for ( var i = 1; i < items.length; i++) {
        items.eq(i).val(checked);
      }
    },

    checkItem : function(obj) {
      var checkboxes = CheckBoxManager.getItems(obj);
      var len = checkboxes.length;
      var state = true;
      if (!gj(obj).val()) {
        checkboxes.eq(0).val(false);
      } else {
        for ( var i = 1; i < len; i++) {
          state = state && checkboxes.eq(i).val();
        }
        checkboxes.eq(0).val(state);
      }
    }
  };
  return CheckBoxManager;
})(utils, gj);
