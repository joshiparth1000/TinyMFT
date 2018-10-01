package com.pjoshi.tinymft.ssh;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collections;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.common.keyprovider.KeyPairProvider;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.AsyncAuthException;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.password.PasswordChangeRequiredException;
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pjoshi.tinymft.Settings;
import com.pjoshi.tinymft.jaas.AuthCallbackHandler;
import com.pjoshi.tinymft.jaas.Module;

public class SSHDaemon {
	private static Logger logger = LoggerFactory.getLogger(SSHDaemon.class);
	private static SshServer sshd = null;
	
	public static void init() {
		sshd = SshServer.setUpDefaultServer();
		sshd.setHost(Settings.BINDIP);
		sshd.setPort(Settings.SSHPORT);
		sshd.setKeyPairProvider(new KeyPairProvider() {
			
			@Override
			public Iterable<KeyPair> loadKeys() {
				ArrayList<KeyPair> kps = new ArrayList<KeyPair>();
				FileInputStream is = null;
				
				try {
					is = new FileInputStream(Settings.KEYSTORE);
					KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
					keystore.load(is, Settings.KEYSTOREPASSWORD.toCharArray());
					Key key = keystore.getKey(Settings.KEYALIAS, Settings.KEYPASSWORD.toCharArray());
					if(key instanceof PrivateKey) {
						Certificate cert = keystore.getCertificate(Settings.KEYALIAS);
						PublicKey publicKey = cert.getPublicKey();
						kps.add(new KeyPair(publicKey, (PrivateKey) key));
					}
					is.close();
				} catch (Exception e) {
					logger.error("Error loading key pair : ", e);
				}
				
				return kps;
			}
		});
		
		sshd.setPasswordAuthenticator(new PasswordAuthenticator() {
			
			@Override
			public boolean authenticate(String username, String password, ServerSession session)
					throws PasswordChangeRequiredException, AsyncAuthException {
				if(Settings.DUALAUTH) {
					boolean ssh_key = (Boolean) session.getIoSession().getAttribute("ssh-key");
					if(!ssh_key)
						return false;
				}
				
				boolean succeeded = false;
				
				try {
					LoginContext lc = new LoginContext(Settings.JAASDOMAIN, new AuthCallbackHandler(username, password, null, Module.SSH_PASSWORD, null));
					lc.login();
					succeeded = true;
				} catch (LoginException | CertificateException | IOException e) {
					logger.error("Error authenticating : ", e);
				}
				
				return succeeded;
			}
		});
		
		sshd.setPublickeyAuthenticator(new PublickeyAuthenticator() {
			
			@Override
			public boolean authenticate(String username, PublicKey key, ServerSession session) throws AsyncAuthException {
				boolean succeeded = false;
				try {
					LoginContext lc = new LoginContext(Settings.JAASDOMAIN, new AuthCallbackHandler(username, null, key, Module.SSH_KEY,null));
					lc.login();
					succeeded = true;
				} catch (LoginException | CertificateException | IOException e) {
					logger.error("Error authenticating : ", e);
				}
				
				if(Settings.DUALAUTH) {
					session.getIoSession().setAttribute("ssh-key", succeeded);
					
					if(succeeded)
						return false;
				}
				
				return succeeded;
			}
		});
		
		SftpSubsystemFactory factory = new SftpSubsystemFactory();
		factory.addSftpEventListener(new SSHDSFTPListenerImpl());
		sshd.setSubsystemFactories(Collections.singletonList(factory));
		sshd.setFileSystemFactory(new VirtualFileSystemFactory() {
	        @Override
	        protected Path computeRootDir(Session session) throws IOException  {
	            String username = session.getUsername();
	            File homedir = new File(Settings.ROOTFOLDER + File.separator + username);
	            
	            return homedir.toPath();
	        }
		});
		sshd.addChannelListener(new SSHDChannelListenerImpl());
		sshd.addSessionListener(new SSHDSessionListenerImpl());
	}
	
	public static void start() throws IOException {
		logger.info("Starting SSH daemon");
		sshd.start();
	}
	
	public static void stop() throws IOException {
		logger.info("Stopping SSH daemon");
		sshd.stop();
	}
	
	public static boolean isStopped() {
		return sshd.isClosed();
	}
	
	public static boolean isStarted() {
		return sshd.isStarted();
	}
}
