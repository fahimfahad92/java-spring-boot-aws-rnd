package rnd.fahim.javaspringbootawsrnd.sqs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
public class SqsConfig {

  private final Region region = Region.AP_SOUTHEAST_1;

  @Bean
  public SqsClient sqsClient() {
    return SqsClient.builder().region(region).build();
  }
}
