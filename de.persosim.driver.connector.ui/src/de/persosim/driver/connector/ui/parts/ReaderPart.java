package de.persosim.driver.connector.ui.parts;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
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
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

import de.persosim.driver.connector.NativeDriverConnector;
import de.persosim.driver.connector.UnsignedInteger;
import de.persosim.driver.connector.VirtualReaderUi;
import de.persosim.driver.connector.features.MctReaderDirect;
import de.persosim.driver.connector.features.MctUniversal;
import de.persosim.driver.connector.features.ModifyPinDirect;
import de.persosim.driver.connector.features.PersoSimPcscProcessor;
import de.persosim.driver.connector.features.VerifyPinDirect;

/**
 * This class defines the appearance and behavior of the PinPad GUI to be used
 * in connection with the respective simulated card reader.
 * 
 * @author slutters
 *
 */
public class ReaderPart implements VirtualReaderUi {

	public enum ReaderType {
		STANDARD, BASIC, NONE;
	}

	private static final String FONT_NAME = "Helvetica";

	public static final boolean ENABLE = true;
	public static final boolean DISABLE = false;
	public static boolean AUTOLOGIN = false;
	public static byte[] autologinpassword = null;

	public static final int KEYS_ALL_PINSAVER = -4;
	public static final int KEYS_ALL_CONTROL = -3;
	public static final int KEYS_ALL_NUMERIC = -2;
	public static final int KEYS_ALL = -1;

	private MenuItem removeTableItem;
	private MenuItem editTableItem;
	private boolean sortPwdTable = false;

	private Text txtOutput;
	private Button[] keysNumeric;
	private Button[] keysControl;
	private TableViewer viewer;
	private Table tbl;
	private Button checkAL;
	private boolean flag;
	private IStructuredSelection selectedRow;
	private int selectedIndex = -1;

	

	public static TableViewerColumn columnPin;
	private List<String> pressedKeys = new ArrayList<>();
	private NativeDriverConnector connector;

	private Composite root;
	private Composite basicReaderControls;
	private Composite standardReaderControls;

	private ReaderType type = ReaderType.NONE;



	/**
	 * Defines the virtual basic reader. It has no own input interface.
	 * 
	 * @param parent composite where the reader will be placed
	 */
	
	private void createBasicReader(Composite parent) {
		disposeReaders();

		basicReaderControls = new Composite(parent, SWT.NONE);

		GridData gridData;
		basicReaderControls.setLayout(new GridLayout(1, false));

		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;

		basicReaderControls.setLayoutData(gridData);

		Text txtOutput = new Text(basicReaderControls, SWT.READ_ONLY
				| SWT.CENTER);
		txtOutput.setFont(new Font(basicReaderControls.getDisplay(), FONT_NAME,
				24, SWT.BOLD));
		txtOutput.setText("Basic Reader");
		txtOutput.setEditable(false);
		txtOutput.setCursor(null);

		parent.layout();
		parent.redraw();
	}

	private void disposeReaders() {
		if (basicReaderControls != null)
			basicReaderControls.dispose();
		if (standardReaderControls != null)
			standardReaderControls.dispose();
	}

	/**
	 * Defines the layout of the virtual standard reader. Unlike the basic
	 * reader it contains a keypad.
	 * 
	 * @param parent composite where the reader will be placed
	 */
	private void createStandardReader(Composite parent) {
		disposeReaders();
		standardReaderControls = new Composite(parent, SWT.NONE);

		GridData gridData;
		standardReaderControls.setLayout(new GridLayout(1, false));

		Composite pinpadComposite = new Composite(standardReaderControls,
				SWT.NONE);
		pinpadComposite.setLayout(new GridLayout(1, false));

		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		pinpadComposite.setLayoutData(gridData);

		txtOutput = new Text(pinpadComposite, SWT.READ_ONLY | SWT.PASSWORD
				| SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.LEFT);
		txtOutput.setFont(new Font(pinpadComposite.getDisplay(), FONT_NAME, 24,
				SWT.BOLD));
		txtOutput.setEditable(false);
		txtOutput.setCursor(null);

		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.heightHint = 80;
		txtOutput.setLayoutData(gridData);

		Composite keyComposite = new Composite(pinpadComposite, SWT.NONE);
		keyComposite.setLayout(new GridLayout(2, false));

		Composite numericComposite = new Composite(keyComposite, SWT.NONE);
		numericComposite.setLayout(new GridLayout(3, false));

		keysNumeric = new Button[10];
		for (int i = 1; i < 10; i++) {
			keysNumeric[i] = getNumericKey(numericComposite, i);
		}

		Button button;

		button = createButton(numericComposite, "", null, 100, 100);
		button.setEnabled(DISABLE);

		keysNumeric[0] = getNumericKey(numericComposite, 0);

		button = createButton(numericComposite, "", null, 100, 100);
		button.setEnabled(DISABLE);
		

		Composite controlComposite = new Composite(keyComposite, SWT.NONE);
		controlComposite.setLayout(new GridLayout(2, false));

		Composite leftControlComposite = new Composite(controlComposite, SWT.NONE);
		leftControlComposite.setLayout(new GridLayout(1, false));
		
		final Composite rightinsideControlComposite = new Composite(controlComposite,SWT.NONE | SWT.TOP);
		rightinsideControlComposite.setLayout(new GridLayout(1, true));
	

		final Composite rightControlComposite = new Composite(rightinsideControlComposite,SWT.NONE);
		
		
		final GridData grid1 = new GridData(SWT.FILL, SWT.FILL, true, true,1,1);
		grid1.heightHint = 390;
		grid1.widthHint = 155;
		rightControlComposite.setLayoutData(grid1);
		rightControlComposite.setLayout(new FillLayout());
		
		

		gridData = new GridData();
		gridData.verticalAlignment = SWT.FILL;
		gridData.horizontalAlignment = SWT.BEGINNING;
		controlComposite.setLayoutData(gridData);
		
		final Composite tableControlComposite = new Composite(rightinsideControlComposite,SWT.NONE);
		tableControlComposite.setLayout(new GridLayout(1, false));
		
		

		// left control composite
		keysControl = new Button[3];
		keysControl[0] = getCancelKey(leftControlComposite);
		keysControl[1] = getCorrectionKey(leftControlComposite);
		keysControl[2] = getConfirmationKey(leftControlComposite);
		
		
		button = createButton(leftControlComposite, "", null, 150, 100);
		button.setEnabled(DISABLE);
		
		checkAL = new Button(tableControlComposite, SWT.CHECK);
		checkAL.setText("AutoLogin "+ "                       ");

		checkAL.setEnabled(false);

		// define the TableViewer
		viewer = new TableViewer(rightControlComposite, SWT.H_SCROLL
				| SWT.V_SCROLL | SWT.BORDER);

		tbl = viewer.getTable();
		tbl.setHeaderVisible(true);
		tbl.setLinesVisible(true);

		
		SelectionListener columnPinHeaderListener = new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				Collections.sort(PinModelProvider.INSTANCE.pins,
						new PasswordComparatorClass(sortPwdTable, "Password"));

				if (sortPwdTable)
					sortPwdTable = false;
				else
					sortPwdTable = true;

				viewer.refresh();

			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {

			}
		};
		

		columnPin = new TableViewerColumn(viewer, SWT.NONE);
		columnPin.getColumn().setWidth(155);
		columnPin.getColumn().setText("Password Management");
		columnPin.getColumn().setResizable(false);
		columnPin.getColumn().addSelectionListener(columnPinHeaderListener);
		

		columnPin.setLabelProvider(new ColumnLabelProvider() {
		      @Override
		      public String getText(Object element) {
		        Pin p = (Pin) element;
		        return p.getPassword();
		      }
		    });
		

		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setInput(PinModelProvider.INSTANCE.getPins());
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

	       
		SelectionListener addNewPinListener = new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				String inputvalue = "";
				boolean passcontrol = true;

				do {
					InputDialog messageBox = new InputDialog(root.getShell(),
							"Password", "Enter your Password", inputvalue, null);

					if (messageBox.open() == org.eclipse.jface.window.Window.OK) {

						String cleanStr;

						if (messageBox.getValue() != null) {
							flag = messageBox.getValue().matches(".*[a-zA-Z]+.*");
							inputvalue = messageBox.getValue();
							cleanStr = messageBox.getValue().replaceAll(" ", "");
							cleanStr = cleanStr.replaceAll("\\D+", "");

						} else {
							cleanStr = "";
							break;

						}

						if (cleanStr.length() > 0) {

							if (flag == false) {

								if (!PinModelProvider.INSTANCE.checkExistence(cleanStr)) {

									Pin pin = new Pin(cleanStr);
									PinModelProvider.INSTANCE.save(pin, "save", "-1");
									viewer.refresh();
									int temp = PinModelProvider.INSTANCE.pins.size() - 1;
									viewer.setSelection(new StructuredSelection(viewer.getElementAt(temp)), true);

									passcontrol = false;
									break;

								} else {

									MessageDialog dialog = new MessageDialog(root.getShell(), "Warnung", null,
									"This Password is already in the list.", MessageDialog.INFORMATION, new String[] { "OK" }, 0);
									dialog.open();

								}
							} else {

								MessageDialog dialog = new MessageDialog(root.getShell(), "Warnung", null,
								"Warning!. Your input includes letters.", MessageDialog.INFORMATION, new String[] { "OK" }, 0);
								dialog.open();

							}
						} else {

							MessageDialog dialog = new MessageDialog(root.getShell(), "Warnung", null,
							"Please enter a valid password!.", MessageDialog.INFORMATION, new String[] { "OK" }, 0);
							dialog.open();

						}

					} else {
						break;
					}

				} while (passcontrol == true);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {

			}
		};
		
		addTableItem.addSelectionListener(addNewPinListener);
		
		
		
		/**
		 * This is the listener for "Remove Password" in the context menu of the list. The
		 * listener removes the Password from the list and preferences.
		 */
		
		SelectionListener removePinListener = new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {


				int index = viewer.getTable().getSelectionIndex();
				
				PinModelProvider.INSTANCE.pins.remove(index);
				PinModelProvider.INSTANCE.deletePinsFromPrefs(index);

				viewer.refresh();
				
				int listempty = PinModelProvider.INSTANCE.pins.size();
				if(listempty == 0)
				{
					checkAL.setEnabled(false);
				}
				removeTableItem.setEnabled(false);
				editTableItem.setEnabled(false);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {

			}
		};
		
		removeTableItem.addSelectionListener(removePinListener);
		
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
		
		SelectionListener editPinListener = new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				int index = viewer.getTable().getSelectionIndex();
				Pin p = (Pin) viewer.getElementAt(index);

				boolean passcontrol = true;

				do {

					InputDialog messageBox = new InputDialog(root.getShell(),
							"Password", "Enter your Password", p.getPassword(),
							null);

					if (messageBox.open() == org.eclipse.jface.window.Window.OK) {

						String cleanStr;

						if (messageBox.getValue() != null) {
							flag = messageBox.getValue().matches(".*[a-zA-Z]+.*");
							cleanStr = messageBox.getValue().replaceAll(" ", "");
							cleanStr = cleanStr.replaceAll("\\D+", "");

						} else {
							cleanStr = "";
							break;
						}

						if (cleanStr.length() > 0) {
							if (flag == false) {
								if (!PinModelProvider.INSTANCE.checkExistence(cleanStr)) {

									index = viewer.getTable().getSelectionIndex();
									p = PinModelProvider.INSTANCE.pins.get(index);

									p.setPassword(cleanStr);
									PinModelProvider.INSTANCE.save(p, "edit", index + "");
									viewer.refresh();
									passcontrol = false;
									break;

								} else {

									MessageDialog dialog = new MessageDialog(root.getShell(), "Warnung", null,
									"This Password is already in the list.", MessageDialog.INFORMATION, new String[] { "OK" }, 0);
									dialog.open();
								}
							} else {

								MessageDialog dialog = new MessageDialog(root.getShell(), "Warnung", null,
								"Warning!. Your input includes letters.", MessageDialog.INFORMATION, new String[] { "OK" }, 0);
								dialog.open();
							}

						} else {

							MessageDialog dialog = new MessageDialog(root.getShell(), "Warnung", null,
							"Please enter a valid password!.", MessageDialog.INFORMATION, new String[] { "OK" }, 0);
							dialog.open();
						}

					} else {
						break;
					}

				} while (passcontrol == true);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {

			}
		};
		
		editTableItem.addSelectionListener(editPinListener);
		
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

				checkAL.setEnabled(true);

			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {

			}
		};
		
		tbl.addSelectionListener(oneClickListener);
		
		/**
		 * This is the listener for the AutoLogin function. When checked, the
		 * selected password from the list will be used over and over again,
		 * when a request comes from outside. Also the list with passwords is
		 * disabled and cant be changed until unchecking the "AutoLogin" button.
		 * Next to the AutoLogin button u can see the password, which u
		 * selected, when checked.
		 */

		SelectionListener checkListener = new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {

				checkAL.setText("AutoLogin: " + viewer.getSelection().toString());

				if (checkAL.getSelection()) {

					tbl.setEnabled(false);
					AUTOLOGIN = true;
					pressedKeys.clear();
					char[] entries = viewer.getSelection().toString().replaceAll("\\D+", "").toCharArray();
					for (int i = 0; i < entries.length; i++) {
						char entry = entries[i];
						setButton(String.valueOf(entry));

					}

					setButton("OK");

				}

				else {

					tbl.setEnabled(true);
					AUTOLOGIN = false;
					txtOutput.setText("");
					checkAL.setText("AutoLogin");
				}

			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {

			}

		};

		checkAL.addSelectionListener(checkListener);
		
		
		tbl.setVisible(true);
		checkAL.setVisible(true);
		setEnabledKeySetController(KEYS_ALL, false);
		parent.layout();
		parent.redraw();
	}

	@PostConstruct
	public void createComposite(Composite parent) {
		root = parent;
		switchToReaderType(ReaderType.STANDARD);
	}
	
	
	
	 

	/**
	 * This method defines the Buttons all getxxxKey-methods use it for creating
	 * buttons.
	 * 
	 * @param parent composite where the button will be placed
	 * @param text displayed on the button
	 * @param selectionListener
	 * @param width of the button
	 * @param height of the button
	 * @return button
	 */
	private Button createButton(Composite parent, String text,
			SelectionListener selectionListener, int width, int height) {
		final Button button = new Button(parent, SWT.PUSH);
		button.setText(text);
		button.setFont(new Font(parent.getDisplay(), FONT_NAME, 36, SWT.BOLD));

		if (selectionListener != null) {
			button.addSelectionListener(selectionListener);
		}

		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.widthHint = width;
		gridData.heightHint = height;

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
				System.out.println("pin pad button pressed: " + text);
				txtOutput.append(text);
				setButton(text);
			}
		};

		return createButton(parent, String.valueOf(number), selectionListener,
				100, 100);
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

		Button button = createButton(parent, text, selectionListener, 150, 100);
		button.setBackground(new Color(parent.getDisplay(), 255, 0, 0));

		return button;
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

		Button button = createButton(parent, text, selectionListener, 150, 100);
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

		Button button = createButton(parent, text, selectionListener, 150, 100);
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
		if (AUTOLOGIN == false) {
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
					throw new IOException(
							"PIN containing non valid characters entered");
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
		// TODO Auto-generated method stub
		return null;
	}

	private void addStandardListeners(NativeDriverConnector connector) {
		connector.addListener(new VerifyPinDirect(new UnsignedInteger(0x3136C8)));
		connector.addListener(new ModifyPinDirect(new UnsignedInteger(0x3136CC)));
		connector.addListener(new MctReaderDirect(new UnsignedInteger(0x3136D0)));
		connector.addListener(new MctUniversal(new UnsignedInteger(0x3136D4)));
		connector.addListener(new PersoSimPcscProcessor(new UnsignedInteger(0x313730)));
	}
	
	/**
	 * Switch the parts user interface and behavior to the reader type
	 * associated with the provided parameter.
	 * 
	 * @param readerType the reader type to use
	 */
	public void switchToReaderType(ReaderType readerType) {
		if ((connector != null) && (connector.isRunning())) {
			try {
				connector.disconnect();
			} catch (IOException | InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		try {
			connector = new NativeDriverConnector("localhost", 5678,
					"localhost", 9876);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			connector.connect();

			connector.addUi(this);

			switch (readerType) {
			case BASIC:
				createBasicReader(root);
				AUTOLOGIN = false;
				
				break;
			case STANDARD:
				
				addStandardListeners(connector);
				createStandardReader(root);
				viewer.getTable().setSelection(selectedIndex);
				if(selectedIndex != -1)
				{
					checkAL.setEnabled(true);
				}
				break;
				
			default:
				;
				break;
			}

			type = readerType;
		} catch (IOException e) {
			disposeReaders();
			MessageDialog
					.openWarning(
							root.getShell(),
							"Warning",
							"Failed to connect to virtual card reader driver!\nTry to restart driver, then re-connect by selecting\ndesired reader type from menu \"Reader Type\".");
		}

	}

	/**
	 * Switch the parts user interface and behavior to the off state.
	 */
	public void disconnectReader() {
		if ((connector != null) && (connector.isRunning())) {

			try {
				connector.disconnect();
			} catch (IOException | InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		disposeReaders();
		type = ReaderType.NONE;

	}
}
