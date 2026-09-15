package com.delivery.backend.image;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

class ImageUploadConfigurationTests {

	@Test
	void servletMultipartLimitsAllowTheDocumentedFiveMegabyteImage() throws IOException {
		Properties properties = PropertiesLoaderUtils.loadProperties(new ClassPathResource("application.properties"));

		assertThat(properties.getProperty("spring.servlet.multipart.max-file-size")).isEqualTo("5MB");
		assertThat(properties.getProperty("spring.servlet.multipart.max-request-size")).isEqualTo("6MB");
	}
}
