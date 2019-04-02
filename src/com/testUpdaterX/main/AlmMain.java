package com.testUpdaterX.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Category;

import com.cts.alm.testupdater.commons.Base64Encoder;

public class AlmMain {
	
	static final Category logger = Category.getInstance(AlmMain.class);
	static final String LOG_PROPERTIES_FILE = "src/Log4J.properties";
	static String almUname;
	static String almPwd;
	static String almProj;
	static String almDomain;
	static String almUrl;
	static String testSetID;
	static String testCaseID;
	static String testRunCycleID;
	static String runStatus;
	static Map<String, List<String>> AuthMap = new HashMap<String, List<String>>();
	static Map<String, List<String>> SessionMap = new HashMap<String, List<String>>();
	static String credEncodedString;
	static HttpURLConnection connection;
	static String respMsg;
	static int respCode;
	static String testRunConfigID;
	
	// read in this order - url, uname, pwd, domain, proj, testset id, tc id , runStatus
	public static void main(String[] args) throws IOException {

		logger.info("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=- ACTIVITY START =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");
		almUrl= args[0];
		almUname = args[1];
		almPwd= args[2];
		almDomain= args[3];
		almProj= args[4];
		testSetID= args[5];
		testCaseID= args[6];
		runStatus = args[7];

		AlmAuthenticate(); // POST REQUEST
		if(respCode == 200)
		{
			logger.info("Successful Authentication");
			AlmRestSession(); // GET REQUEST
			if(respCode == 201)
			{
				logger.info("Successful REST session creation");
				AlmTestInstanceInfo(); // POST REQUEST
				if(respCode == 200)
				{
					logger.info("Successful Test Instance fetch");
					AlmRunCreation(); // POST REQUEST
					if(respCode == 201)
					{
						logger.info("Successful Run Creation");
						AlmLogout();
					}
					else
					{
						logger.info("FAILED RUN creation");
						AlmLogout();
					}
				}
				else
				{
					logger.info("FAILED Test Instance creation");
					AlmLogout();
				}
			}
			else
			{
				logger.info("FAILED REST session creation");
				AlmLogout();
			}
		}
		else
		{
			logger.info("FAILED Authentication");
		}
		logger.info("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=- ACTIVITY END =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");
	}

	private static void AlmAuthenticate() throws IOException
	{
		respCode = 0;
		String cookie = null;
		logger.info("\n\n=-=-=-=-=-=-LOGIN TO ALM-=-=-=-=-=-=-=-=-=-=-=-=-=-=-==-=-");
		URL url = new URL (almUrl + "/qcbin/authentication-point/authenticate");
		logger.info("URL for auth:" + url);
		byte[] credBytes = (almUname + ":" + almPwd).getBytes();
		credEncodedString = "Basic " + Base64Encoder.encode(credBytes);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty  ("Authorization",credEncodedString);
		logger.debug("AuthMap saved the connection header fields and their values.");
		AuthMap = connection.getHeaderFields();
		respCode = connection.getResponseCode();
		if(respCode == 200)
		{
			logger.debug("Cookie received is = " +AuthMap.get("Set-Cookie").toString());
			logger.info("req method == " +connection.getRequestMethod());
			logger.info("request props == " +connection.getRequestProperties());
			logger.info("all header fields  ==" +connection.getHeaderFields());
			logger.info("Resp Code  ==" +connection.getResponseCode());
		}
				
		// 200
	}

	private static void AlmRestSession() throws IOException
	{
		respCode = 0;
		logger.info("\n\n=-=-=-=-=-=-=-REST SITE SESSION-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");
		URL url = new URL (almUrl + "/qcbin/rest/site-session");
		logger.info("REST Auth URL = " + url);
		connection = (HttpURLConnection) url.openConnection();
		connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty  ("Authorization",credEncodedString);
		connection.setRequestProperty("cookie", AuthMap.get("Set-Cookie").toString());
		logger.debug("cookie sent to server is = " +AuthMap.get("Set-Cookie").toString());
		logger.debug("req method ==" +connection.getRequestMethod());
		logger.debug("all header fields == " +connection.getHeaderFields());
		SessionMap = connection.getHeaderFields();
		logger.debug("SessionMap saved the connection header fields and their values.");
		respCode = connection.getResponseCode();
		// 201
	}

	private static void AlmTestInstanceInfo() throws IOException
	{
		respCode = 0;
		logger.info("\n\n=-=-=-=-=-=-=-GET TEST INSTANCE INFO-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");
		URL url = new URL (almUrl + "/qcbin/rest/domains/"+almDomain+"/projects/"+almProj+"/test-instances?query={cycle-id["+testSetID+"];test-id["+testCaseID+"]}");
		logger.info(url);
		connection = (HttpURLConnection) url.openConnection();
		connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");
		connection.setRequestProperty("Content Type", "application/xml");
		connection.setRequestProperty  ("Authorization",credEncodedString);
		connection.setRequestProperty("cookie", AuthMap.get("Set-Cookie").toString() + SessionMap.get("Set-Cookie").toString());
		logger.debug("cookie sent to server is = " +SessionMap.get("Set-Cookie").toString());
		logger.info("req method ==" +connection.getRequestMethod());
		logger.info("all header fields == " +connection.getHeaderFields());
		logger.info("RESPONSE Code ==" +connection.getResponseCode());
		logger.info("RESPONSE message ==" +connection.getResponseMessage());
		System.out.println("RESPONSE message ==" +connection.getResponseMessage());
		// 200
		if(connection.getResponseCode() != 200)
		{
			logger.error("SYSTEM thrown an error. Please check the prerequisites");
			logger.error("RESPONSE Code == "+ connection.getResponseCode() + "RESPONSE message ==" +connection.getResponseMessage());
		}
		else
		{
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			while((respMsg = in.readLine()) != null) 
			{
				logger.info("XML received is =" +respMsg);
				String respLocal = respMsg;
				testRunCycleID = XMLDataExtractor(respLocal, "<Field Name=\"id\"><Value>(.*?)</Value></Field><Field Name=\"ver-stamp\">");
				testRunConfigID = XMLDataExtractor(respLocal, "<Field Name=\"test-config-id\"><Value>(.*?)</Value></Field><Field Name=\"data-obj\">");
				logger.info("Test Run Cycle ID = \n" +testRunCycleID);
				logger.info("Test Case Config ID = \n" +testRunConfigID);
			}
		}
		respCode = connection.getResponseCode();
	}

	private static String XMLDataExtractor(String XMLdata, String PatternData) {
		int count = 0;
		String data = null;
		System.out.println("count = " + count + 1);
		logger.info(XMLdata);
		logger.info(PatternData);
		Pattern pattern = Pattern.compile(PatternData);
		Matcher matcher = pattern.matcher(XMLdata);
		while (matcher.find()) {
		   data =  matcher.group(1);
		}
		return data;
	}

	private static void AlmRunCreation() throws IOException
	{
		respCode = 0;
		logger.info("\n\n=-=-=-=-=-=-=-CREATE TEST RUN-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");
		URL url = new URL (almUrl + "/qcbin/rest/domains/"+almDomain+"/projects/"+almProj+"/runs");
		logger.info(url);
		String xmlDataToHP ="<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\r\n" + 
				"<Entity Type=\"run\">\r\n" + 
				"<Fields>\r\n" + 
				"<Field Name=\"test-config-id\"><Value>"+ testRunConfigID +"</Value></Field>\r\n" + 
				"<Field Name=\"cycle-id\"><Value>"+ testSetID +"</Value></Field>\r\n" + 
				"<Field Name=\"test-id\"><Value>"+ testCaseID +"</Value></Field>\r\n" + 
				"<Field Name=\"testcycl-id\"><Value>"+ testRunCycleID +"</Value></Field>\r\n" + 
				"<Field Name=\"name\"><Value>REST-API-run-"+ System.currentTimeMillis()  +"</Value></Field>\r\n" + 
				"<Field Name=\"owner\"><Value>"+ almUname +"</Value></Field>\r\n" + 
				"<Field Name=\"status\"><Value>"+ runStatus +"</Value></Field>\r\n" + 
				"<Field Name=\"subtype-id\"><Value>hp.qc.run.MANUAL</Value></Field>\r\n" + 
				"</Fields>\r\n" + 
				"<RelatedEntities/>\r\n" + 
				"</Entity>"; 
		logger.info("XML data compiled = \n" +xmlDataToHP);
		connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/xml");
		connection.setRequestProperty  ("Authorization",credEncodedString);
		connection.setRequestProperty("cookie", AuthMap.get("Set-Cookie").toString() + SessionMap.get("Set-Cookie").toString());
		connection.setDoOutput(true);
		OutputStreamWriter osw = new OutputStreamWriter(connection.getOutputStream());
		osw.write(xmlDataToHP);
		osw.flush();
		osw.close();
		System.out.println("cookie sent to server is = " +SessionMap.get("Set-Cookie").toString());
		System.out.println("req method ==" +connection.getRequestMethod());
		System.out.println("all header fields == " +connection.getHeaderFields());
		System.out.println("RESPONSE Code ==" +connection.getResponseCode());
		System.out.println("RESPONSE message ==" +connection.getResponseMessage());
		System.out.println("RESPONSE message ==" +connection.getResponseMessage());
		// 201
		if(connection.getResponseCode() != 201)
		{
			logger.error("SYSTEM thrown an error. Please check the prerequisites");
			logger.error("RESPONSE Code == "+ connection.getResponseCode() + "RESPONSE message ==" +connection.getResponseMessage());
		}
		else
		{
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			while((respMsg = in.readLine()) != null) 
			{
				logger.info("XML received is = \n" +respMsg);
				logger.info("RUN CREATED SUCCESSFULLY with Run ID == " + XMLDataExtractor(respMsg, "<Field Name=\"id\"><Value>(.*?)</Value></Field><Field Name=\"ver-stamp\">"));
			}
		}
		respCode = connection.getResponseCode();
	}

	private static void AlmLogout() throws IOException
	{
		respCode = 0;
		logger.info("\n\n=-=-=-=-=-LOGOUT ACTIVITY=-=-=-=-=-=-=-=-=--=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");
		URL url = new URL (almUrl + "/qcbin/authentication-point/logout");
		logger.info(url);
		connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");
		connection.setRequestProperty("Content Type", "application/xml");
		connection.setRequestProperty  ("Authorization",credEncodedString);
		logger.info("req method ==" +connection.getRequestMethod());
		logger.info("all header fields == " +connection.getHeaderFields());
		logger.info("RESPONSE Code ==" +connection.getResponseCode());
		logger.info("RESPONSE message ==" +connection.getResponseMessage());
		respCode = connection.getResponseCode();
		if(respCode == 200)
		{
			logger.info("Logout successful");
		}
		else
		{
			logger.info("Not logged out. some issue in overall execution of REST API");
		}
	}
}
