package org.jormunit;

/**
 * Exception thrown by the execution of a {@link JPATest}
 *
 */
public class JormUnitException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public JormUnitException() {
		super();
	}

	public JormUnitException(String message) {
		super(message);
	}

	public JormUnitException(Throwable cause) {
		super(cause);
	}

	public JormUnitException(String message, Throwable cause) {
		super(message, cause);
	}

}
