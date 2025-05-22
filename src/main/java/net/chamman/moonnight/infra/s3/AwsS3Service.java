package net.chamman.moonnight.infra.s3;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import net.chamman.moonnight.global.exception.infra.s3.S3DeleteException;
import net.chamman.moonnight.global.exception.infra.s3.S3UploadException;

@Service
@RequiredArgsConstructor
public class AwsS3Service {

	private final AwsS3Client awsS3Cilent;
	
	/** s3 견적서 이미지 업로드
	 * @param images
	 * @param phone
	 * @throws S3UploadException {@link AwsS3Client#uploadFile}
	 * @return
	 */
	public List<String> uploadEstimateImages(List<MultipartFile> images, String phone) {
		
		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
		String keyPrefix = "estimateImages/" + timestamp + "_" + phone.replace("-", "") + "/";
		
		return awsS3Cilent.uploadFiles(images, keyPrefix);
	}
	
	/** 견적서 이미지 경로의 이미지 삭제
	 * @param imagesPath
	 * @throws S3DeleteException {@link AwsS3Client#deleteFiles}
	 */
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
