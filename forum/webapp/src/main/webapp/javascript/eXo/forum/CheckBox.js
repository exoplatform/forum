(function($, window, document) {
  var CheckBox = {
    jqCheckAll : null,
    init : function(cont) {
      if (typeof (cont) == 'string') cont = findId(cont);
      if (cont.exists()) {
        var checkboxes = cont.find('input.checkbox');
        if (!checkboxes.exists()) return;
        checkboxes.on('click', CheckBox.check);
        CheckBox.jqCheckAll = cont.find('input.checkbox[name=checkAll]:first');
        CheckBox.jqCheckAll.off('click').on('click', CheckBox.checkAll);
        $.each(checkboxes, function(i, it) {
          if($(it).attr('name') !== 'checkAll') {
            CheckBox.checkItem($(it));
          }
        });
      }
    },
    check : function(e) {
      CheckBox.checkItem($(this));
      eXo.forum.UIForumPortlet.setChecked(this.checked);
    },
    checkItem : function(elm) {
      var row = elm.parents('tr:first');
      if (elm.is(':checked')) {
        row.addClass('selectedItem');
        var parent = row.parents('tbody:first');
        if (parent.find("input[type=checkbox]:checked").length === parent.find('input[type=checkbox]').length) {
          CheckBox.jqCheckAll.prop('checked', true);
        }
      } else {
        row.removeClass('selectedItem');
        CheckBox.jqCheckAll.prop('checked', false);
      }
    },
    checkAll : function(e) {
      eXo.forum.UIForumPortlet.checkAll(this);
      var rows = $(this).parents('table').find('tbody:first').find('tr');
      if (this.checked) {
        rows.addClass('selectedItem');
      } else {
        rows.removeClass('selectedItem');
      }
    }
  };

  // Expose
  window.eXo = eXo || {};
  window.eXo.forum = eXo.forum || {} ;
  window.eXo.forum.CheckBox = CheckBox;
  
  return CheckBox;
})(gj, window, document);
