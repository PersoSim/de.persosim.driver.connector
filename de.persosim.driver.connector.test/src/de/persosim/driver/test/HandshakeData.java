package de.persosim.driver.test;

import de.persosim.driver.connector.UnsignedInteger;

/**
 * This class stores data that is generated and transmitted during the handshake
 * for later use.
 * 
 * @author mboonk
 *
 */
public class HandshakeData {
	private UnsignedInteger lun;
	private boolean handshakeDone;

	/**
	 * @return the handshakeDone
	 */
	public boolean isHandshakeDone() {
		return handshakeDone;
	}

	/**
	 * @param handshakeDone
	 *            the handshakeDone to set
	 */
	public void setHandshakeDone(boolean handshakeDone) {
		this.handshakeDone = handshakeDone;
	}

	/**
	 * @return the lun
	 */
	public UnsignedInteger getLun() {
		return lun;
	}

	public void setLun(UnsignedInteger lun) {
		this.lun = lun;
	}
}
