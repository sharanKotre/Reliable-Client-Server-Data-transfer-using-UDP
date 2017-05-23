package UDPClientServerPackage;

import java.net.*;
import java.io.*;
import java.util.Scanner;

public class UDPClient {
	static int numberOfTimeOuts = 0;
	static int initialTimeoutInterval = 1000;
	static int currentTimeoutInterval = 1000;
	static final int LOCAL_PORT_NUMBER = 12000; // Port number to initialize the
												// packet to send requests.

	private DatagramSocket clientSocket; // UDP Socket on the client side.
	private DatagramPacket currentRequestPacket;// Packet to send requests.
	private DatagramPacket currentResponsePacket;// Packet to receive response.
	private UDPRequest currentRequest;
	private UDPResponse currentResponse;
	private int portNumber;
	private int[] dataArray;// container to hold the available measurement ID's
							// read from data.txt file

	/*
	 * Entry point for the client side application. Iterates through the
	 * dataArray after initializing it, one measurement ID at a time. Sends
	 * request with randomly generated request ID and measurement value at
	 * current index of dataArray. Waits for response from the server.
	 */
	public static void main(String[] args) {
		try {
			UDPClient clientInstance = new UDPClient();
			clientInstance.initializeDataArray();
			int index = 0;
			while (index < clientInstance.getDataArray().length) {
				clientInstance.setCurrentRequest(
						new UDPRequest(UDPGlobals.randomObject.nextInt(65536), clientInstance.getDataArray()[index]));
				byte[] byteArray = clientInstance.getCurrentRequest().getRequestByteArray();
				clientInstance.setCurrentRequestPacket(new DatagramPacket(byteArray, byteArray.length,
						InetAddress.getLocalHost(), clientInstance.getPortNumber()));
				clientInstance.sendRequest(initialTimeoutInterval);
				++index;
			}
		} catch (SocketException socketException) {
			UDPGlobals.displayMessage(socketException.getMessage());
			// clientSocket.close();
		} catch (UnknownHostException unknownHostException) {
			UDPGlobals.displayMessage(unknownHostException.getMessage());
		}
	}

	/* Constructor to initialize DatagramSocket at the Client. */
	public UDPClient() throws SocketException {
		setPortNumber(LOCAL_PORT_NUMBER);
		clientSocket = new DatagramSocket();
	}

	/*
	 * Function to send the current request packet through the client socket
	 * with timeout interval value passed to this function. Waits for response.
	 * Prior to sending the request, function checks the number of timeouts that
	 * has occurred. If it is more than 4 then printing an error message on the
	 * console declaring communication error and moves onto next available
	 * measurement ID.
	 */
	public void sendRequest(int timeoutInterval) {
		if (numberOfTimeOuts > 3) {
			UDPGlobals.displayMessage("Connection Failure!Try again later.");
			numberOfTimeOuts = 0;
			currentTimeoutInterval = initialTimeoutInterval;
			return;
		}
		try {
			getClientSocket().send(getCurrentRequestPacket());
			getClientSocket().setSoTimeout(timeoutInterval);
			UDPGlobals.displayMessage("-------------------------------->");
			UDPGlobals.displayMessage(
					"Requesting packet with ID: " + getCurrentRequest().getRequestID() + " and measurement ID: "
							+ getCurrentRequest().getMeasurementID() + " with timeout " + currentTimeoutInterval);
			UDPGlobals.displayMessage(new String(getCurrentRequest().getRequestByteArray()));
			setCurrentResponsePacket(new DatagramPacket(UDPGlobals.dummyByteArray, UDPGlobals.dummyByteArray.length));
			receiveResponse();
		} catch (IOException ioException) {
			ioException.getMessage();
		}
	}

	/*
	 * Function where the client socket waits to receive the response from
	 * server. If timeout occurs resends the request with double timeout
	 * interval. Else processes the response.
	 */
	public void receiveResponse() {
		try {
			getClientSocket().receive(getCurrentResponsePacket());
			byte[] newByteArray = new byte[getCurrentResponsePacket().getLength()];
			System.arraycopy(getCurrentResponsePacket().getData(), getCurrentResponsePacket().getOffset(), newByteArray,
					0, newByteArray.length);
			numberOfTimeOuts = 0;
			currentTimeoutInterval = initialTimeoutInterval;
			setCurrentResponse(new UDPResponse(newByteArray));
			UDPGlobals.displayMessage("Received response for request with ID: " + getCurrentRequest().getRequestID()
					+ " and measurement ID: " + getCurrentRequest().getMeasurementID());
			UDPGlobals.displayMessage((new String(newByteArray)));
			// Perform integrity check on response. If check fails, send the
			// request again, else process the response further.
			if (!getCurrentResponse().performIntegrityCheckOnResponse()) {
				UDPGlobals.displayMessage("Sending request again");
				getCurrentRequest().setRequestID(UDPGlobals.randomObject.nextInt(65536));
				sendRequest(initialTimeoutInterval);
			} else {
				// Further processing the response by reading error code.
				UDPError responseError = getCurrentResponse().getResponseError();
				// If error code received from response == 1, then ask the user
				// whether to re-send the current request. If yes, re-send the
				// request, else move onto the next request. If error code == 2
				// or error code == 3, print appropriate error message onto the
				// console. Else, read the measurement value from the response
				// and print it onto console. Then move onto the next available
				// measurement ID
				if ((responseError.getErrorCode() == 1)) {
					if (Character.toLowerCase(getUserInput()) == 'y') {
						sendRequest(initialTimeoutInterval);
					}
				} else if (responseError.getErrorCode() == 2 || responseError.getErrorCode() == 3) {
					UDPGlobals.displayMessage(getCurrentResponse().getResponseError().getErrorMessage());
				} else {
					UDPGlobals.displayMessage(
							"Measurement ID:" + getCurrentResponse().getMeasurementID() + "\nMeasurement Value: "
									+ getCurrentResponse().getMeasurementValue() + " degree Fahrenheit.");
				}
				UDPGlobals.displayMessage("<--------------------------------");
			}
		} catch (IOException ioException) {
			++numberOfTimeOuts;
			sendRequest(currentTimeoutInterval *= 2);
		} catch (IllegalArgumentException illegalArgumentException) {
			UDPGlobals.displayMessage(illegalArgumentException.getMessage());
		} catch (StackOverflowError stackOverflowError) {
			UDPGlobals.displayMessage("Connection Failure!Try again later.");
			return;
		}
	}

	/*
	 * Function to initialize the container to hold the available measurement
	 * ID's in the data.txt file. Function first reads the number of lines and
	 * initializes dataArray to hold those many integer values. Then goes
	 * through the file line by line reading next available Integer and storing
	 * it into respective index of the dataArray. The path where the data.txt
	 * file resides must be correct. Else a FileNotFoundException is thrown.
	 */
	private void initializeDataArray() {

		try {
			Scanner scannerObject = new Scanner(
					new File("C:/Users/Sharan O Kotre/Project/UDPClient/src/UDPClientServerPackage/data.txt"));
			int i = 0;
			while (scannerObject.hasNextLine()) {
				++i;
				scannerObject.nextLine();
			}
			dataArray = new int[i];
			i = 0;
			scannerObject.close();
			scannerObject = new Scanner(
					new File("C:/Users/Sharan O Kotre/Project/UDPClient/src/UDPClientServerPackage/data.txt"));
			while (scannerObject.hasNextLine()) {
				int reqID = scannerObject.nextInt();
				dataArray[i] = reqID;
				++i;
				scannerObject.nextLine();
			}
			scannerObject.close();
		} catch (FileNotFoundException fileNotFoundException) {
			UDPGlobals.displayMessage(fileNotFoundException.getMessage());
		}
	}

	private char getUserInput() {
		Reader readerObject = new InputStreamReader(System.in);
		char reqChar = 'a';
		try {
			while (Character.toLowerCase(reqChar) != 'y' && Character.toLowerCase(reqChar) != 'n') {
				UDPGlobals.displayMessage("Integrity check for the request with requestID:"
						+ currentRequest.getRequestID() + " and measurementID:" + currentRequest.getMeasurementID()
						+ " failed.\nDo you wish to resend this packet?(y/n)");
				int asciiOfCharRead = readerObject.read();
				reqChar = (char) asciiOfCharRead;
				UDPGlobals.displayMessage("entered " + reqChar);
			}

		} catch (IOException ioException) {
			UDPGlobals.displayMessage(ioException.getMessage());
		}
		return reqChar;
	}

	/* Getters */
	public int getPortNumber() {
		return portNumber;
	}

	public DatagramSocket getClientSocket() {
		return clientSocket;
	}

	public DatagramPacket getCurrentRequestPacket() {
		return currentRequestPacket;
	}

	public UDPRequest getCurrentRequest() {
		return currentRequest;
	}

	public DatagramPacket getCurrentResponsePacket() {
		return currentResponsePacket;
	}

	public UDPResponse getCurrentResponse() {
		return currentResponse;
	}

	private int[] getDataArray() {
		return dataArray;
	}

	/* Setters */
	public void setClientSocket(DatagramSocket aSocket) {
		clientSocket = aSocket;
	}

	public void setPortNumber(int aPortNumber) {
		portNumber = aPortNumber;
	}

	public void setCurrentRequestPacket(DatagramPacket aRequestPacket) {
		currentRequestPacket = aRequestPacket;
	}

	public void setCurrentRequest(UDPRequest aRequest) {
		currentRequest = aRequest;
	}

	public void setCurrentResponsePacket(DatagramPacket aPacket) {
		currentResponsePacket = aPacket;
	}

	public void setCurrentResponse(UDPResponse aResponse) {
		currentResponse = aResponse;
	}
}
