(function(gj) {
  var UIFormScrollSelectBox = {
      parent : null,
      inforData : null,
      inputValue : null,
      dataConfig : {
        onchange : '',
        sizeScroll : 10,
        isDisabled : false
      },
      menuContainer : null,
      selectedDisplay : null,
      optionListCont : null,
      menu : null,
      init : function(parentId) {
        //
        this.inputValue = gj('#' + parentId.replace('ScrollSelect', ''));
        
        this.parent = gj('#' + parentId);
        
        this.inforData = this.parent.find('div.selectInfoData:first')
        
        this.dataConfig.onchange = this.inforData.attr('data-onchange');
        this.dataConfig.sizeScroll = this.inforData.attr('data-size');
        this.dataConfig.isDisabled = this.inforData.attr('data-disabled');
        
        this.menuContainer = this.parent.find('.uiFormScrollMenu:first');
        this.selectedDisplay = this.menuContainer.find('span:first');

        this.menu = this.menuContainer.find('.optionMenu:first');

        this.optionListCont = this.menu.find('ul.option-list:first');

        if(String(this.dataConfig.isDisabled) === 'false') {
          var dropdownToggle = this.menuContainer.find('.dropdown-toggle:first');
          dropdownToggle.off('click').on('click', function(e) {
            e.stopPropagation();
            gj('.filterMenu').css({'height': '0px','visibility' :'hidden'});

            //
            UIFormScrollSelectBox.menu.css('height', 'auto');
            var h = UIFormScrollSelectBox.menu.height();
            UIFormScrollSelectBox.menu.css({'height': '0px','visibility' :'visible'})
            .animate({height: h + 'px'}, 400, function() {
              gj(this).css('height', 'auto');
            });
          });
        }

        this.initItem();

        var uiForm = this.parent.parents('.UIForm:first');
        function parentClick() {
          var pr = gj(this);
          pr.find('.optionMenu').animate({'height': '0px'}, 200, function() {
            gj(this).css({'visibility': 'hidden'});
          });
        }
        uiForm.off('click', parentClick).on('click', parentClick);
      },
      initItem : function() {
        
        var opsList = UIFormScrollSelectBox.optionListCont.find('li');
        
        opsList.on('click', function(e) {
          e.stopPropagation();
          var item = gj(this);
          UIFormScrollSelectBox.selectedDisplay.html(item.html());
          UIFormScrollSelectBox.inputValue.val(item.attr('data-value'));

          UIFormScrollSelectBox.menu.animate({'height': '0px'}, 400, function() {
            UIFormScrollSelectBox.menu.css({'visibility': 'hidden'});
            if(UIFormScrollSelectBox.dataConfig.onchange && UIFormScrollSelectBox.dataConfig.onchange !== '') {
              gj('<div onclick="' + UIFormScrollSelectBox.dataConfig.onchange + '"></div>').trigger('click');
            }
          });
          
        });
        
		var itemAs = this.optionListCont.find('a');
		itemAs.on('mouseover', function(e) {
			UIFormScrollSelectBox.optionListCont.find('a').attr('class', '');
			e.stopPropagation();
			var item = gj(this);
			item.attr('class', 'selected');
		})
		.on('mouseout', function(e) {
			e.stopPropagation();
			gj(this).attr('class', '');
			gj.each(opsList, function(id, elm) {
				var jElm = gj(elm);
				if(jElm.attr('data-selected') === 'true') {
					jElm.find('a:first').attr('class', 'selected');
				}
			});
		});
     
      }
    };
  return UIFormScrollSelectBox;
})(gj);
