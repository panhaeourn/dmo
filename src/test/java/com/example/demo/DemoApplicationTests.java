package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:cito-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
		"app.frontend-url=http://localhost:5173",
		"app.jwt.secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
		"spring.security.oauth2.client.registration.google.client-id=test-client",
		"spring.security.oauth2.client.registration.google.client-secret=test-secret",
		"cloudflare.r2.endpoint=https://example.invalid",
		"cloudflare.r2.access-key=test-access-key",
		"cloudflare.r2.secret-key=test-secret-key",
		"bakong.token=test-token",
		"bakong.merchant.account-information=test-account",
		"bakong.merchant.acquiring-bank=test-bank",
		"bakong.merchant.name=CITO Test",
		"bakong.merchant.bakong-account-id=test@bakong"
})
class DemoApplicationTests {

	@Test
	void contextLoads() {
	}

}
