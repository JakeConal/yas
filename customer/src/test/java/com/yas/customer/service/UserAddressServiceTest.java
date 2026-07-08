package com.yas.customer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.AccessDeniedException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.customer.model.UserAddress;
import com.yas.customer.repository.UserAddressRepository;
import com.yas.customer.utils.Constants;
import com.yas.customer.viewmodel.address.ActiveAddressVm;
import com.yas.customer.viewmodel.address.AddressDetailVm;
import com.yas.customer.viewmodel.address.AddressPostVm;
import com.yas.customer.viewmodel.address.AddressVm;
import com.yas.customer.viewmodel.useraddress.UserAddressVm;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class UserAddressServiceTest {

    @Mock
    private UserAddressRepository userAddressRepository;

    @Mock
    private LocationService locationService;

    @InjectMocks
    private UserAddressService userAddressService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getUserAddressList_whenAnonymousUser_thenThrowsAccessDeniedException() {
        setAuthenticationName("anonymousUser");

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
            () -> userAddressService.getUserAddressList());

        assertThat(exception.getMessage()).isEqualTo(Constants.ErrorCode.UNAUTHENTICATED);
        verifyNoInteractions(userAddressRepository, locationService);
    }

    @Test
    void getUserAddressList_whenAddressesExist_thenReturnsSortedList() {
        setAuthenticationName("user-1");
        UserAddress inactiveAddress = userAddress(1L, "user-1", 10L, false);
        UserAddress activeAddress = userAddress(2L, "user-1", 20L, true);

        when(userAddressRepository.findAllByUserId("user-1")).thenReturn(List.of(inactiveAddress, activeAddress));
        when(locationService.getAddressesByIdList(List.of(10L, 20L))).thenReturn(List.of(
            addressDetail(10L, "Contact 1"),
            addressDetail(20L, "Contact 2")
        ));

        List<ActiveAddressVm> result = userAddressService.getUserAddressList();

        assertThat(result).hasSize(2);
        assertEquals(20L, result.get(0).id());
        assertEquals(true, result.get(0).isActive());
        assertEquals(10L, result.get(1).id());
        assertEquals(false, result.get(1).isActive());
    }

    @Test
    void getUserAddressList_whenLocationServiceReturnsEmpty_thenReturnsEmptyList() {
        setAuthenticationName("user-1");
        when(userAddressRepository.findAllByUserId("user-1")).thenReturn(List.of(userAddress(1L, "user-1", 10L, true)));
        when(locationService.getAddressesByIdList(List.of(10L))).thenReturn(List.of());

        List<ActiveAddressVm> result = userAddressService.getUserAddressList();

        assertThat(result).isEmpty();
    }

    @Test
    void getAddressDefault_whenAnonymousUser_thenThrowsAccessDeniedException() {
        setAuthenticationName("anonymousUser");

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
            () -> userAddressService.getAddressDefault());

        assertThat(exception.getMessage()).isEqualTo(Constants.ErrorCode.UNAUTHENTICATED);
        verifyNoInteractions(userAddressRepository, locationService);
    }

    @Test
    void getAddressDefault_whenAddressMissing_thenThrowsNotFoundException() {
        setAuthenticationName("user-1");
        when(userAddressRepository.findByUserIdAndIsActiveTrue("user-1")).thenReturn(Optional.empty());
        when(userAddressRepository.findFirstByUserIdOrderByIdAsc("user-1")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userAddressService.getAddressDefault());
        verify(locationService, org.mockito.Mockito.never()).getAddressById(any());
    }

    @Test
    void getAddressDefault_whenNoActiveAddress_thenActivatesFirstAddress() {
        setAuthenticationName("user-1");
        UserAddress firstAddress = userAddress(1L, "user-1", 20L, false);
        AddressDetailVm addressDetail = addressDetail(20L, "Contact 2");

        when(userAddressRepository.findByUserIdAndIsActiveTrue("user-1")).thenReturn(Optional.empty());
        when(userAddressRepository.findFirstByUserIdOrderByIdAsc("user-1")).thenReturn(Optional.of(firstAddress));
        when(userAddressRepository.save(firstAddress)).thenReturn(firstAddress);
        when(locationService.getAddressById(20L)).thenReturn(addressDetail);

        AddressDetailVm result = userAddressService.getAddressDefault();

        assertEquals(addressDetail, result);
        assertThat(firstAddress.getIsActive()).isTrue();
        verify(userAddressRepository).save(firstAddress);
    }

    @Test
    void getAddressDefault_whenAddressExists_thenReturnsAddressDetail() {
        setAuthenticationName("user-1");
        UserAddress userAddress = userAddress(1L, "user-1", 20L, true);
        when(userAddressRepository.findByUserIdAndIsActiveTrue("user-1")).thenReturn(Optional.of(userAddress));
        AddressDetailVm addressDetail = addressDetail(20L, "Contact 2");
        when(locationService.getAddressById(20L)).thenReturn(addressDetail);

        AddressDetailVm result = userAddressService.getAddressDefault();

        assertEquals(addressDetail, result);
    }

    @Test
    void createAddress_whenFirstAddress_thenMarksItActive() {
        setAuthenticationName("user-1");
        AddressPostVm addressPostVm = addressPostVm();
        when(userAddressRepository.findAllByUserId("user-1")).thenReturn(List.of());
        when(locationService.createAddress(addressPostVm)).thenReturn(addressVm(100L));
        when(userAddressRepository.save(any(UserAddress.class))).thenAnswer(invocation -> {
            UserAddress saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        UserAddressVm result = userAddressService.createAddress(addressPostVm);

        assertEquals(1L, result.id());
        assertEquals("user-1", result.userId());
        assertEquals(100L, result.addressGetVm().id());
        assertEquals(true, result.isActive());
    }

    @Test
    void createAddress_whenUserHasExistingAddress_thenMarksItInactive() {
        setAuthenticationName("user-1");
        AddressPostVm addressPostVm = addressPostVm();
        when(userAddressRepository.findAllByUserId("user-1")).thenReturn(List.of(userAddress(1L, "user-1", 10L, true)));
        when(locationService.createAddress(addressPostVm)).thenReturn(addressVm(100L));
        when(userAddressRepository.save(any(UserAddress.class))).thenAnswer(invocation -> {
            UserAddress saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });

        UserAddressVm result = userAddressService.createAddress(addressPostVm);

        assertEquals(2L, result.id());
        assertEquals(false, result.isActive());
    }

    @Test
    void createAddress_whenUserHasNoActiveAddress_thenMarksItActive() {
        setAuthenticationName("user-1");
        AddressPostVm addressPostVm = addressPostVm();
        when(userAddressRepository.findAllByUserId("user-1")).thenReturn(
            List.of(userAddress(1L, "user-1", 10L, false)));
        when(locationService.createAddress(addressPostVm)).thenReturn(addressVm(100L));
        when(userAddressRepository.save(any(UserAddress.class))).thenAnswer(invocation -> {
            UserAddress saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });

        UserAddressVm result = userAddressService.createAddress(addressPostVm);

        assertEquals(2L, result.id());
        assertEquals(true, result.isActive());
    }

    @Test
    void deleteAddress_whenAddressMissing_thenThrowsNotFoundException() {
        setAuthenticationName("user-1");
        when(userAddressRepository.findOneByUserIdAndAddressId("user-1", 10L)).thenReturn(null);

        assertThrows(NotFoundException.class, () -> userAddressService.deleteAddress(10L));
        verifyNoInteractions(locationService);
    }

    @Test
    void deleteAddress_whenAddressExists_thenDeletesIt() {
        setAuthenticationName("user-1");
        UserAddress userAddress = userAddress(1L, "user-1", 10L, true);
        when(userAddressRepository.findOneByUserIdAndAddressId("user-1", 10L)).thenReturn(userAddress);

        userAddressService.deleteAddress(10L);

        verify(userAddressRepository).delete(userAddress);
    }

    @Test
    void chooseDefaultAddress_whenCalled_thenUpdatesActiveFlags() {
        setAuthenticationName("user-1");
        UserAddress first = userAddress(1L, "user-1", 10L, false);
        UserAddress second = userAddress(2L, "user-1", 20L, false);
        when(userAddressRepository.findAllByUserId("user-1")).thenReturn(List.of(first, second));

        userAddressService.chooseDefaultAddress(20L);

        ArgumentCaptor<List<UserAddress>> captor = ArgumentCaptor.forClass(List.class);
        verify(userAddressRepository).saveAll(captor.capture());
        List<UserAddress> saved = captor.getValue();
        assertEquals(false, saved.get(0).getIsActive());
        assertEquals(true, saved.get(1).getIsActive());
    }

    private static void setAuthenticationName(String name) {
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authentication.getName()).thenReturn(name);
        SecurityContext securityContext = org.mockito.Mockito.mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    private static UserAddress userAddress(Long id, String userId, Long addressId, Boolean isActive) {
        return UserAddress.builder()
            .id(id)
            .userId(userId)
            .addressId(addressId)
            .isActive(isActive)
            .build();
    }

    private static AddressDetailVm addressDetail(Long id, String contactName) {
        return AddressDetailVm.builder()
            .id(id)
            .contactName(contactName)
            .phone("123-456-7890")
            .addressLine1("123 Elm Street")
            .city("Springfield")
            .zipCode("12345")
            .districtId(101L)
            .districtName("District A")
            .stateOrProvinceId(10L)
            .stateOrProvinceName("State A")
            .countryId(1L)
            .countryName("Country A")
            .build();
    }

    private static AddressVm addressVm(Long id) {
        return AddressVm.builder()
            .id(id)
            .contactName("Contact")
            .phone("123-456-7890")
            .addressLine1("123 Elm Street")
            .city("Springfield")
            .zipCode("12345")
            .districtId(101L)
            .stateOrProvinceId(10L)
            .countryId(1L)
            .build();
    }

    private static AddressPostVm addressPostVm() {
        return new AddressPostVm(
            "Contact",
            "123-456-7890",
            "123 Elm Street",
            "Springfield",
            "12345",
            101L,
            10L,
            1L
        );
    }
}
