 
package de.persosim.driver.connector.ui.handlers;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.menu.MItem;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.globaltester.logging.BasicLogger;
import org.globaltester.logging.tags.LogLevel;

import de.persosim.driver.connector.VirtualDriverComm;
import de.persosim.driver.connector.exceptions.IfdCreationException;
import de.persosim.driver.connector.ui.parts.ReaderPart;

public class VirtualDriverHandler {
	
	@CanExecute
	public boolean checkState(final MPart mPart, final MItem mItem) {
		if (mPart.getObject() instanceof ReaderPart) {
			ReaderPart readerPartObject = (ReaderPart) mPart.getObject();
			mItem.setSelected(readerPartObject.getCurrentCommType() == VirtualDriverComm.NAME);
		}
		return true;
	}
	
	@Execute
	public void execute(final MPart mPart, final MItem mItem) {
		BasicLogger.log(this.getClass(), "Switch to use virtual driver", LogLevel.INFO);
		
		if (mPart.getObject() instanceof ReaderPart) {
			ReaderPart readerPartObject = (ReaderPart) mPart.getObject();

			try {
				readerPartObject.switchReaderType(new VirtualDriverComm(VirtualDriverComm.DEFAULT_HOST, VirtualDriverComm.DEFAULT_PORT));
			} catch (IfdCreationException e) {
				BasicLogger.logException(this.getClass(), e, LogLevel.WARN);
				MessageDialog.openWarning(Display.getCurrent().getActiveShell(), "Connector could not be created", "The virtual driver communication could not be established.");
			}
		}
	}
		
}