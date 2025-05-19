package net.chamman.moonnight.infra.s3;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AwsS3Service {

  private final AwsS3Cilent awsS3Cilent;

  public List<String> uploadEstimateImages(List<MultipartFile> images, String phone) throws IOException {
    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    String keyPrefix = "estimateImages/" + timestamp + "_" + phone.replace("-", "") + "/";
    return awsS3Cilent.uploadFiles(images, keyPrefix);
  }
  
  public void deleteEstimateImages(List<String> imagesPath) {
    for(String path : imagesPath) {
        if(!path.startsWith("estimateImages/")) {
          throw new IllegalStateException("AWSS3 이미지 조회 실패 : 데이터 형식 부적합.");
        }
    }
    awsS3Cilent.deleteFiles(imagesPath.toArray(String[]::new));
  }

//  public List<File> getEstimateImages(String imagesPath) throws IOException {
//    String[] paths = imagesPath.split(",");
//    for(String path : paths) {
//      if(!path.startsWith("estimateImages/")) {
//        throw new IllegalArgumentException("AWSS3 이미지 조회 실패 : 데이터 형식 부적합.");
//      }
//    }
//    List<File> imagesFile = awsS3Dao.getFiles(paths);
//    if(imagesFile.isEmpty()) {
//      throw new IllegalArgumentException("AWSS3 이미지 조회 실패 : 경로에 일치하는 파일 없음.");
//    }
//    return imagesFile;
//  }
  

}
