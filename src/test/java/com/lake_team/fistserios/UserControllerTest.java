package com.lake_team.fistserios;

import com.lake_team.fistserios.controller.AuthController;
import com.lake_team.fistserios.service.UserService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@ExtendWith(MockitoExtension.class)
public class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthController userController;

    @ParameterizedTest
    @CsvSource({
            "test@gmail.com, 123456, true, 200, Login successful!",
            "wrong@gmail.com, wrongpass, false, 401, Invalid credentials"
    })
    public void testLoginScenarios(String email, String password, boolean loginResult, int expectedStatus, String expectedMessage) throws Exception {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();

        // Налаштовуємо UserService відповідно до параметрів
        when(userService.login(email, password)).thenReturn(loginResult);

        mockMvc.perform(post("/auth/login")
                        .param("email", email)
                        .param("password", password)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().is(expectedStatus))
                .andExpect(content().string(expectedMessage));
    }
}