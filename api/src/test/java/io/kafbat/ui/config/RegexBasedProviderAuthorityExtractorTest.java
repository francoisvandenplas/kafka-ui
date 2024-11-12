package io.kafbat.ui.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.security.oauth2.client.registration.ClientRegistration.withRegistrationId;

import io.kafbat.ui.config.auth.OAuthProperties;
import io.kafbat.ui.model.rbac.Role;
import io.kafbat.ui.service.rbac.AccessControlService;
import io.kafbat.ui.service.rbac.extractor.CognitoAuthorityExtractor;
import io.kafbat.ui.service.rbac.extractor.GithubAuthorityExtractor;
import io.kafbat.ui.service.rbac.extractor.GoogleAuthorityExtractor;
import io.kafbat.ui.service.rbac.extractor.OauthAuthorityExtractor;
import io.kafbat.ui.service.rbac.extractor.ProviderAuthorityExtractor;
import io.kafbat.ui.util.AccessControlServiceMock;
import java.io.InputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.BeanAccess;

public class RegexBasedProviderAuthorityExtractorTest {


  private final AccessControlService accessControlService = new AccessControlServiceMock().getMock();
  Yaml yaml;
  ProviderAuthorityExtractor extractor;

  @BeforeEach
  void setUp() {
    yaml = new Yaml();
    yaml.setBeanAccess(BeanAccess.FIELD);

    InputStream rolesFile = this.getClass()
        .getClassLoader()
        .getResourceAsStream("roles_definition.yaml");

    Role[] roleArray = yaml.loadAs(rolesFile, Role[].class);
    when(accessControlService.getRoles()).thenReturn(List.of(roleArray));

  }

  @SneakyThrows
  @Test
  void extractOauth2Authorities() {

    extractor = new OauthAuthorityExtractor();

    OAuth2User oauth2User = new DefaultOAuth2User(
        AuthorityUtils.createAuthorityList("SCOPE_message:read"),
        Map.of("role_definition", Set.of("ROLE-ADMIN", "ANOTHER-ROLE"), "user_name", "john@kafka.com"),
        "user_name");

    HashMap<String, Object> additionalParams = new HashMap<>();
    OAuthProperties.OAuth2Provider provider = new OAuthProperties.OAuth2Provider();
    provider.setCustomParams(Map.of("roles-field", "role_definition"));
    additionalParams.put("provider", provider);

    Set<String> roles = extractor.extract(accessControlService, oauth2User, additionalParams).block();

    assertEquals(Set.of("viewer", "admin"), roles);

  }

  @SneakyThrows
  @Test
  void extractCognitoAuthorities() {

    extractor = new CognitoAuthorityExtractor();

    OAuth2User oauth2User = new DefaultOAuth2User(
        AuthorityUtils.createAuthorityList("SCOPE_message:read"),
        Map.of("cognito:groups", List.of("ROLE-ADMIN", "ANOTHER-ROLE"), "user_name", "john@kafka.com"),
        "user_name");

    HashMap<String, Object> additionalParams = new HashMap<>();

    OAuthProperties.OAuth2Provider provider = new OAuthProperties.OAuth2Provider();
    provider.setCustomParams(Map.of("roles-field", "role_definition"));
    additionalParams.put("provider", provider);

    Set<String> roles = extractor.extract(accessControlService, oauth2User, additionalParams).block();

    assertEquals(Set.of("viewer", "admin"), roles);

  }

  @SneakyThrows
  @Test
  void extractGithubAuthorities() {

    extractor = new GithubAuthorityExtractor();

    OAuth2User oauth2User = new DefaultOAuth2User(
        AuthorityUtils.createAuthorityList("SCOPE_message:read"),
        Map.of("login", "john@kafka.com"),
        "login");

    HashMap<String, Object> additionalParams = new HashMap<>();

    OAuthProperties.OAuth2Provider provider = new OAuthProperties.OAuth2Provider();
    additionalParams.put("provider", provider);

    additionalParams.put("request", new OAuth2UserRequest(
        withRegistrationId("registration-1")
            .clientId("client-1")
            .clientSecret("secret")
            .redirectUri("https://client.com")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationUri("https://provider.com/oauth2/authorization")
            .tokenUri("https://provider.com/oauth2/token")
            .clientName("Client 1")
            .build(),
        new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "XXXX", Instant.now(),
            Instant.now().plus(10, ChronoUnit.HOURS))));

    Set<String> roles = extractor.extract(accessControlService, oauth2User, additionalParams).block();

    assertEquals(Set.of("viewer"), roles);

  }

  @SneakyThrows
  @Test
  void extractGoogleAuthorities() {

    extractor = new GoogleAuthorityExtractor();

    OAuth2User oauth2User = new DefaultOAuth2User(
        AuthorityUtils.createAuthorityList("SCOPE_message:read"),
        Map.of("hd", "test.domain.com", "email", "john@kafka.com"),
        "email");

    HashMap<String, Object> additionalParams = new HashMap<>();

    OAuthProperties.OAuth2Provider provider = new OAuthProperties.OAuth2Provider();
    provider.setCustomParams(Map.of("roles-field", "role_definition"));
    additionalParams.put("provider", provider);

    Set<String> roles = extractor.extract(accessControlService, oauth2User, additionalParams).block();

    assertEquals(Set.of("viewer", "admin"), roles);

  }

}
