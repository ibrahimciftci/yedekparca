package com.parca.yedekparca;

import com.parca.yedekparca.config.SecurityConfig;
import com.parca.yedekparca.controller.DemoImageController;
import com.parca.yedekparca.security.JwtAuthenticationFilter;
import com.parca.yedekparca.service.JwtService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DemoImageController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@ActiveProfiles("test")
class DemoImageUnavailableOutsideDemoProfileTest {
    @Autowired MockMvc mvc;
    @MockitoBean JwtService jwtService;
    @MockitoBean UserDetailsService userDetailsService;

    @Test void anonymousGetIsNotPublicOutsideDemoProfile() throws Exception {
        mvc.perform(get("/api/dev-demo/images/engine.svg")).andExpect(status().isForbidden());
        mvc.perform(get("/api/dev-demo/images/parts/brake-pads.jpg")).andExpect(status().isForbidden());
    }
}
