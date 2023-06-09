package peaksoft.services;

import peaksoft.dto.requests.UserInfoRequest;

import peaksoft.dto.responses.UserInfoResponse;

/**
 * @author :ЛОКИ Kelsivbekov
 * @created 16.03.2023
 */

public interface UserService {
    UserInfoResponse authenticate(UserInfoRequest userInfoRequest);
}
