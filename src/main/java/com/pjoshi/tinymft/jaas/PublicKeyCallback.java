package com.pjoshi.tinymft.jaas;

import java.security.PublicKey;

import javax.security.auth.callback.Callback;

public class PublicKeyCallback implements Callback {
	private String prompt = null;
	private PublicKey key = null;
	
	public PublicKeyCallback(String prompt) {
		super();
		this.prompt = prompt;
	}

	public PublicKey getKey() {
		return key;
	}

	public void setKey(PublicKey key) {
		this.key = key;
	}

	public String getPrompt() {
		return prompt;
	}

	public void setPrompt(String prompt) {
		this.prompt = prompt;
	}
}
