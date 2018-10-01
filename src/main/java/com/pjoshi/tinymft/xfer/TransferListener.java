package com.pjoshi.tinymft.xfer;

public interface TransferListener {
	public void onStart(Transfer transfer);
	
	public void Inprogress(Transfer transfer);
	
	public void onAbort(Transfer transfer);
	
	public void onEnd(Transfer transfer);
}
