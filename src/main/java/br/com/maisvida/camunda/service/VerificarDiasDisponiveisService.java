package br.com.maisvida.camunda.service;

import java.util.Random;

import org.springframework.stereotype.Service;

@Service
public class VerificarDiasDisponiveisService {

	public int recuperarQuantidadeDeDiasDiasponiveis() {

		return new Random().nextInt(( 30 - 0 ) + 1) + 0;
	}

}
