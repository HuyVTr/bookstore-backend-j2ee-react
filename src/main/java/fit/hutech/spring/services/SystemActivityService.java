package fit.hutech.spring.services;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import fit.hutech.spring.entities.Role;
import fit.hutech.spring.entities.SystemActivity;
import fit.hutech.spring.entities.User;
import fit.hutech.spring.repositories.ISystemActivityRepository;
import fit.hutech.spring.repositories.IUserRepository;
import lombok.RequiredArgsConstructor;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SystemActivityService {

    private final ISystemActivityRepository activityRepository;
    private final IUserRepository userRepository;

    public void log(String type, String content) {
        String username = "System";
        String userRole = "SYSTEM";

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            Object principal = auth.getPrincipal();
            if (principal instanceof UserDetails) {
                username = ((UserDetails) principal).getUsername();
                // Get role
                try {
                    User user = userRepository.findByUsername(username).orElse(null);
                    if (user != null) {
                        String nameInfo = user.getUsername();
                        if (user.getFullName() != null && !user.getFullName().isBlank()) {
                            nameInfo += " (" + user.getFullName() + ")";
                        }
                        username = "[" + user.getId() + "] " + nameInfo; // Store as [ID] Name (Full Name)
                        if (user.getRoles() != null) {
                            userRole = user.getRoles().stream()
                                    .map(Role::getName)
                                    .collect(Collectors.joining(", "));
                        }
                    }
                } catch (Exception e) {
                    userRole = "UNKNOWN";
                }
            }
        }

        SystemActivity activity = SystemActivity.builder()
                .type(type)
                .content(content)
                .username(username)
                .userRole(userRole)
                .build();

        activityRepository.save(activity);
    }
}
