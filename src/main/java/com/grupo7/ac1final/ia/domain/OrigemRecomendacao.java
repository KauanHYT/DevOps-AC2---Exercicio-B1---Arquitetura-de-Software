package com.grupo7.ac1final.ia.domain;

/** De onde veio a recomendação: do modelo de IA ou do fallback do próprio Core (ADR-003). */
public enum OrigemRecomendacao {
    IA,
    PADRAO
}