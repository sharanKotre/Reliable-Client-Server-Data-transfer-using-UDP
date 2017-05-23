package UDPClientServerPackage;

/* 
 * Class to handle the message format of the response from the server. 
 * Provides useful functionality to perform integrity and syntax check for the client application.
 */
public class UDPResponse {

	private int responseID;
	private int measurementID;
	private UDPError responseError;
	private float measurementValue;
	private int checksum;
	private String responseMessage;
	private byte[] responseByteArray;

	/*
	 * Constructor meant for use at server side when measurement value for the
	 * requested measurement ID is found. Initializes the response ID,
	 * measurement ID, measurement value passed to the constructor. Initializes
	 * response error code to 0, the checksum field calculated for string formed
	 * using formNoErrorResponse() and the response in bytes.
	 */
	public UDPResponse(int requestID, int aMeasurementID, float measurementValue) {
		setResponseID(requestID);
		setMeasurementID(aMeasurementID);
		setMeasurementValue(measurementValue);
		setResponseError(new UDPError(UDPErrorCodes.errorCodeZero));
		String preChecksumResponse = formNoErrorResponse();
		setChecksum(calculateChecksum(preChecksumResponse));
		setResponseMessage(preChecksumResponse + getChecksum());
		setResponseByteArray(getResponseMessage().getBytes());
	}

	/*
	 * Constructor meant for the server side when an error occurs during the
	 * processing of the request. Initializes response ID, and error code with
	 * values passed through arguments. Calculates and initializes checksum for
	 * string formed by formErrorResponse(). Initializes the response message
	 * and the response in bytes.
	 */
	public UDPResponse(int aRequestID, UDPErrorCodes anErrorCode) {
		setResponseID(aRequestID);
		setResponseError(new UDPError(anErrorCode));
		String preChecksumResponseString = formErrorResponse();
		setChecksum(calculateChecksum(preChecksumResponseString));
		setResponseMessage(preChecksumResponseString + getChecksum());
		setResponseByteArray(getResponseMessage().getBytes());
	}

	/*
	 * Constructor for response message to be used at the client side on
	 * requesting the response in bytes.
	 */
	public UDPResponse(byte[] responseBytes) throws IllegalArgumentException {
		setResponseByteArray(responseBytes);
		setResponseMessage(new String(responseBytes).replaceAll("\\s+", ""));
		setResponseError(new UDPError((parseErrorCode())));
		setResponseID(parseResponseID());
		setMeasurementID(parseMeasurementID());
		setMeasurementValue(parseMeasurementValue());
		setChecksum(parseChecksum());
	}

	/*
	 * Member function that returns a boolean true if integrity check on the
	 * response passes, else returns false. Performs integrity check by
	 * comparing the integrity check value received in the message with the
	 * value calculated by the calculateChecksum() method.
	 */
	public boolean performIntegrityCheckOnResponse() {
		String response = getResponseMessage();
		String responseWithoutChecksum = response.split("</response>")[0] + "</response>";
		int receivedChecksum = Integer.valueOf(response.split("</response>")[1]);
		if (receivedChecksum == calculateChecksum(responseWithoutChecksum))
			return true;
		return false;
	}

	/* Forms an error response with response ID and error code. */
	private String formErrorResponse() {
		return "<response><id>" + getResponseID() + "</id><code>" + getResponseError().getErrorCode()
				+ "</code></response>";
	}

	/*
	 * Forms a no error response with response ID, error code, measurement ID
	 * and measurement value
	 */
	public String formNoErrorResponse() {
		return "<response><id>" + getResponseID() + "</id><code>" + getResponseError().getErrorCode()
				+ "</code><measurement>" + getMeasurementID() + "</measurement><value>" + getMeasurementValue()
				+ "</value></response>";
	}

	/* Calculates and returns integrity check value. */
	private int calculateChecksum(String preChecksumResponseString) {
		byte[] newByteArray = preChecksumResponseString.replaceAll("\\s+", "").getBytes();
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
	 * Function that returns error code between the <code> and </code> tags. If
	 * the sequence between the 2 tags cannot be converted to an integer an
	 * IllegalArgumentException is thrown saying Illegal code received. When
	 * cannot find either of the 2 tags in the response message an
	 * IllegalArgumentException is thrown saying Illegal response received.
	 */
	private int parseErrorCode() throws IllegalArgumentException {
		try {
			String[] newString = getResponseMessage().split("<code>");
			String[] anotherString = newString[1].split("</code>");
			for (int i = 0; i < anotherString[0].length(); ++i)
				if ((int) (anotherString[0].charAt(i)) - 48 > 9 || anotherString[0].charAt(i) - 48 < 0)
					throw new IllegalArgumentException("Illegal code received.");
			return Integer.valueOf(anotherString[0]);
		} catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBounds) {
			throw new IllegalArgumentException("Illegal response received.");
		}
	}

	/*
	 * Function that returns integer between the <id> and </id> tags. If the
	 * sequence between the 2 tags cannot be converted to an integer an
	 * IllegalArgumentException is thrown saying Illegal id received. When
	 * cannot find either of the 2 tags in the response message an
	 * IllegalArgumentException is thrown saying Illegal response received.
	 */
	private int parseResponseID() throws IllegalArgumentException {
		try {
			String[] newString = getResponseMessage().split("<id>");
			String[] anotherString = newString[1].split("</id>");
			for (int i = 0; i < anotherString[0].length(); ++i)
				if ((int) (anotherString[0].charAt(i)) - 48 > 9 || anotherString[0].charAt(i) - 48 < 0)
					throw new IllegalArgumentException("Illegal id received.");
			return Integer.valueOf(anotherString[0]);
		} catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBounds) {
			throw new IllegalArgumentException("Illegal response received.");
		}
	}

	/*
	 * Function that returns integer between the <measurement> and
	 * </measurement> tags. If the sequence between the 2 tags cannot be
	 * converted to an integer an IllegalArgumentException is thrown saying
	 * Illegal measurement ID received. When cannot find either of the 2 tags in
	 * the response message an IllegalArgumentException is thrown saying Illegal
	 * response received.
	 */
	private int parseMeasurementID() throws IllegalArgumentException {
		try {
			String[] newString = getResponseMessage().split("<measurement>");
			String[] anotherString = newString[1].split("</measurement>");
			for (int i = 0; i < anotherString[0].length(); ++i)
				if ((int) (anotherString[0].charAt(i)) - 48 > 9 || anotherString[0].charAt(i) - 48 < 0)
					throw new IllegalArgumentException("Illegal measurement ID received.");
			return Integer.valueOf(anotherString[0]);
		} catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBounds) {
			return 0;// throw new IllegalArgumentException("Illegal response
						// received.");
		}
	}

	/*
	 * Function that returns fixed point decimal number between the <value> and
	 * </value> tags. If the sequence between the 2 tags cannot be converted to
	 * an integer an IllegalArgumentException is thrown saying Illegal value
	 * received. When cannot find either of the 2 tags in the response message
	 * an IllegalArgumentException is thrown saying Illegal response received.
	 */
	private float parseMeasurementValue() throws IllegalArgumentException {
		try {
			String[] newString = getResponseMessage().split("<value>");
			String[] anotherString = newString[1].split("</value>");
			for (int i = 0; i < anotherString[0].length(); ++i)
				if ((anotherString[0].charAt(i)) - 48 > 9 || anotherString[0].charAt(i) - 48 < 0) {
					if (anotherString[0].charAt(i) != 46)
						throw new IllegalArgumentException("Illegal value received.");
				}
			return Float.valueOf(anotherString[0]);
		} catch (ArrayIndexOutOfBoundsException arrayIndexOutOfBounds) {
			return 0;// throw new IllegalArgumentException("Illegal key
						// received.");
		}
	}

	/*
	 * Function that returns the checksum value from the response message.
	 * Assumes that the response has no syntax errors.
	 */
	private int parseChecksum() {
		String requestString = getResponseMessage();
		int checkSum = Integer.valueOf((requestString.split("</response>"))[1]);
		return checkSum;
	}

	/* Getters */
	public byte[] getResponseByteArray() {
		return responseByteArray;
	}

	public String getResponseMessage() {
		return responseMessage;
	}

	public int getResponseID() {
		return responseID;
	}

	public int getMeasurementID() {
		return measurementID;
	}

	public UDPError getResponseError() {
		return responseError;
	}

	public float getMeasurementValue() {
		return measurementValue;
	}

	private int getChecksum() {
		return checksum;
	}

	/* Setters */
	private void setResponseID(int aResponseID) {
		responseID = aResponseID;
	}

	private void setMeasurementID(int aMeasurementID) {
		measurementID = aMeasurementID;
	}

	private void setResponseError(UDPError anError) {
		responseError = anError;
	}

	private void setChecksum(int aChecksum) {
		checksum = aChecksum;
	}

	private void setMeasurementValue(float value) {
		measurementValue = value;
	}

	private void setResponseMessage(String aString) {
		responseMessage = aString;
	}

	private void setResponseByteArray(byte[] aByteArray) {
		responseByteArray = aByteArray;
	}

}
