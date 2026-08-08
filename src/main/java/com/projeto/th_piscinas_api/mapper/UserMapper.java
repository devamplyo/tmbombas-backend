package com.projeto.th_piscinas_api.mapper;

import com.projeto.th_piscinas_api.dto.auth.UserRequest;
import com.projeto.th_piscinas_api.dto.auth.UserResponse;
import com.projeto.th_piscinas_api.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    UserResponse toUserResponse(User user);

    User toDtoUser(UserRequest request);
}
