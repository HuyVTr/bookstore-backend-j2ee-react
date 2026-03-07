package fit.hutech.spring.security;

import fit.hutech.spring.entities.Role;
import fit.hutech.spring.entities.User;
import fit.hutech.spring.repositories.IRoleRepository;
import fit.hutech.spring.repositories.IUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final IUserRepository userRepository;
    private final IRoleRepository roleRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        return processOAuth2User(userRequest, oAuth2User);
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        String provider = userRequest.getClientRegistration().getRegistrationId();
        String email = (String) oAuth2User.getAttribute("email");
        String name = (String) oAuth2User.getAttribute("name");

        // Handle GitHub - might use 'login' if email is private
        if (email == null && "github".equalsIgnoreCase(provider)) {
            email = (String) oAuth2User.getAttribute("login") + "@github.com";
        }

        if (name == null) {
            name = (String) oAuth2User.getAttribute("login");
            if (name == null)
                name = email;
        }

        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;
        if (userOptional.isPresent()) {
            user = userOptional.get();
            user.setFullName(name);
            user.setProvider(provider);
            if (user.getActive() == null) {
                user.setActive(true);
            }
            userRepository.save(user);
        } else {
            user = registerNewUser(provider, email, name);
        }

        return oAuth2User;
    }

    private User registerNewUser(String provider, String email, String name) {
        User user = new User();
        user.setEmail(email);
        user.setFullName(name);
        user.setProvider(provider);
        user.setUsername(email);
        user.setPassword(UUID.randomUUID().toString());
        user.setActive(true);

        Role customerRole = roleRepository.findByName("CUSTOMER");
        if (customerRole == null) {
            customerRole = roleRepository.findByName("USER"); // Fallback
        }

        if (customerRole != null) {
            user.setRoles(Collections.singleton(customerRole));
        }

        return userRepository.save(user);
    }
}
