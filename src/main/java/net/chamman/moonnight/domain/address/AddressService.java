package net.chamman.moonnight.domain.address;

import static net.chamman.moonnight.global.exception.HttpStatusCode.ADDRESSS_NOT_FOUND;
import static net.chamman.moonnight.global.exception.HttpStatusCode.AUTHORIZATION_FAILED;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.auth.crypto.Obfuscator;
import net.chamman.moonnight.domain.user.User;
import net.chamman.moonnight.domain.user.UserService;
import net.chamman.moonnight.global.exception.ForbiddenException;
import net.chamman.moonnight.global.exception.NoSuchDataException;
import net.chamman.moonnight.global.exception.StatusDeleteException;
import net.chamman.moonnight.global.exception.StatusStayException;
import net.chamman.moonnight.global.exception.StatusStopException;
import net.chamman.moonnight.global.exception.infra.DaumStateException;
import net.chamman.moonnight.global.exception.infra.IllegalAddressValueException;
import net.chamman.moonnight.infra.kakao.DaumMapClient;

@Service
@Slf4j
@RequiredArgsConstructor
public class AddressService {
	private final AddressRepository addressRepository;
	private final UserService userService;
	private final DaumMapClient daumMapClient;
	private final Obfuscator obfuscator;
	
	/** 주소 등록
	 * @param userId
	 * @param addressRequestDto
	 * 
	 * @throws IllegalAddressValueException {@link DaumMapClient#validateAddress} 일치하는 주소가 없음
	 * @throws DaumStateException {@link DaumMapClient#validateAddress} 다음 서버에서 응답 이상
	 * 
	 * @throws NoSuchDataException {@link UserService#getUserByUserId} 찾을 수 없는 유저
	 * @throws StatusStayException {@link UserService#getUserByUserId} 일시정지 유저
	 * @throws StatusStopException {@link UserService#getUserByUserId} 중지 유저
	 * @throws StatusDeleteException {@link UserService#getUserByUserId} 탈퇴 유저
	 * 
	 * @return
	 */
	@Transactional
	public AddressResponseDto registerAddress(int userId, AddressRequestDto addressRequestDto) {
		
		daumMapClient.validateAddress(addressRequestDto.postcode(), addressRequestDto.mainAddress());
		
		User user = userService.getUserByUserId(userId);
		
		Address address = addressRequestDto.toEntity(user);
		addressRepository.save(address);
		
		return AddressResponseDto.fromEntity(address, obfuscator);
	}
	
	/** 단일 주소 조회
	 * @param userId
	 * @param addressId
	 * 
	 * @throws NoSuchDataException {@link #getAuthorizedAddress} 찾을 수 없는 유저
	 * @throws StatusStayException {@link #getAuthorizedAddress} 일시정지 유저
	 * @throws StatusStopException {@link #getAuthorizedAddress} 중지 유저
	 * @throws StatusDeleteException {@link #getAuthorizedAddress} 탈퇴 유저
	 * @throws ForbiddenException {@link #getAuthorizedAddress} 접근 권한 이상
	 * 
	 * @return 주소
	 */
	public AddressResponseDto getAddress(int userId, int addressId) {
		
		Address address = getAuthorizedAddress(userId, addressId);
		
		return AddressResponseDto.fromEntity(address, obfuscator);
	}
	
	/** 주소 리스트 조회
	 * @param userId
	 * 
	 * @throws NoSuchDataException {@link UserService#getUserByUserId} 찾을 수 없는 유저
	 * @throws StatusStayException {@link UserService#getUserByUserId} 일시정지 유저
	 * @throws StatusStopException {@link UserService#getUserByUserId} 중지 유저
	 * @throws StatusDeleteException {@link UserService#getUserByUserId} 탈퇴 유저
	 * 
	 * @return 주소 리스트
	 */
	public List<AddressResponseDto> getAddressList(int userId) {
		
		userService.getUserByUserId(userId);
		
		List<Address> list = addressRepository.findByUserOrderByPrimaryAndDate(userId);
		
		if(list==null || list.isEmpty() || list.size()==0) {
			return null;
		}
		
		return list.stream()
				.map(e->AddressResponseDto.fromEntity(e, obfuscator))
				.collect(Collectors.toList());
	}
	
	/** 주소 수정
	 * @param userId
	 * @param addressId
	 * @param addressRequestDto
	 * 
	 * @throws IllegalAddressValueException {@link DaumMapClient#validateAddress} 일치하는 주소가 없음
	 * @throws DaumStateException {@link DaumMapClient#validateAddress} 다음 서버에서 응답 이상
	 * 
	 * @throws NoSuchDataException {@link #getAuthorizedAddress} 찾을 수 없는 유저
	 * @throws StatusStayException {@link #getAuthorizedAddress} 일시정지 유저
	 * @throws StatusStopException {@link #getAuthorizedAddress} 중지 유저
	 * @throws StatusDeleteException {@link #getAuthorizedAddress} 탈퇴 유저
	 * @throws ForbiddenException {@link #getAuthorizedAddress} 접근 권한 이상
	 * 
	 * @return
	 */
	@Transactional
	public void updateAddress(int userId, int addressId, AddressRequestDto addressRequestDto) {
		
		daumMapClient.validateAddress(addressRequestDto.postcode(), addressRequestDto.mainAddress());
		
		Address address = getAuthorizedAddress(userId, addressId);
		
		address.update(addressRequestDto);
	}
	
	/** 대표 주소 선정
	 * @param userId
	 * @param addressId
	 * 
	 * @throws NoSuchDataException {@link #getAuthorizedAddress} 찾을 수 없는 유저
	 * @throws StatusStayException {@link #getAuthorizedAddress} 일시정지 유저
	 * @throws StatusStopException {@link #getAuthorizedAddress} 중지 유저
	 * @throws StatusDeleteException {@link #getAuthorizedAddress} 탈퇴 유저
	 * @throws ForbiddenException {@link #getAuthorizedAddress} 접근 권한 이상
	 */
	@Transactional
	public void updatePrimary(int userId, int addressId) {
		
		Address address = getAuthorizedAddress(userId, addressId);
		
		addressRepository.unsetPrimaryForUser(userId);
		
		address.setPrimary(true);
	}
	
	/** 주소 삭제
	 * @param userId
	 * @param addressId
	 * 
	 * @throws NoSuchDataException {@link #getAuthorizedAddress} 찾을 수 없는 유저
	 * @throws StatusStayException {@link #getAuthorizedAddress} 일시정지 유저
	 * @throws StatusStopException {@link #getAuthorizedAddress} 중지 유저
	 * @throws StatusDeleteException {@link #getAuthorizedAddress} 탈퇴 유저
	 * @throws ForbiddenException {@link #getAuthorizedAddress} 접근 권한 이상
	 */
	@Transactional
	public void deleteAddress(int userId, int addressId) {
		
		Address address = getAuthorizedAddress(userId, addressId);
		
		addressRepository.delete(address);
	}
	
	/** 주소 GET 및 유저 검증
	 * @param userId
	 * @param addressId
	 * 
	 * @throws NoSuchDataException {@link UserService#getUserByUserId} 찾을 수 없는 유저
	 * @throws StatusStayException {@link UserService#getUserByUserId} 일시정지 유저
	 * @throws StatusStopException {@link UserService#getUserByUserId} 중지 유저
	 * @throws StatusDeleteException {@link UserService#getUserByUserId} 탈퇴 유저
	 * 
	 * @throws ForbiddenException {@link #getAuthorizedAddress} 접근 권한 이상
	 * 
	 * @return Address
	 */
	private Address getAuthorizedAddress(int userId, int addressId){
		
		userService.getUserByUserId(userId);
		
		Address address = addressRepository.findById(obfuscator.decode(addressId))
				.orElseThrow(() -> new NoSuchDataException(ADDRESSS_NOT_FOUND,"일치하는 데이터 없음. addressId: " + addressId));
		
		if(address.getUser().getUserId()!=userId) {
			throw new ForbiddenException(AUTHORIZATION_FAILED,"주소 조회 권한 이상. address.getUser().getUserId(): "+address.getUser().getUserId()+"!= userId: "+userId);
		}
		
		return address;
	}
}