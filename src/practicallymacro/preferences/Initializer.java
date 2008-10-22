package practicallymacro.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;

import practicallymacro.editormacros.Activator;

public class Initializer extends AbstractPreferenceInitializer {

	public static final String Pref_RecordRawCharacterKeys="PracticallyMacro_RecordRawCharacterKeys";
//	public static final String Pref_DefaultScriptContents="PracticallyMacro_DefaultScriptConents";
	public static final String Pref_ShowMacroConsole="PracticallyMacro_ShowMacroConsole";
	public static final String Pref_ShowSaveDialogAfterRecording="PracticallyMacro_ShowSaveDialog";
	public static final String Pref_MaximumTempMacroCount="PracticallyMacro_MaxTempMacroCount";
	
	public Initializer() {
	}

	@Override
	public void initializeDefaultPreferences()
	{
		Activator.getDefault().getPreferenceStore().setDefault(Pref_RecordRawCharacterKeys, false);
//		StringBuffer buffer=new StringBuffer();
//		buffer.append("//Scripts are beanshell format (see http://www.beanshell.org/) \n\n");
//		buffer.append("//variable               type\n");
//		buffer.append("//styledText             the org.eclipse.swt.custom.StyledText instance for the current editor\n");
//		buffer.append("//console                write output to the macro console via console.write(String), .writeln(String), .write(Exception)\n");
//		//invokeAction(ST.)
//		buffer.append("//findTarget             the instance of org.eclipse.jface.text.IFindReplaceTarget\n");
//		buffer.append("import org.eclipse.swt.custom.StyledText;\n");
//		buffer.append("import org.eclipse.jface.text.IFindReplaceTarget;\n");
//		buffer.append("\n");
////		buffer.append("\n");
////		buffer.append("\n");
////		buffer.append("\n");
//		
//		Activator.getDefault().getPreferenceStore().setDefault(Pref_DefaultScriptContents, buffer.toString());
		Activator.getDefault().getPreferenceStore().setDefault(Pref_ShowMacroConsole, true);
		Activator.getDefault().getPreferenceStore().setDefault(Pref_ShowSaveDialogAfterRecording, true);
		Activator.getDefault().getPreferenceStore().setDefault(Pref_MaximumTempMacroCount, 50);
	}

}
