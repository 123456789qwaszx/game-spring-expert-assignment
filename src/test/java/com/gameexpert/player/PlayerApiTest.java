package com.gameexpert.player;

import com.gameexpert.player.controller.PlayerController;
import com.gameexpert.player.dto.CreatePlayerRequest;
import com.gameexpert.player.service.PlayerService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PlayerApiTest {

    private PlayerService playerService;
    private LocalValidatorFactoryBean validator;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        playerService = mock(PlayerService.class);
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new PlayerController(playerService))
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void tearDown() {
        validator.close();
    }

    @Test
    @DisplayName("정상_플레이어_등록_요청은_201과_빈_본문을_반환한다")
    void createsPlayerFromJsonAndReturnsCreatedWithoutBody() throws Exception {
        mockMvc.perform(post("/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"player_1\"}"))
                .andExpect(status().isCreated())
                .andExpect(content().string(""));

        ArgumentCaptor<CreatePlayerRequest> request =
                ArgumentCaptor.forClass(CreatePlayerRequest.class);

        verify(playerService).createPlayer(request.capture());
        assertEquals("player_1", request.getValue().getNickname());
    }

    @Test
    @DisplayName("잘못된_닉네임은_서비스_호출_전에_400으로_거절한다")
    void rejectsInvalidNicknameBeforeCallingService() throws Exception {
        mockMvc.perform(post("/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"a\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(playerService);
    }

    @Test
    @DisplayName("닉네임이_누락되면_서비스_호출_전에_400으로_거절한다")
    void rejectsMissingNicknameBeforeCallingService() throws Exception {
        mockMvc.perform(post("/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(playerService);
    }
}
