package com.fabiankevin.app.web.controllers;

import com.fabiankevin.app.exceptions.AccountNotFoundException;
import com.fabiankevin.app.models.Account;
import com.fabiankevin.app.models.Page;
import com.fabiankevin.app.services.AccountService;
import com.fabiankevin.app.services.commands.CreateAccountCommand;
import com.fabiankevin.app.services.queries.PageQuery;
import com.fabiankevin.app.web.controllers.dtos.CreateAccountRequest;
import com.fabiankevin.app.web.controllers.dtos.CreateIconRequest;
import com.fabiankevin.app.web.controllers.dtos.IconResponse;
import com.fabiankevin.app.web.controllers.dtos.PatchAccountRequest;
import com.github.fabiankevin.lemon.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.fabiankevin.app.models.enums.AccountType.E_WALLET;
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import({GlobalExceptionHandler.class})
@WebMvcTest(AccountController.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @Autowired
    private JsonMapper jsonMapper;

    private Jwt jwt;

    @BeforeEach
    void setup() {
        jwt = Jwt.withTokenValue(UUID.randomUUID().toString())
                .subject(UUID.randomUUID().toString())
                .header("alg", "RS256")
                .audience(List.of("financial-tracker-test"))
                .claim("role", "USER")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    @Test
    void createAccount_givenValidRequest_thenShouldCreateAccount() throws Exception {
        CreateAccountRequest request = CreateAccountRequest.builder()
                .name("GCASH")
                .currency("PHP")
                .type(E_WALLET)
                .build();

        when(accountService.createAccount(any())).thenAnswer(invocation -> {
            java.util.UUID id = UUID.randomUUID();
            CreateAccountCommand command = invocation.getArgument(0);
            UUID userId = command.userId() != null ? command.userId() : UUID.randomUUID();
            return Account.builder()
                    .id(id)
                    .name(command.name())
                    .userId(userId)
                    .currency(command.currency())
                    .type(command.type())
                    .icon(command.icon())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
        });

        mockMvc.perform(post("/api/accounts")
                        .with(jwt().jwt(jwt))
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(header().string("Location", matchesPattern("http://localhost/api/accounts/[-a-f0-9]{36}")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("GCASH"))
                .andExpect(jsonPath("$.icon").doesNotExist());

        verify(accountService, times(1)).createAccount(any());
    }

    @Test
    void createAccount_givenValidRequestWithIcon_thenShouldCreateAccountWithIcon() throws Exception {
        CreateIconRequest iconRequest = CreateIconRequest.builder()
                .codePoint(128161)
                .fontFamily("Material Icons")
                .build();

        CreateAccountRequest request = CreateAccountRequest.builder()
                .name("GCASH")
                .currency("PHP")
                .type(E_WALLET)
                .icon(iconRequest)
                .build();

        when(accountService.createAccount(any())).thenAnswer(invocation -> {
            java.util.UUID id = UUID.randomUUID();
            CreateAccountCommand command = invocation.getArgument(0);
            UUID userId = command.userId() != null ? command.userId() : UUID.randomUUID();
            return Account.builder()
                    .id(id)
                    .name(command.name())
                    .userId(userId)
                    .currency(command.currency())
                    .type(command.type())
                    .icon(command.icon())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
        });

        mockMvc.perform(post("/api/accounts")
                        .with(jwt().jwt(jwt))
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(header().string("Location", matchesPattern("http://localhost/api/accounts/[-a-f0-9]{36}")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("GCASH"))
                .andExpect(jsonPath("$.icon.codePoint").value(128161))
                .andExpect(jsonPath("$.icon.fontFamily").value("Material Icons"));

        verify(accountService, times(1)).createAccount(any());
    }

    @Test
    void createAccount_givenNoJwt_thenShouldReturnForbidden() throws Exception {
        CreateAccountRequest request = CreateAccountRequest.builder()
                .name("GCASH")
                .currency("PHP")
                .type(E_WALLET)
                .build();

        mockMvc.perform(post("/api/accounts")
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(accountService);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void createAccount_givenNullAndEmptySource_thenShouldReturnBadRequest(String name) throws Exception {
        CreateAccountRequest request = CreateAccountRequest.builder()
                .name(name)
                .currency("PHP")
                .type(E_WALLET)
                .build();

        mockMvc.perform(post("/api/accounts")
                        .with(jwt().jwt(jwt))
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(accountService);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void createAccount_givenNullAndEmptyCurrency_thenShouldReturnBadRequest(String currency) throws Exception {
        CreateAccountRequest request = CreateAccountRequest.builder()
                .name("GCASH")
                .currency(currency)
                .type(E_WALLET)
                .build();

        mockMvc.perform(post("/api/accounts")
                        .with(jwt().jwt(jwt))
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(accountService);
    }

    @Test
    void getAccountById_givenExistingId_thenShouldReturnAccount() throws Exception {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.fromString(jwt.getSubject());

        when(accountService.getAccountById(id, userId)).thenReturn(Account.builder()
                .id(id)
                .name("GCASH")
                .userId(userId)
                .currency(java.util.Currency.getInstance("PHP"))
                .type(E_WALLET)
                .icon(null)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

        mockMvc.perform(get("/api/accounts/" + id)
                        .with(jwt().jwt(jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("GCASH"))
                .andExpect(jsonPath("$.icon").doesNotExist());

        verify(accountService, times(1)).getAccountById(id, userId);
    }

    @Test
    void getAccountById_givenExistingIdWithIcon_thenShouldReturnAccountWithIcon() throws Exception {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.fromString(jwt.getSubject());
        UUID iconId = UUID.randomUUID();

        when(accountService.getAccountById(id, userId)).thenReturn(Account.builder()
                .id(id)
                .name("GCASH")
                .userId(userId)
                .currency(java.util.Currency.getInstance("PHP"))
                .type(E_WALLET)
                .icon(com.fabiankevin.app.models.IconData.builder()
                        .id(iconId)
                        .codePoint(128161)
                        .fontFamily("Material Icons")
                        .build())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

        mockMvc.perform(get("/api/accounts/" + id)
                        .with(jwt().jwt(jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("GCASH"))
                .andExpect(jsonPath("$.icon.id").value(iconId.toString()))
                .andExpect(jsonPath("$.icon.codePoint").value(128161))
                .andExpect(jsonPath("$.icon.fontFamily").value("Material Icons"));

        verify(accountService, times(1)).getAccountById(id, userId);
    }

    @Test
    void getAccountById_givenNoJwt_thenShouldReturnForbidden() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(get("/api/accounts/" + id))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(accountService);
    }

    @Test
    void getAccountById_givenAccountNotFound_thenReturnNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.fromString(jwt.getSubject());

        when(accountService.getAccountById(id, userId)).thenThrow(new AccountNotFoundException());

        mockMvc.perform(get("/api/accounts/" + id)
                        .with(jwt().jwt(jwt)))
                .andDo(print())
                .andExpect(status().isNotFound());

        verify(accountService, times(1)).getAccountById(id, userId);
    }

    @Test
    void deleteAccountById_givenExistingId_thenShouldReturnNoContent() throws Exception {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.fromString(jwt.getSubject());

        mockMvc.perform(delete("/api/accounts/" + id)
                        .with(jwt().jwt(jwt)))
                .andExpect(status().isNoContent());

        verify(accountService, times(1)).deleteAccountById(id, userId);
    }

    @Test
    void deleteAccountById_givenNoJwt_thenShouldReturnForbidden() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/accounts/" + id))
                .andExpect(status().isForbidden());

        verifyNoInteractions(accountService);
    }

    @Test
    void getAccounts_givenUser_thenShouldReturnPagedAccounts() throws Exception {
        UUID userId = UUID.fromString(jwt.getSubject());

        var accounts = List.of(
                Account.builder().id(UUID.randomUUID()).name("A1").userId(userId).currency(java.util.Currency.getInstance("PHP")).type(E_WALLET).icon(null).createdAt(Instant.now()).updatedAt(Instant.now()).build(),
                Account.builder().id(UUID.randomUUID()).name("A2").userId(userId).currency(java.util.Currency.getInstance("PHP")).type(E_WALLET).icon(null).createdAt(Instant.now()).updatedAt(Instant.now()).build()
        );

        when(accountService.getAccountsByPageAndUserId(new PageQuery(0, 2, "name", "ASC"), userId))
                .thenReturn(new Page<>(accounts, 0, 2, accounts.size(), 1, true, true));

        mockMvc.perform(get("/api/accounts?page=0&size=2&sort=name&direction=ASC")
                        .with(jwt().jwt(jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].name").value("A1"))
                .andExpect(jsonPath("$.totalElements").value(2));

        verify(accountService, times(1)).getAccountsByPageAndUserId(new PageQuery(0, 2, "name", "ASC"), userId);
    }

    @Test
    void getAccounts_givenUserWithIcons_thenShouldReturnPagedAccountsWithIcons() throws Exception {
        UUID userId = UUID.fromString(jwt.getSubject());
        UUID iconId1 = UUID.randomUUID();
        UUID iconId2 = UUID.randomUUID();

        var accounts = List.of(
                Account.builder().id(UUID.randomUUID()).name("A1").userId(userId).currency(java.util.Currency.getInstance("PHP")).type(E_WALLET).icon(com.fabiankevin.app.models.IconData.builder().id(iconId1).codePoint(128161).fontFamily("Material Icons").build()).createdAt(Instant.now()).updatedAt(Instant.now()).build(),
                Account.builder().id(UUID.randomUUID()).name("A2").userId(userId).currency(java.util.Currency.getInstance("PHP")).type(E_WALLET).icon(com.fabiankevin.app.models.IconData.builder().id(iconId2).codePoint(128175).fontFamily("Material Icons").build()).createdAt(Instant.now()).updatedAt(Instant.now()).build()
        );

        when(accountService.getAccountsByPageAndUserId(new PageQuery(0, 2, "name", "ASC"), userId))
                .thenReturn(new Page<>(accounts, 0, 2, accounts.size(), 1, true, true));

        mockMvc.perform(get("/api/accounts?page=0&size=2&sort=name&direction=ASC")
                        .with(jwt().jwt(jwt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].name").value("A1"))
                .andExpect(jsonPath("$.content[0].icon.codePoint").value(128161))
                .andExpect(jsonPath("$.content[1].icon.codePoint").value(128175))
                .andExpect(jsonPath("$.totalElements").value(2));

        verify(accountService, times(1)).getAccountsByPageAndUserId(new PageQuery(0, 2, "name", "ASC"), userId);
    }


    @Test
    void getAccounts_givenNoJwt_thenShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/accounts?page=0&size=2&sort=name&direction=ASC"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(accountService);
    }

    @Test
    void patchAccount_givenValidRequest_thenShouldReturnUpdated() throws Exception {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.fromString(jwt.getSubject());

        PatchAccountRequest request = PatchAccountRequest.builder()
                .name("GCASH_MAIN")
                .currency("PHP")
                .build();

        when(accountService.patchAccount(any())).thenAnswer(invocation -> {
            com.fabiankevin.app.services.commands.PatchAccountCommand cmd = invocation.getArgument(0);
            return Account.builder()
                    .id(cmd.id())
                    .name(cmd.name())
                    .userId(userId)
                    .currency(java.util.Currency.getInstance("PHP"))
                    .type(cmd.type())
                    .icon(cmd.icon())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
        });

        mockMvc.perform(patch("/api/accounts/" + id)
                        .with(jwt().jwt(jwt))
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("GCASH_MAIN"))
                .andExpect(jsonPath("$.icon").doesNotExist());

        verify(accountService, times(1)).patchAccount(any());
    }

    @Test
    void patchAccount_givenValidRequestWithIcon_thenShouldReturnUpdatedWithIcon() throws Exception {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.fromString(jwt.getSubject());
        UUID iconId = UUID.randomUUID();

        IconResponse iconResponse = IconResponse.builder()
                .id(iconId)
                .codePoint(128161)
                .fontFamily("Material Icons")
                .build();

        PatchAccountRequest request = PatchAccountRequest.builder()
                .name("GCASH_MAIN")
                .currency("PHP")
                .icon(iconResponse)
                .build();

        when(accountService.patchAccount(any())).thenAnswer(invocation -> {
            com.fabiankevin.app.services.commands.PatchAccountCommand cmd = invocation.getArgument(0);
            return Account.builder()
                    .id(cmd.id())
                    .name(cmd.name())
                    .userId(userId)
                    .currency(java.util.Currency.getInstance("PHP"))
                    .type(cmd.type())
                    .icon(cmd.icon())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
        });

        mockMvc.perform(patch("/api/accounts/" + id)
                        .with(jwt().jwt(jwt))
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("GCASH_MAIN"))
                .andExpect(jsonPath("$.icon.id").value(iconId.toString()))
                .andExpect(jsonPath("$.icon.codePoint").value(128161))
                .andExpect(jsonPath("$.icon.fontFamily").value("Material Icons"));

        verify(accountService, times(1)).patchAccount(any());
    }

    @Test
    void patchAccount_givenNoJwt_thenShouldReturnForbidden() throws Exception {
        UUID id = UUID.randomUUID();

        PatchAccountRequest request = PatchAccountRequest.builder()
                .name("GCASH_MAIN")
                .currency("PHP")
                .build();

        mockMvc.perform(patch("/api/accounts/" + id)
                        .contentType("application/json")
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(accountService);
    }
}
