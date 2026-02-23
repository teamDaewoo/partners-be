package com.dooring.domain.identity.dto;

import com.dooring.domain.identity.entity.Creator;
import com.dooring.domain.identity.entity.Seller;
import com.dooring.domain.identity.entity.UserType;

public record SignupResponse(Long id, String email, UserType userType) {

    public static SignupResponse ofSeller(Seller seller) {
        return new SignupResponse(seller.getId(), seller.getEmail(), UserType.SELLER);
    }

    public static SignupResponse ofCreator(Creator creator) {
        return new SignupResponse(creator.getId(), creator.getEmail(), UserType.CREATOR);
    }
}
