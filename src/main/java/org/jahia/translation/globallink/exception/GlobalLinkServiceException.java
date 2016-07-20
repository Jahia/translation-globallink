package org.jahia.translation.globallink.exception;

/**
 * Service Exception for Global link translation operations.
 * 
 * @author Prince.Arora, WebItUp
 */
public class GlobalLinkServiceException extends RuntimeException {

	private static final long serialVersionUID = 7049630966775813325L;
	
	public GlobalLinkServiceException() {
	}
	
	public GlobalLinkServiceException(String message) {
		super(message);
	}
	
	public GlobalLinkServiceException(String message, Throwable throwable) {
		super(message, throwable);
	}

}
