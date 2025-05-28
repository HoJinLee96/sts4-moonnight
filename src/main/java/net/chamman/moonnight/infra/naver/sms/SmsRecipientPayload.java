package net.chamman.moonnight.infra.naver.sms;

public class SmsRecipientPayload {
	
	private String to;
	private String content;
	
	public SmsRecipientPayload() {
	}
	
	public SmsRecipientPayload(String to) {
		this.to = to;
	}
	
	public SmsRecipientPayload(String to, String content) {
		this.to = to;
		this.content = content;
	}
	
	public String getTo() {
		return to;
	}
	
	public String getContent() {
		return content;
	}
	
}
