package practicallymacro.editormacros;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import practicallymacro.model.MacroManager;
import practicallymacro.preferences.Initializer;


/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "PracticallyMacro";

	// The shared instance
	private static Activator plugin;
	
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
		cacheStatsURL();
		Thread t=new Thread(new Runnable()
		{
			public void run()
			{
				while (true)
				{
					try
					{
						//grab items and clear protected queue
						List<Map<String, String>> items=new ArrayList<Map<String,String>>();
						synchronized (getDefault().mAuditQueue)
						{
							items.addAll(getDefault().mAuditQueue);
							getDefault().mAuditQueue.clear();
						}
						
						String statsUrl=getPreferenceStore().getString(Initializer.Pref_StatsURL);
						for (Map<String, String> props : items)
						{
							StringBuffer buffer=new StringBuffer();
							buffer.append(statsUrl);
							buffer.append('?');
							boolean first=true;
							for (Map.Entry<String, String> entry : props.entrySet())
							{
								if (!first)
									buffer.append('&');
								first=false;
								buffer.append(entry.getKey()+"="+entry.getValue());
							}
							
							String data=buffer.toString();
							data=data.replace(' ','_');
							URL url=new URL(data);
							
							InputStream is=url.openStream();
							if (is!=null)
								is.close();
						}
						
						Thread.sleep(1000*60*60);
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
				}
			}
		});
		t.start();
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

	/**
	 * This method puts out a check for updates on a separate thread (to prevent locking the
	 * UI if there are network problems).  It posts back to the main thread with a dialog
	 * if there are updates (or always if the flag is true)
	 */
	public void cacheStatsURL()
	{
		Job j=new Job("PracticallyMacro-find statistics URL")
		{
			@Override
			protected IStatus run(IProgressMonitor monitor)
			{
				if (!getPreferenceStore().getBoolean(Initializer.Pref_CaptureStatsPrompted))
				{
					PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable()
					{
						public void run()
						{
							boolean share=MessageDialog.openQuestion(PlatformUI.getWorkbench().getDisplay().getActiveShell(), "Practically Macro - Allow logging of usage information", "I would like to collect some usage information about this plugin. \nIf you consent to allow random usage information to be shared, click the 'Yes' button.\nThis information is for my interest and includes a sampling of what operations were performed and the context.\nYou can change your preference later via the preference page.  \nDo you consent to sharing some information?");
							getDefault().getPreferenceStore().setValue(Initializer.Pref_CaptureStats, share);
							getDefault().getPreferenceStore().setValue(Initializer.Pref_CaptureStatsPrompted, true);
						}
					});
				}
				
				try
				{
					String temp=Platform.getBundle("org.eclipse.platform").getHeaders().get("Bundle-Version").toString();
					if (temp!=null)
						getDefault().mEclipseVersion=temp;
				}
				catch (Exception e){}
				
				try
				{
					String temp=Platform.getBundle(PLUGIN_ID).getHeaders().get("Bundle-Version").toString();
					if (temp!=null)
						getDefault().mPluginVersion=temp;
				}
				catch (Exception e){}
				
				String temp;
				
				temp=System.getProperty("java.vm.version");
				if (temp!=null)
					getDefault().mJavaVersion=temp;

				temp=System.getProperty("user.name");
				if (temp!=null)
					getDefault().mUserID=temp;

				temp=System.getProperty("user.language");
				if (temp!=null)
					getDefault().mUserLang=temp;

				temp=System.getProperty("user.country");
				if (temp!=null)
					getDefault().mUserCountry=temp;

				temp=System.getProperty("user.timezone");
				if (temp!=null)
					getDefault().mTimeZoneID=temp;

				temp=System.getProperty("os.name");
				if (temp!=null)
					getDefault().mOS=temp;

				temp=System.getProperty("os.version");
				if (temp!=null)
					getDefault().mOSVersion=temp;

				temp=System.getProperty("os.arch");
				if (temp!=null)
					getDefault().mArch=temp;

				String siteURL="http://flexformatter.googlecode.com/svn/trunk/FlexFormatter/StatsURL.txt?sid=" + System.currentTimeMillis();
				BufferedReader in = null;
				try
				{
					URL ffSite = new URL(siteURL);
					in = new BufferedReader(new InputStreamReader(ffSite.openStream()));
					String statsURLBase=in.readLine();
					in.close();
					getPreferenceStore().setValue(Initializer.Pref_StatsURL, statsURLBase);
				}
				catch (Exception ex)
				{
					System.err.println("Exception retrieving stats URL");
					ex.printStackTrace();
				}
				finally
				{
					if (in!=null)
					{
						try {
							in.close();
						} catch (IOException e1) {
						}
					}
				}
				
				//do the mac address last in case there's a problem
				try
				{
					/*
					 * Get NetworkInterface for the current host and then read the 
					 * hardware address.
					 */
					InetAddress address = InetAddress.getLocalHost();
					NetworkInterface ni = NetworkInterface.getByInetAddress(address);
					Method getMac=ni.getClass().getMethod("getHardwareAddress");
					if (getMac!=null)
					{
						byte[] mac = (byte[])getMac.invoke(ni); //ni.getHardwareAddress();
						StringBuffer buffer=new StringBuffer();
						for (byte b : mac) {
							buffer.append(Byte.toString(b)+" ");
						}
						getDefault().mMacAddress=buffer.toString();
					}
				}
				catch (Throwable e)
				{
					e.printStackTrace();
				}
				
				if (getDefault().mMacAddress.length()==0)
				{
					getDefault().mMacAddress=getDefault().mUserID+getDefault().mTimeZoneID;
				}
				
				return Status.OK_STATUS;
			}
		};
		
		j.schedule();
	}
	
	
	private List<Map<String, String>> mAuditQueue=new ArrayList<Map<String,String>>(); 
	
	private String mApplication="PracticallyMacro";
	private String mEclipseVersion="";
	private String mPluginVersion="";
	private String mJavaVersion="";
	private String mUserID="";
	private String mUserLang="";
	private String mUserCountry="";
	private String mMacAddress="";
	private String mTimeZoneID="";
	private String mOS="";
	private String mArch="";
	private String mOSVersion="";
	
	public static final String Audit_FileExtension="fileType";
	public static final String Audit_Operation="operation";
	public static final String Audit_Operation_Record="Record";
	public static final String Audit_Operation_PlayLast="PlayLast";
	public static final String Audit_Operation_PlayAny="PlayAny";
	public static final String Audit_Operation_QuickPlay="QuickPlay";
	public static final String Audit_Operation_Edit="Edit";
	public static final String Audit_Operation_PlaySaved="PlaySaved";
	
	public static void logStatistics(Map<String, String> props)
	{
		try
		{
			if (!getDefault().getPreferenceStore().getBoolean(Initializer.Pref_CaptureStats))
				return;
			
			props.put("userID", getDefault().mUserID);
			props.put("userCountry", getDefault().mUserCountry);
			props.put("userLang", getDefault().mUserLang);
			props.put("eclipseVersion", getDefault().mEclipseVersion);
			props.put("jdkVersion", getDefault().mJavaVersion);
			props.put("pluginVersion", getDefault().mPluginVersion);
			props.put("os", getDefault().mOS);
			props.put("osVersion", getDefault().mOSVersion);
			props.put("osArch", getDefault().mArch);
			props.put("timezone", getDefault().mTimeZoneID);
			props.put("mac", getDefault().mMacAddress);
			props.put("app", getDefault().mApplication);
			props.put("timestamp", Long.toString(System.currentTimeMillis()));
			props.put("datestamp", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
			
			synchronized (getDefault().mAuditQueue)
			{
				if (getDefault().mAuditQueue.size()<2)
				{
					getDefault().mAuditQueue.add(props);
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static String getExtension(IEditorPart editor)
	{
		if (editor==null)
			return "";
		
		IEditorInput input=editor.getEditorInput();
		if (input instanceof IPathEditorInput)
		{
			IPath path=((IPathEditorInput)input).getPath();
			String lastSeg=path.lastSegment().toLowerCase();
			int lastDot=lastSeg.lastIndexOf('.');
			if (lastDot>=0)
				return lastSeg.substring(lastDot+1);
		}
		
		return "";
	}
}
