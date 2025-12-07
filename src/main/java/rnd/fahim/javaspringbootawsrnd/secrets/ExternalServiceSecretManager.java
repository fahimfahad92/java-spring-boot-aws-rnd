package rnd.fahim.javaspringbootawsrnd.secrets;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathRequest;
import software.amazon.awssdk.services.ssm.model.SsmException;

import java.util.HashMap;
import java.util.Map;

@Component
public class ExternalServiceSecretManager {

    private static final Logger log = LoggerFactory.getLogger(ExternalServiceSecretManager.class);

    @Value("${externalServiceSecrets}")
    private String externalServiceSecrets;

    private final ObjectMapper objectMapper;
    private final SecretsManagerClient secretsClient;
    private final SsmClient ssmClient;

    private final Region region = Region.of("ap-southeast-1");

    public ExternalServiceSecretManager(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.secretsClient = SecretsManagerClient.builder().region(region).build();
        this.ssmClient = SsmClient.builder().region(region).build();
    }

    public ExternalServiceSecrets getExternalServiceSecrets() {
        try {
            GetSecretValueRequest externalServiceSecretsRequest =
                    GetSecretValueRequest.builder().secretId(externalServiceSecrets).build();

            GetSecretValueResponse response =
                    secretsClient.getSecretValue(externalServiceSecretsRequest);

            String json = response.secretString();
            return objectMapper.readValue(json, ExternalServiceSecrets.class);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public String getParameter(String name, boolean decrypt) {
        try {
            GetParameterRequest request =
                    GetParameterRequest.builder().name(name).withDecryption(decrypt).build();

            return ssmClient.getParameter(request).parameter().value();
        } catch (SsmException e) {
            throw new RuntimeException("Failed to load parameter " + name, e);
        }
    }

    public Map<String, String> getParametersByPath(String pathPrefix) {
        try {
            Map<String, String> params = new HashMap<>();

            var request =
                    GetParametersByPathRequest.builder()
                            .path(pathPrefix)
                            .recursive(true)
                            .withDecryption(true)
                            .build();

            ssmClient.getParametersByPathPaginator(request).stream()
                    .iterator()
                    .next()
                    .parameters()
                    .forEach(p -> params.put(p.name(), p.value()));

            return params;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load parameter " + e.getMessage());
        }
    }
}
