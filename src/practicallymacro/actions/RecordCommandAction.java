package practicallymacro.actions;

import java.util.List;

import org.eclipse.core.commands.IHandler;
import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.ExpressionInfo;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.StatusLineLayoutData;
import org.eclipse.jface.commands.ActionHandler;
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
import org.eclipse.ui.ISources;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.texteditor.AbstractTextEditor;

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
//	private static IAction mSavedFindAction;
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

			ICommandService cs = MacroManager.getOldCommandService();
			cs.addExecutionListener(mRecorder);

			registerFindAction();

			MacroManager.getManager().setMacroState(MacroManager.State_Recording);
		} 
		else if (state == MacroManager.State_Recording)
		{
			shutDownRecorder();
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
	
				ICommandService cs = MacroManager.getOldCommandService();
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
				if (Activator.getDefault().getPreferenceStore().getBoolean(Initializer.Pref_CompressCharInsertsWhenRecording))
				{
					commands=EditorMacro.compressStringInsertions(commands);
				}
				
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
		//This is a hack.  It's not a hack to call activateHandler, but I need to make sure
		//my version of the command is used, so I'm assigning it a high priority via
		//the 'Expression'.  This is necessary starting in Juno, where they have 
		//tweaked some of the command mapping code.
		
//		AbstractTextEditor ate=findTextEditor(mRecorder.getEditor());
//		if (ate!=null)
		{
//			mSavedFindAction=ate.getAction(ITextEditorActionConstants.FIND);
			IAction macroFindAction=new MacroFindAction(mRecorder);
//			macroFindAction.setActionDefinitionId(IWorkbenchActionDefinitionIds.FIND_REPLACE);
////			ate.removeActionActivationCode(ITextEditorActionConstants.FIND);
////			ate.setAction(ITextEditorActionConstants.FIND, null);
//			ate.setAction(ITextEditorActionConstants.FIND, macroFindAction);
			
			IHandlerService hs = MacroManager.getOldHandlerService();
			IHandler actionHandler = new ActionHandler(macroFindAction);
//			String id="org.eclipse.ui.textEditorScope";
//			Set<String> partIDs=new HashSet<String>();
//			partIDs.add("partID");
			Expression expr=new HighPriorityExpression();//id, partIDs);
			mFindReplaceHandler = hs.activateHandler(IWorkbenchCommandConstants.EDIT_FIND_AND_REPLACE, actionHandler, expr);
		}
		
	}
	
	private static IHandlerActivation mFindReplaceHandler=null;
	
	private static void unregisterFindAction()
	{
		if (mFindReplaceHandler!=null)
			mFindReplaceHandler.getHandlerService().deactivateHandler(mFindReplaceHandler);
//		AbstractTextEditor ate=findTextEditor(mRecorder.getEditor());
//		if (ate!=null)
//		{
//			ate.setAction(ITextEditorActionConstants.FIND, mSavedFindAction);
//		}
	}
	
	public static AbstractTextEditor findTextEditor(IEditorPart editor)
	{
		if (editor instanceof AbstractTextEditor)
			return (AbstractTextEditor)editor;
		
		if (editor instanceof MultiPageEditorPart)
		{
			MultiPageEditorPart mpe=(MultiPageEditorPart)editor;
			IEditorPart[] parts=mpe.findEditors(editor.getEditorInput());
			for (IEditorPart editorPart : parts)
			{
				if (editorPart instanceof AbstractTextEditor)
				{
					return (AbstractTextEditor)editorPart;
				}
			}
		}
		
		return null;
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

	static class HighPriorityExpression extends Expression 
	{
//		private String id;
//		private Set<String> partIds;

		public HighPriorityExpression() //String id, Set<String> associatedPartIds) 
		{
//			this.id = id;
//			this.partIds = associatedPartIds;
		}

		@Override
		public void collectExpressionInfo(ExpressionInfo info) {
			//assign some high-priority items to make sure I get picked
			//up before the main findReplace command.  I don't know
			//if these variables are used for anything other than assigning 
			//priority.  If so, there might be some unintended side effects.
			info.addVariableNameAccess(ISources.ACTIVE_CONTEXT_NAME);
			info.addVariableNameAccess(ISources.ACTIVE_PART_ID_NAME);
			info.addVariableNameAccess(ISources.ACTIVE_SITE_NAME);
		}

		@Override
		public EvaluationResult evaluate(IEvaluationContext context) throws CoreException {
			//if I understood more about this, I might try to match up the part id of the 
			//editor etc.  As it is, I'm just allowing it to be invoked regardless.  This
			//will only be available if a macro is currently being recorded, so the risk
			//doesn't seem that high.
			return EvaluationResult.TRUE;
//			Object obj = context.getVariable(ISources.ACTIVE_CONTEXT_NAME);
//			if (obj instanceof Collection<?>) {
//				boolean rc = ((Collection) obj).contains(id);
//				if (rc) {
//					return EvaluationResult.TRUE;
//				}
//			}
//			if (!partIds.isEmpty()) {
//				return EvaluationResult.valueOf(partIds.contains(context
//						.getVariable(ISources.ACTIVE_PART_ID_NAME)));
//			}
//			return EvaluationResult.FALSE;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof HighPriorityExpression)) {
				return false;
			}
			return true;
//			ActionSetAndPartExpression exp = (ActionSetAndPartExpression) obj;
//			return id.equals(exp.id) && partIds.equals(exp.partIds);
		}

		@Override
		public int hashCode() {
			return 20;
//			return id.hashCode() + partIds.hashCode();
		}
	}
	
}
