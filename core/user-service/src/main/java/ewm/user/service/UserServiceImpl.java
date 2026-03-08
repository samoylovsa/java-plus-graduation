package ewm.user.service;

import ewm.common.exception.ConflictException;
import ewm.common.exception.NotFoundException;
import ewm.user.client.dto.UserDto;
import ewm.user.dto.AdminUserGetParams;
import ewm.user.mapper.UserMapper;
import ewm.user.model.User;
import ewm.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository repository;
    private final UserMapper userMapper;

    @Override
    public UserDto createUser(UserDto userDto) {
        log.debug("createUser(userDto={})", userDto);
        if (repository.existsByEmail(userDto.getEmail())) {
            throw new ConflictException("User with this email already exists");
        }
        User user = repository.save(userMapper.toUser(userDto));
        return userMapper.toUserDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getUsers(AdminUserGetParams params) {
        log.debug("getUsers() with params: ids={}, from={}, size={}",
                params.getIds(), params.getFrom(), params.getSize());
        Pageable pageable = PageRequest.of(params.getFrom() / params.getSize(), params.getSize());

        if (params.getIds() != null && !params.getIds().isEmpty()) {
            return repository.findByIdIn(params.getIds(), pageable).stream()
                    .map(userMapper::toUserDto)
                    .toList();
        } else {
            return repository.findAll(pageable).stream()
                    .map(userMapper::toUserDto)
                    .toList();
        }
    }

    @Override
    public void deleteBy(Long userId) {
        log.debug("deleteBy(userId={})", userId);
        getUserById(userId);
        repository.deleteById(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUserById(Long userId) {
        return userMapper.toUserDto(
                repository.findById(userId)
                        .orElseThrow(() -> new NotFoundException("User with id= " + userId + " was not found")));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getUsersByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return repository.findByIdIn(ids).stream()
                .map(userMapper::toUserDto)
                .toList();
    }
}
