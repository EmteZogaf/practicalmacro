package practicallymacro.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class InsertStringConfigureDlg extends Dialog
{
	private Text mText;
	private String mResultText;
	
	public InsertStringConfigureDlg(Shell parentShell, String initialData)
	{
		super(parentShell);
		mResultText=initialData;
	}

	@Override
	protected Control createDialogArea(Composite parent)
	{
		getShell().setText("Insert String");
		Composite comp=new Composite(parent, SWT.None);
		comp.setLayout(new GridLayout());
		comp.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Label l=new Label(comp, SWT.None);
		l.setText("Enter string to insert");
		
		mText=new Text(comp, SWT.SINGLE | SWT.H_SCROLL | SWT.BORDER);
		mText.setLayoutData(new GridData(GridData.FILL_BOTH));
		mText.addTraverseListener(new TraverseListener ()
		{
			public void keyTraversed(TraverseEvent e)
			{
				if (e.detail==SWT.TRAVERSE_TAB_NEXT)
				{
					e.doit = false;
					mText.insert("\t");
				}
			}
		});
		if (mResultText!=null)
			mText.setText(mResultText);
		
		return comp;
	}

	public String getText()
	{
		return mResultText;
	}

	@Override
	protected void okPressed()
	{
		mResultText=mText.getText();
		super.okPressed();
	}

	
}
