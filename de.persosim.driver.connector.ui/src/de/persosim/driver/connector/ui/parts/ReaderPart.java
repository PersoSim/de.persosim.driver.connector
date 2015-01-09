package de.persosim.driver.connector.ui.parts;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

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
public class ReaderPart implements VirtualReaderUi{

	public enum ReaderType {
		STANDARD, BASIC, NONE;
	}
	
	private static final String FONT_NAME = "Helvetica";
	
	public static final boolean ENABLE = true;
	public static final boolean DISABLE = false;
	
	public static final int KEYS_ALL_CONTROL = -3;
	public static final int KEYS_ALL_NUMERIC = -2;
	public static final int KEYS_ALL         = -1;
	
	private Text txtOutput;
	
	private Button[] keysNumeric;
	private Button[] keysControl;
	
	private List<String> pressedKeys = new ArrayList<>();
	private NativeDriverConnector connector;

	private Composite root;
	private Composite basicReaderControls;
	private Composite standardReaderControls;
	
	private ReaderType type = ReaderType.NONE;
	
	/**
	 * Defines the virtual basic reader. It no own input interface. 
	 * @param parent
	 */
	private void createBasicReader(Composite parent){
		disposeReaders();
		
		basicReaderControls = new Composite(parent, SWT.NONE);
				
		GridData gridData;
		basicReaderControls.setLayout(new GridLayout(1, false));
		
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		
		basicReaderControls.setLayoutData(gridData);
		
		Text txtOutput = new Text(basicReaderControls, SWT.READ_ONLY | SWT.CENTER);
		txtOutput.setFont(new Font(basicReaderControls.getDisplay(), FONT_NAME, 24, SWT.BOLD));
		txtOutput.setText("Basic Reader");
		txtOutput.setEditable(false);
		txtOutput.setCursor(null);
		
		parent.layout();
		parent.redraw();
	}
	
	private void disposeReaders(){
		if (basicReaderControls != null)
		basicReaderControls.dispose();
		if (standardReaderControls != null)
		standardReaderControls.dispose();
	}
	
	/**
	 * Defines the layout of the virtual standard reader. Unlike the basic reader
	 * it contains a keypad.
	 * 
	 * @param parent
	 */
	private void createStandardReader(Composite parent){
		disposeReaders();
		standardReaderControls = new Composite(parent, SWT.NONE);

		GridData gridData;
		standardReaderControls.setLayout(new GridLayout(1, false));
		
		
		
		Composite pinpadComposite = new Composite(standardReaderControls, SWT.NONE);
		pinpadComposite.setLayout(new GridLayout(1, false));
		
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		pinpadComposite.setLayoutData(gridData);
		
		txtOutput = new Text(pinpadComposite, SWT.READ_ONLY | SWT.PASSWORD | SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.LEFT);
		txtOutput.setFont(new Font(pinpadComposite.getDisplay(), FONT_NAME, 24, SWT.BOLD));
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
		for(int i=1; i<10; i++) {keysNumeric[i] = getNumericKey(numericComposite, i);}
		
		Button button;
		
		button = createDefaultButton(numericComposite, "", null, 100, 100);
		button.setEnabled(DISABLE);
//		button.setVisible(false);
		
		keysNumeric[0] = getNumericKey(numericComposite, 0);
		
		button = createDefaultButton(numericComposite, "", null, 100, 100);
		button.setEnabled(DISABLE);
//		button.setVisible(false);
		
		
		
		Composite controlComposite = new Composite(keyComposite, SWT.NONE);
		controlComposite.setLayout(new GridLayout(1, false));
		
		gridData = new GridData();
		gridData.verticalAlignment = SWT.BEGINNING;
		controlComposite.setLayoutData(gridData);
		
		keysControl = new Button[4];
		keysControl[0] = getCancelKey(controlComposite);
		keysControl[1] = getCorrectionKey(controlComposite);
		keysControl[2] = getConfirmationKey(controlComposite);
		keysControl[3] = getSavedPin(controlComposite);
		keysControl[3].setFont(new Font(parent.getDisplay(), FONT_NAME, 32, SWT.BOLD));
		setEnableKeySet(KEYS_ALL, false);
		
		parent.layout();
		parent.redraw();
	}
	
	@PostConstruct
	public void createComposite(Composite parent) {
		root = parent;
		switchToReaderType(ReaderType.STANDARD);
	}
	
	/**
	 * This method defines the Default Buttons
	 * 
	 * @param parent
	 * @param text displayed on the button
	 * @param selectionListener
	 * @param width of the button
	 * @param height of the button
	 * @return button
	 */
	private Button createDefaultButton(Composite parent, String text, SelectionListener selectionListener, int width, int height) {
		final Button button = new Button(parent, SWT.PUSH);
		button.setText(text);
		button.setFont(new Font(parent.getDisplay(), FONT_NAME, 36, SWT.BOLD));
		
		if(selectionListener != null) {
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
	 * @param parent 
	 * @param number (0-9)
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
		
		return createDefaultButton(parent, String.valueOf(number), selectionListener, 100, 100);
	}

	/**
	 * This method returns the cancel button. It is used to cancel the user
	 * input. The virtual keypad will be disabled after pressing.
	 * 
	 * @param parent
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
		
		Button button = createDefaultButton(parent, text, selectionListener, 150, 100);
		button.setBackground(new Color(parent.getDisplay(), 255, 0, 0));
		
		return button;
	}
	
	/**
	 * This method returns the Correction button. It is used to clear the
	 * display. The user can retype the pin.
	 * 
	 * @param parent
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
		
		Button button = createDefaultButton(parent, text, selectionListener, 150, 100);
		button.setBackground(new Color(parent.getDisplay(), 255, 255, 0));
		
		return button;
	}
	
	/**
	 * This method returns the Confirmation button.
	 * It is used to send pins after their input.
	 * 
	 * @param parent
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
		
		Button button = createDefaultButton(parent, text, selectionListener, 150, 100);
		button.setBackground(new Color(parent.getDisplay(), 0, 255, 0));
		
		return button;
	}
	
	/**
	 * This Method returns a button that simulates the button clicks for the
	 * 123456-Pin. Pressing OK is still required.
	 * 
	 * @param parent
	 * @return button
	 */
	private Button getSavedPin(Composite parent){
		final String text = "123456";
	
		SelectionListener selectionListener = new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent event) {
				widgetDefaultSelected(event);
				
			}
	
			@Override
			public void widgetDefaultSelected(SelectionEvent event) {
								
				String savedPin = "123456";
				String[] parts = savedPin.split("");
				for (int i = 0; i < parts.length; i++) {
					keysNumeric[Integer.parseInt(parts[i])].notifyListeners(
							SWT.Selection, null);
				}

			}
			
			
			
		};
			Button button = createDefaultButton(parent, text, selectionListener, 150, 100);
			button.setBackground(new Color(parent.getDisplay(), 0, 0, 255));
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
	
	public void setEnableKeySet(int keySet, final boolean enabled) {
		switch (keySet) {
        case KEYS_ALL_NUMERIC:
        	for(final Button button : keysNumeric) {
        		Display.getDefault().syncExec(new Runnable() {
					
					@Override
					public void run() {
		        		button.setEnabled(enabled);
					}
				});
        	}
        	break;
        case KEYS_ALL_CONTROL:
        	for(final Button button : keysControl) {
        		Display.getDefault().syncExec(new Runnable() {
					
					@Override
					public void run() {
		        		button.setEnabled(enabled);
					}
				});
        	}
        	break;
        case KEYS_ALL:
        	setEnableKeySet(KEYS_ALL_NUMERIC, enabled);
        	setEnableKeySet(KEYS_ALL_CONTROL, enabled);
        	break;
        default:
        	break;
		}
	}
	
	private void setButton(String value){
		pressedKeys.add(value);
		notifyIfReady();
	}
	
	private void notifyIfReady(){
		synchronized(pressedKeys){
			if (pressedKeys.get(pressedKeys.size()-1).equals("OK") || pressedKeys.get(pressedKeys.size()-1).equals("C")){
				pressedKeys.notifyAll();
				setText("");
			}
		}
	}

	@Override
	public byte[] getPin() throws IOException {
		if (type.equals(ReaderType.STANDARD)) {
			setEnableKeySet(KEYS_ALL, true);
			pressedKeys.clear();
			synchronized (pressedKeys) {
				while (pressedKeys.size() == 0
						|| (!pressedKeys.get(pressedKeys.size() - 1)
								.equals("OK") && !pressedKeys.get(pressedKeys.size() - 1).equals("C"))) {
					try {
						pressedKeys.wait();
					} catch (InterruptedException e) {
						throw new IOException(
								"The reading of the PIN could not be completed");
					}
				}
			}
			if (pressedKeys.get(pressedKeys.size() - 1).equals("C")){
				setText("");
				setEnableKeySet(KEYS_ALL, false);
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
			setEnableKeySet(KEYS_ALL, false);
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

	private void addStandardListeners(NativeDriverConnector connector){
		connector.addListener(new VerifyPinDirect(new UnsignedInteger(
				0x3136C8)));
		connector.addListener(new ModifyPinDirect(new UnsignedInteger(
				0x3136CC)));
		connector.addListener(new MctReaderDirect(new UnsignedInteger(
				0x3136D0)));
		connector.addListener(new MctUniversal(new UnsignedInteger(
				0x3136D4)));
		connector.addListener(new PersoSimPcscProcessor(new UnsignedInteger(
				0x313730)));
	}
	
	/**
	 * Switch the parts user interface and behavior to the reader type associated with the provided parameter.
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
			connector = new NativeDriverConnector("localhost", 5678, "localhost", 9876);
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
			MessageDialog.openWarning(root.getShell(), "Warning", "Failed to connect to virtual card reader driver!\nTry to restart driver, then re-connect by selecting\ndesired reader type from menu \"Reader Type\".");
		}

	}
	
	/**
	 * Switch the parts user interface and behavior to the off state.
	 */
	public void disconnectReader(){
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

