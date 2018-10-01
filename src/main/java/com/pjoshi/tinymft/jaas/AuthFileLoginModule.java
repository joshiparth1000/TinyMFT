package com.pjoshi.tinymft.jaas;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextInputCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthFileLoginModule extends AbstractLoginModule {
	private CallbackHandler callbackHandler = null;
	private boolean succeeded = false;
	private HashMap<String, String> authmap = null;
	private static final Logger logger = LoggerFactory.getLogger(AuthFileLoginModule.class);
	private static CertificateFactory factory = null;
	
	static {
		try {
			factory = CertificateFactory.getInstance("X.509");
		} catch (CertificateException e) {
			logger.error("Unable to load X509 certificate factory");
		}
	}
	
	@Override
	public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
			Map<String, ?> options) {
		this.callbackHandler = callbackHandler;
		this.authmap = new HashMap<String, String>();
		
		try {
			List<String> lines = FileUtils.readLines(new File((String) options.get("authfile")), "UTF-8");
			for(String line : lines) {
				String []tokens = line.split("\\|", -1);
				
				if(tokens.length < 2)
					continue;
				
				authmap.put(tokens[0], tokens[1] + "|" + tokens[2]);
			}
		} catch (IOException e) {
			logger.error("Error loading authentication file : ", e);
		}
	}
	
	private X509Certificate getCertificate(String file) throws CertificateException, IOException {
		X509Certificate x509cert = null;
		
		if(factory != null) {
			FileInputStream is = new FileInputStream(file);
			x509cert = (X509Certificate) factory.generateCertificate(is);
			is.close();
		}
		
		return x509cert;
	}
	
	protected boolean checkPassword(String password, PasswordCallback passwordCallback) {
		return password.equals(new String(passwordCallback.getPassword()));
	}

	@Override
	public boolean login() throws LoginException {
		if (callbackHandler == null) {
			throw new LoginException("Oops, callbackHandler is null");
		}
		
		if (authmap == null || authmap.isEmpty()) {
			throw new LoginException("Oops, authentication map is empty");
		}
		
		Callback[] callbacks = new Callback[5];
		callbacks[0] = new NameCallback("name:");
		callbacks[1] = new PasswordCallback("password:", false);
		callbacks[2] = new PublicKeyCallback("key:");
		callbacks[3] = new CertificateCallback("certificates:");
		callbacks[4] = new TextInputCallback("protocol:");
		
		try {
			callbackHandler.handle(callbacks);
		} catch (IOException e) {
			throw new LoginException("Oops, IOException calling handle on callbackHandler");
		} catch (UnsupportedCallbackException e) {
			throw new LoginException("Oops, UnsupportedCallbackException calling handle on callbackHandler");
		}
		
		NameCallback nameCallback = (NameCallback) callbacks[0];
		PasswordCallback passwordCallback = (PasswordCallback) callbacks[1];
		PublicKeyCallback publickeyCallback = (PublicKeyCallback) callbacks[2];
		CertificateCallback certificateCallback = (CertificateCallback) callbacks[3];
		TextInputCallback textinputCallback = (TextInputCallback) callbacks[4];
		
		String authfactors = authmap.get(nameCallback.getName());
		if(authfactors != null) {
			String password = authfactors.split("\\|", -1)[0];
			if(!password.isEmpty()) {
				Module module = Module.valueOf(textinputCallback.getText());
				X509Certificate cert = null;
				if(!authfactors.split("\\|", -1)[1].isEmpty()) {
					try {
						cert = getCertificate(authfactors.split("\\|", -1)[1]);
					} catch (CertificateException | IOException e) {
						throw new LoginException("Unable to load login certificate");
					}
				}
				
				succeeded = 
						authenticate(module, nameCallback.getName(), password, passwordCallback, publickeyCallback,
								cert, certificateCallback);
			}
		}

		if (succeeded)
			logger.debug("Success! You get to log in!");
		else
			logger.debug("Failure! You don't get to log in");
		
		return succeeded;
	}

	@Override
	public boolean commit() throws LoginException {
		return succeeded;
	}

	@Override
	public boolean abort() throws LoginException {
		return succeeded;
	}

	@Override
	public boolean logout() throws LoginException {
		return false;
	}

}
