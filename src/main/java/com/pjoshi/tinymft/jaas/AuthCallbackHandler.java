package com.pjoshi.tinymft.jaas;

import java.io.IOException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextInputCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

public class AuthCallbackHandler implements CallbackHandler {
	private String username = null;
	private String password = null;
	private PublicKey key = null;
	private Module module = null;
	private X509Certificate[] certificates = null;
	
	public AuthCallbackHandler(String username, String password, PublicKey key, Module module,
			X509Certificate []certificates) throws CertificateException, IOException {
		this.username = username;
		this.module = module;
		
		if(password != null)
			this.password = password;
		
		if(key != null)
			this.key = key;
		
		this.certificates = certificates;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public PublicKey getKey() {
		return key;
	}

	public void setKey(PublicKey key) {
		this.key = key;
	}

	public X509Certificate[] getCertificates() {
		return certificates;
	}

	public void setCertificates(X509Certificate[] certificates) {
		this.certificates = certificates;
	}

	public Module getModule() {
		return module;
	}

	public void setModule(Module module) {
		this.module = module;
	}

	@Override
	public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
		for (int i = 0; i < callbacks.length; i++) {
			if (callbacks[i] instanceof NameCallback) {
				NameCallback nameCallback = (NameCallback) callbacks[i];
				nameCallback.setName(username);
			} else if (callbacks[i] instanceof PasswordCallback) {
				PasswordCallback passwordCallback = (PasswordCallback) callbacks[i];
				passwordCallback.setPassword((password == null)? null : password.toCharArray());
			} else if (callbacks[i] instanceof PublicKeyCallback) {
				PublicKeyCallback publickeyCallback = (PublicKeyCallback) callbacks[i];
				publickeyCallback.setKey(key);
			} else if (callbacks[i] instanceof CertificateCallback) {
				CertificateCallback certificateCallback = (CertificateCallback) callbacks[i];
				certificateCallback.setCertificates(certificates);
			} else if (callbacks[i] instanceof TextInputCallback) {
				TextInputCallback textinputCallback = (TextInputCallback) callbacks[i];
				textinputCallback.setText(module.toString());
			}
			else {
				throw new UnsupportedCallbackException(callbacks[i], "The submitted Callback is unsupported");
			}
		}
	}

}
