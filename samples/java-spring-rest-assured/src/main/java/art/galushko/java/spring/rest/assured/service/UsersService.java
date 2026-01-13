package art.galushko.java.spring.rest.assured.service;

import art.galushko.java.spring.rest.assured.model.User;
import art.galushko.java.spring.rest.assured.model.UserList;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class UsersService {

    private final List<User> users;

    public UsersService() {
        this.users = new ArrayList<>();
        users.add(new User()
                .id("u_123")
                .name("Ada Lovelace")
                .email("ada@example.com")
                .createdAt(OffsetDateTime.parse("2025-07-01T12:00:00Z"))
        );
        users.add(new User()
                .id("u_124")
                .name("Alan Turing")
                .email("alan@example.com")
                .createdAt(OffsetDateTime.parse("2025-07-03T12:00:00Z"))
        );
    }

    public UserList listUsers(Integer page, Integer pageSize, String q) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 100);

        List<User> filtered = users;
        if (q != null && !q.isBlank()) {
            String needle = q.toLowerCase();
            filtered = users.stream()
                    .filter(u -> u.getName().toLowerCase().contains(needle) || u.getEmail().toLowerCase().contains(needle))
                    .collect(Collectors.toList());
        }

        int from = Math.min((safePage - 1) * safeSize, filtered.size());
        int to = Math.min(from + safeSize, filtered.size());
        List<User> pageItems = filtered.subList(from, to);

        UserList result = new UserList();
        result.setPage(safePage);
        result.setPageSize(safeSize);
        result.setTotal(filtered.size());
        result.setItems(pageItems);
        return result;
    }

    public User getUser(String userId) {
        return users.stream()
                .filter(u -> Objects.equals(u.getId(), userId))
                .findFirst()
                .orElse(null);
    }
}


