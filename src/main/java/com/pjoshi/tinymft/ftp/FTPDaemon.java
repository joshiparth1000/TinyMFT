package com.pjoshi.tinymft.ftp;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authentication;
import org.apache.ftpserver.ftplet.AuthenticationFailedException;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.Ftplet;
import org.apache.ftpserver.ftplet.User;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.ssl.SslConfigurationFactory;
import org.apache.ftpserver.usermanager.UsernamePasswordAuthentication;
import org.apache.ftpserver.usermanager.impl.AbstractUserManager;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.ConcurrentLoginPermission;
import org.apache.ftpserver.usermanager.impl.TransferRatePermission;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pjoshi.tinymft.Settings;
import com.pjoshi.tinymft.jaas.AuthCallbackHandler;
import com.pjoshi.tinymft.jaas.Module;

public class FTPDaemon {
	private static FtpServer server = null;
	private static Logger logger = LoggerFactory.getLogger(FTPDaemon.class);
	
	public static void init() {
		FtpServerFactory serverFactory = new FtpServerFactory();
		serverFactory.setFtplets(new HashMap<String,Ftplet>() {
			private static final long serialVersionUID = 7768998996071363992L;

			{
				    put("ftpcommands", new FTPCommandListener());
				 }
			}
		);
		ListenerFactory factory = new ListenerFactory();
		factory.setServerAddress(Settings.BINDIP);
		factory.setPort(Settings.FTPPORT);
		SslConfigurationFactory ssl = new SslConfigurationFactory();
		ssl.setKeystoreFile(new File(Settings.KEYSTORE));
		ssl.setKeystorePassword(Settings.KEYSTOREPASSWORD);
		ssl.setKeyPassword(Settings.KEYPASSWORD);
		ssl.setKeyAlias(Settings.KEYALIAS);
		ssl.setTruststoreFile(new File(Settings.TRUSTSTORE));
		ssl.setTruststorePassword(Settings.TRUSTSTOREPASSWORD);
		
		if(Settings.DUALAUTH)
			ssl.setClientAuthentication("NEED");
		else
			ssl.setClientAuthentication("WANT");
		
		factory.setSslConfiguration(ssl.createSslConfiguration());
		serverFactory.addListener("default", factory.createListener());
		serverFactory.setUserManager(new JAASUserManager());
		
		server = serverFactory.createServer(); 
	}
	
	public static void start() throws FtpException {
		logger.info("Starting FTP Daemon");
		server.start();
	}
	
	public static void stop() {
		logger.info("Stopping FTP Daemon");
		server.stop();
	}
	
	public static boolean isStarted() {
		Socket s = null;
		try {
			s = new Socket(Settings.BINDIP, Settings.FTPPORT);
			if(s != null)
				s.close();
			
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	public static boolean isStopped() {
		return server.isStopped();
	}
	
	static class JAASUserManager extends AbstractUserManager {
		private static Logger logger = LoggerFactory.getLogger(JAASUserManager.class);

		@Override
		public User authenticate(Authentication authentication) throws AuthenticationFailedException {
			User user = null;
			
			if (authentication instanceof UsernamePasswordAuthentication) {
				UsernamePasswordAuthentication upauth = (UsernamePasswordAuthentication) authentication;
	            String username = upauth.getUsername();
	            String password = upauth.getPassword();
	            X509Certificate[] certificates = null;
	            
	            try {
	            	if(upauth.getUserMetadata().getCertificateChain() != null && 
		            		upauth.getUserMetadata().getCertificateChain().length > 0) {
		    			certificates = new X509Certificate[upauth.getUserMetadata().getCertificateChain().length];
		    			CertificateFactory cf = CertificateFactory.getInstance("X.509");
		    			
		    			for(int i=0;i<certificates.length;i++) {
		    				ByteArrayInputStream bis = 
		    						new ByteArrayInputStream(upauth.getUserMetadata().getCertificateChain()[i].getEncoded());
		    				certificates[i] = (X509Certificate) cf.generateCertificate(bis);
		    				bis.close();
		    			}
		    		}
	            	
					LoginContext lc = new LoginContext(Settings.JAASDOMAIN, new AuthCallbackHandler(username, password, null,
							Module.FTP, certificates));
					lc.login();
					user = getUserByName(username);
				} catch (LoginException | CertificateException | IOException e) {
					logger.error("Error authenticating : ", e);
					throw new AuthenticationFailedException(e);
				}
			} else
	            throw new IllegalArgumentException("Authentication not supported by this user manager");
			
			return user;
		}

		@Override
		public void delete(String username) throws FtpException {
			
		}

		@Override
		public boolean doesExist(String username) throws FtpException {
			return false;
		}

		@Override
		public String[] getAllUserNames() throws FtpException {
			return null;
		}

		@Override
		public User getUserByName(String username) {
			BaseUser user = new BaseUser();
			List<Authority> authorities = new ArrayList<Authority>();
			
			authorities.add(new WritePermission());
			authorities.add(new ConcurrentLoginPermission(0, 0));
			authorities.add(new TransferRatePermission(0, 0));
			user.setName(username);
			user.setEnabled(true);
			user.setHomeDirectory(Settings.ROOTFOLDER + File.separator + username);
			user.setAuthorities(authorities);
			
			return user;
		}

		@Override
		public void save(User arg0) throws FtpException {
			
		}
	}
}
