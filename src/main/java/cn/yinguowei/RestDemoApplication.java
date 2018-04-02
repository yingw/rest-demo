package cn.yinguowei;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findOneByName(String name);
}

/**
 * @author yinguowei
 */
@SpringBootApplication
public class RestDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(RestDemoApplication.class, args);
    }

    @Bean
    RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

@Entity
@Data
@NoArgsConstructor
@RequiredArgsConstructor
class User {
    @Id @GeneratedValue Long id;
    @NotNull @NonNull String name;

    public boolean isNew() {return this.id == null;}
}

@Component
class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;

    @Autowired
    public DataLoader(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        Stream.of("Jojo", "Gary", "Anna").forEach(name -> userRepository.save(new User(name)));
        userRepository.findAll().forEach(System.out::println);
    }
}

@RestController
@RequestMapping("/api")
class UserResource {
    private final UserRepository userRepository;

    UserResource(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/user")
    public ResponseEntity<List<User>> listAllUsers() {
        final List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @GetMapping("/user/{id}")
    public ResponseEntity<User> getUser(@PathVariable long id) {
        final Optional<User> userOptional = userRepository.findById(id);
        if (userOptional.isPresent()) {
            return new ResponseEntity<>(userOptional.get(), HttpStatus.OK);
        } else {
            return new ResponseEntity<User>(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/user")
    public ResponseEntity<User> createUser(@RequestBody User user, UriComponentsBuilder builder) {
        final Optional<User> userOptional = userRepository.findOneByName(user.getName());
        if (userOptional.isPresent()) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }
        userRepository.save(user);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(builder.path("/user/{id}").buildAndExpand(user.getId()).toUri());
        return new ResponseEntity<>(user, headers, HttpStatus.CREATED);
    }

    @PutMapping("/user/{id}")
    public ResponseEntity<User> updateUser(@PathVariable long id, @RequestBody User user) {
        User currentUser = userRepository.findById(id).get();
        if (currentUser == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        currentUser.setName(user.getName());
        userRepository.save(currentUser);
        return new ResponseEntity<>(currentUser, HttpStatus.OK);
    }

    @DeleteMapping("/user/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable long id) {
        Optional<User> currentUser = userRepository.findById(id);
        if (!currentUser.isPresent()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        userRepository.deleteById(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}

@Controller
class UserController {
    private static final String REST_SERVICE_URI = "http://localhost:8080/api";

    private final RestTemplate restTemplate;

    UserController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping({"/", "/users"})
    public String queryUser(Model model) {
//        List<LinkedHashMap<String, Object>> users = restTemplate.getForObject(REST_SERVICE_URI + "/users", List.class);
        List<User> users = restTemplate.getForObject(REST_SERVICE_URI + "/user", List.class);
        System.out.println("users = " + users);
        model.addAttribute("users", users);
        return "userList";
    }

    @GetMapping("/users/{id}/edit")
    public String modifyUser(@PathVariable long id, Model model) {
        //ResponseEntity<User>
        final User user = restTemplate.getForObject(REST_SERVICE_URI + "/user/{id}", User.class, id);
        System.out.println("user = " + user);
        model.addAttribute("user", user);
        return "userForm";
    }

    @PutMapping("/users/{id}")
    public String modifyUser(@PathVariable long id, @Valid User user) {
        restTemplate.put(REST_SERVICE_URI + "/user/{id}", user, id);
        return "redirect:/";
    }

    @DeleteMapping("/users/{id}")
    public String deleteUser(@PathVariable long id) {
        restTemplate.delete(REST_SERVICE_URI + "/user/{id}", id);
        return "redirect:/";
    }

    @GetMapping("/users/new")
    public String createUser(Model model) {
        model.addAttribute("user", new User());
        return "userForm";
    }

    @PostMapping("/users")
    public String createUser(@Valid User user, Model model) {
        User newUser = restTemplate.postForObject(REST_SERVICE_URI + "/user", user, User.class);
        model.addAttribute("user", newUser);
        return "redirect:/";
    }
}