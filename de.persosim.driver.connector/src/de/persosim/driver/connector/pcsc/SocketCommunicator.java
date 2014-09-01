package de.persosim.driver.connector.pcsc;

import java.net.Socket;

/**
 * Implementing classes use a {@link Socket} for communication that is set after the
 * creation of the object. Thus the socket used for communication can be changed
 * during runtime.
 * 
 * @author mboonk
 *
 */
public interface SocketCommunicator {
	/**
	 * @param socket
	 *            the new socket for future communication
	 */
	public abstract void setCommunicationSocket(Socket socket);
}
