package com.pjoshi.tinymft;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Properties;
import java.util.UUID;

import org.jasypt.util.text.BasicTextEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Settings {
	public static String SECRET = null;
	public static String JAASCONFIG = null;
	public static String JAASDOMAIN = null;
	public static String BINDIP = null;
	public static int SSHPORT = 22;
	public static int FTPPORT = 22;
	public static int HTTPPORT = 8080;
	public static String KEYSTORE = null;
	public static String KEYSTOREPASSWORD = null;
	public static String KEYPASSWORD = null;
	public static String KEYALIAS = null;
	public static String TRUSTSTORE = null;
	public static String TRUSTSTOREPASSWORD = null;
	public static String ROOTFOLDER = null;
	public static String ROUTESFOLDER = null;
	public static boolean DUALAUTH = false;
	
	private static Logger logger = LoggerFactory.getLogger(Settings.class);
	
	public static void load() throws IOException {
		Properties prop = new Properties();
		InputStream input = new FileInputStream("conf" + File.separator + "config.properties");
		prop.load(input);
			
		SECRET = prop.getProperty("SECRET");
		JAASCONFIG = prop.getProperty("JAASCONFIG");
		JAASDOMAIN = prop.getProperty("JAASDOMAIN");
		BINDIP = prop.getProperty("BINDIP");
		SSHPORT = Integer.parseInt(prop.getProperty("SSHPORT"));
		FTPPORT = Integer.parseInt(prop.getProperty("FTPPORT"));
		HTTPPORT = Integer.parseInt(prop.getProperty("HTTPPORT"));
		KEYSTORE = prop.getProperty("KEYSTORE");
		KEYSTOREPASSWORD = prop.getProperty("KEYSTOREPASSWORD");
		KEYPASSWORD = prop.getProperty("KEYPASSWORD");
		KEYALIAS = prop.getProperty("KEYALIAS");
		TRUSTSTORE = prop.getProperty("TRUSTSTORE");
		TRUSTSTOREPASSWORD = prop.getProperty("TRUSTSTOREPASSWORD");
		ROOTFOLDER = prop.getProperty("ROOTFOLDER");
		ROUTESFOLDER = prop.getProperty("ROUTESFOLDER");
		DUALAUTH = Boolean.parseBoolean(prop.getProperty("DUALAUTH"));
		
		if(SECRET == null) {
			logger.debug("Generating secret key");
			BasicTextEncryptor  textencryptor = new BasicTextEncryptor();
			textencryptor.setPassword(Base64.getEncoder().encodeToString(UUID.randomUUID().toString().getBytes()));
			SECRET = textencryptor.encrypt(Base64.getEncoder().encodeToString(UUID.randomUUID().toString().getBytes()));
		}
		
		Main.textencryptor.setPassword(SECRET);
		
		if(prop.getProperty("SECRET") == null) {
			KEYSTOREPASSWORD = Main.textencryptor.encrypt(KEYSTOREPASSWORD);
			KEYPASSWORD = Main.textencryptor.encrypt(KEYPASSWORD);
			TRUSTSTOREPASSWORD = Main.textencryptor.encrypt(TRUSTSTOREPASSWORD);
			
			prop.setProperty("SECRET", SECRET);
			prop.setProperty("KEYSTOREPASSWORD", KEYSTOREPASSWORD);
			prop.setProperty("KEYPASSWORD", KEYPASSWORD);
			prop.setProperty("TRUSTSTOREPASSWORD", TRUSTSTOREPASSWORD);
			prop.store(new FileOutputStream("conf" + File.separator + "config.properties"), null);
		}
		
		logger.debug("SECRET : " + SECRET);
		logger.debug("JAASCONFIG : " + JAASCONFIG);
		logger.debug("JAASDOMAIN : " + JAASDOMAIN);
		logger.debug("BINDIP : " + BINDIP);
		logger.debug("SSHPORT : " + SSHPORT);
		logger.debug("FTPPORT : " + FTPPORT);
		logger.debug("HTTPPORT : " + HTTPPORT);
		logger.debug("KEYSTORE : " + KEYSTORE);
		logger.debug("KEYSTOREPASSWORD : " + KEYSTOREPASSWORD);
		logger.debug("KEYPASSWORD : " + KEYPASSWORD);
		logger.debug("KEYALIAS : " + KEYALIAS);
		logger.debug("TRUSTSTORE : " + TRUSTSTORE);
		logger.debug("TRUSTSTOREPASSWORD : " + TRUSTSTOREPASSWORD);
		logger.debug("ROOTFOLDER : " + ROOTFOLDER);
		logger.debug("ROUTESFOLDER : " + ROUTESFOLDER);
		logger.debug("DUALAUTH : " + DUALAUTH);
		
		KEYSTOREPASSWORD = Main.textencryptor.decrypt(KEYSTOREPASSWORD);
		KEYPASSWORD = Main.textencryptor.decrypt(KEYPASSWORD);
		TRUSTSTOREPASSWORD = Main.textencryptor.decrypt(TRUSTSTOREPASSWORD);
	}
}
