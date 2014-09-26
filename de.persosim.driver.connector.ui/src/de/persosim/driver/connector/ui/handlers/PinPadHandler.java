package de.persosim.driver.connector.ui.handlers;

import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimmedWindow;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.eclipse.swt.widgets.Shell;

/**
 * This class implements the handler for the presentation and toggling the presence of the PinPad part.
 * 
 * @author slutters
 *
 */
public class PinPadHandler {
	
	@Inject private MApplication application;
	@Inject private EModelService modelService;
	@Inject private EPartService partService;
	
	@Execute
	public void execute(Shell shell) {
		
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
			
		} else{
			
			mainWindowPart.setWidth(width -= 500);
			
			pinPadPart.setVisible(false);
			pinPadPart.setToBeRendered(false);
			
		}
		
	}
	
}
