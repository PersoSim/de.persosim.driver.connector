package de.persosim.driver.connector.ui.parts;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

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

	public static final int KEYS_ALL_PINSAVER = -4;
	public static final int KEYS_ALL_CONTROL = -3;
	public static final int KEYS_ALL_NUMERIC = -2;
	public static final int KEYS_ALL = -1;

	private Text txtOutput;
	private String[] savedPins = new String[4];
	
	// regular Expression to remove non digits from PINS
	private String pinRegex = "\\D";

	private Button[] keysNumeric;
	private Button[] keysControl;
	private Button[] keysPinSaver;

	private List<String> pressedKeys = new ArrayList<>();
	private NativeDriverConnector connector;

	private Composite root;
	private Composite basicReaderControls;
	private Composite standardReaderControls;

	private Preferences prefsUi = InstanceScope.INSTANCE
			.getNode("de.persosim.driver.connector.ui");
	private Preferences nodePin = prefsUi.node("nodePin");

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
		
		Composite rightControlComposite = new Composite(controlComposite,SWT.NONE);
		rightControlComposite.setLayout(new GridLayout(1, false));

		gridData = new GridData();
		gridData.verticalAlignment = SWT.BEGINNING;
		controlComposite.setLayoutData(gridData);

		// left control composite
		keysControl = new Button[3];
		keysControl[0] = getCancelKey(leftControlComposite);
		keysControl[1] = getCorrectionKey(leftControlComposite);
		keysControl[2] = getConfirmationKey(leftControlComposite);

		button = createButton(leftControlComposite, "", null, 150, 100);
		button.setEnabled(DISABLE);

		// right control composite
		keysPinSaver = new Button[4];
		for (int i = 0; i < keysPinSaver.length; i++) {
			keysPinSaver[i] = getCustomPinSaverKey(rightControlComposite, i);
			keysPinSaver[i].setFont(new Font(parent.getDisplay(), FONT_NAME, 30, SWT.BOLD));
		}

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

	/**
	 * Returns a button that simulates the button clicks for a custom Pin. To
	 * save with a right click on the button a pin has to be visible on the
	 * screen.
	 * 
	 * @param parent composite where the customPinSaver button will be placed
	 * @param number of the pin saver button
	 * @return button
	 */
	private Button getCustomPinSaverKey(Composite parent, final int number) {
		
		//key to identify the pin in the preferences for button x
		final String key = "b" + number;
		final String defaultPin = "";
		
		savedPins[number] = nodePin.get(key, defaultPin);
		
		// remove all non digits with a regex
		savedPins[number] = savedPins[number].replaceAll(pinRegex, "");

		SelectionListener selectionListener = new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent event) {
				widgetDefaultSelected(event);

			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {

				for (int i = 0; i < savedPins[number].length(); i++) {
					keysNumeric[Integer.parseInt(""
							+ savedPins[number].charAt(i))].notifyListeners(
							SWT.Selection, null);
				}

			}

		};

		Button button;
		
		if (checkExistenceOfKey(key)){
			//Show the saved pin on the button
			button = createButton(parent, savedPins[number], selectionListener,
					150, 100);
		} else {
			//This is a default button that has no connection to a saved pin
			button = createButton(parent, "PIN " + (number + 1),
					selectionListener, 150, 100);
		}

		// add right click menu
		Menu popupPinSaver = new Menu(button);

		// add a menu entry for saving a displayed pin
		MenuItem newPinSaverMenuItem = new MenuItem(popupPinSaver, SWT.CASCADE);
		newPinSaverMenuItem.setText("Change saved PIN");

		// Listener for new Pin
		SelectionListener listenerNewPinMenu = new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) { 
				try {
					// get pin from display and remove everything else
					String pin = txtOutput.getText().replaceAll(pinRegex, "");
					
					if (pin.equals("")) {
						
						MessageDialog.openInformation(root.getShell(),
								"No PIN entered",
								"Please enter a PIN before saving");
					} else {

						savedPins[number] = pin;
						// save in prefs
						nodePin.put(key, pin);
						prefsUi.flush();

						// rename Button
						keysPinSaver[number].setText(savedPins[number]);
					}

				}catch (BackingStoreException e1) {
					
					/*
					 * Nothing to do here. This Exception only exists because
					 * Eclipse wants it. I'm not able to produce an error with
					 * the Preferences file. Eclipse seems to solve most
					 * Problems with it independently.
					 */
				} 
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		};

		newPinSaverMenuItem.addSelectionListener(listenerNewPinMenu);

		// add a menu entry for resetting a pin button
		MenuItem resetButtonMenuItem = new MenuItem(popupPinSaver, SWT.CASCADE);
		resetButtonMenuItem.setText("Reset");

		// Listener for resetting a Button
		SelectionListener listenerResetButton = new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				try {
					
					//put the default value at the position of the old key
					savedPins[number] = defaultPin;

					// remove key from prefs
					nodePin.remove("b" + number);
					prefsUi.flush();

					// rename Button
					keysPinSaver[number].setText("PIN " + (number+1));
				} catch (BackingStoreException e1) {
					/*
					 * Nothing to do here. This Exception only exists because
					 * Eclipse wants it. I'm not able to produce an error with
					 * the Preferences file. Eclipse seems to solve most
					 * Problems with it independently.
					 */
				}

			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		};
		resetButtonMenuItem.addSelectionListener(listenerResetButton);
		button.setMenu(popupPinSaver);
		
		button.setToolTipText("How to use:\n"
				+ "1. Activate the PIN pad\n"
				+ "2. Enter a PIN with it\n"
				+ "3. Right click on the button and save");
		return button;

	}
	
	/**
	 * checks the existence of a key in the preferences. A key is created when a
	 * pin is saved in {@link #getCustomPinSaverKey(Composite, int)}. It can be
	 * removed by resetting a pin saver button.
	 * 
	 * @param the key to check
	 * @return true/false
	 */
	public boolean checkExistenceOfKey(String key) {
		try {
			String[] keylist = nodePin.keys();
			for (int i = 0; i < keylist.length; i++) {
				if (keylist[i].equals(key)){
					return true;
				}					
			}
			return false;
		} catch (BackingStoreException e) {
			/*
			 * Nothing to do here. This Exception only exists because
			 * Eclipse wants it. I'm not able to produce an error with
			 * the Preferences file. Eclipse seems to solve most
			 * Problems with it independently.
			 */
		}
		return false;
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
		case KEYS_ALL_PINSAVER:
			setEnabledKeySet(keysPinSaver, enabled);
			break;
		case KEYS_ALL:
			setEnabledKeySetController(KEYS_ALL_NUMERIC, enabled);
			setEnabledKeySetController(KEYS_ALL_CONTROL, enabled);
			setEnabledKeySetController(KEYS_ALL_PINSAVER, enabled);
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
		if (type.equals(ReaderType.STANDARD)) {
			setEnabledKeySetController(KEYS_ALL, true);
			pressedKeys.clear();
			synchronized (pressedKeys) {
				while (pressedKeys.size() == 0
						|| (!pressedKeys.get(pressedKeys.size() - 1).equals(
								"OK") && !pressedKeys.get(
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
					byte currentNumber = (byte) pressedKeys.get(i).charAt(0);
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
				break;
			case STANDARD:
				addStandardListeners(connector);
				createStandardReader(root);
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
