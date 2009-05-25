package practicallymacro.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;

import practicallymacro.editormacros.Activator;

public class Initializer extends AbstractPreferenceInitializer {

	public static final String Pref_RecordRawCharacterKeys="PracticallyMacro_RecordRawCharacterKeys";
	public static final String Pref_ShowMacroConsole="PracticallyMacro_ShowMacroConsole";
	public static final String Pref_ShowSaveDialogAfterRecording="PracticallyMacro_ShowSaveDialog";
	public static final String Pref_MaximumTempMacroCount="PracticallyMacro_MaxTempMacroCount";
	public static final String Pref_WriteToMacroConsole="PracticallyMacro_WriteToMacroConsole";
	public static final String Pref_ExecuteMacrosAtomically="PracticallyMacro_ExecuteMacrosAtomically";
	public static final String Pref_CompressCharInsertsWhenRecording="PracticallyMacro_CompressCharInsertsWhenRecording";

	public static final String Pref_QuickPlayCommands="PracticallyMacro_QuickPlayCommands";
	
	public Initializer() {
	}

	@Override
	public void initializeDefaultPreferences()
	{
		Activator.getDefault().getPreferenceStore().setDefault(Pref_RecordRawCharacterKeys, false);
		Activator.getDefault().getPreferenceStore().setDefault(Pref_ShowMacroConsole, true);
		Activator.getDefault().getPreferenceStore().setDefault(Pref_ShowSaveDialogAfterRecording, true);
		Activator.getDefault().getPreferenceStore().setDefault(Pref_MaximumTempMacroCount, 50);
		Activator.getDefault().getPreferenceStore().setDefault(Pref_WriteToMacroConsole, true);
		Activator.getDefault().getPreferenceStore().setDefault(Pref_ExecuteMacrosAtomically, true);
		Activator.getDefault().getPreferenceStore().setDefault(Pref_CompressCharInsertsWhenRecording, true);
		Activator.getDefault().getPreferenceStore().setDefault(Pref_QuickPlayCommands, "");
	}

}
