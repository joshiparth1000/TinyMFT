package com.pjoshi.tinymft.jaas;

public enum Module {
	SSH_KEY("SSH_KEY"), SSH_PASSWORD("SSH_PASSWORD"), FTP("FTP"), HTTP("HTTP");
	
	private String module;
	
	private Module(String module) {
		this.module = module;
	}
	
	public String toString() {
		return module;
	}
}
