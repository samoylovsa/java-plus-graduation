package ewm.user.mapper;

import ewm.user.client.dto.UserDto;
import ewm.user.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDto toUserDto(User user);

    User toUser(UserDto userDto);
}
