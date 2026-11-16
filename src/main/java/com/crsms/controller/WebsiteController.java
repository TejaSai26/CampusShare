package com.crsms.controller;

import com.crsms.dto.ApiDtos.LoginRequest;
import com.crsms.dto.ApiDtos.RegisterRequest;
import com.crsms.dto.ApiDtos.ListingRequest;
import com.crsms.dto.ApiDtos.UnitRequest;
import com.crsms.dto.ApiDtos.ReservationRequest;
import com.crsms.service.ReservationService;
import com.crsms.security.CampusUserDetails;
import java.time.LocalDateTime;
import java.time.ZoneId;
import com.crsms.model.resource.ResourceCategory;
import com.crsms.repository.ResourceCategoryRepository;
import com.crsms.service.CurrentUserService;
import com.crsms.service.ListingService;
import com.crsms.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.context.SecurityContextHolder;

@Controller
public class WebsiteController {
    private final UserService users;
    private final CurrentUserService current;
    private final ListingService listings;
    private final ResourceCategoryRepository categories;
    private final ReservationService reservations;

    public WebsiteController(UserService users, CurrentUserService current,
                             ListingService listings, ResourceCategoryRepository categories,
                             ReservationService reservations) {
        this.users = users;
        this.current = current;
        this.listings = listings;
        this.categories = categories;
        this.reservations = reservations;
    }

    @ModelAttribute("signedIn")
    public boolean signedIn() {
        Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof CampusUserDetails;
    }

    @ModelAttribute("currentMemberName")
    public String currentMemberName() {
        Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof CampusUserDetails details) {
            return details.getUser().getFullName();
        }
        return "";
    }

    @ModelAttribute("currentMemberId")
    public Long currentMemberId() {
        Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof CampusUserDetails details) {
            return details.getUser().getId();
        }
        return null;
    }

    @GetMapping("/")
    public String root() { return "redirect:/home"; }

    @GetMapping("/home")
    public String home() { return "home"; }

    @GetMapping("/login")
    public String loginPage() { return signedIn() ? "redirect:/dashboard" : "login"; }

    @PostMapping("/login")
    public String login(@RequestParam String email, @RequestParam String password,
                        HttpServletRequest request, HttpServletResponse response,
                        RedirectAttributes redirect) {
        try {
            users.login(new LoginRequest(email, password), request, response);
            return "redirect:/dashboard";
        } catch (Exception ex) {
            redirect.addFlashAttribute("error", "Invalid email or password. Please try again.");
            return "redirect:/login";
        }
    }

    @GetMapping("/register")
    public String registerPage() { return signedIn() ? "redirect:/dashboard" : "register"; }

    @PostMapping("/register")
    public String register(@RequestParam String fullName, @RequestParam String email,
                           @RequestParam String password, @RequestParam String role,
                           @RequestParam(required=false) String department,
                           @RequestParam(required=false) String memberId,
                           HttpServletRequest request, HttpServletResponse response,
                           RedirectAttributes redirect) {
        try {
            users.register(new RegisterRequest(fullName, email, password, role, department, memberId, null));
            users.login(new LoginRequest(email, password), request, response);
            redirect.addFlashAttribute("success", "Your account is ready. You are now signed in.");
            return "redirect:/dashboard";
        } catch (Exception ex) {
            redirect.addFlashAttribute("error", ex.getMessage() == null ? "Could not create account." : ex.getMessage());
            return "redirect:/register";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        if (request.getSession(false) != null) request.getSession(false).invalidate();
        return "redirect:/home";
    }

    @GetMapping("/browse")
    public String browse(@RequestParam(required=false) String q, Model model) {
        model.addAttribute("query", q == null ? "" : q);
        model.addAttribute("page", listings.browse(q, null, 0, 50));
        return "browse";
    }

    @GetMapping("/resources/{id}")
    public String resourceDetail(@org.springframework.web.bind.annotation.PathVariable Long id, Model model) {
        model.addAttribute("item", listings.get(id));
        return "resource-detail";
    }

    @PostMapping("/resources/{id}/reserve")
    public String reserve(@org.springframework.web.bind.annotation.PathVariable Long id,
                          @RequestParam Long unitId,
                          @RequestParam String startAt,
                          @RequestParam String endAt,
                          RedirectAttributes redirect) {
        try {
            LocalDateTime start = LocalDateTime.parse(startAt);
            LocalDateTime end = LocalDateTime.parse(endAt);
            reservations.request(current.require(), new ReservationRequest(unitId,
                    start.atZone(ZoneId.systemDefault()).toInstant(),
                    end.atZone(ZoneId.systemDefault()).toInstant()));
            redirect.addFlashAttribute("success", "Reservation request sent to the resource owner.");
        } catch (Exception ex) {
            redirect.addFlashAttribute("error", ex.getMessage() == null ? "Could not request this resource." : ex.getMessage());
        }
        return "redirect:/resources/" + id;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("member", current.require());
        model.addAttribute("myListings", listings.mine(current.require(), 0, 50).items());
        return "dashboard";
    }

    @GetMapping("/listing/new")
    public String newListing(Model model) {
        current.require();
        model.addAttribute("categories", categories.findAll());
        return "listing-form";
    }

    @PostMapping("/listing/new")
    public String createListing(@RequestParam String title, @RequestParam String description,
                                @RequestParam Long categoryId, @RequestParam String pricingType,
                                @RequestParam BigDecimal rate, RedirectAttributes redirect) {
        try {
            var created = listings.create(current.require(), new ListingRequest(title, description, categoryId, pricingType, rate));
            listings.addUnit(current.require(), created.id(), new UnitRequest(null, "GOOD", null));
            redirect.addFlashAttribute("success", "Your resource and its first unit have been listed.");
            return "redirect:/dashboard";
        } catch (Exception ex) {
            redirect.addFlashAttribute("error", ex.getMessage() == null ? "Could not create listing." : ex.getMessage());
            return "redirect:/listing/new";
        }
    }
}
