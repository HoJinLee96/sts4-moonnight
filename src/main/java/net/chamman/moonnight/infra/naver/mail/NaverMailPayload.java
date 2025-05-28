package net.chamman.moonnight.infra.naver.mail;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class NaverMailPayload {
	String senderAddress;
	String title;
	String body;
	List<MailRecipientPayload> recipients;
	boolean individual;
	boolean advertising;
}
