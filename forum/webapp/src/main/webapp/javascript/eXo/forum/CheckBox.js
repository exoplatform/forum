;(function($, window, document) {
  var CheckBox = {
    init : function(cont) {
      if (typeof(cont) == 'string') cont = findId(cont) ;
      if (cont.exists()){
        var checkboxes = cont.find('input.checkbox');
        if (!checkboxes.exists()) return ;
        checkboxes.eq(0).on('click', this.checkAll);
        var len = checkboxes.length ;
        for(var i = 1 ; i < len ; i ++) {
          checkboxes.eq(i).on('click', this.check);
          checkBoxManager.CheckBoxManager.checkItem(checkboxes.eq(i));
        }
      }
    },
    check : function(){
      checkBoxManager.CheckBoxManager.checkItem(this);
      var row = $(this).parents('tr');
      if (this.checked) {
        row.addClass('SelectedItem');
        eXo.forum.UIForumPortlet.setChecked(true);
      }else{
        eXo.forum.UIForumPortlet.setChecked(false);
        row.removeClass('SelectedItem');
      }
    },
    checkAll : function(){
      eXo.forum.UIForumPortlet.checkAll(this);
      var table = $(this).find('table tbody').eq(0);
      var rows = $(table).find('tr');
      var i = rows.length ;
      if (this.checked) {
        while(i--) {
          rows.eq(i).addClass('SelectedItem');
        }
      } else{
        while(i--){
          rows.eq(i).removeClass('SelectedItem');
        }
      }
    }
  };

  // Expose
  window.eXo = eXo || {};
  window.eXo.forum = eXo.forum || {} ;
  window.eXo.forum.CheckBox = CheckBox;
})(gj, window, document);
