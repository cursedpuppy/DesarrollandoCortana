package com.cortana_masterchief;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class Inicio extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Establecer el archivo de layout correspondiente
        setContentView(R.layout.inicio);  // Reemplaza con el nombre de tu archivo XML

        // Configurar botones para acciones
        Button h_cortana = findViewById(R.id.hablar_cortana);
        h_cortana.setOnClickListener(v -> {
            // Crea un Intent para abrir MainActivity
            Intent intent = new Intent(Inicio.this, MainActivity.class);
            startActivity(intent);
        });

        Button explicar_btn = findViewById(R.id.explicar);
        explicar_btn.setOnClickListener(v -> {
            // Abrir nueva actividad para reproducir video
            Intent intent = new Intent(Inicio.this, Video.class);
            startActivity(intent);
        });
    }
}
