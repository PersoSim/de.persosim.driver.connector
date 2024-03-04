package de.persosim.driver.connector.ui.parts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.globaltester.logging.BasicLogger;
import org.globaltester.logging.tags.LogLevel;

import de.persosim.driver.connector.CommManager;
import de.persosim.driver.connector.DriverConnectorFactory;
import de.persosim.driver.connector.IfdComm;
import de.persosim.driver.connector.UnsignedInteger;
import de.persosim.driver.connector.VirtualDriverComm;
import de.persosim.driver.connector.VirtualReaderUi;
import de.persosim.driver.connector.features.DefaultListener;
import de.persosim.driver.connector.features.MctReaderDirect;
import de.persosim.driver.connector.features.MctUniversal;
import de.persosim.driver.connector.features.ModifyPinDirect;
import de.persosim.driver.connector.features.PersoSimPcscProcessor;
import de.persosim.driver.connector.features.VerifyPinDirect;
import de.persosim.driver.connector.service.IfdConnector;
import de.persosim.simulator.preferences.PersoSimPreferenceManager;
import de.persosim.simulator.utils.BitField;
import de.persosim.simulator.utils.HexString;

/**
 * This class defines the appearance and behavior of the PinPad GUI to be used
 * in connection with the respective simulated card reader.
 * 
 * @author slutters
 *
 */
public class ReaderPart implements VirtualReaderUi {

	private static final String LAST_COMM_TYPE = "LAST_COMM_TYPE";

	private static final String LAST_READER_TYPE = "LAST_READER_TYPE";

	public enum ReaderType {
		STANDARD, BASIC, NONE;
	}

	private static final String FONT_NAME = "Helvetica";

	public static final boolean ENABLE = true;
	public static final boolean DISABLE = false;
	public static boolean autologin = false;

	public static final int KEYS_ALL_PINSAVER = -4;
	public static final int KEYS_ALL_CONTROL = -3;
	public static final int KEYS_ALL_NUMERIC = -2;
	public static final int KEYS_ALL = -1;

	private MenuItem removeTableItem;
	private MenuItem editTableItem;

	private Text txtOutput;
	private Label lblAccessRights;
	private Button[] keysNumeric;
	private Button[] keysControl;
	private TableViewer viewer;
	private Table tablePwdManagement;
	private Button checkAutoLogin;
	private IStructuredSelection selectedRow;
	private int selectedIndex = -1;

	public static TableViewerColumn columnPassword;
	private List<String> pressedKeys = new ArrayList<>();

	private Composite root;
	private Composite readerUi;

	private ReaderType type = ReaderType.NONE;

	private PasswordModelProvider passwordModelProvider = PasswordModelProvider.getInstance();

	private IfdComm currentComm;

	private ReaderType currentReaderType;

	private ReaderType lastReaderType;

	private Label lblCardPresence;


	/**
	 * Defines the virtual basic reader. It has no own input interface.
	 * 
	 * @param parent composite where the reader will be placed
	 */
	private void createBasicReader(Composite parent) {
		GridLayout parentLayout = new GridLayout(1, false);
		parentLayout.marginWidth = 0;
		parent.setLayout(parentLayout );

		GridData gridData;
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;

		Label lblOutput = new Label(parent, SWT.FILL);
		Font font = new Font(parent.getDisplay(), FONT_NAME, 24, SWT.BOLD);
		lblOutput.setFont(font);
		lblOutput.addListener(SWT.Dispose, event -> font.dispose());
		lblOutput.setText("Basic Reader");
		
		parent.layout();
		parent.redraw();
	}

	/**
	 * Defines the layout of the virtual standard reader. Unlike the basic
	 * reader it contains a keypad.
	 * 
	 * @param parent composite where the reader will be placed
	 */
	private void createStandardReader(Composite pinpadReaderUi) {
		pinpadReaderUi.setLayout(new GridLayout(6, false));

		txtOutput = new Text(pinpadReaderUi, SWT.READ_ONLY | SWT.PASSWORD
				| SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.LEFT);
		Font font = new Font(pinpadReaderUi.getDisplay(), FONT_NAME, 24, SWT.BOLD);
		txtOutput.setFont(font);
		txtOutput.addListener(SWT.Dispose, event -> font.dispose());
		txtOutput.setEditable(false);
		txtOutput.setCursor(null);
		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.horizontalSpan=6;
		gridData.heightHint = 80;
		txtOutput.setLayoutData(gridData);
		
		lblAccessRights = new Label(pinpadReaderUi, SWT.NONE);
		lblAccessRights.setCursor(null);
		lblAccessRights.setText("");
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.horizontalSpan=6;
		lblAccessRights.setLayoutData(gridData);
		
		//numeric keys
		keysNumeric = new Button[10];
		keysNumeric[1] = getNumericKey(pinpadReaderUi, 1);
		keysNumeric[2] = getNumericKey(pinpadReaderUi, 2);
		keysNumeric[3] = getNumericKey(pinpadReaderUi, 3);
		
		// control keys
		keysControl = new Button[3];
		keysControl[0] = getCancelKey(pinpadReaderUi);
		
        placeholderColumn(pinpadReaderUi);

		viewer = createPasswordTableViewer(pinpadReaderUi);
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.verticalAlignment = SWT.FILL;
		gridData.verticalSpan=3;		
		viewer.getControl().setLayoutData(gridData);
		

		keysNumeric[4] = getNumericKey(pinpadReaderUi, 4);
		keysNumeric[5] = getNumericKey(pinpadReaderUi, 5);
		keysNumeric[6] = getNumericKey(pinpadReaderUi, 6);
		
		keysControl[1] = getCorrectionKey(pinpadReaderUi);
		
        placeholderColumn(pinpadReaderUi);
		
		
		keysNumeric[7] = getNumericKey(pinpadReaderUi, 7);
		keysNumeric[8] = getNumericKey(pinpadReaderUi, 8);
		keysNumeric[9] = getNumericKey(pinpadReaderUi, 9);
		
		keysControl[2] = getConfirmationKey(pinpadReaderUi);
		
        placeholderColumn(pinpadReaderUi);
		
		Button button = createButton(pinpadReaderUi, "", null);
		button.setEnabled(DISABLE);

		keysNumeric[0] = getNumericKey(pinpadReaderUi, 0);

		button = createButton(pinpadReaderUi, "", null);
		button.setEnabled(DISABLE);

		// keep the cell below the control keys empty
		new Label(pinpadReaderUi, SWT.NONE);
		
        placeholderColumn(pinpadReaderUi);
		
		checkAutoLogin = new Button(pinpadReaderUi, SWT.CHECK);
		checkAutoLogin.setText("AutoLogin "+ "                       ");
		checkAutoLogin.setEnabled(false);
		gridData = new GridData();
		gridData.verticalAlignment = SWT.TOP;
		checkAutoLogin.setLayoutData(gridData);
		
		checkAutoLogin.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				checkAutoLogin.setText("AutoLogin: " + viewer.getSelection().toString());

				if (checkAutoLogin.getSelection()) {

					tablePwdManagement.setEnabled(false);
					autologin = true;
					pressedKeys.clear();
					char[] entries = viewer.getSelection().toString().replaceAll("\\D+", "").toCharArray();
					for (int i = 0; i < entries.length; i++) {
						char entry = entries[i];
						setButton(String.valueOf(entry));

					}

					setButton("OK");

				}

				else {

					tablePwdManagement.setEnabled(true);
					autologin = false;
					txtOutput.setText("");
					lblAccessRights.setText("");
					lblAccessRights.setToolTipText(null);
					checkAutoLogin.setText("AutoLogin");
				}

			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {

			}

		});

		
		
		
		
		
		
		
		
		tablePwdManagement.setVisible(true);
		checkAutoLogin.setVisible(true);
		setEnabledKeySetController(KEYS_ALL, false);
	}

	private void placeholderColumn(Composite parent) {
		Label label = new Label(parent, SWT.NONE);
		GridData gd = new GridData();
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalAlignment = SWT.FILL;
		label.setLayoutData(gd);
	}

	private TableViewer createPasswordTableViewer(Composite parent) {
		viewer = new TableViewer(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		
		// define the TableViewer
		tablePwdManagement = viewer.getTable();
		tablePwdManagement.setHeaderVisible(true);
		tablePwdManagement.setLinesVisible(true);


		columnPassword = new TableViewerColumn(viewer, SWT.NONE);
		columnPassword.getColumn().setWidth(155);
		columnPassword.getColumn().setText("Passwords");
		columnPassword.getColumn().setResizable(false);
		
		

		columnPassword.setLabelProvider(new ColumnLabelProvider() {
		      @Override
		      public String getText(Object element) {
		        
		        return (String) element;
		      }
		    });
		

		viewer.setContentProvider(new ArrayContentProvider());
		
		viewer.setInput(passwordModelProvider.getPins());
		final Menu popupTable = new Menu(viewer.getTable());
		
		
		viewer.getTable().addListener(SWT.MenuDetect, new Listener() {

			@Override
			public void handleEvent(Event event) {

			}
		});
		
		
		
		viewer.getTable().setMenu(popupTable);
		MenuItem addTableItem = new MenuItem(popupTable, SWT.CASCADE);
		addTableItem.setText("Add new Password");
		editTableItem = new MenuItem(popupTable, SWT.CASCADE);
		editTableItem.setText("Edit Password");
		editTableItem.setEnabled(false);
		removeTableItem = new MenuItem(popupTable, SWT.CASCADE);
		removeTableItem.setText("Remove Password");
		removeTableItem.setEnabled(false);
		
		
		
		
		
		
		/**
		 * This is the listener for "New Password" in the context menu of the
		 * list. This method created a input dialog box, where u can enter the
		 * new password. If the password meets all requirements the password
		 * will be saved in the list and in preferences.
		 */

	       
		SelectionListener addNewPasswordListener = new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				String newPassword = querryUserForNewPassword(null);
				
				if(newPassword != null){
					//input operation has been canceled by user, nothing to do
					passwordModelProvider.save(newPassword, "save", -1);
					Collections.sort(passwordModelProvider.getPins(),new PasswordComparator());
					passwordModelProvider.deletePinsFromPrefs();
					viewer.setInput(passwordModelProvider.getPins());
					int selectionIndex = passwordModelProvider.getPins().lastIndexOf(newPassword);
					viewer.setSelection(new StructuredSelection(viewer.getElementAt(selectionIndex)), true);
				}

									
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {

			}
		};
		
		addTableItem.addSelectionListener(addNewPasswordListener);
		
		
		
		/**
		 * This is the listener for "Remove Password" in the context menu of the list. The
		 * listener removes the Password from the list and preferences.
		 */
		
		SelectionListener removePasswordListener = new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {


				int index = viewer.getTable().getSelectionIndex();
				
				
				List<String> pinList = passwordModelProvider.getPins();
				pinList.remove(index);
				passwordModelProvider.setPins(pinList);
				passwordModelProvider.deletePinsFromPrefs();
				viewer.setInput(passwordModelProvider.getPins());
				viewer.refresh();
				
				int listempty = passwordModelProvider.getPins().size();
				if(listempty == 0)
				{
					checkAutoLogin.setEnabled(false);
				}
				removeTableItem.setEnabled(false);
				editTableItem.setEnabled(false);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {

			}
		};
		
		removeTableItem.addSelectionListener(removePasswordListener);
		
		/**
		 * This is the listener for the double click in the list. This method
		 * simulates the entry of the password entered and confirms with the
		 * "OK" button.
		 */
		
		viewer.addDoubleClickListener(new IDoubleClickListener() {

			@Override
			public void doubleClick(DoubleClickEvent event) {
				pressedKeys.clear();
				char[] entries = viewer.getSelection().toString()
						.replaceAll("\\D+", "").toCharArray();
				for (int i = 0; i < entries.length; i++) {
					char entry = entries[i];
					setButton(String.valueOf(entry));
				}

				setButton("OK");

			}

		});
		
		
		/**
		 * This is the listener for "Edit Password" in the context menu of the
		 * list. This method created a input dialog box, where u can edit the
		 * previous password. If the password meets all requirements the
		 * password will be saved in the list and in preferences.
		 */
		
		SelectionListener editPasswordListener = new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				int index = viewer.getTable().getSelectionIndex();
				String p =  passwordModelProvider.getPins().get(index);
				String newPassword = querryUserForNewPassword(p);
				

				if(newPassword != null){
					//input operation has been canceled by user, nothing to do
					passwordModelProvider.save(newPassword, "edit", index);
					Collections.sort(passwordModelProvider.getPins(), new PasswordComparator());
					passwordModelProvider.deletePinsFromPrefs();
					viewer.setInput(passwordModelProvider.getPins());
					int selectionIndex = passwordModelProvider.getPins().lastIndexOf(newPassword);
					viewer.setSelection(new StructuredSelection(viewer.getElementAt(selectionIndex)), true);
				}
									
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {

			}
		};
		
		editTableItem.addSelectionListener(editPasswordListener);
		
		
		
		/**
		 * This is the listener for the single click in the list. The listener
		 * makes it possible to select a password and highlight the choice.
		 * This listener also triggers the "AutoLogin" function to be enables and visible.
		 */
		
		SelectionListener oneClickListener = new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				 selectedRow = (IStructuredSelection) viewer.getSelection();
				 
				 selectedIndex = viewer.getTable().getSelectionIndex();
				 

				Object firstElement = selectedRow.getFirstElement();

				if (firstElement != null) {
					removeTableItem.setEnabled(true);
					editTableItem.setEnabled(true);
				}

				checkAutoLogin.setEnabled(true);

			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {

			}
		};
		
		tablePwdManagement.addSelectionListener(oneClickListener);
		
		return viewer;
	}
	
	private void disposeReaderControls() {
		if (readerUi != null)
			readerUi.dispose();
			readerUi = null;
	}
	
	
	
	@PostConstruct
	public void createComposite(Composite parent) {
		root = parent;
		
		String lastReaderType = PersoSimPreferenceManager.getPreference(LAST_READER_TYPE);
		String lastCommType = PersoSimPreferenceManager.getPreference(LAST_COMM_TYPE);
		
		IfdComm comm = CommManager.getCommForType(lastCommType);
		
		if (comm == null){
			comm = getDefaultComm();
		}
		
		ReaderType readerType;
		try {
			readerType = ReaderType.valueOf(lastReaderType);
		} catch (IllegalArgumentException | NullPointerException e){
			//NOSONAR: if no value existing, use default
			readerType = getDefaultReaderType();
		}
		
		switchReaderType(readerType, comm);
	}

	private IfdComm getDefaultComm() {
		return CommManager.getWorkingComm(VirtualDriverComm.NAME);
	}

	/**
	 * This method defines the Buttons all getxxxKey-methods use it for creating
	 * buttons.
	 * 
	 * @param parent composite where the button will be placed
	 * @param text displayed on the button
	 * @param selectionListener
	 * @return button
	 */
	private Button createButton(Composite parent, String text,
			SelectionListener selectionListener) {
		final Button button = new Button(parent, SWT.PUSH);
		button.setText(text);
		Font font = new Font(parent.getDisplay(), FONT_NAME, 36, SWT.BOLD);
		button.setFont(font);
		button.addListener(SWT.Dispose, event -> font.dispose());
		if (selectionListener != null) {
			button.addSelectionListener(selectionListener);
		}

		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.verticalAlignment = SWT.FILL;
		gridData.heightHint = 72;

		button.setLayoutData(gridData);

		return button;
	}
	
	public void setFocus() {
	    viewer.getControl().setFocus();
	}
	

	/**
	 * This method defines the Numeric Buttons.
	 * 
	 * @param parent composite where the numeric buttons will be placed
	 * @param number on the button (0-9)
	 * @return button
	 */
	private Button getNumericKey(Composite parent, int number) {
		final String text = String.valueOf(number);

		SelectionListener selectionListener = new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent event) {
				widgetDefaultSelected(event);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent event) {
				System.out.println("PIN pad button pressed: " + text);
				txtOutput.append(text);
				setButton(text);
			}
		};

		return createButton(parent, String.valueOf(number), selectionListener);
	}

	/**
	 * Returns the cancel button. It is used to cancel the user input and
	 * disables the virtual keypad, which will lead to an abort of the
	 * authentication procedure.
	 * 
	 * @param parent composite where the cancel button will be placed
	 * @return button
	 */
	private Button getCancelKey(Composite parent) {
		final String text = "C";

		SelectionListener selectionListener = new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent event) {
				widgetDefaultSelected(event);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent event) {
				setButton(text);
			}
		};

		Button button = createButton(parent, text, selectionListener);
		button.setBackground(new Color(parent.getDisplay(), 255, 0, 0));

		return button;
	}
	
	/**
	 * Queries the user for a password (either to modify an existing on or to
	 * add a anew one). If the user is intended to modify an existing password
	 * the old value is provided in the parameter to be shown to user.
	 * 
	 * @param previousValue
	 * @return new password or null if user canceled the operation
	 */
	public String querryUserForNewPassword(String previousValue)
	{
		
		String retVal = null;
		
		do {
			// query user for password
			InputDialog messageBox = new InputDialog(root.getShell(),
					"Password", "Enter your Password", previousValue, null);

			if (messageBox.open() != org.eclipse.jface.window.Window.OK) {
				// user aborted
				break;
			}

			// blank characters are beeing removed
			String currentValue = messageBox.getValue().replaceAll(" ", "");


			// null check
			if (currentValue == null) {
				MessageDialog dialog = new MessageDialog(root.getShell(), "Warnung", null, 
						"Warning! Input invalid.",
						MessageDialog.INFORMATION, new String[] { "OK" }, 0);
				dialog.open();
				continue;
			}

			// check that new value is a number
			if (currentValue.matches(".*\\D.*")) {
				MessageDialog dialog = new MessageDialog(root.getShell(), "Warnung", null,
						"Warning your input contains invalid characters.",
						MessageDialog.INFORMATION, new String[] { "OK" }, 0);
				dialog.open();
				continue;

			}

			// check length of new value
			if (currentValue.length() < 1 || currentValue.length() > 11) {
				MessageDialog dialog = new MessageDialog(root.getShell(), "Warnung", null,
						"The passoword length is not valid!. The maximum length is 10",
						MessageDialog.INFORMATION, new String[] { "OK" }, 0);
				dialog.open();
				continue;
			}

			// check if entered password already in the list
			if (passwordModelProvider.contains(currentValue)) {
				MessageDialog dialog = new MessageDialog(root.getShell(), "Warnung", null,
						"This password is already in the list.",
						MessageDialog.INFORMATION, new String[] { "OK" }, 0);
				dialog.open();

				continue;
			}

			retVal = currentValue;

		} while (retVal == null);
		
		return retVal;
		
	}
	
		
	

	/**
	 * This method returns the Correction button. It is used to clear the
	 * display. The user can retype the pin.
	 * 
	 * @param parent composite where the correction button will be placed
	 * @return button
	 */
	private Button getCorrectionKey(Composite parent) {
		final String text = "CLR";

		SelectionListener selectionListener = new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent event) {
				widgetDefaultSelected(event);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent event) {
				synchronized (pressedKeys) {
					pressedKeys.clear();
					setText("");
				}
			}
		};

		Button button = createButton(parent, text, selectionListener);
		button.setBackground(new Color(parent.getDisplay(), 255, 255, 0));

		return button;
	}

	/**
	 * This method returns the Confirmation button. It is used to send pins
	 * after their input.
	 * 
	 * @param parent composite where the confirmation button will be placed
	 * @return button
	 */
	private Button getConfirmationKey(Composite parent) {
		final String text = "OK";

		SelectionListener selectionListener = new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent event) {
				widgetDefaultSelected(event);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent event) {
				setButton(text);
			}
		};

		Button button = createButton(parent, text, selectionListener);
		button.setBackground(new Color(parent.getDisplay(), 0, 255, 0));

		return button;
	}
	
	public void setText(final String text) {
		Display.getDefault().syncExec(new Runnable() {

			@Override
			public void run() {
				txtOutput.setText(text);
			}
		});
	}

	/**
	 * Controls the clickability of button groups (numeric buttons, control
	 * buttons, pinsaver buttons or just all). The real setting is done by the
	 * {@link #setEnabledKeySet(Button[], boolean)} method
	 * 
	 * @param keySet is an array of buttons (numeric, control, pinSaver or all)
	 * @param enabled is a boolean value to enable or disable a keySet
	 * 
	 */
	public void setEnabledKeySetController(int keySet, final boolean enabled) {
		switch (keySet) {
		case KEYS_ALL_NUMERIC:
			setEnabledKeySet(keysNumeric, enabled);
			break;
		case KEYS_ALL_CONTROL:
			setEnabledKeySet(keysControl, enabled);
			break;
		case KEYS_ALL:
			setEnabledKeySetController(KEYS_ALL_NUMERIC, enabled);
			setEnabledKeySetController(KEYS_ALL_CONTROL, enabled);
			break;
		}
	}

	/**
	 * Sets the clickability of button groups. The method is called by the
	 * {@link #setEnabledKeySetController(int, boolean)}.
	 * 
	 * @param buttonSet is the button array which the setEnableKeySetController wants
	 *        to enable or disable
	 * @param enabled is a boolean value to enable or disable a keySet
	 * 
	 */
	public void setEnabledKeySet(Button[] buttonSet, final boolean enabled) {
		for (final Button button : buttonSet) {
			
			if (button == null) continue;
			
			Display.getDefault().syncExec(new Runnable() {

				@Override
				public void run() {
					button.setEnabled(enabled);
				}
			});
		}
	}
	
	private void setButton(String value) {
		pressedKeys.add(value);
		notifyIfReady();
	}

	private void notifyIfReady() {
		synchronized (pressedKeys) {
			if (pressedKeys.get(pressedKeys.size() - 1).equals("OK")
					|| pressedKeys.get(pressedKeys.size() - 1).equals("C")) {
				pressedKeys.notifyAll();
				setText("");
			}
		}
	}

	@Override
	public byte[] getPin() throws IOException {
		if (autologin == false) {
			if (type.equals(ReaderType.STANDARD)) {
				
				setEnabledKeySetController(KEYS_ALL, true);
				
				pressedKeys.clear();
				synchronized (pressedKeys) {
					while (pressedKeys.size() == 0
							|| (!pressedKeys.get(pressedKeys.size() - 1)
									.equals("OK") && !pressedKeys.get(
									pressedKeys.size() - 1).equals("C"))) {
						try {
							pressedKeys.wait();
						} catch (InterruptedException e) {
							throw new IOException(
									"The reading of the PIN could not be completed");
						}
					}
				}
				if (pressedKeys.get(pressedKeys.size() - 1).equals("C")) {
					setText("");
					setEnabledKeySetController(KEYS_ALL, false);
					return null;
				}

				byte[] result = new byte[pressedKeys.size() - 1];
				for (int i = 0; i < result.length; i++) {
					try {
						byte currentNumber = (byte) pressedKeys.get(i)
								.charAt(0);
						result[i] = currentNumber;
					} catch (NumberFormatException e) {
						throw new IOException(
								"PIN containing non valid characters entered");
					}
				}
 
				setEnabledKeySetController(KEYS_ALL, false);
				return result;
				
			}
		return null;
			
		} else {
			byte[] result = new byte[pressedKeys.size() - 1];
			for (int i = 0; i < result.length; i++) {
				try {
					byte currentNumber = (byte) pressedKeys.get(i)
							.charAt(0);
					result[i] = currentNumber;
				} catch (NumberFormatException e) {
					throw new IOException("PIN containing non valid characters entered");
				}
			}
			setText("");
			return result;
			
		}
		
	}

	@Override
	public void display(String... lines) {
		if (type.equals(ReaderType.STANDARD)) {
			StringBuilder text = new StringBuilder();
			for (String line : lines) {
				text.append(line);
				text.append(System.lineSeparator());
			}
			setText(text.toString());
		}
	}

	@Override
	public byte[] getDeviceDescriptors() {
		return null;
	}

	private void addStandardListeners(IfdConnector connector) {
		connector.addListener(new VerifyPinDirect(new UnsignedInteger(0x3136C8)));
		connector.addListener(new ModifyPinDirect(new UnsignedInteger(0x3136CC)));
		connector.addListener(new MctReaderDirect(new UnsignedInteger(0x3136D0)));
		connector.addListener(new MctUniversal(new UnsignedInteger(0x3136D4)));
		connector.addListener(new PersoSimPcscProcessor(new UnsignedInteger(0x313730)));
	}

	private void addBasicListeners(IfdConnector connector) {
		connector.addListener(new DefaultListener());
	}
	
	public void switchReaderType(ReaderType readerType) {
		switchReaderType(readerType, null);
	}
	
	public void switchReaderType(IfdComm comm) {
		switchReaderType(null, comm);
	}

	
	/**
	 * Switch the parts user interface and behavior to the reader type
	 * associated with the provided parameter.
	 * 
	 * @param readerType the reader type to use
	 * @param comm the {@link IfdComm} object to use
	 * @throws NoConnectorAvailableException 
	 * @throws IOException 
	 */
	private void switchReaderType(ReaderType readerType, IfdComm comm) {
		resetReader();

		disposeConnector();
		
		if (ReaderType.NONE.equals(readerType)) {
			return;
		}
		
		if (readerType != null) {
			currentReaderType = readerType;
		}
		
		if (currentReaderType == null) {
			currentReaderType = getDefaultReaderType();
		}
		
		lastReaderType = currentReaderType;
		
		IfdConnector connector;
		try {
			connector = getConnector();
			connector.addUi(this);
		} catch (IOException e) {
			throw new IllegalStateException("Could not get reader connector object", e);
		}
		

		disposeReaderControls();
		
		root.setLayout(new FillLayout());
		
		readerUi = new Composite(root, SWT.NONE);
		
		GridLayout layout = new GridLayout(2,false);
		readerUi.setLayout(layout);
		
		Label lblConnection = new Label(readerUi, SWT.NONE);
		GridData gdConnection = new GridData();
		gdConnection.grabExcessHorizontalSpace = true;
		lblConnection.setLayoutData(gdConnection);
		
		lblCardPresence = new Label(readerUi, SWT.NONE);
		GridData gdCardPresence = new GridData();
		gdCardPresence.widthHint = 75;
		lblCardPresence.setLayoutData(gdCardPresence);
		lblCardPresence.setText("Card\nunknown");
		lblCardPresence.setAlignment(SWT.CENTER);

		
		Composite readerComposite = new Composite(readerUi, SWT.FILL);
		GridData gdReader = new GridData();
		gdReader.horizontalSpan=2;
		gdReader.horizontalAlignment=SWT.FILL;
		readerComposite.setLayoutData(gdReader);
		
		switch (currentReaderType) {
		case BASIC:
			addBasicListeners(connector);
			createBasicReader(readerComposite);
			autologin = false;
			break;
		case STANDARD:
			addBasicListeners(connector);
			addStandardListeners(connector);
			createStandardReader(readerComposite);
			viewer.getTable().setSelection(selectedIndex);
			if(selectedIndex != -1)
			{
				checkAutoLogin.setEnabled(true);
			}
			break;
		default:
			break;
		}
		
		if (comm != null) {
			currentComm = comm;
		}
		
		if (currentComm == null) {
			currentComm = getDefaultComm();
		} else {
			currentComm.reset();
		}
		
		readerUi.pack();
		
		try {
			connector.connect(currentComm);
			type = currentReaderType;
			lblConnection.setText("Card connection type\n" + currentComm.getUserString());
			PersoSimPreferenceManager.storePreference(LAST_READER_TYPE, type.name());
			PersoSimPreferenceManager.storePreference(LAST_COMM_TYPE, currentComm.getName());
			root.layout();
			root.redraw();
		} catch (IOException | IllegalStateException e) {
			BasicLogger.logException(getClass(), "Creation of the ifd connector failed using " + currentComm.getName(), e, LogLevel.WARN);
			MessageDialog.openWarning(Display.getCurrent().getActiveShell(), "Connector could not be created", "The IFD connector using " + currentComm.getName() + " could not be created, please choose another ifd type.");
		}

		root.pack();
		root.layout();
		root.redraw();

		root.getShell().setMinimumSize(root.getSize().x+10,root.getSize().y+100);

		//root.getShell().setMinimumSize(root.getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT, true));
		
		
	}
	
	private ReaderType getDefaultReaderType() {
		return ReaderType.STANDARD;
	}

	private IfdConnector getConnector() throws IOException{
		DriverConnectorFactory factory = de.persosim.driver.connector.Activator.getFactory();
		return factory.getConnector(de.persosim.driver.connector.Activator.PERSOSIM_CONNECTOR_CONTEXT_ID);
	}
	
	private void disposeConnector(){
		DriverConnectorFactory factory = de.persosim.driver.connector.Activator.getFactory();
		try {
			factory.returnConnector(getConnector());
		} catch (IOException e) {
			BasicLogger.logException(this.getClass(), e);
		}
	}
	
	/**
	 * Reset all reader-related elements to default values and states. Any
	 * connection to a reader is lost and reader type needs to be set before
	 * attempting a new connection.
	 */
	public void resetReader() {
		disposeReaderControls();
		type = ReaderType.NONE;
	}

	public void restartReader() {
		switchReaderType(lastReaderType, null);
	}

	@Override
	public void iccPresence(boolean iccPresent) {
		if (lblCardPresence == null) return;
		
		lblCardPresence.getDisplay().asyncExec(new Runnable() {
			
			@Override
			public void run() {
				if (lblCardPresence.isDisposed()) return;
				Color green = new Color(lblCardPresence.getDisplay(),   0, 255, 0);
				Color red   = new Color(lblCardPresence.getDisplay(), 255,   0, 0);

				lblCardPresence.setText(iccPresent ? "card\npresent": "card\nabsent");
				lblCardPresence.setBackground(iccPresent ? green: red);				
			}
		});

	}

	@Override
	public void displayChat(byte[] chat) {
		if (lblAccessRights != null) {
			Display.getDefault().syncExec(new Runnable() {

				@Override
				public void run() {
					String terminalType = "";
					String accessRights = "";
					String tooltipText = "no description available";
					if (chat != null && chat.length > 13) {
						terminalType = "Terminaltype: ";
						switch (chat[13]) {
						case 1:
							terminalType += "IS";
							break;
						case 2:
							terminalType += "AT";
							tooltipText = getToolTipAt(chat);
							break;
						case 3:
							terminalType += "ST";
							break;

						default:
							terminalType += "unknown";
							break;
						}
						accessRights = "Requested accessrights: " + HexString.encode(chat).substring(32);
					}
					lblAccessRights.setText(terminalType+" "+accessRights);
					lblAccessRights.setToolTipText(tooltipText);
				}
			});
		}
	}

	private String getToolTipAt(byte[] chat) {
		String terminalType = "Authentication Terminal";
		
		byte[] auth = Arrays.copyOfRange(chat, 16, chat.length);
//		BitField authBits = new BitField(auth);
		BitField authBits = BitField.buildFromBigEndian(auth);
		
		String role = "unknown";
		int roleByte = auth[0] & 0xC0;
		switch (roleByte) {
			case 0xC0:
				role = "CVCA";
				break;
				
			case 0x80:
				role = "DV (official domestic)";
				break;
				
			case 0x40:
				role = "DV (non-official / foreign)";
				break;

			default:
				role = "AT";
		}
		
		String accessRights = "";
		if (authBits.getBit(0)) {
			accessRights += "\n - Age Verification";
		}
		if (authBits.getBit(1)) {
			accessRights += "\n - Community ID Verification";
		}
		if (authBits.getBit(2)) {
			accessRights += "\n - Restricted Identification";
		}
		if (authBits.getBit(3)) {
			accessRights += "\n - Privileged Terminal";
		}
		if (authBits.getBit(4)) {
			accessRights += "\n - CAN allowed";
		}
		if (authBits.getBit(5)) {
			accessRights += "\n - PIN Management";
		}
		if (authBits.getBit(6)) {
			accessRights += "\n - Install Certificate";
		}
		if (authBits.getBit(7)) {
			accessRights += "\n - Install Qualified Certificate";
		}

		if (authBits.getBit(8)) {
			accessRights += "\n - Read Access(eID) DG1";
		}	
		if (authBits.getBit(9)) {
			accessRights += "\n - Read Access(eID) DG2";
		}	
		if (authBits.getBit(10)) {
			accessRights += "\n - Read Access(eID) DG3";
		}	
		if (authBits.getBit(11)) {
			accessRights += "\n - Read Access(eID) DG4";
		}	
		if (authBits.getBit(12)) {
			accessRights += "\n - Read Access(eID) DG5";
		}	
		if (authBits.getBit(13)) {
			accessRights += "\n - Read Access(eID) DG6";
		}	
		if (authBits.getBit(14)) {
			accessRights += "\n - Read Access(eID) DG7";
		}	
		if (authBits.getBit(15)) {
			accessRights += "\n - Read Access(eID) DG8";
		}	
		if (authBits.getBit(16)) {
			accessRights += "\n - Read Access(eID) DG9";
		}	
		if (authBits.getBit(17)) {
			accessRights += "\n - Read Access(eID) DG10";
		}	
		if (authBits.getBit(18)) {
			accessRights += "\n - Read Access(eID) DG11";
		}	
		if (authBits.getBit(19)) {
			accessRights += "\n - Read Access(eID) DG12";
		}	
		if (authBits.getBit(20)) {
			accessRights += "\n - Read Access(eID) DG13";
		}	
		if (authBits.getBit(21)) {
			accessRights += "\n - Read Access(eID) DG14";
		}	
		if (authBits.getBit(22)) {
			accessRights += "\n - Read Access(eID) DG15";
		}	
		if (authBits.getBit(23)) {
			accessRights += "\n - Read Access(eID) DG16";
		}	
		if (authBits.getBit(24)) {
			accessRights += "\n - Read Access(eID) DG17";
		}	
		if (authBits.getBit(25)) {
			accessRights += "\n - Read Access(eID) DG18";
		}	
		if (authBits.getBit(26)) {
			accessRights += "\n - Read Access(eID) DG19";
		}	
		if (authBits.getBit(27)) {
			accessRights += "\n - Read Access(eID) DG20";
		}		
		if (authBits.getBit(28)) {
			accessRights += "\n - Read Access(eID) DG21";
		}	
		if (authBits.getBit(29)) {
			accessRights += "\n - Read Access(eID) DG22";
		}	
		

		if (authBits.getBit(32)) {
			accessRights += "\n - Write Access(eID) DG22";
		}
		if (authBits.getBit(33)) {
			accessRights += "\n - Write Access(eID) DG21";
		}
		if (authBits.getBit(34)) {
			accessRights += "\n - Write Access(eID) DG20";
		}
		if (authBits.getBit(35)) {
			accessRights += "\n - Write Access(eID) DG19";
		}
		if (authBits.getBit(36)) {
			accessRights += "\n - Write Access(eID) DG18";
		}
		if (authBits.getBit(37)) {
			accessRights += "\n - Write Access(eID) DG17";
		}

		
		if (accessRights.length()<1) {
			accessRights = "\n - none";
		}

		String retVal = "Type: "+terminalType;
		retVal += "\nRole: "+role;
		retVal += "\nAccess Rights:"+accessRights;
		return retVal;
	}
}
