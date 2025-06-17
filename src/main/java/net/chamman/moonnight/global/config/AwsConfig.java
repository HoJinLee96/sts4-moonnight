package net.chamman.moonnight.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AwsConfig {

    // application.properties에 설정한 값들을 가져온다.
    @Value("${aws.credentials.access-key}")
    private String accessKey;

    @Value("${aws.credentials.secret-key}")
    private String secretKey;

    @Value("${aws.region.static}")
    private String region;

    @Bean
    public S3Client s3Client() {
      return S3Client.builder()
          .region(Region.of(region))
          .credentialsProvider(
              StaticCredentialsProvider.create(
                  AwsBasicCredentials.create(accessKey, secretKey)))
          .build();
    }
}