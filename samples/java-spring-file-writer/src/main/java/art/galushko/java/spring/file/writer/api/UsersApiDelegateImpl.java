package art.galushko.java.spring.file.writer.api;

import art.galushko.java.spring.file.writer.service.UsersService;
import art.galushko.java.spring.file.writer.model.NewUser;
import art.galushko.java.spring.file.writer.model.User;
import art.galushko.java.spring.file.writer.model.UserList;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class UsersApiDelegateImpl implements UsersApiDelegate {

    private final UsersService usersService;

    public UsersApiDelegateImpl(UsersService usersService) {
        this.usersService = usersService;
    }

    @Override
    public ResponseEntity<UserList> listUsers(Integer page, Integer pageSize, String q) {
        UserList result = usersService.listUsers(page, pageSize, q);
        return ResponseEntity.ok(result);
    }

    @Override
    public ResponseEntity<User> getUser(String userId) {
        User user = usersService.getUser(userId);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user);
    }

    @Override
    public ResponseEntity<User> createUser(NewUser newUser, String idempotencyKey) {
        User created = new User()
                .id("u_999")
                .name(newUser.getName())
                .email(newUser.getEmail());
        return ResponseEntity.created(java.net.URI.create("/v1/users/" + created.getId())).body(created);
    }
}


