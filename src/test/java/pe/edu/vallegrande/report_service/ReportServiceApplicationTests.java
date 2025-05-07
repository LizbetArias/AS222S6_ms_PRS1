package pe.edu.vallegrande.report_service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import pe.edu.vallegrande.report_service.util.DotenvInitializer;

@SpringBootTest
class ReportServiceApplicationTests {

	@BeforeAll
	static void setup() {
		// Cargar las variables del .env antes del contexto
		new DotenvInitializer();
	}

	@Test
	void contextLoads() {
	}
}
