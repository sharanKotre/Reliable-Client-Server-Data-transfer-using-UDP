package UDPClientServerPackage;

/*Enumeration for all valid error codes(0,1,2,3). 
 * errorCodeZero(0) - Successful request. 
 * errorCodeOne(1) - Integrity check of the request failed at the server. 
 * errorCodeTwo(2) - Syntax check of the request failed at the server. 
 * errorCodeThree(3) - Error in finding the Measurement value for the requested measurement ID.
 * */
public enum UDPErrorCodes {
	errorCodeZero, errorCodeOne, errorCodeTwo, errorCodeThree;
}