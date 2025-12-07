package rnd.fahim.javaspringbootawsrnd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import rnd.fahim.javaspringbootawsrnd.secrets.ExternalServiceSecretManager;
import rnd.fahim.javaspringbootawsrnd.secrets.ExternalServiceSecrets;

import java.util.Map;

@SpringBootApplication
public class JavaSpringBootAwsRndApplication implements ApplicationListener<ApplicationReadyEvent> {


    public static void main(String[] args) {
        SpringApplication.run(JavaSpringBootAwsRndApplication.class, args);
    }

    private final ExternalServiceSecretManager externalServiceSecretManager;

    public JavaSpringBootAwsRndApplication(ExternalServiceSecretManager externalServiceSecretManager) {
        this.externalServiceSecretManager = externalServiceSecretManager;
    }

    @Override
    public void onApplicationEvent(final ApplicationReadyEvent event) {
        ExternalServiceSecrets externalServiceSecrets = externalServiceSecretManager.getExternalServiceSecrets();
        System.out.println("External service secrets loaded successfully:");

        int maxRetryCount = Integer.parseInt(externalServiceSecretManager.getParameter("maxRetryCount", false));

        System.out.println(maxRetryCount);

        Map<String, String> params = externalServiceSecretManager.getParametersByPath("/dev/");

        System.out.println(params);
    }
}
