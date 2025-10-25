package rnd.fahim.javaspringbootawsrnd.s3;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.time.Duration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Service
public class S3Service {

  private static final Log log = LogFactory.getLog(S3Service.class);
  private final S3Client s3Client;
  private final S3Presigner s3presigner;

  @Value("${s3BucketName}")
  private String bucketName;

  public S3Service(S3Client s3Client, S3Presigner s3presigner) {
    this.s3Client = s3Client;
    this.s3presigner = s3presigner;
  }

  public String uploadFile(MultipartFile file) {
    String filename = file.getOriginalFilename();

    if (StringUtils.isEmpty(filename)) {
      return "No file name";
    }

    filename = file.getOriginalFilename().substring(0, filename.lastIndexOf("."));

    try {
      log.info("Uploading file to S3: " + filename);
      PutObjectRequest putObjectRequest =
          PutObjectRequest.builder()
              .bucket(bucketName)
              .key(filename)
              .contentType(file.getContentType())
              .build();

      s3Client.putObject(
          putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

      log.info("File uploaded to S3: " + filename);
      return filename;
    } catch (IOException e) {
      log.error(e.getMessage());
      throw new RuntimeException("Failed to upload file to S3", e);
    }
  }

  public String uploadFileStream(MultipartFile file) {
    String filename = file.getOriginalFilename();

    if (StringUtils.isEmpty(filename)) {
      return "No file name";
    }

    try (InputStream inputStream = file.getInputStream()) {

      PutObjectRequest request =
          PutObjectRequest.builder()
              .bucket(bucketName)
              .key(filename)
              .contentType(file.getContentType())
              .build();

      s3Client.putObject(request, RequestBody.fromInputStream(inputStream, file.getSize()));
      log.info("File uploaded to S3: " + filename);
      return filename;
    } catch (IOException e) {
      log.error(e.getMessage());
      throw new RuntimeException("Failed to upload file", e);
    }
  }

  public String getPreSignedURL(String fileName) {
    try {
      GetObjectPresignRequest request =
          GetObjectPresignRequest.builder()
              .signatureDuration(Duration.ofMinutes(60))
              .getObjectRequest(GetObjectRequest.builder().bucket(bucketName).key(fileName).build())
              .build();

      URL url = s3presigner.presignGetObject(request).url();

      return url.toString();
    } catch (Exception e) {
      log.error(e.getMessage());
      return null;
    }
  }

  public void downloadFile(String fileName, HttpServletResponse response) {
    GetObjectRequest getRequest =
        GetObjectRequest.builder().bucket(bucketName).key(fileName).build();

    try (ResponseInputStream<GetObjectResponse> s3Stream = s3Client.getObject(getRequest);
        OutputStream out = response.getOutputStream()) {

      response.setContentType(
          s3Stream.response().contentType() != null
              ? s3Stream.response().contentType()
              : "application/octet-stream");
      response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

      byte[] buffer = new byte[8192];
      int bytesRead;
      while ((bytesRead = s3Stream.read(buffer)) != -1) {
        out.write(buffer, 0, bytesRead);
      }
      out.flush();

    } catch (IOException e) {
      log.error(e.getMessage());
      throw new RuntimeException("Failed to download file from S3", e);
    } catch (S3Exception e) {
      log.error(e.getMessage());
      if (e.statusCode() == 404) {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      } else {
        throw e;
      }
    }
  }

  public boolean deleteFile(String keyName) {
    log.info("Deleting file from S3: " + keyName);
    DeleteObjectRequest deleteRequest =
        DeleteObjectRequest.builder().bucket(bucketName).key(keyName).build();

    DeleteObjectResponse response = s3Client.deleteObject(deleteRequest);

    return response.sdkHttpResponse().isSuccessful();
  }
}
