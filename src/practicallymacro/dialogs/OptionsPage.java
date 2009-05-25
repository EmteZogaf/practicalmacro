package practicallymacro.dialogs;


import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

import practicallymacro.editormacros.Activator;
import practicallymacro.preferences.Initializer;

public class OptionsPage extends PreferencePage implements
		IWorkbenchPreferencePage {

	private Button mRecordRawChars;
	private Button mShowMacroConsole;
	private Button mWriteToMacroConsole;
	private Button mShowSaveDialog;
	private Button mCompressCharInserts;
	private Button mExecuteMacrosAtomically;
//	private Text mBaseScriptText;
	private Spinner mMaxTempMacroSpinner;
	
	public OptionsPage() {
		// TODO Auto-generated constructor stub
	}

	public OptionsPage(String title) {
		super(title);
		// TODO Auto-generated constructor stub
	}

	public OptionsPage(String title, ImageDescriptor image) {
		super(title, image);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected Control createContents(Composite parent)
	{
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getShell(), Activator.PLUGIN_ID+".overallHelp");
		Composite comp=new Composite(parent, SWT.None);
		comp.setLayout(new GridLayout());
		comp.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		mRecordRawChars=new Button(comp, SWT.CHECK);
		mRecordRawChars.setText("Record raw character events (not recommended)");
		mRecordRawChars.setToolTipText("If set, raw keyup/down events will be recorded into the macro.\n  This allows the capturing of processing of special events like adding matching quotes.\nNote: Many options on this page will be ignored for macros containing raw character events.");
		mRecordRawChars.setSelection(Activator.getDefault().getPreferenceStore().getBoolean(Initializer.Pref_RecordRawCharacterKeys));
		
		mCompressCharInserts=new Button(comp, SWT.CHECK);
		mCompressCharInserts.setText("Compress character insertions into String insertions while recording");
		mCompressCharInserts.setToolTipText("If set, String events will be created in place of individual character inserts when recording.");
		mCompressCharInserts.setSelection(Activator.getDefault().getPreferenceStore().getBoolean(Initializer.Pref_CompressCharInsertsWhenRecording));
		
		mExecuteMacrosAtomically=new Button(comp, SWT.CHECK);
		mExecuteMacrosAtomically.setText("Execute macros atomically");
		mExecuteMacrosAtomically.setToolTipText("If set, execute macros as an atomic editor command.  This can be overridden per macro.  This is the recommended behavior for most editor macros since undo/redo will treat the macro as one event.");
		mExecuteMacrosAtomically.setSelection(Activator.getDefault().getPreferenceStore().getBoolean(Initializer.Pref_ExecuteMacrosAtomically));
		
		mShowMacroConsole=new Button(comp, SWT.CHECK);
		mShowMacroConsole.setText("Show macro console");
		mShowMacroConsole.setToolTipText("If set, make the macro console visible during command record/play.");
		mShowMacroConsole.setSelection(Activator.getDefault().getPreferenceStore().getBoolean(Initializer.Pref_ShowMacroConsole));
		
		mWriteToMacroConsole=new Button(comp, SWT.CHECK);
		mWriteToMacroConsole.setText("Write to macro console");
		mWriteToMacroConsole.setToolTipText("If set, write normal macro execution trace statements to the macro console during command record/play.");
		mWriteToMacroConsole.setSelection(Activator.getDefault().getPreferenceStore().getBoolean(Initializer.Pref_WriteToMacroConsole));
		
		mShowSaveDialog=new Button(comp, SWT.CHECK);
		mShowSaveDialog.setText("Show save dialog");
		mShowSaveDialog.setToolTipText("If set, bring up the save dialog each time a macro is recorded.  If not checked, each macro will be automatically saved to a unique (but meaningless) name.");
		mShowSaveDialog.setSelection(Activator.getDefault().getPreferenceStore().getBoolean(Initializer.Pref_ShowSaveDialogAfterRecording));
		
		Composite spincomp=new Composite(comp, SWT.None);
		spincomp.setLayout(new GridLayout(2, false));
		spincomp.setLayoutData(new GridData(GridData.FILL_BOTH));
		Label l=new Label(spincomp, SWT.None);
		l.setText("Maximum number of temporary macros");
		mMaxTempMacroSpinner=new Spinner(spincomp, SWT.BORDER);
		mMaxTempMacroSpinner.setMinimum(2);
		mMaxTempMacroSpinner.setMaximum(500);
		mMaxTempMacroSpinner.setSelection(Activator.getDefault().getPreferenceStore().getInt(Initializer.Pref_MaximumTempMacroCount));
		
//		Group g=new Group(comp, SWT.None);
//		g.setText("Default script text");
//		g.setLayout(new GridLayout());
//		g.setLayoutData(new GridData(GridData.FILL_BOTH));
//		
//		mBaseScriptText=new Text(g, SWT.MULTI | SWT.BORDER);
//		mBaseScriptText.setLayoutData(new GridData(GridData.FILL_BOTH));
//		mBaseScriptText.setText(Activator.getDefault().getPreferenceStore().getString(Initializer.Pref_DefaultScriptContents));
		
		return comp;
	}

	public void init(IWorkbench workbench)
	{
		//nothing to do
	}

	@Override
	public boolean performOk()
	{
		Activator.getDefault().getPreferenceStore().setValue(Initializer.Pref_RecordRawCharacterKeys, mRecordRawChars.getSelection());
		Activator.getDefault().getPreferenceStore().setValue(Initializer.Pref_ShowMacroConsole, mShowMacroConsole.getSelection());
		Activator.getDefault().getPreferenceStore().setValue(Initializer.Pref_WriteToMacroConsole, mWriteToMacroConsole.getSelection());
		Activator.getDefault().getPreferenceStore().setValue(Initializer.Pref_ShowSaveDialogAfterRecording, mShowSaveDialog.getSelection());
		Activator.getDefault().getPreferenceStore().setValue(Initializer.Pref_CompressCharInsertsWhenRecording, mCompressCharInserts.getSelection());
		Activator.getDefault().getPreferenceStore().setValue(Initializer.Pref_ExecuteMacrosAtomically, mExecuteMacrosAtomically.getSelection());
//		Activator.getDefault().getPreferenceStore().setValue(Initializer.Pref_DefaultScriptContents, mBaseScriptText.getText());
		Activator.getDefault().getPreferenceStore().setValue(Initializer.Pref_MaximumTempMacroCount, mMaxTempMacroSpinner.getSelection());
		return super.performOk();
	}

	@Override
	protected void performDefaults()
	{
		mRecordRawChars.setSelection(Activator.getDefault().getPreferenceStore().getDefaultBoolean(Initializer.Pref_RecordRawCharacterKeys));
		mShowMacroConsole.setSelection(Activator.getDefault().getPreferenceStore().getDefaultBoolean(Initializer.Pref_ShowMacroConsole));
		mWriteToMacroConsole.setSelection(Activator.getDefault().getPreferenceStore().getDefaultBoolean(Initializer.Pref_WriteToMacroConsole));
		mCompressCharInserts.setSelection(Activator.getDefault().getPreferenceStore().getDefaultBoolean(Initializer.Pref_CompressCharInsertsWhenRecording));
		mExecuteMacrosAtomically.setSelection(Activator.getDefault().getPreferenceStore().getDefaultBoolean(Initializer.Pref_ExecuteMacrosAtomically));
		mShowSaveDialog.setSelection(Activator.getDefault().getPreferenceStore().getDefaultBoolean(Initializer.Pref_ShowSaveDialogAfterRecording));
//		mBaseScriptText.setText(Activator.getDefault().getPreferenceStore().getDefaultString(Initializer.Pref_DefaultScriptContents));
		mMaxTempMacroSpinner.setSelection(Activator.getDefault().getPreferenceStore().getDefaultInt(Initializer.Pref_MaximumTempMacroCount));
		super.performDefaults();
	}

	
}
