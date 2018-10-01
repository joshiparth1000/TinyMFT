package com.pjoshi.tinymft;

import java.io.File;
import java.io.IOException;
import java.security.Security;

import org.apache.log4j.PropertyConfigurator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jasypt.util.text.BasicTextEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pjoshi.tinymft.camel.CamelDaemon;
import com.pjoshi.tinymft.ftp.FTPDaemon;
import com.pjoshi.tinymft.http.HTTPDaemon;
import com.pjoshi.tinymft.ssh.SSHDaemon;

public class Main {
	private static Logger logger = LoggerFactory.getLogger(Main.class);
	public static BasicTextEncryptor  textencryptor = new BasicTextEncryptor();
	
	static {
		PropertyConfigurator.configure("conf" + File.separator + "log4j.properties");
		Security.addProvider(new BouncyCastleProvider());
		
		try {
			Settings.load();
		} catch (IOException e) {
			logger.error("Error loading configuration : ", e);
			System.exit(1);
		}
		
		System.setProperty("java.security.auth.login.config", Settings.JAASCONFIG);
		if(Settings.SSHPORT != 0)
			SSHDaemon.init();
		if(Settings.FTPPORT != 0)
			FTPDaemon.init();
		if(Settings.HTTPPORT != 0)
			HTTPDaemon.init();
		CamelDaemon.init();
	}

	public static void main(String[] args) {
		try {
			CamelDaemon.start();
			if(Settings.SSHPORT != 0)
				SSHDaemon.start();
			if(Settings.FTPPORT != 0)
				FTPDaemon.start();
			if(Settings.HTTPPORT != 0)
				HTTPDaemon.start();
		} catch (Exception e) {
			logger.error("Error starting TinyMFT : ", e);
			System.exit(1);
		}
		
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

            @Override
            public void run() {
            	logger.info("Stopping TinyMFT");
                try {
                	if(SSHDaemon.isStarted())
                		SSHDaemon.stop();
                	if(FTPDaemon.isStarted())
                		FTPDaemon.stop();
                	if(HTTPDaemon.isStarted())
                		HTTPDaemon.stop();
                	HibernateUtil.shutdown();
					CamelDaemon.stop();
				} catch (Exception e) {
					logger.error("Error stopping TinyMFT : ", e);
				}
            }
		}));
	}

}
