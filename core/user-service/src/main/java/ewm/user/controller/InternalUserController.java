package ewm.user.controller;

import ewm.user.dto.UserDto;
import ewm.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class InternalUserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public UserDto getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @GetMapping
    public List<UserDto> getUsersByIds(@RequestParam("ids") List<Long> ids) {
        return userService.getUsersByIds(ids);
    }
}
