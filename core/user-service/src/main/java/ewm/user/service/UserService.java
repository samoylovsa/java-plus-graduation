package ewm.user.service;

import ewm.user.client.dto.UserDto;
import ewm.user.dto.AdminUserGetParams;

import java.util.List;

public interface UserService {

    UserDto createUser(UserDto userDto);

    List<UserDto> getUsers(AdminUserGetParams params);

    void deleteBy(Long userId);

    UserDto getUserById(Long userId);

    List<UserDto> getUsersByIds(List<Long> ids);
}
