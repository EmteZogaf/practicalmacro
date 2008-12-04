package practicallymacro.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import practicallymacro.commands.FindCommand;

public class FindConfigureDialog extends Dialog
{
	private FindCommand mSourceCommand;
	
	private Button mSearchStringRadio;
	private Text mSearchText;
	private Button mSearchSelectionRadio;
	private Button mReplaceButton;
	private Text mReplaceText;
	private Button mReplaceAll;
//	private Button mFindAfterReplace;
	private Button mCaseSensitive;
	private Button mSearchForward;
	private Button mRegExpMode;
	private Button mMatchWord;
	private Button mWrap;
	private Button mSelectionScope;
//	private Button mNormalReplace;
	
	private String mInitialSearchString;
	
	public FindConfigureDialog(Shell shell, FindCommand sourceCommand, String initialSearchString)
	{
		super(shell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		mSourceCommand=sourceCommand;
		mInitialSearchString=initialSearchString;
	}
	
	@Override
	protected Control createDialogArea(Composite parent)
	{
		getShell().setText("Find/Replace settings");
		Composite comp=new Composite(parent, SWT.None);
		comp.setLayout(new GridLayout());
		comp.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Group stringComp=new Group(comp, SWT.None);
		stringComp.setText("Search terms");
		stringComp.setLayout(new GridLayout(2, false));
		stringComp.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Composite searchStringComp=new Composite(stringComp, SWT.None);
		searchStringComp.setLayout(new GridLayout());
		searchStringComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		mSearchStringRadio=new Button(searchStringComp, SWT.RADIO);
		mSearchStringRadio.setText("Search String");
		mSearchStringRadio.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e) {
				enableWidgets();
			}
		});
		mSearchText=new Text(searchStringComp, SWT.BORDER | SWT.SINGLE);
		mSearchText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		mSearchSelectionRadio=new Button(searchStringComp, SWT.RADIO);
		mSearchSelectionRadio.setText("Use clipboard (or selection)");
		mSearchSelectionRadio.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e) {
				enableWidgets();
			}
		});
		
		Composite replaceStringComp=new Composite(stringComp, SWT.None);
		replaceStringComp.setLayout(new GridLayout());
		replaceStringComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		mReplaceButton=new Button(replaceStringComp, SWT.CHECK);
		mReplaceButton.setText("Replace");
		mReplaceButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e) {
				enableWidgets();
			}
		});
		mReplaceText=new Text(replaceStringComp, SWT.SINGLE | SWT.BORDER);
		mReplaceText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//		mNormalReplace=new Button(replaceStringComp, SWT.RADIO);
//		mNormalReplace.setText("Replace");
		mReplaceAll=new Button(replaceStringComp, SWT.CHECK);
		mReplaceAll.setText("Replace All");
//		mFindAfterReplace=new Button(replaceStringComp, SWT.RADIO);
//		mFindAfterReplace.setText("Find after replace");
		
		
		Composite buttonComp=new Composite(comp, SWT.None);
		buttonComp.setLayout(new GridLayout());
		mCaseSensitive=new Button(buttonComp, SWT.CHECK);
		mCaseSensitive.setText("Case Sensitive");
		mSearchForward=new Button(buttonComp, SWT.CHECK);
		mSearchForward.setText("Search Forward");
		mRegExpMode=new Button(buttonComp, SWT.CHECK);
		mRegExpMode.setText("Regular Expression mode");
		mMatchWord=new Button(buttonComp, SWT.CHECK);
		mMatchWord.setText("Match Whole Word");
		mWrap=new Button(buttonComp, SWT.CHECK);
		mWrap.setText("Wrap Search");
		mSelectionScope=new Button(buttonComp, SWT.CHECK);
		mSelectionScope.setText("Search in selection");
		
		//populate fields from command
		mCaseSensitive.setSelection(mSourceCommand.isCaseSensitive());
		mSearchForward.setSelection(mSourceCommand.isSearchForward());
		mRegExpMode.setSelection(mSourceCommand.isRegExpMode());
		mMatchWord.setSelection(mSourceCommand.isMatchWholeWord());
		mWrap.setSelection(mSourceCommand.isWrapSearch());
		mSelectionScope.setSelection(mSourceCommand.isScopeIsSelection());
		
		if (mSourceCommand.getSearchString()!=null)
		{
			mSearchStringRadio.setSelection(true);
			mSearchText.setText(mSourceCommand.getSearchString());
		}
		else if (mInitialSearchString!=null)
		{
			mSearchStringRadio.setSelection(true);
			mSearchText.setText(mInitialSearchString);
		}
		else
		{
			mSearchSelectionRadio.setSelection(true);
		}
		
		if (mSourceCommand.getReplaceString()==null)
		{
			mReplaceButton.setSelection(false);
		}
		else
		{
			mReplaceText.setText(mSourceCommand.getReplaceString());
			mReplaceButton.setSelection(true);
		}
		
		mReplaceAll.setSelection(mSourceCommand.isReplaceAll());
//		mFindAfterReplace.setSelection(mSourceCommand.isFindAfterReplace());
		
		enableWidgets();
		
		return comp;
	}
	
	private void enableWidgets()
	{
		mReplaceAll.setEnabled(mReplaceButton.getSelection());
//		mFindAfterReplace.setEnabled(mReplaceButton.getSelection());
		mReplaceText.setEnabled(mReplaceButton.getSelection());
//		mNormalReplace.setEnabled(mReplaceButton.getSelection());
		mSearchText.setEnabled(mSearchStringRadio.getSelection());
		mSelectionScope.setEnabled(mSearchStringRadio.getSelection());
		if (!mSearchStringRadio.getSelection())
			mSelectionScope.setSelection(false);
	}

	@Override
	protected void okPressed()
	{
		//modify find command with new data
		mSourceCommand.setCaseSensitive(mCaseSensitive.getSelection());
//		mSourceCommand.setFindAfterReplace(mFindAfterReplace.getSelection());
		mSourceCommand.setMatchWholeWord(mMatchWord.getSelection());
		mSourceCommand.setRegExpMode(mRegExpMode.getSelection());
		mSourceCommand.setReplaceAll(mReplaceAll.getSelection());
		mSourceCommand.setSearchForward(mSearchForward.getSelection());
		mSourceCommand.setWrapSearch(mWrap.getSelection());
		
		if (mSearchSelectionRadio.getSelection())
		{
			mSourceCommand.setSearchString(null);
		}
		else
		{
			mSourceCommand.setSearchString(mSearchText.getText());
		}
		
		mSourceCommand.setScopeIsSelection(mSelectionScope.getSelection());
		
		if (mReplaceButton.getSelection())
		{
			mSourceCommand.setReplaceString(mReplaceText.getText());
			mSourceCommand.setReplaceAll(mReplaceAll.getSelection());
//			mSourceCommand.setFindAfterReplace(mFindAfterReplace.getSelection());
		}
		else
		{
			mSourceCommand.setReplaceString(null);
		}
		super.okPressed();
	}
	
}
