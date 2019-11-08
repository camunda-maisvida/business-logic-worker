package br.com.maisvida.camunda.worker;

import java.util.Collections;

import org.camunda.bpm.client.ExternalTaskClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.maisvida.camunda.service.VerificarDiasDisponiveisService;

public class VerificarDiasDisponiveisWorkerMain {

	private final static Logger LOGGER = LoggerFactory.getLogger(VerificarDiasDisponiveisWorkerMain.class.getName());

	public static void main(final String[] args) {

		ExternalTaskClient client = ExternalTaskClient
				.create()
				.baseUrl("http://localhost:8080/engine-rest")
				.asyncResponseTimeout(10000) // long polling timeout
				.build();

		client.subscribe("verifica-dias-disponiveis-topic").lockDuration(1000) // the default lock duration is 20 seconds, but you can override this
				.handler((externalTask, externalTaskService) -> {

					final String nome = externalTask.getVariable("FormField_NomeSolicitante");

					final int qtdDiasDisponiveis = new VerificarDiasDisponiveisService().recuperarQuantidadeDeDiasDiasponiveis();

					VerificarDiasDisponiveisWorkerMain.LOGGER.info("[VerificarDiasDisponiveisWorker] Quantidade de dis disponíveis '" + qtdDiasDisponiveis + "' para o solicitante: '" + nome + "'");

					externalTaskService.complete(externalTask, Collections.singletonMap("qtdDiasDisponiveis", qtdDiasDisponiveis));
				}).open();
	}
}
