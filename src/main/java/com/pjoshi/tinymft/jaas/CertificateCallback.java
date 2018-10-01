package com.pjoshi.tinymft.jaas;

import java.security.cert.X509Certificate;

import javax.security.auth.callback.Callback;

public class CertificateCallback implements Callback {
	private String prompt = null;
	private X509Certificate[] certificates = null;
	
	public CertificateCallback(String prompt) {
		super();
		this.prompt = prompt;
	}

	public String getPrompt() {
		return prompt;
	}

	public void setPrompt(String prompt) {
		this.prompt = prompt;
	}

	public X509Certificate[] getCertificates() {
		return certificates;
	}

	public void setCertificates(X509Certificate[] certificates) {
		this.certificates = certificates;
	}
}
