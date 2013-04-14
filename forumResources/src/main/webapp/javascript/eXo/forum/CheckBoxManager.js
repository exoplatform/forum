(function(utils, gj) {
  var CheckBoxManager = {
    init : function(cont) {
      if (typeof (cont) == "string") {
        cont = findId(cont);
      } else {
        cont = gj(cont);
      }
      var table = cont.find('table:first');
      var checkboxes = table.find('tbody').find('input.checkbox');
      if (!checkboxes.exists())
        return;

      checkboxes.on('click', CheckBoxManager.check);
      table.find('thead').find('input.checkbox:first').on('click', CheckBoxManager.checkAll);
    },

    checkAll : function() {
      CheckBoxManager.checkAllItem(this);
    },

    getItems : function(obj) {
      var tbody = gj(obj).parents('table:first').find('tbody');
      return tbody.find('input.checkbox');
    },

    check : function() {
      CheckBoxManager.checkItem(this);
    },

    checkAllItem : function(obj) {
      var items = CheckBoxManager.getItems(obj);
    items.prop("checked", obj.checked);
    },

    checkItem : function(obj) {
      var checkboxes = CheckBoxManager.getItems(obj);
      var checkAllBox = gj(obj).parents('table:first').find('thead').find('input.checkbox:first');
      var len = checkboxes.length;
      var state = true;
      if (obj.checked == false) {
        checkAllBox.prop("checked", false);
      } else {
        for ( var i = 1; i < len; i++) {
          state = state && checkboxes.eq(i).prop("checked");
          if(state == false) break;
        }
        checkAllBox.prop("checked", state);
      }
    }
  };
  return CheckBoxManager;
})(utils, gj);