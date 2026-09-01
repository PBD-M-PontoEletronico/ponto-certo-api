package com.mobdata.pontocerto;


import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GerarHash {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String senha = "12345678"; // troque pela senha que quiser usar no teste
        String hash = encoder.encode(senha);

        System.out.println("Senha original: " + senha);
        System.out.println("Hash gerado: " + hash);
    }
}
