package practicallymacro.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.StatusLineLayoutData;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.IWorkbenchActionDefinitionIds;

import practicallymacro.commands.IMacroCommand;
import practicallymacro.dialogs.SaveMacroDialog;
import practicallymacro.editormacros.Activator;
import practicallymacro.model.EditorMacro;
import practicallymacro.model.MacroManager;
import practicallymacro.model.MacroRecorder;
import practicallymacro.preferences.Initializer;
import practicallymacro.util.MacroConsole;
import practicallymacro.util.Utilities;


public class RecordCommandAction extends Action implements IWorkbenchWindowActionDelegate
{

	private static MacroRecorder mRecorder=null;
	private static IAction mSavedFindAction;
	private static ContributionItem mRecordingWidget=null;
	private static DisposeListener mDisposeListener=new DisposeListener()
	{
		public void widgetDisposed(DisposeEvent e)
		{
			//called when styled text is closed.  Should end record mode if still on
			shutDownRecorder();
		}
	};
	
	public RecordCommandAction()
	{
	}
	
	public void dispose()
	{
		//nothing to do here
	}

	public void init(IWorkbenchWindow window) {
		// TODO Auto-generated method stub

	}

	public void run(IAction action) {

		String state = MacroManager.getManager().getMacroState();

		if (state == MacroManager.State_Idle)
		{
			IEditorPart editor=Utilities.getActiveEditor();
			StyledText styledText = Utilities.getStyledText(editor);
			if (styledText==null)
				return;
			
			styledText.addDisposeListener(mDisposeListener);
			
			IStatusLineManager statusLineManager=editor.getEditorSite().getActionBars().getStatusLineManager();
			addStatusBarWidget(statusLineManager);
			mRecorder = new MacroRecorder(editor);
			mRecorder.start();

			styledText.addListener(SWT.KeyDown, mRecorder);
			styledText.addListener(SWT.KeyUp, mRecorder);

			IDocument document=Utilities.getIDocumentForEditor(mRecorder.getEditor());
			document.addDocumentListener(mRecorder);
			document.addDocumentListener(mRecorder.getMarkUpdater());

			ICommandService cs = (ICommandService) PlatformUI.getWorkbench().getAdapter(ICommandService.class);
			cs.addExecutionListener(mRecorder);

			registerFindAction();

			MacroManager.getManager().setMacroState(MacroManager.State_Recording);
		} 
		else if (state == MacroManager.State_Recording)
		{
			shutDownRecorder();
//			if (mRecorder==null)
//				return;
//			
//			IEditorPart editor=mRecorder.getEditor();
//			IEditorPart currentEditor=Utilities.getActiveEditor();
////			if (editor!=currentEditor)
////			{
////				MacroConsole.getConsole().writeln("Record-mode stop failed because editor has changed");
////				return;
////			}
//			
//			IStatusLineManager statusLineManager=currentEditor.getEditorSite().getActionBars().getStatusLineManager();
//			removeStatusBarWidget(statusLineManager);
//			
//			StyledText styledText = Utilities.getStyledText(editor);
//
//			try {
//				if (styledText!=null)
//				{
//					styledText.removeListener(SWT.KeyDown, mRecorder);
//					styledText.removeListener(SWT.KeyUp, mRecorder);
//					styledText.removeDisposeListener(mDisposeListener);
//				}
//
//				IDocument document=Utilities.getIDocumentForEditor(editor);
//				if (document!=null)
//				{
//					document.removeDocumentListener(mRecorder);
//					document.removeDocumentListener(mRecorder.getMarkUpdater());
//				}
//
//				ICommandService cs = (ICommandService) PlatformUI.getWorkbench().getAdapter(ICommandService.class);
//				cs.removeExecutionListener(mRecorder);
//
//				unregisterFindAction();
//			}
//			finally {
//				shutDownRecorder();
//			}
		}
	}

	private static void shutDownRecorder()
	{
		if (mRecorder==null)
			return;
		
		try
		{
			IEditorPart editor=mRecorder.getEditor();
			IEditorPart currentEditor=Utilities.getActiveEditor();
//			if (editor!=currentEditor)
//			{
//				MacroConsole.getConsole().writeln("Record-mode stop failed because editor has changed");
//				return;
//			}
			
			try
			{
				if (currentEditor!=null)
				{
					IStatusLineManager statusLineManager=currentEditor.getEditorSite().getActionBars().getStatusLineManager();
					removeStatusBarWidget(statusLineManager);
				}
				
				try
				{
					StyledText styledText = Utilities.getStyledText(editor);
		
					if (styledText!=null)
					{
						styledText.removeListener(SWT.KeyDown, mRecorder);
						styledText.removeListener(SWT.KeyUp, mRecorder);
						styledText.removeDisposeListener(mDisposeListener);
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}

				try
				{
					IDocument document=Utilities.getIDocumentForEditor(editor);
					if (document!=null)
					{
						document.removeDocumentListener(mRecorder);
						document.removeDocumentListener(mRecorder.getMarkUpdater());
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
	
				ICommandService cs = (ICommandService) PlatformUI.getWorkbench().getAdapter(ICommandService.class);
				cs.removeExecutionListener(mRecorder);
	
				try
				{
					unregisterFindAction();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
			catch (Exception e)
			{
				//catch all exceptions since we don't want anything bad that happens to prevent other cleanup
				e.printStackTrace();
			}
			
			mRecorder.stop();
	
			List<IMacroCommand> commands=mRecorder.getMacroCommands();
			if (commands.size()>0)
			{
				EditorMacro newMacro=null;
				if (Activator.getDefault().getPreferenceStore().getBoolean(Initializer.Pref_ShowSaveDialogAfterRecording))
				{					
					Shell shell = Display.getDefault().getActiveShell();
					SaveMacroDialog dlg=new SaveMacroDialog(shell, commands);
					if (dlg.open()==Dialog.OK)
					{
						//store as last recorded macro
						newMacro=dlg.getMacro();
					}
				}
				else
				{
					newMacro=new EditorMacro(commands, "", MacroManager.getManager().getUniqueMacroName(), "");
				}
				
				if (newMacro!=null)
				{
					MacroManager.getManager().addMacro(newMacro);
					MacroManager.getManager().setLastMacro(newMacro);
					newMacro.dump();
				}
				
			} else {
				//do nothing; empty set of commands
				MacroConsole.getConsole().writeln("No commands recorded; no macro defined", MacroConsole.Type_RecordingCommand);
			}
		}
		finally
		{
			mRecorder=null;
			MacroManager.getManager().setMacroState(MacroManager.State_Idle);
		}
	}

	private void addStatusBarWidget(IStatusLineManager statusLineManager)
	{
		if (mRecordingWidget==null)
		{
			mRecordingWidget=new RecordingWidget();
		}
		
		
		statusLineManager.add(mRecordingWidget);
		statusLineManager.update(true);
	}

	private static void removeStatusBarWidget(IStatusLineManager statusLineManager)
	{
		if (mRecordingWidget!=null)
			statusLineManager.remove(mRecordingWidget);
		statusLineManager.update(true);
	}

	public void selectionChanged(IAction action, ISelection selection) {
		// nothing to do
	}

	private void registerFindAction()
	{
		IEditorPart editor=mRecorder.getEditor();
		if (editor instanceof AbstractTextEditor)
		{
			AbstractTextEditor ate = (AbstractTextEditor) editor;
			mSavedFindAction=ate.getAction(ITextEditorActionConstants.FIND);
			IAction macroFindAction=new MacroFindAction(mRecorder);
			macroFindAction.setActionDefinitionId(IWorkbenchActionDefinitionIds.FIND_REPLACE);
			ate.setAction(ITextEditorActionConstants.FIND, macroFindAction);
		}
		
	}
	
	private static void unregisterFindAction()
	{
		IEditorPart editor=mRecorder.getEditor();
		if (editor instanceof AbstractTextEditor)
		{
			AbstractTextEditor ate = (AbstractTextEditor) editor;
			ate.setAction(ITextEditorActionConstants.FIND, mSavedFindAction);
		}
	}

	public static MacroRecorder getRecorder()
	{
		return mRecorder;
	}
	
	private static class RecordingWidget extends ContributionItem
	{
		private static int DRAW_FLAGS = SWT.DRAW_MNEMONIC | SWT.DRAW_TAB | SWT.DRAW_TRANSPARENT | SWT.DRAW_DELIMITER;
		CLabel mStatus;

		public static final String ID="Recording_STATUS";

		public RecordingWidget()
		{
			super(ID);
		}

		@Override
		public void fill(Composite parent)
		{
			Label sep=new Label(parent, SWT.SEPARATOR);

			Composite comp=new Composite(parent, SWT.None);
			GridLayout gl=new GridLayout(1, false);
			gl.marginHeight=0;
			gl.marginWidth=0;
			gl.horizontalSpacing=1;
			gl.verticalSpacing=0;
			comp.setLayout(gl);

			String recordingString = "Recording";

			mStatus=new CLabel(comp, SWT.SHADOW_NONE|SWT.CENTER);
			mStatus.setBackground(new Color[]{parent.getDisplay().getSystemColor(SWT.COLOR_DARK_BLUE), 
		                           parent.getDisplay().getSystemColor(SWT.COLOR_CYAN)},
		                 new int[] {100}, true);

			GridData lgd=new GridData(GridData.FILL_BOTH);

			int widthHint=(-1);
			int heightHint=(-1);
			{
				GC gc = new GC(mStatus);
				gc.setFont(mStatus.getFont());
				FontMetrics fm = gc.getFontMetrics();
				widthHint = gc.textExtent(recordingString, DRAW_FLAGS).x+6; //6 pixels is built in as an indent that we can't get at.
				heightHint = fm.getHeight();
				gc.dispose();
			}

			lgd.widthHint=widthHint;
			mStatus.setLayoutData(lgd);

			StatusLineLayoutData data = new StatusLineLayoutData();
			data.heightHint = heightHint;
			sep.setLayoutData(data);

			StatusLineLayoutData overallData = new StatusLineLayoutData();
			overallData.heightHint=heightHint;
			overallData.widthHint=widthHint;
			comp.setLayoutData(overallData);

			mStatus.setText(recordingString);
		}
	}

	
}
