package de.persosim.driver.connector.ui.parts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimmedWindow;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import de.persosim.driver.connector.VirtualReaderUi;

/**
 * This class defines the appearance and behavior of the PinPad GUI to be used
 * in connection with the respective simulated card reader.
 * 
 * @author slutters
 *
 */
public class PinPad implements VirtualReaderUi{
	
	private static final String FONT_NAME = "Helvetica";
	
	public static final boolean ENABLE = true;
	public static final boolean DISABLE = false;
	
	public static final int KEYS_ALL_CONTROL = -3;
	public static final int KEYS_ALL_NUMERIC = -2;
	public static final int KEYS_ALL         = -1;
	
	public static final String message_001_de = "Bitte Karte" + System.lineSeparator() + "bereitstellen";
	public static final String message_002_de = "Bitte Karte" + System.lineSeparator() + "entfernen";
	public static final String message_003_de = "Karte unlesbar." + System.lineSeparator() + "Falsche Lage?";
	public static final String message_004_de = "Bitte Geheimzahl" + System.lineSeparator() + "eingeben";
	public static final String message_005_de = "Aktion" + System.lineSeparator() + "erfolgreich";
	public static final String message_006_de = "Geheimzahl" + System.lineSeparator() + "falsch/gesperrt";
	public static final String message_007_de = "Neue Geheimzahl" + System.lineSeparator() + "eingeben";
	public static final String message_008_de = "Eingabe" + System.lineSeparator() + "wiederholen";
	public static final String message_009_de = "Geheimzahl nicht" + System.lineSeparator() + "gleich. Abbruch";
	public static final String message_010_de = "Bitte Eingabe" + System.lineSeparator() + "best�tigen";
	public static final String message_011_de = "Bitte" + System.lineSeparator() + "Dateneingabe";
	public static final String message_012_de = "Abbruch";
	
	public static final String message_101_de = "PersoSim PinPad" + System.lineSeparator() + "bereit";
	public static final String message_102_de = "Warte auf" + System.lineSeparator() + "Antwort";
	
	private Text txtOutput;
	
	private String currentDefaultMessage;
	
	private Button[] keysNumeric;
	private Button[] keysControl;
	
	private List<String> pressedKeys = new ArrayList<>();

	
	@Inject private MApplication application;
	@Inject private EModelService modelService;
	@Inject private EPartService partService;
	
	@PostConstruct
	public void createComposite(Composite parent) {
		
		GridData gridData;
		
		currentDefaultMessage = message_101_de;
		
		parent.setLayout(new GridLayout(1, false));
		
		
		
		Composite pinpadComposite = new Composite(parent, SWT.NONE);
		pinpadComposite.setLayout(new GridLayout(1, false));
		
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		pinpadComposite.setLayoutData(gridData);
		
		txtOutput = new Text(pinpadComposite, SWT.READ_ONLY | SWT.PASSWORD | SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.LEFT);
		txtOutput.setFont(new Font(pinpadComposite.getDisplay(), FONT_NAME, 24, SWT.BOLD));
		txtOutput.setText(message_101_de);
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
		
		keysControl = new Button[3];
		keysControl[0] = getCancelKey(controlComposite);
		keysControl[1] = getCorrectionKey(controlComposite);
		keysControl[2] = getConfirmationKey(controlComposite);
		
		button = createDefaultButton(controlComposite, "", null, 150, 100);
		button.setEnabled(DISABLE);
//		button.setVisible(false);
		
	}
	
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
	
	private Button getNumericKey(Composite parent, int number) {
		final String text = String.valueOf(number);
		
		SelectionListener selectionListener = new SelectionListener() {

			public void widgetSelected(SelectionEvent event) {
				widgetDefaultSelected(event);
			}

			public void widgetDefaultSelected(SelectionEvent event) {
				System.out.println("pin pad button pressed: " + text);
				txtOutput.append(text);
				setButton(text);
			}
		};
		
		return createDefaultButton(parent, String.valueOf(number), selectionListener, 100, 100);
	}
	
	private Button getCancelKey(Composite parent) {
		final String text = "C";
		
		SelectionListener selectionListener = new SelectionListener() {

			public void widgetSelected(SelectionEvent event) {
				widgetDefaultSelected(event);
			}

			public void widgetDefaultSelected(SelectionEvent event) {
				System.out.println("pin pad button pressed: " + text);
				txtOutput.setText(message_102_de);
				txtOutput.setFocus();
				setEnableKeySet(KEYS_ALL, DISABLE);
				setButton(text);
			}
		};
		
		Button button = createDefaultButton(parent, text, selectionListener, 150, 100);
		button.setBackground(new Color(parent.getDisplay(), 255, 0, 0));
		
		return button;
	}
	
	private Button getCorrectionKey(Composite parent) {
		final String text = "CLR";
		
		SelectionListener selectionListener = new SelectionListener() {

			public void widgetSelected(SelectionEvent event) {
				widgetDefaultSelected(event);
			}

			public void widgetDefaultSelected(SelectionEvent event) {
				System.out.println("pin pad button pressed: " + text);
				txtOutput.setText(currentDefaultMessage);
				setButton(text);
			}
		};
		
		Button button = createDefaultButton(parent, text, selectionListener, 150, 100);
		button.setBackground(new Color(parent.getDisplay(), 255, 255, 0));
		
		return button;
	}
	
	private Button getConfirmationKey(Composite parent) {
		final String text = "OK";
		
		SelectionListener selectionListener = new SelectionListener() {

			public void widgetSelected(SelectionEvent event) {
				widgetDefaultSelected(event);
			}

			public void widgetDefaultSelected(SelectionEvent event) {
				System.out.println("pin pad button pressed: " + text);
				txtOutput.setText(message_102_de);
				setEnableKeySet(KEYS_ALL, DISABLE);
				setButton(text);
			}
		};
		
		Button button = createDefaultButton(parent, text, selectionListener, 150, 100);
		button.setBackground(new Color(parent.getDisplay(), 0, 255, 0));
		
		return button;
	}
	
	public void setText(String text) {
		txtOutput.setText(text);
	}
	
	public void setEnableKeySet(int keySet, boolean enabled) {
		switch (keySet) {
        case KEYS_ALL_NUMERIC:
        	for(Button button : keysNumeric) {
        		button.setEnabled(enabled);
        	}
        	break;
        case KEYS_ALL_CONTROL:
        	for(Button button : keysControl) {
        		button.setEnabled(enabled);
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
	
	private void hideMe(){
		
		// ID of part as defined in fragment.e4xmi application model
		MPart pinPadPart = partService.findPart("de.persosim.driver.connector.ui.parts.pinPad");
		// ID of window as defined in fragment.e4xmi application model
		MTrimmedWindow mainWindowPart = (MTrimmedWindow) modelService.find("de.persosim.rcp.mainWindow", application);
		int width = mainWindowPart.getWidth();
		
		if(pinPadPart.isVisible() || !pinPadPart.isToBeRendered()) {
			mainWindowPart.setWidth(width -= 500);
			
			pinPadPart.setVisible(false);
			pinPadPart.setToBeRendered(false);
			
		}
		
	}
	
	private void setButton(String value){
		pressedKeys.add(value);
		notifyIfReady();
	}
	
	private void notifyIfReady(){
		synchronized(pressedKeys){
			if (pressedKeys.get(pressedKeys.size()-1).equals("OK")){
				pressedKeys.notifyAll();
			}
		}
	}
	
	private void showMe(){
		// ID of part as defined in fragment.e4xmi application model
		MPart pinPadPart = partService.findPart("de.persosim.driver.connector.ui.parts.pinPad");
		// ID of window as defined in fragment.e4xmi application model
		MTrimmedWindow mainWindowPart = (MTrimmedWindow) modelService.find("de.persosim.rcp.mainWindow", application);
		int width = mainWindowPart.getWidth();
		
		if((!pinPadPart.isVisible()) || (!pinPadPart.isToBeRendered())) {
			
			mainWindowPart.setWidth(width += 500);
			
			pinPadPart.setVisible(true);
			pinPadPart.setToBeRendered(true);
			partService.showPart(pinPadPart, PartState.ACTIVATE);
			
		}
	}

	@Override
	public byte[] getPin() throws IOException {
		pressedKeys.clear();
		showMe();
		synchronized (pressedKeys) {
			while(pressedKeys.size() == 0 || !pressedKeys.get(pressedKeys.size()-1).equals("OK")){
				try {
					pressedKeys.wait();
				} catch (InterruptedException e) {
					throw new IOException("The reading of the PIN could not be completed");
				}
			}
		}
		byte[] result = new byte[pressedKeys.size() - 1];
		for (int i = 0; i < result.length; i++) {
			try {
				byte currentNumber = Byte.parseByte(pressedKeys.get(i));
				result[i] = currentNumber;
			} catch (NumberFormatException e) {
				throw new IOException(
						"PIN containing non valid characters entered");
			}
		}
		return result;
	}

	@Override
	public void display(String... lines) {
		showMe();
		StringBuilder text = new StringBuilder();
		for (String line : lines){
			text.append(line);
			text.append(System.lineSeparator());
		}
		setText(text.toString());
	}

	@Override
	public byte[] getDeviceDescriptors() {
		// TODO Auto-generated method stub
		return null;
	}
	
}

