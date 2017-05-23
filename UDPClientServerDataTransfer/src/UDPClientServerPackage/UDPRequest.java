package UDPClientServerPackage;

/* 
 * Class to handle the message format of the request from the client. 
 * Provides useful functionality to perform integrity and syntax check for the server side application.
 */

public class UDPRequest {

	private int requestID;
	private int measurementID;
	private byte[] requestByteArray;
	private String requestString;
	private int checksum;
	private boolean elementsCheck;

	/*
	 * Constructor meant to be used at the client side to initialize the request
	 * that needs to be sent. Initializes the request ID, measurement ID,
	 * calculates and initializes the integrityCheck value, initializes the
	 * request message and the request in bytes.
	 */
	public UDPRequest(int aRequestID, int aMeasurementID) {
		setRequestID(aRequestID);
		setMeasurementID(aMeasurementID);
		String preChecksumRequestString = formRequest();
		setChecksum(calculateChecksum(preChecksumRequestString));
		setRequest(preChecksumRequestString + getChecksum());
		setRequestByteArray(getRequest().getBytes());
	}

	/*
	 * Constructor meant to be used at the server side to initialize the
	 * received request.
	 */
	public UDPRequest(byte[] requestByteArray) {
		setRequestByteArray(requestByteArray);
		setRequest(new String(requestByteArray).replaceAll("\\s+", ""));
	}

	/* Function to form the request message. */
	private String formRequest() {
		return "<request><id>" + getRequestID() + "</id><measurement>" + getMeasurementID()
				+ "</measurement></request>";
	}

	/*
	 * Function that calculates integrity check for the request message. White
	 * space is not taken into account during the calculation of integrity check
	 * value.
	 */
	private int calculateChecksum(String preChecksumRequestString) {
		byte[] newByteArray = preChecksumRequestString.replaceAll("\\s+", "").getBytes();
		short[] asciiArray = new short[newByteArray.length % 2 == 0 ? newByteArray.length / 2
				: (newByteArray.length / 2) + 1];

		for (int i = 1; i < newByteArray.length; i += 2) {
			asciiArray[(i - 1) / 2] = (short) ((newByteArray[i - 1] << 8) + (newByteArray[i]));
		}
		if (newByteArray.length % 2 != 0)
			asciiArray[asciiArray.length - 1] = (short) (newByteArray[newByteArray.length - 1] << 8);

		int s = 0;
		for (int i = 0; i < asciiArray.length; ++i) {
			int index = ((s ^ asciiArray[i]));
			s = ((7919 * index) % 65536);
		}
		return s;
	}

	/*
	 * Function that performs integrity check on request message. When integrity
	 * check value cannot be found assumes integrity check passes but
	 * initializes elements check as false which then is evaluated while
	 * performing syntax check for the request.
	 */
	public boolean performIntegrityCheckOnRequest() {
		String request = getRequest();
		try {
			setRequestID(parseRequestID());
		} catch (IllegalArgumentException illegalArgumentException) {
			setElementsCheck(false);
			setRequestID(UDPGlobals.randomObject.nextInt(65536));
		}
		try {
			setMeasurementID(parseMeasurementID());
		} catch (IllegalArgumentException illegalArgumentException) {
			setElementsCheck(false);
		}
		try {
			setChecksum(parseChecksum());
		} catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException) {
			setElementsCheck(false);
			return true;
		}
		String requestWithoutChecksum = request.split("</request>")[0] + "</request>";
		if (getChecksum() == calculateChecksum(requestWithoutChecksum)) {
			setElementsCheck(true);
			return true;
		}
		return false;

	}

	/*
	 * Function that performs syntax check on the request message if elements
	 * check has passed during integrity check of request message. Else returns
	 * false indicating syntax check fails.
	 */
	public boolean performSyntaxCheckOnRequest() {
		if (getElementsCheck()) {
			String request = getRequest();
			try {
				String[] splitRequestOpeningTag = request.split("<request>");
				if (splitRequestOpeningTag[0].equals("")) {
					String[] splitRequestIDOpeningTag = splitRequestOpeningTag[1].split("<id>");
					if (splitRequestIDOpeningTag[0].equals("")) {
						String[] splitRequestIDClosingTag = splitRequestIDOpeningTag[1].split("</id>");
						Integer.valueOf(splitRequestIDClosingTag[0]);
						String[] splitMeasurementIDOpeningTag = splitRequestIDClosingTag[1].split("<measurement>");
						if (splitMeasurementIDOpeningTag[0].equals("")) {
							String[] splitMeasurementIDClosingTag = splitMeasurementIDOpeningTag[1]
									.split("</measurement>");
							Integer.valueOf(splitMeasurementIDClosingTag[0]);
							String[] splitRequestClosingTag = splitMeasurementIDClosingTag[1].split("</request>");
							if (splitRequestClosingTag[0].equals("")) {
								Integer.valueOf(splitRequestClosingTag[1]);
								return true;
							}
						}
					}
				}
				return false;
			} catch (ArrayIndexOutOfBoundsException arrayRangeOutOfBounds) {
				return false;
			} catch (NumberFormatException numberFormatException) {
				return false;
			}
		}
		return false;
	}

	/*
	 * Function that returns integer between the <id> and </id> tags. If the
	 * sequence between the 2 tags cannot be converted to an integer an
	 * IllegalArgumentException is thrown saying Illegal id received. When
	 * cannot find either of the 2 tags in the request message an
	 * IllegalArgumentException is thrown saying Illegal request received.
	 */
	private int parseRequestID() throws IllegalArgumentException, NumberFormatException {
		try {
			String[] newString = getRequest().split("<id>");
			String[] anotherString = newString[1].split("</id>");
			return Integer.valueOf(anotherString[0]);
		} catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBounds) {
			throw new IllegalArgumentException(arrayIndexOutOfBounds);
		} catch (NumberFormatException numberFormatException) {
			throw new IllegalArgumentException(numberFormatException);
		}
	}

	/*
	 * Function that returns integer between the <measurement> and
	 * </measurement> tags. If the sequence between the 2 tags cannot be
	 * converted to an integer an IllegalArgumentException is thrown saying
	 * Illegal measurement received. When cannot find either of the 2 tags in
	 * the request message an IllegalArgumentException is thrown saying Illegal
	 * request received.
	 */
	private int parseMeasurementID() throws NumberFormatException, IllegalArgumentException {
		try {
			String[] newString = getRequest().split("<measurement>");
			String[] anotherString = newString[1].split("</measurement>");
			return Integer.valueOf(anotherString[0]);
		} catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBounds) {
			throw new IllegalArgumentException(arrayIndexOutOfBounds);
		} catch (NumberFormatException numberFormatException) {
			throw new IllegalArgumentException(numberFormatException);
		}
	}

	/*
	 * Function the returns the integrity check value from the request message.
	 * If integrity check value cannot be found an
	 * ArrayIndexOutOfBoundsException is thrown. If checksum is not of proper
	 * type then an IllegalArgumentException is automatically thrown.
	 */
	private int parseChecksum() throws ArrayIndexOutOfBoundsException {
		String requestString = getRequest();
		int checkSum = Integer.valueOf((requestString.split("</request>"))[1]);
		return checkSum;
	}

	/* Getters */
	public byte[] getRequestByteArray() {
		return requestByteArray;
	}

	public int getRequestID() {
		return requestID;
	}

	public int getMeasurementID() {
		return measurementID;
	}

	private String getRequest() {
		return requestString;
	}

	public int getChecksum() {
		return checksum;
	}

	private boolean getElementsCheck() {
		return elementsCheck;
	}

	/* Setters */
	public void setRequestID(int reqID) {
		requestID = reqID;
	}

	private void setMeasurementID(int mID) {
		measurementID = mID;
	}

	private void setChecksum(int sum) {
		checksum = sum;
	}

	private void setRequest(String req) {
		requestString = req;
	}

	private void setRequestByteArray(byte[] reqByteArray) {
		requestByteArray = reqByteArray;
	}

	private void setElementsCheck(boolean bool) {
		elementsCheck = bool;
	}

}
