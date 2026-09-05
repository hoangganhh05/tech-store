package com.techstore.service;

import com.techstore.dto.request.AddressRequest;
import com.techstore.dto.response.AddressResponse;

import java.util.List;

public interface AddressService {

    List<AddressResponse> listAddresses(Long userId);

    AddressResponse addAddress(Long userId, AddressRequest request);

    AddressResponse updateAddress(Long userId, Long addressId, AddressRequest request);

    void deleteAddress(Long userId, Long addressId);

    AddressResponse setDefaultAddress(Long userId, Long addressId);
}
