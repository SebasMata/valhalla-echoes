package com.mataflex.clan;

/**
 * Representa los distintos clanes vikingos dentro de Valhalla Echoes.
 * Cada aldea generada podrá estar asociada a uno de estos clanes,
 * definiendo su identidad visual y sus estandartes.
 */
public enum VikingClan {
    // TODO: Definir los clanes definitivos (nombres, colores, símbolos/runas)
    NONE("none");

    private final String id;

    VikingClan(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }
}
