package net.chamman.moonnight.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PrimaryConfig {

@Bean
public PasswordEncoder passwordEncoder() {
//비밀번호를 해시할 때 몇 번 반복해서 계산할지를 정하는 값
int strength = 12; 
return new BCryptPasswordEncoder(strength);
}

}