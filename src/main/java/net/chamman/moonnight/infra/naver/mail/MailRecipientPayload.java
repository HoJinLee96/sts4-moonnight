package net.chamman.moonnight.infra.naver.mail;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class MailRecipientPayload {
	
	String address;
	String name;
	String type;
	
}
