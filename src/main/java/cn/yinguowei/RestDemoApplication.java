package cn.yinguowei;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.omg.CosNaming.NamingContextPackage.NotFound;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Stream;

interface UserRepository extends JpaRepository<User, Long> {

}

/**
 * @author yinguowei
 */
@SpringBootApplication
public class RestDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(RestDemoApplication.class, args);
    }
}

@Configuration
class AppConfiguration {
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
    @Id @GeneratedValue long id;
    @NotNull @NonNull String name;
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
class UserRestController {
    private final UserRepository userRepository;

    UserRestController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> listAllUsers() {
        return new ResponseEntity<>(userRepository.findAll(), HttpStatus.OK);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<User> getUser(@PathVariable long id) {
        return new ResponseEntity<User>(userRepository.findById(id).get(), HttpStatus.OK);
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<User> updateUser(@PathVariable long id, @RequestBody User user) {
        User currentUser = userRepository.findById(id).get();
        if (currentUser == null) {
            return new ResponseEntity<User>(HttpStatus.NOT_FOUND);
        }
        currentUser.setName(user.getName());
        userRepository.save(currentUser);
        return new ResponseEntity<User>(currentUser, HttpStatus.OK);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<User> deleteUser(@PathVariable long id) {
        User currentUser = userRepository.findById(id).get();
        if (currentUser == null) {
            return new ResponseEntity<User>(HttpStatus.NOT_FOUND);
        }
        userRepository.deleteById(id);
        return new ResponseEntity<User>(currentUser, HttpStatus.OK);
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
        List<User> users = restTemplate.getForObject(REST_SERVICE_URI + "/users", List.class);
        System.out.println("users = " + users);
        model.addAttribute("users", users);
        return "index";
    }

    @GetMapping("/users/{id}/modify")
    public String modifyUser(@PathVariable long id, Model model) {
        //ResponseEntity<User>
        final User user = restTemplate.getForObject(REST_SERVICE_URI + "/users/{id}", User.class, id);
        System.out.println("user = " + user);
        model.addAttribute("user", user);
        return "modify";
    }

    @PutMapping("/users/{id}")
    public String updateUser(@PathVariable long id, @Valid User user) {
        restTemplate.put(REST_SERVICE_URI + "/users/{id}", user, id);
        return "redirect:/";
    }

    @DeleteMapping("/users/{id}")
    public String deleteUser(@PathVariable long id) {
        restTemplate.delete(REST_SERVICE_URI + "/users/{id}", id);
        return "redirect:/";
    }
}