CKEDITOR.plugins.add('helpBBCode',
	{
    lang : ['en','fr','vi'],
		init : function(editor) {
			var pluginName = 'helpBBCode';
			var mypath = this.path;	
			editor.ui.addButton(
				'helpBBCode.btn',
				{
					label : editor.lang.helpBBCode.HelpBBCode,
					command : 'helpBBCode.cmd',
					icon : mypath + 'helpBBCode.png'
				}
			);
			var cmd = editor.addCommand('helpBBCode.cmd', {exec:showHelpBBCode});
			cmd.modes = {wysiwyg: 1, source: 1};
			cmd.canUndo = false;	
		}
	}
);

function showHelpBBCode(e){   
  window.open('/forumResources/eXoPlugins/helpBBCode/helpBBCode.html','helpBBCode','width=800,height=600');
}
