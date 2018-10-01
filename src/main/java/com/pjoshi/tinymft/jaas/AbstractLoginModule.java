package com.pjoshi.tinymft.jaas;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.spi.LoginModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pjoshi.tinymft.Settings;

public abstract class AbstractLoginModule implements LoginModule {
	private static final Logger logger = LoggerFactory.getLogger(AbstractLoginModule.class);
	
	protected abstract boolean checkPassword(String password, PasswordCallback passwordCallback);
	
	protected boolean authenticate(Module module, String username, String password, PasswordCallback passwordCallback,
			PublicKeyCallback publickeyCallback, X509Certificate cert, CertificateCallback certificateCallback) {
		boolean succeeded = false;
		
		if(!Settings.DUALAUTH || module == Module.SSH_KEY || module == Module.SSH_PASSWORD) {
			if(module == Module.SSH_PASSWORD || module == Module.FTP || module == Module.HTTP)
				succeeded = checkPassword(password, passwordCallback);
			else if(module == Module.SSH_KEY) {
				PublicKey key = publickeyCallback.getKey();
				succeeded = Arrays.equals(key.getEncoded(), cert.getPublicKey().getEncoded());
			}
		}
		
		if(Settings.DUALAUTH && (module == Module.FTP || module == Module.HTTP)) {
			X509Certificate x509cert = certificateCallback.getCertificates()[0];
			try {
				succeeded = Arrays.equals(cert.getEncoded(), x509cert.getEncoded()) && 
						checkPassword(password, passwordCallback);
			} catch (CertificateEncodingException e) {
				logger.error("Error comparing certificates : ", e);
			}
		}
		
		if(succeeded) {
			File homedir = new File(Settings.ROOTFOLDER + File.separator + username);
			if(!homedir.exists())
				try {
					Files.createDirectories(homedir.toPath());
				} catch (IOException e) {
					logger.error("Error creating home directory for " + username, e);
					succeeded = false;
				}
		}
		
		return succeeded;
	}
}
