 
package de.persosim.driver.connector.ui.handlers;

import org.eclipse.e4.core.di.annotations.Execute;
import org.globaltester.logging.BasicLogger;
import org.globaltester.logging.tags.LogLevel;

public class NativeDriverHandler {
	
	@Execute
	public void execute() {
		//FIXME implement this
		BasicLogger.log(this.getClass(), "Switch to use NativeDriver", LogLevel.FATAL);
	}
		
}