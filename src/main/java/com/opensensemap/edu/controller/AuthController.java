package com.opensensemap.edu.controller;

import com.opensensemap.edu.model.entity.EduUser;
import com.opensensemap.edu.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Authentication Controller
 * Handles login, registration, and authentication-related pages
 */
@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Login page
     */
    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid username or password");
        }
        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully");
        }
        model.addAttribute("title", "Login");
        return "auth/login";
    }

    /**
     * Handle successful login - redirect based on role and update last login
     */
    @GetMapping("/login-success")
    public String loginSuccess(Authentication authentication) {
        // Update last login time
        String username = authentication.getName();
        userService.updateLastLogin(username);
        
        if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            return "redirect:/admin/dashboard";
        }
        return "redirect:/dashboard";
    }

    /**
     * Registration page
     */
    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerForm", new RegisterForm());
        model.addAttribute("title", "Register");
        return "auth/register";
    }

    /**
     * Handle registration
     */
    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerForm") RegisterForm form,
                           BindingResult result,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        
        // Check for validation errors
        if (result.hasErrors()) {
            model.addAttribute("title", "Register");
            return "auth/register";
        }

        // Check password confirmation
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            model.addAttribute("error", "Passwords do not match");
            model.addAttribute("title", "Register");
            return "auth/register";
        }

        try {
            userService.registerUser(
                    form.getUsername(),
                    form.getEmail(),
                    form.getPassword(),
                    form.getFullName()
            );
            
            redirectAttributes.addFlashAttribute("message", "Registration successful! Please log in.");
            return "redirect:/login";
            
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("title", "Register");
            return "auth/register";
        }
    }

    /**
     * Access denied page
     */
    @GetMapping("/access-denied")
    public String accessDenied(Model model) {
        model.addAttribute("title", "Access Denied");
        return "auth/access-denied";
    }

    /**
     * Registration form DTO with validation
     */
    public static class RegisterForm {
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        private String username;

        @NotBlank(message = "Email is required")
        @Email(message = "Please provide a valid email")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        private String password;

        @NotBlank(message = "Please confirm your password")
        private String confirmPassword;

        @Size(max = 100, message = "Full name cannot exceed 100 characters")
        private String fullName;

        // Getters and Setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getConfirmPassword() { return confirmPassword; }
        public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
    }
}
