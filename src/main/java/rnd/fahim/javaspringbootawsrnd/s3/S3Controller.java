package rnd.fahim.javaspringbootawsrnd.s3;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class S3Controller {

  private final S3Service s3Service;

  public S3Controller(S3Service s3Service) {
    this.s3Service = s3Service;
  }

  @PostMapping
  public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
    String result = s3Service.uploadFile(file);
    return ResponseEntity.ok("File uploaded successfully: " + result);
  }

  @PostMapping("/stream")
  public ResponseEntity<String> uploadFileStream(@RequestParam("file") MultipartFile file) {
    String result = s3Service.uploadFileStream(file);
    return ResponseEntity.ok("File uploaded successfully: " + result);
  }

  @GetMapping("/{fileName}")
  public String getPresignedUrl(@PathVariable String fileName) {
    return s3Service.getPreSignedURL(fileName);
  }

  @GetMapping("/download/{fileName}")
  public void downloadFile(@PathVariable String fileName, HttpServletResponse response) {
    s3Service.downloadFile(fileName, response);
  }

  @DeleteMapping("/{fileName}")
  public boolean deleteFile(@PathVariable String fileName) {
    return s3Service.deleteFile(fileName);
  }
}
