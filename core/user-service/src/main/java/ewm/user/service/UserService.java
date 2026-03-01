package ewm.user.service;

import ewm.user.dto.AdminUserGetParams;
import ewm.user.dto.UserDto;

import java.util.List;

public interface UserService {

    UserDto createUser(UserDto userDto);

    List<UserDto> getUsers(AdminUserGetParams params);

    void deleteBy(Long userId);

    UserDto getUserById(Long userId);

    List<UserDto> getUsersByIds(List<Long> ids);
}
