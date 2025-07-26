package net.chamman.moonnight;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
class MoonnightApplicationTests {
	
	@Autowired
	PasswordEncoder passwordEncoder;

	@Test
	void contextLoads() {
		String a = passwordEncoder.encode("Leeought21!@");
		System.out.println(a);
	}
	

}
