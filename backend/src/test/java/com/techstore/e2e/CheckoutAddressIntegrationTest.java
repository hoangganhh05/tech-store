package com.techstore.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techstore.dto.request.AddressRequest;
import com.techstore.entity.Address;
import com.techstore.entity.Role;
import com.techstore.entity.User;
import com.techstore.enums.RoleCode;
import com.techstore.repository.AddressRepository;
import com.techstore.repository.RoleRepository;
import com.techstore.repository.UserRepository;
import com.techstore.security.TokenIssuer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:checkout_address_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE")
@AutoConfigureMockMvc
class CheckoutAddressIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private TokenIssuer tokenIssuer;

    private User customerUser1;
    private String tokenUser1;
    private User customerUser2;
    private String tokenUser2;

    @BeforeEach
    void setUp() {
        addressRepository.deleteAll();
        userRepository.deleteAll();

        Role customerRole = roleRepository.findByCode(RoleCode.CUSTOMER)
                .orElseGet(() -> roleRepository.save(new Role(RoleCode.CUSTOMER, "Khách hàng")));

        customerUser1 = new User("customer1.checkout@example.com", "hashed_pwd", "Nguyen Van Checkout 1", "0987111222");
        customerUser1.addRole(customerRole);
        customerUser1 = userRepository.save(customerUser1);
        tokenUser1 = tokenIssuer.issue(customerUser1).accessToken();

        customerUser2 = new User("customer2.checkout@example.com", "hashed_pwd", "Nguyen Van Checkout 2", "0987333444");
        customerUser2.addRole(customerRole);
        customerUser2 = userRepository.save(customerUser2);
        tokenUser2 = tokenIssuer.issue(customerUser2).accessToken();
    }

    @AfterEach
    void tearDown() {
        addressRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("US-08.1: Lấy danh sách địa chỉ giao hàng của user đã đăng nhập, sắp xếp địa chỉ mặc định lên đầu")
    void listMyAddresses_returnsAddressesWithDefaultFirst() throws Exception {
        Address addr1 = new Address(customerUser1, "Người nhận 1", "0987111222", "Hà Nội", "Cầu Giấy", "Dịch Vọng", "Số 1 Duy Tân");
        addressRepository.save(addr1);

        Address addr2 = new Address(customerUser1, "Người nhận 2", "0987111333", "Hồ Chí Minh", "Quận 1", "Bến Nghé", "Số 10 Lê Lợi");
        addr2.markAsDefault();
        addr1.unmarkAsDefault();
        addressRepository.save(addr1);
        addressRepository.save(addr2);

        mockMvc.perform(get("/api/v1/users/me/addresses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUser1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", equalTo(true)))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].recipientName", equalTo("Người nhận 2")))
                .andExpect(jsonPath("$.data[0].isDefault", equalTo(true)))
                .andExpect(jsonPath("$.data[1].recipientName", equalTo("Người nhận 1")))
                .andExpect(jsonPath("$.data[1].isDefault", equalTo(false)));
    }

    @Test
    @DisplayName("US-08.1: Thêm địa chỉ mới đầu tiên cho user tự động trở thành địa chỉ mặc định")
    void addMyAddress_firstAddress_automaticallyDefault() throws Exception {
        AddressRequest request = new AddressRequest();
        request.setRecipientName("Nguyễn Văn A");
        request.setPhone("0912345678");
        request.setProvince("TP. Hà Nội");
        request.setDistrict("Quận Ba Đình");
        request.setWard("Phường Cống Vị");
        request.setStreetAddress("Số 12 Kim Mã");

        mockMvc.perform(post("/api/v1/users/me/addresses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUser1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", equalTo(true)))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.recipientName", equalTo("Nguyễn Văn A")))
                .andExpect(jsonPath("$.data.phone", equalTo("0912345678")))
                .andExpect(jsonPath("$.data.province", equalTo("TP. Hà Nội")))
                .andExpect(jsonPath("$.data.isDefault", equalTo(true)));
    }

    @Test
    @DisplayName("US-08.1: Thêm địa chỉ thứ 2 trở đi thì không tự động là mặc định")
    void addMyAddress_subsequentAddress_notDefault() throws Exception {
        Address addr1 = new Address(customerUser1, "Địa chỉ cũ", "0987111222", "Hà Nội", "Cầu Giấy", "Dịch Vọng", "Số 1 Duy Tân");
        addr1.markAsDefault();
        addressRepository.save(addr1);

        AddressRequest request = new AddressRequest();
        request.setRecipientName("Địa chỉ phụ");
        request.setPhone("0912345679");
        request.setProvince("Đà Nẵng");
        request.setDistrict("Hải Châu");
        request.setWard("Thạch Thang");
        request.setStreetAddress("Số 99 Bạch Đằng");

        mockMvc.perform(post("/api/v1/users/me/addresses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUser1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.isDefault", equalTo(false)));
    }

    @Test
    @DisplayName("US-08.1: Validate bắt buộc các trường địa chỉ khi thêm mới (trả về 400 Bad Request)")
    void addMyAddress_validationError_returns400() throws Exception {
        AddressRequest invalidRequest = new AddressRequest();
        invalidRequest.setRecipientName("");
        invalidRequest.setPhone("invalid-phone");
        invalidRequest.setProvince("");
        invalidRequest.setDistrict("");
        invalidRequest.setWard("");
        invalidRequest.setStreetAddress("");

        mockMvc.perform(post("/api/v1/users/me/addresses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUser1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", equalTo(false)))
                .andExpect(jsonPath("$.code", equalTo("VALIDATION_ERROR")));
    }

    @Test
    @DisplayName("US-08.1: Chưa đăng nhập truy cập API địa chỉ trả về 401 Unauthorized")
    void addressApis_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/addresses"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", equalTo("INVALID_ACCESS_TOKEN")));

        AddressRequest request = new AddressRequest();
        request.setRecipientName("Test");
        request.setPhone("0912345678");
        request.setProvince("HN");
        request.setDistrict("CG");
        request.setWard("DV");
        request.setStreetAddress("123");

        mockMvc.perform(post("/api/v1/users/me/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", equalTo("INVALID_ACCESS_TOKEN")));
    }

    @Test
    @DisplayName("US-08.1: Đảm bảo tính cô lập dữ liệu (User 2 không thấy địa chỉ của User 1)")
    void addressApis_userIsolation_verified() throws Exception {
        Address addrUser1 = new Address(customerUser1, "Địa chỉ User 1", "0987111222", "Hà Nội", "Cầu Giấy", "Dịch Vọng", "Số 1 Duy Tân");
        addrUser1.markAsDefault();
        addressRepository.save(addrUser1);

        mockMvc.perform(get("/api/v1/users/me/addresses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUser2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }
}

