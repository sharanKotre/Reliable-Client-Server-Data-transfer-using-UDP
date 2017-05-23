package UDPClientServerPackage;

import java.io.*;
import java.net.*;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class UDPServer {
	private static final int PORT_NUMBER = 12000; // Local Server listening at
													// PORT_NUMBER
	private DatagramSocket serverSocket; // UDP Socket on server side
	private UDPRequest receivedRequest;
	private DatagramPacket receivedPacket;// Packet to receive request
	private DatagramPacket currentResponsePacket;// Packet to send response
	private UDPResponse sentResponse;

	/*
	 * Entry point for server side application. Initializes UDPServer class and
	 * starts listening for requests from client on the specified port number.
	 * Processes the request, sends appropriate response and continues to listen
	 * for requests.
	 */
	public static void main(String[] args) {
		// Initialize server instance and start listening for requests.
		UDPServer serverInstance = new UDPServer();
		while (true) {
			try {
				serverInstance.getServerSocket().receive(serverInstance.getReceivedPacket());
				byte[] receivedBytes = new byte[serverInstance.getReceivedPacket().getLength()];
				System.arraycopy(serverInstance.getReceivedPacket().getData(),
						serverInstance.getReceivedPacket().getOffset(), receivedBytes, 0, receivedBytes.length);
				serverInstance.setReceivedRequest(new UDPRequest(receivedBytes));
				UDPGlobals.displayMessage("Received Request is \n" + new String(receivedBytes));
				// Perform integrity check. If integrity check fails send
				// response with ID = request ID and error code = 1, else
				// perform syntax check.
				if (!serverInstance.performIntegrityCheckOnRequest()) {
					serverInstance.setToBeSentResponse(new UDPResponse(
							serverInstance.getReceivedRequest().getRequestID(), UDPErrorCodes.errorCodeOne));
					serverInstance.setCurrentResponsePacket(
							new DatagramPacket(serverInstance.getToBeSentResponse().getResponseByteArray(),
									serverInstance.getToBeSentResponse().getResponseByteArray().length));
					serverInstance.getCurrentResponsePacket()
							.setAddress(serverInstance.getReceivedPacket().getAddress());
					serverInstance.getCurrentResponsePacket().setPort(serverInstance.getReceivedPacket().getPort());
					serverInstance.sendResponse();
				} else {
					// Perform syntax check. If syntax check fails send response
					// with ID = request ID and error code = 2, else try finding
					// measurement value
					if (!serverInstance.performSyntaxCheckOnRequest()) {
						serverInstance.setToBeSentResponse(new UDPResponse(
								serverInstance.getReceivedRequest().getRequestID(), (UDPErrorCodes.errorCodeTwo)));
						serverInstance.setCurrentResponsePacket(
								new DatagramPacket(serverInstance.getToBeSentResponse().getResponseByteArray(),
										serverInstance.getToBeSentResponse().getResponseByteArray().length));
						serverInstance.getCurrentResponsePacket()
								.setAddress(serverInstance.getReceivedPacket().getAddress());
						serverInstance.getCurrentResponsePacket().setPort(serverInstance.getReceivedPacket().getPort());
						serverInstance.sendResponse();
					} else {

						try {
							// Try to find measurement value in data.txt file at
							// server. If exception occurs, handle it by
							// sending a response with ID = request ID and error
							// code = 3. If measurement
							// value is found, response with measurement value
							// and error code 0 is sent
							float measurementValue = serverInstance.findMeasurementValue();
							serverInstance.setToBeSentResponse(
									new UDPResponse(serverInstance.getReceivedRequest().getRequestID(),
											serverInstance.getReceivedRequest().getMeasurementID(), measurementValue));
							serverInstance.setCurrentResponsePacket(
									new DatagramPacket(serverInstance.getToBeSentResponse().getResponseByteArray(),
											serverInstance.getToBeSentResponse().getResponseByteArray().length));
							serverInstance.getCurrentResponsePacket()
									.setAddress(serverInstance.getReceivedPacket().getAddress());
							serverInstance.getCurrentResponsePacket()
									.setPort(serverInstance.getReceivedPacket().getPort());
							serverInstance.sendResponse();

						} catch (IllegalArgumentException illegalArgumentException) {
							serverInstance.setToBeSentResponse(
									new UDPResponse(serverInstance.getReceivedRequest().getRequestID(),
											(UDPErrorCodes.errorCodeThree)));
							serverInstance.setCurrentResponsePacket(
									new DatagramPacket(serverInstance.getToBeSentResponse().getResponseByteArray(),
											serverInstance.getToBeSentResponse().getResponseByteArray().length));
							serverInstance.getCurrentResponsePacket()
									.setAddress(serverInstance.getReceivedPacket().getAddress());
							serverInstance.getCurrentResponsePacket()
									.setPort(serverInstance.getReceivedPacket().getPort());
							serverInstance.sendResponse();
						}
					}
				}
			} catch (IOException ioException) {
				UDPGlobals.displayMessage(ioException.getMessage());
			}
		}
	}

	/* Constructor to initialize DatagramSocket at the Server. */
	public UDPServer() {
		setReceivedPacket(new DatagramPacket(UDPGlobals.dummyByteArray, UDPGlobals.dummyByteArray.length));
		try {
			setServerSocket(new DatagramSocket(PORT_NUMBER));
		} catch (SocketException socketException) {
			UDPGlobals.displayMessage(socketException.getMessage());
		}
	}

	/*
	 * Send response for the current request received. Assumes current response
	 * packet has been initialized with data to be sent, port number the packet
	 * needs to go to at the address from which the request was received.
	 */
	public void sendResponse() {
		try {
			UDPGlobals.displayMessage("Sending Response for request ID:" + getReceivedRequest().getRequestID() + "\n"
					+ new String(getToBeSentResponse().getResponseByteArray()));
			getServerSocket().send(getCurrentResponsePacket());
		} catch (IOException ioException) {
			ioException.getMessage();
		}
	}

	/*
	 * This function tries to find the measurement value by going through
	 * data.txt file at server line by line trying to match the measurement ID's
	 * in the file with the measurement ID at the current received request. If a
	 * match is found tries to read the measurement value and if it can be read
	 * the value is returned, else an IllegalArgumentException exception is
	 * thrown indicating the measurement value is not present or is in incorrect
	 * type for the requested ID. If no measurement ID's in the file matches the
	 * measurement ID of the request an IllegalArgumentException is thrown
	 * indicating measurement ID not found in the file. Both the exceptions are
	 * handled the same way at the server by sending a response with error code
	 * 3. Path of the file must be correct. Otherwise a FileNotFoundException is
	 * thrown.
	 */
	private float findMeasurementValue() throws IllegalArgumentException, FileNotFoundException {
		try {
			Scanner scannerObject = new Scanner(
					new File("C:/Users/Sharan O Kotre/workspace2/ProjectServer/src/UDPClientServerPackage/data.txt"));

			while (scannerObject.hasNextLine()) {
				if (scannerObject.hasNextInt()) {
					if (scannerObject.nextInt() == getReceivedRequest().getMeasurementID()) {
						scannerObject.useDelimiter("\n");
						float value = Float.parseFloat(scannerObject.next());
						scannerObject.close();
						return (value);
					}
					scannerObject.nextLine();
				}
			}
			scannerObject.close();
			throw new IllegalArgumentException("Measurement ID not found!");
		} catch (FileNotFoundException fileNotFoundException) {
			throw fileNotFoundException;
		} catch (NoSuchElementException noSuchElementException) {
			throw new IllegalArgumentException("Measurement ID not found!");
		} catch (NullPointerException nullPointerException) {
			UDPGlobals.displayMessage(nullPointerException.getMessage());
			return 0;
		}
	}

	/*
	 * Uses the value returned by function provided by UDPRequest to perform
	 * Integrity check on the request received and passes the same value to the
	 * calling function.
	 */
	public boolean performIntegrityCheckOnRequest() {
		return getReceivedRequest().performIntegrityCheckOnRequest();
	}

	/*
	 * Uses the value returned by function provided by UDPRequest to perform
	 * Syntax check on the request received and passes the same value to the
	 * calling function.
	 */
	public boolean performSyntaxCheckOnRequest() {
		return getReceivedRequest().performSyntaxCheckOnRequest();
	}

	/* Getters */
	public DatagramSocket getServerSocket() {
		return serverSocket;
	}

	public DatagramPacket getReceivedPacket() {
		return receivedPacket;
	}

	public DatagramPacket getCurrentResponsePacket() {
		return currentResponsePacket;
	}

	public UDPRequest getReceivedRequest() {
		return receivedRequest;
	}

	public UDPResponse getToBeSentResponse() {
		return sentResponse;
	}

	/* Setters */
	public void setServerSocket(DatagramSocket aSocket) {
		serverSocket = aSocket;
	}

	public void setReceivedPacket(DatagramPacket aPacket) {
		receivedPacket = aPacket;
	}

	public void setCurrentResponsePacket(DatagramPacket aPacket) {
		currentResponsePacket = aPacket;
	}

	public void setReceivedRequest(UDPRequest aRequest) {
		receivedRequest = aRequest;
	}

	public void setToBeSentResponse(UDPResponse aResponse) {
		sentResponse = aResponse;
	}
}
