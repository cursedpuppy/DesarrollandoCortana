package com.cortana_masterchief;

import static androidx.fragment.app.FragmentManager.TAG;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.net.Uri;
import android.os.Handler;
import android.speech.tts.UtteranceProgressListener;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.VideoView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;



public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MessageAdapter messageAdapter;
    private List<Message> messageList;

    private DatabaseReference messagesRef;

    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;
    private TextToSpeech textToSpeech;
    private boolean isSpeaking = false;
    private String lastMessageId = "...";  // Inicializamos como null


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerView);

        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(messageAdapter);

        VideoView videoView = findViewById(R.id.videoCortana);

// Establece la URI del video (puede ser desde recursos locales o una URL)
        videoView.setVideoURI(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.gifcortanad));

// Escucha el evento de preparación para habilitar el bucle
        videoView.setOnPreparedListener(mp -> mp.setLooping(true));

// Inicia la reproducción
        videoView.start();

        // Configurar Speech-to-Text
        initializeSpeechRecognizer();

        // Configurar Text-to-Speech
        initializeTextToSpeech();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        messagesRef = database.getReference("messages");

// Verifica que no sea nula
        if (messagesRef != null) {
            messagesRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    List<Message> newMessages = new ArrayList<>(); // Nueva lista de mensajes

                    // Itera sobre los datos recibidos
                    for (DataSnapshot messageSnapshot : dataSnapshot.getChildren()) {
                        // Obtener el mensaje usando el valor del nodo actual
                        Message message = messageSnapshot.getValue(Message.class);

                        // Log para ver todos los datos de un mensaje recuperado
                        if (message != null) {
                            Log.d("Firebase", "Mensaje recuperado:");
                            Log.d("Firebase", "ID (clave alfanumérica de Firebase): " + messageSnapshot.getKey());
                            Log.d("Firebase", "Usuario: " + message.getUsuario());
                            Log.d("Firebase", "Modelo: " + message.getModelo());

                            Log.d("Firebase", "Timestamp: " + message.getTimestamp());
                        }

                        if (message != null) {
                            // Aquí ya no necesitamos validar el 'id' dentro del mensaje,
                            // ya que Firebase nos da la clave del nodo (id único generado)
                            String firebaseMessageId = messageSnapshot.getKey(); // ID único del mensaje en Firebase

                            // Verifica si el mensaje tiene respuesta del modelo
                            if (!TextUtils.isEmpty(message.getModelo()) && !"...".equals(message.getModelo())) {
                                if (lastMessageId == null || !lastMessageId.equals(firebaseMessageId)) {
                                    lastMessageId = firebaseMessageId; // Actualizar el último mensaje leído
                                    newMessages.add(message); // Agregar el mensaje con la respuesta del modelo
                                }
                            } else {
                                // Si no tiene respuesta del modelo, agregar el mensaje con "..."
                                if (lastMessageId == null || !lastMessageId.equals(firebaseMessageId)) {
                                    lastMessageId = firebaseMessageId; // Actualizar el último mensaje leído
                                    message.setModelo("..."); // Establecer el mensaje provisional
                                    newMessages.add(message); // Agregar el mensaje provisional

                                    // Verifica si la clave del mensaje no es null antes de agregar el listener
                                    if (firebaseMessageId != null) {
                                        // Listener para esperar la respuesta del modelo
                                        messagesRef.child(firebaseMessageId).addValueEventListener(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot dataSnapshot) {
                                                Message updatedMessage = dataSnapshot.getValue(Message.class);
                                                if (updatedMessage != null && !TextUtils.isEmpty(updatedMessage.getModelo()) && !"...".equals(updatedMessage.getModelo())) {
                                                    // Si la respuesta del modelo está disponible
                                                    int index = newMessages.indexOf(message);
                                                    if (index != -1) {
                                                        // Actualizar el mensaje con la respuesta
                                                        newMessages.set(index, updatedMessage);

                                                        // Actualiza el adaptador en el hilo principal
                                                        runOnUiThread(() -> {
                                                            messageAdapter.notifyItemChanged(index); // Notificar al adaptador
                                                            speakMessage(updatedMessage.getModelo()); // Leer la respuesta en voz alta
                                                            recyclerView.smoothScrollToPosition(index); // Desplazarse al mensaje actualizado
                                                        });

                                                        Log.v("Cambio Servidor", "Respuesta del modelo actualizada");
                                                    }
                                                }
                                            }

                                            @Override
                                            public void onCancelled(DatabaseError databaseError) {
                                                Log.e("Firebase", "Error al leer datos: " + databaseError.getMessage());
                                            }
                                        });
                                    }
                                }
                            }
                        }
                    }

                    // Actualiza la lista de mensajes solo después de iterar
                    if (!newMessages.isEmpty()) {
                        runOnUiThread(() -> {
                            messageList.clear(); // Limpiar lista anterior
                            messageList.addAll(newMessages); // Agregar nuevos mensajes
                            messageAdapter.notifyDataSetChanged(); // Notificar al adaptador
                            recyclerView.smoothScrollToPosition(messageList.size() - 1); // Desplazarse al final
                        });
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e("Firebase", "Error al leer datos: " + databaseError.getMessage());
                }
            });
        } else {
            Log.e("Firebase", "messagesRef es nulo. Verifica la inicialización.");
        }




    }

    private void speakMessage(String message) {
        if (textToSpeech != null && !TextUtils.isEmpty(message)) {
            isSpeaking = true;
            textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, "MessageID");

            // Detener el reconocimiento de voz antes de comenzar a hablar
            speechRecognizer.stopListening();

            // Configurar el oyente para cuando se termine de hablar
            textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {}

                @Override
                public void onDone(String utteranceId) {
                    runOnUiThread(() -> {
                        isSpeaking = false;
                        // Una vez que termine de hablar, reanudar el reconocimiento de voz
                        if (speechRecognizer != null) {
                            speechRecognizer.startListening(recognizerIntent);
                        }
                    });
                }

                @Override
                public void onError(String utteranceId) {
                    runOnUiThread(() -> {
                        isSpeaking = false;
                        if (speechRecognizer != null) {
                            speechRecognizer.startListening(recognizerIntent);
                        }
                    });
                }
            });
        }
    }

    private void initializeSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-MX");

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                Log.d("Reconocimiento VOZ", "Listo para hablar.");
            }

            @Override
            public void onBeginningOfSpeech() {
                Log.d("Reconocimiento VOZ", "Comenzando a escuchar.");
            }

            @Override
            public void onRmsChanged(float rmsdB) {
                Log.d("Reconocimiento VOZ", "Nivel de audio cambiado: " + rmsdB);
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
                Log.d("Reconocimiento VOZ", "Buffer de audio recibido.");
            }

            @Override
            public void onEndOfSpeech() {
                Log.d("Reconocimiento VOZ", "Fin del discurso.");
            }

            @Override
            public void onError(int error) {
                Log.e("Reconocimiento VOZ", "Error: " + error);
                if (!isSpeaking) {
                    speechRecognizer.startListening(recognizerIntent);
                }
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String spokenText = matches.get(0); // Obtener el primer resultado
                    sendMessage(spokenText); // Enviar el mensaje a Firebase
                    stopAndRestartSpeechRecognition();
                }
                if (!isSpeaking) {
                    speechRecognizer.startListening(recognizerIntent);
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {}

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });

        speechRecognizer.startListening(recognizerIntent);
    }

    private void initializeTextToSpeech() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int langResult = textToSpeech.setLanguage(Locale.forLanguageTag("es-MX"));
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TextToSpeech", "Idioma no soportado o faltan datos.");
                } else {
                    Log.d("TextToSpeech", "TextToSpeech inicializado correctamente.");
                }
            } else {
                Log.e("TextToSpeech", "Error al inicializar TextToSpeech.");
            }
        });
    }

    private void stopAndRestartSpeechRecognition() {
        speechRecognizer.stopListening();
        new Handler().postDelayed(() -> speechRecognizer.startListening(recognizerIntent), 5000);
    }

    private void sendMessage(String message) {
        String messageId = messagesRef.push().getKey(); // Generar un ID único para el mensaje
        if (messageId != null) {
            Map<String, Object> messageMap = new HashMap<>();
            messageMap.put("usuario", message);
            messageMap.put("timestamp", System.currentTimeMillis()); // Agregar marca de tiempo

            // Se coloca "..." en el modelo mientras esperamos la respuesta
            messageMap.put("modelo", "...");

            // Guardar el mensaje en Firebase con el ID generado automáticamente
            messagesRef.child(messageId).setValue(messageMap)
                    .addOnSuccessListener(aVoid -> Log.d("Firebase", "Mensaje enviado con éxito."))
                    .addOnFailureListener(e -> Log.e("Firebase", "Error al enviar mensaje: " + e.getMessage()));
        } else {
            Log.e("Firebase", "El ID generado es nulo.");
        }
    }

}
