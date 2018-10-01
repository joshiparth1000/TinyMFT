package com.pjoshi.tinymft.ssh;

import org.apache.sshd.common.channel.Channel;
import org.apache.sshd.common.channel.ChannelListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SSHDChannelListenerImpl implements ChannelListener {
	private static Logger logger = LoggerFactory.getLogger(SSHDChannelListenerImpl.class);
	
	public void channelClosed(Channel channel, Throwable reason) {
		logger.debug("Channel closed");
	}
	
	public void channelInitialized(Channel channel) {
		logger.debug("Channel initialized");
	}
	
	public void channelOpenFailure(Channel channel, Throwable reason) {
		logger.debug("Channel open failure");
	}
	
	public void channelOpenSuccess(Channel channel) {
		logger.debug("Channel open success");
	}
	
	public void channelStateChanged(Channel channel, String hint) {
		logger.debug("Channel state changed : " + hint);
	}
}
