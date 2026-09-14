package com.mobdata.pontocerto.security;

import com.mobdata.pontocerto.model.Perfil;

import java.util.UUID;

public class TenantContext {
    private static final ThreadLocal<UUID> empresaId = new ThreadLocal<>();
    private static final ThreadLocal<Perfil> perfil = new ThreadLocal<>();
    private static final ThreadLocal<UUID> usuarioId = new ThreadLocal<>();

    public static void set(UUID empresaIdValue, Perfil perfilValue,  UUID usuarioIdValue) {
        empresaId.set(empresaIdValue);
        perfil.set(perfilValue);
        usuarioId.set(usuarioIdValue);
    }

    public static UUID getUsuarioId() {
        return usuarioId.get();
    }
    public static UUID getEmpresaId() {
        return empresaId.get();
    }

    public static Perfil getPerfil() {
        return perfil.get();
    }

    public static boolean isSuperAdmin() {
        return perfil.get() == Perfil.SUPERADMIN;
    }

    public static void clear() {
        empresaId.remove();
        perfil.remove();
        usuarioId.remove();
    }
}
