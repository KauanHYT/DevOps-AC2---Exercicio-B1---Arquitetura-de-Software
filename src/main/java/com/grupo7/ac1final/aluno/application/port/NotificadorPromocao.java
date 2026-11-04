package com.grupo7.ac1final.aluno.application.port;

import com.grupo7.ac1final.aluno.domain.AlunoPromovidoPremium;

public interface NotificadorPromocao {

    enum Resultado {
        /** a notificação já foi entregue; o aluno pode ser marcado como notificado agora */
        ENTREGUE,
        /** a notificação será entregue depois; o consumidor marca o aluno quando terminar */
        AGENDADA
    }

    Resultado notificar(AlunoPromovidoPremium evento);
}