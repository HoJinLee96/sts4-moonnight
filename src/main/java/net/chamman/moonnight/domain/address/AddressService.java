package net.chamman.moonnight.domain.address;

import static net.chamman.moonnight.global.exception.HttpStatusCode.ADDRESSS_AUTHORIZATION_FAILED;
import static net.chamman.moonnight.global.exception.HttpStatusCode.ADDRESSS_NOT_FOUND;
import static net.chamman.moonnight.global.exception.HttpStatusCode.ADDRESS_INVALID_VALUE;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.chamman.moonnight.auth.crypto.Obfuscator;
import net.chamman.moonnight.domain.user.User;
import net.chamman.moonnight.domain.user.UserService;
import net.chamman.moonnight.global.exception.ForbiddenException;
import net.chamman.moonnight.global.exception.IllegalValueException;
import net.chamman.moonnight.global.exception.NoSuchDataException;
import net.chamman.moonnight.infra.kakao.DaumMapClient;

@Service
@Slf4j
@RequiredArgsConstructor
public class AddressService {
  private final AddressRepository addressRepository;
  private final UserService userService;
  private final DaumMapClient daumMapClient;
  private final Obfuscator obfuscator;
  private final EntityManager em;
  
  @Transactional
  public AddressResponseDto registerAddress(int userId, AddressRequestDto addressRequestDto) {
    
    validateAddressWithDaumClient(addressRequestDto);
    
    User user = userService.getUserByUserId(userId);
    
    Address address = addressRequestDto.toEntity(user);
    addressRepository.save(address);
    
    return AddressResponseDto.fromEntity(address, obfuscator);
  }
  
  public AddressResponseDto getAddress(int userId, int addressId) {
    
    Address address = getAuthorizedAddress(userId, addressId);
    
    return AddressResponseDto.fromEntity(address, obfuscator);
  }
  
  public List<AddressResponseDto> getAddressList(int userId) {
    
    // User user는 안쓰이지만 휴면,정지,탈퇴 유저인지 검증
    userService.getUserByUserId(userId);
    
    List<Address> list = addressRepository.findByUserOrderByPrimaryAndDate(userId);
    
    if(list==null || list.isEmpty() || list.size()==0) {
      return null;
    }
    
    return list.stream()
        .map(e->AddressResponseDto.fromEntity(e, obfuscator))
        .collect(Collectors.toList());
  }
  
  @Transactional
  public AddressResponseDto updateAddress(int userId, int addressId, AddressRequestDto addressRequestDto) {
    
    validateAddressWithDaumClient(addressRequestDto);
    
    Address address = getAuthorizedAddress(userId, addressId);

    address.update(addressRequestDto);
    em.refresh(address);
    
    return AddressResponseDto.fromEntity(address,obfuscator);
  }
  
  @Transactional
  public void updatePrimary(int userId, int addressId) {
    
    addressRepository.unsetPrimaryForUser(userId);
    
    Address address = getAuthorizedAddress(userId, addressId);
    
    address.setPrimary(true);
  }
  
  @Transactional
  public void deleteAddress(int userId, int addressId) {
    
    Address address = getAuthorizedAddress(userId, addressId);
    
    addressRepository.delete(address);
  }
  

  private void validateAddressWithDaumClient(AddressRequestDto addressRequestDto) {
    if (!daumMapClient.validateAddress(addressRequestDto.postcode(),
        addressRequestDto.mainAddress())) {
      throw new IllegalValueException(ADDRESS_INVALID_VALUE,"주소를 다시 확인해 주세요.");
    }
  }

  private Address getAuthorizedAddress(int userId, int addressId){
    
    userService.getUserByUserId(userId);

    Address address = addressRepository.findById(obfuscator.decode(addressId))
        .orElseThrow(() -> new NoSuchDataException(ADDRESSS_NOT_FOUND,"addressId에 일치하는 주소가 없습니다. addressId: " + addressId));
    
    if(address.getUser().getUserId()!=userId) {
      throw new ForbiddenException(ADDRESSS_AUTHORIZATION_FAILED,"주소 조회 권한 이상. address.getUser().getUserId(): "+address.getUser().getUserId()+"!= userId: "+userId);
    }
    
    return address;
  }
}