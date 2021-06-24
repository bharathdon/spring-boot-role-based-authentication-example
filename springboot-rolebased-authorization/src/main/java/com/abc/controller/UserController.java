package com.abc.controller;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.abc.entity.User;
import com.abc.repository.UserRepository;

@RestController
@RequestMapping("/user")
public class UserController {

	private static final String DEFAULT_ROLE = "ROLE_USER";
	private static final String[] ADMIN_ACCESS = { "ROLE_ADMIN", "ROLE_MODERATOR" };
	private static final String[] MODERATOR_ACCESS = { "ROLE_MODERATOR" };

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private BCryptPasswordEncoder passwordEncoder;

	@PostMapping("/join")
	public String joinGroup(@RequestBody User user) {

		user.setRoles(DEFAULT_ROLE);

		String encryptPwd = passwordEncoder.encode(user.getPassword());
		user.setPassword(encryptPwd);

		userRepository.save(user);
		return "Hi" + user.getUserName() + "welcome";
	}

	// if loggedin user is admin -> he has admin,moderator

	// if loggedin user is moderator -> he has moderator

	@GetMapping("/access/{userId}/{userRole}")
	@PreAuthorize(value = "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MODERATOR')")
	public String giveAccessToUser(@PathVariable Integer userId, @PathVariable String userRole, Principal principal) {

		User user = userRepository.findById(userId).get();
		List<String> activeRoles = getRoleByLoggedInUser(principal);
		String newRole = "";
		if (activeRoles.contains(userRole)) {
			newRole = activeRoles + "," + userRole;
			user.setRoles(newRole);
		}
		userRepository.save(user);

		return "Hi" + user.getUserName() + "new role assigned by" + principal.getName();

	}

	@GetMapping
	@PreAuthorize(value = "hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MODERATOR')") 	
	public List<User> loadUsers(){
		return userRepository.findAll();
	}

	@GetMapping("/test")
	@PreAuthorize(value = "hasAuthority('ROLE_USER')")
	public String testUserAccess() {
		return "user can only access this";
	}
	
	
	
	
	

	private List<String> getRoleByLoggedInUser(Principal principal) {
		String roles = getLoggedInUser(principal).getRoles();
		List<String> assignRoles = Arrays.stream(roles.split(",")).collect(Collectors.toList());

		if (assignRoles.contains("ROLE_ADMIN")) {
			return Arrays.stream(ADMIN_ACCESS).collect(Collectors.toList());
		}
		if (assignRoles.contains("ROLE_MODERATOR")) {
			return Arrays.stream(MODERATOR_ACCESS).collect(Collectors.toList());
		}

		return Collections.emptyList();

	}

	private User getLoggedInUser(Principal principal) {
		return userRepository.findByUserName(principal.getName()).get();
	}

}
