package UDPClientServerPackage;

public class UDPError {
	UDPErrorCodes errorCode;
	String errorMessage;

	/*
	 * Constructor to initialize an object of UDPError with given UDPErrorCodes.
	 */
	public UDPError(UDPErrorCodes anErrorCode) {
		errorCode = anErrorCode;
		errorMessage = getErrorMessage();
	}

	/*
	 * Constructor to initialize object of type UDPError with the given integer
	 * error code.
	 */
	public UDPError(int error) {
		errorCode = getErrorCodesFromInt(error);
	}

	/*
	 * Function returns the integer error code from the initialized
	 * UDPErrorCodes.
	 */
	public int getErrorCode() {
		switch (errorCode) {
		case errorCodeZero:
			return 0;
		case errorCodeOne:
			return 1;
		case errorCodeTwo:
			return 2;
		case errorCodeThree:
			return 3;
		default:
			return 99;
		}
	}

	/* Function returns UDPErrorCodes from the argument integer error code. */
	private UDPErrorCodes getErrorCodesFromInt(int errorCode) {
		switch (errorCode) {
		case 0:
			return UDPErrorCodes.errorCodeZero;
		case 1:
			return UDPErrorCodes.errorCodeOne;
		case 2:
			return UDPErrorCodes.errorCodeTwo;
		case 3:
			return UDPErrorCodes.errorCodeThree;
		default:
			return UDPErrorCodes.errorCodeThree;
		}
	}

	/* Function returns the error message corresponding to the UDPErrorCodes */
	public String getErrorMessage() {
		switch (errorCode) {
		case errorCodeZero:
			return "OK. The response has been created according to the request.";
		case errorCodeOne:
			return "Error: integrity check failure. The request has one or more bit errors.";
		case errorCodeTwo:
			return "Error: malformed request. The syntax of the request message is not correct.";
		case errorCodeThree:
			return "Error: non-existent measurement. The measurement with the requested measurement ID does not exist.";
		default:
			return "";
		}
	}
}
