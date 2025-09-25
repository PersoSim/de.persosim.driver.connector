
package de.persosim.driver.connector.ui.handlers;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.menu.MItem;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.globaltester.logging.BasicLogger;
import org.globaltester.logging.tags.LogLevel;
import org.globaltester.logging.tags.LogTag;

import de.persosim.driver.connector.VirtualDriverComm;
import de.persosim.driver.connector.exceptions.IfdCreationException;
import de.persosim.driver.connector.ui.parts.ReaderPart;
import jakarta.inject.Inject;
import de.persosim.simulator.log.PersoSimLogTags;

public class VirtualDriverHandler
{
	private EPartService partService;

	@Inject
	public VirtualDriverHandler(EPartService partService)
	{
		this.partService = partService;
	}

	@CanExecute
	public boolean checkState(final MItem mItem)
	{
		// ID of part as defined in fragment.e4xmi application model
		MPart readerPart = partService.findPart("de.persosim.driver.connector.ui.parts.reader");
		if (readerPart.getObject() instanceof ReaderPart mPart) {
			ReaderPart readerPartObject = mPart;
			mItem.setSelected(VirtualDriverComm.NAME.equals(readerPartObject.getCurrentCommType()));
		}
		return true;
	}

	@Execute
	public void execute()
	{
		BasicLogger.log(this.getClass(), "Virtual driver menu entry toggled", LogLevel.INFO);
		// ID of part as defined in fragment.e4xmi application model
		MPart readerPart = partService.findPart("de.persosim.driver.connector.ui.parts.reader");
		if (readerPart.getObject() instanceof ReaderPart mPart) {
			BasicLogger.log("Virtual driver interface selected", LogLevel.INFO, new LogTag(BasicLogger.LOG_TAG_TAG_ID, PersoSimLogTags.VIRTUAL_DRIVER_TAG_ID));

			ReaderPart readerPartObject = mPart;
			try {
				readerPartObject.switchReaderType(new VirtualDriverComm(VirtualDriverComm.DEFAULT_HOST, VirtualDriverComm.DEFAULT_PORT));
			}
			catch (IfdCreationException e) {
				BasicLogger.logException(this.getClass(), e, LogLevel.WARN);
				MessageDialog.openWarning(Display.getCurrent().getActiveShell(), "Connector could not be created", "The virtual driver communication could not be established.");
			}
		}
	}

}
