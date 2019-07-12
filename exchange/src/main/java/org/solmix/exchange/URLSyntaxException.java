package org.solmix.exchange;

/**
 * URL syntax exception.
 */
public class URLSyntaxException extends Exception {

	private static final long serialVersionUID = 1L;

	URLSyntaxException(String message) {
		super(message);
	}

	URLSyntaxException(Throwable cause) {
		super(cause);
	}

}
