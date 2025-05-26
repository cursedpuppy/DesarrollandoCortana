import os
import firebase_admin
from firebase_admin import credentials, db
from langchain_ollama import OllamaLLM

# Obtener la ruta absoluta del archivo JSON
script_dir = os.path.dirname(os.path.abspath(__file__))
cred_path = os.path.join(script_dir, 'cortana-masterchief2-firebase-adminsdk-dwgyf-7dc5a272e4.json')

# Inicializar Firebase
if not firebase_admin._apps:
    cred = credentials.Certificate(cred_path)
    firebase_admin.initialize_app(cred, {
        'databaseURL': 'https://cortana-masterchief2-default-rtdb.firebaseio.com/'  # Reemplaza con tu URL de Realtime Database
    })
else:
    app = firebase_admin.get_app()

# Inicializar el modelo de Ollama
llm = OllamaLLM(model="llama3.2")

# Almacenar los IDs de mensajes procesados para evitar duplicados
mensajes_procesados = set()

# Referencia a la base de datos de mensajes
messages_ref = db.reference('messages')

# Función para manejar cambios en Realtime Database
def manejar_cambios(event):
    print(f"Evento detectado: {event}")
    global mensajes_procesados

    try:
        # Obtener el ID del mensaje y los datos del evento
        mensaje_path = event.path.strip('/')  # Ejemplo: '-ODgk8vOOZ7tOMy0g7v4/usuario'
        mensaje_id = mensaje_path.split('/')[0]  # ID único del mensaje
        contenido = event.data

        # Verificar si el mensaje tiene contenido y contiene la clave "usuario"
        if not contenido or 'usuario' not in contenido:
            print("Mensaje sin contenido o sin campo 'usuario'.")
            return

        # Verificar si el mensaje ya fue procesado
        if mensaje_id in mensajes_procesados:
            print("Mensaje ya procesado.")
            return

        # Marcar el mensaje como procesado
        mensajes_procesados.add(mensaje_id)

        # Acceder al contenido del mensaje (el texto del usuario)
        mensaje_usuario = contenido['usuario']
        print(f"Procesando mensaje: {mensaje_usuario}")

        # Generar la respuesta
        system_prompt = (
            "Eres un modelo de lenguaje diseñado para responder preguntas de manera corta y precisa. "
            "Tu nombre es Cortana y debes rolear como el personaje de Halo."
        )
        response = llm.generate(prompts=[system_prompt + "\nUsuario: " + mensaje_usuario])
        respuesta_generada = response.generations[0][0].text
        print(f"Respuesta generada: {respuesta_generada}")

        # Guardar la respuesta en Realtime Database
        messages_ref.child(mensaje_id).update({"modelo": respuesta_generada})
        print(f"Respuesta guardada en Realtime Database para mensaje {mensaje_id}.")

    except Exception as e:
        print(f"Error al procesar el mensaje: {e}")


# Configurar el listener en tiempo real
messages_ref.listen(manejar_cambios)

# Mantener el script corriendo
try:
    print("Escuchando cambios en Realtime Database...")
    while True:
        pass  # Mantener el script corriendo
except KeyboardInterrupt:
    print("Script terminado por el usuario.")
