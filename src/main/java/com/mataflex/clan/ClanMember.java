package com.mataflex.clan;

/**
 * Interfaz o contrato para entidades, estructuras o elementos que pertenecen
 * a un clan vikingo específico.
 */
public interface ClanMember {

    /**
     * Obtiene el clan al que pertenece este elemento.
     */
    VikingClan getClan();

    /**
     * Asigna el clan al que pertenece este elemento.
     */
    void setClan(VikingClan clan);
}
