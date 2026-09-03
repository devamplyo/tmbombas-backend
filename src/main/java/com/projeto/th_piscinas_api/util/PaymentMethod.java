package com.projeto.th_piscinas_api.util;

public enum PaymentMethod {

    // achado F10: o frontend sempre ofereceu Cartão de Crédito/Débito como
    // opções distintas na tela; o backend só tinha um "CARTAO" genérico.
    // Dividido para bater com o que a tela realmente oferece.
    DINHEIRO, PIX, CARTAO_CREDITO, CARTAO_DEBITO, BOLETO, TRANSFERENCIA
}
