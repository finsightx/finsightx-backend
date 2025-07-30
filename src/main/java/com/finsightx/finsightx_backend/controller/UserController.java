package com.finsightx.finsightx_backend.controller;

import com.finsightx.finsightx_backend.dto.response.PortfolioItemResponse;
import com.finsightx.finsightx_backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/my-assets/{userId}")
    public ResponseEntity<List<PortfolioItemResponse>> getMyAssets(@PathVariable Long userId) {
        try {
            List<PortfolioItemResponse> myAssets = userService.getMyAssetsAsDto(userId);
            return ResponseEntity.ok(myAssets);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

}
