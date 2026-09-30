package com.releaseowl.sample.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Exercises the generated OData V4 endpoint over real HTTP, using the JDK HTTP
 * client so no additional test dependency is required.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderServiceODataTest {

	private static final HttpClient HTTP = HttpClient.newHttpClient();

	@LocalServerPort
	private int port;

	private URI uri(String path) {
		return URI.create("http://localhost:" + port + "/odata/v4/OrderService" + path);
	}

	private HttpResponse<String> get(String path) throws IOException, InterruptedException {
		return HTTP.send(HttpRequest.newBuilder().uri(uri(path)).GET().build(),
				HttpResponse.BodyHandlers.ofString());
	}

	private HttpResponse<String> post(String path, String body) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(uri(path))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(body))
				.build();
		return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
	}

	@Test
	void serviceExposesMetadata() throws Exception {
		HttpResponse<String> response = get("/$metadata");

		assertEquals(200, response.statusCode());
		assertTrue(response.body().contains("submitOrder"), "metadata should describe the submitOrder action");
	}

	@Test
	void submitOrderReturnsTheCalculatedTotal() throws Exception {
		HttpResponse<String> response = post("/submitOrder", "{\"quantity\":3,\"unitPrice\":19.99}");

		assertEquals(200, response.statusCode(), response.body());
		assertTrue(response.body().contains("59.97"), response.body());
		assertTrue(response.body().contains("\"accepted\":true"), response.body());
	}

	@Test
	void submitOrderRejectsInvalidQuantityWithBadRequest() throws Exception {
		HttpResponse<String> response = post("/submitOrder", "{\"quantity\":0,\"unitPrice\":19.99}");

		assertEquals(400, response.statusCode(), response.body());
		assertTrue(response.body().contains("greater than 0"), response.body());
	}

	@Test
	void maxQuantityPerOrderFunctionIsCallable() throws Exception {
		HttpResponse<String> response = get("/maxQuantityPerOrder()");

		assertEquals(200, response.statusCode(), response.body());
		assertTrue(response.body().contains("100"), response.body());
	}
}
