package practicallymacro.editormacros;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import practicallymacro.model.EditorMacro;
import practicallymacro.model.MacroManager;


/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "PracticallyMacro";

	// The shared instance
	private static Activator plugin;
	
	private String mCurrentMacroState;
	private EditorMacro mLastMacro;
	
	public static final String State_Idle="MACROSTATE_IDLE";
	public static final String State_Recording="MACROSTATE_RECORDING";
	public static final String State_Playing="MACROSTATE_PLAYING";
	
	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		mCurrentMacroState=State_Idle;
		
		
//		Display.getDefault().asyncExec(new Runnable()
//		{
//			@Override
//			public void run()
//			{
//				ToolBar toolBar = new ToolBar(Display.getDefault().getActiveShell(), SWT.FLAT | SWT.RIGHT);
//			    final ToolBarManager manager = new ToolBarManager(toolBar);
////			    toolBarManager.add (getAction (IncrementDecrementAction.DECREMENT_ID));
//			}
//		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		if (!MacroManager.isNull())
			MacroManager.getManager().saveMacros();
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	public String getMacroState()
	{
		return mCurrentMacroState;
	}

	public void setMacroState(String newState)
	{
		mCurrentMacroState=newState;
	}

	public void setLastMacro(EditorMacro newMacro)
	{
		mLastMacro=newMacro;
	}
	
	public EditorMacro getLastMacro()
	{
		return mLastMacro;
	}
}
