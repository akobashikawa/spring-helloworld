package com.example.demo.bdd.steps;

import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Entonces;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

public class HomePageSteps {

    @Autowired
    private TestRestTemplate restTemplate;

    private ResponseEntity<String> response;

    @Cuando("un usuario accede a la página de inicio")
    public void unUsuarioAccedeALaPaginaDeInicio() {
        response = restTemplate.getForEntity("/", String.class);
    }

    @Entonces("ve el mensaje {string}")
    public void veElMensaje(String mensaje) {
        assertThat(response.getBody()).contains(mensaje);
    }
}
