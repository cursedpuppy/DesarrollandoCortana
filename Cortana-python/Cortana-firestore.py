#Uso de FireStore
import firebase_admin
from firebase_admin import credentials, firestore
from langchain_ollama import OllamaLLM
import os

# Obtener la ruta absoluta del archivo JSON
script_dir = os.path.dirname(os.path.abspath(__file__))
cred_path = os.path.join(script_dir, 'cortana-masterchief-firebase-adminsdk-fc31q-ed2cc53a20.json')

# Inicializar Firebase
if not firebase_admin._apps:
    cred = credentials.Certificate(cred_path)
    firebase_admin.initialize_app(cred)
else:
    app = firebase_admin.get_app()

db = firestore.client()

# Inicializar el modelo de Ollama
llm = OllamaLLM(model="llama3.2")

# Almacenar los IDs de mensajes procesados para evitar duplicados
mensajes_procesados = set()

# Referencia a la colección de mensajes
messages_ref = db.collection('chat').document('mensajes').collection('entries')

# Función para manejar cambios en Firestore
def manejar_cambios(snapshot, cambios, tiempo):
    global mensajes_procesados

    for cambio in cambios:
        try:
            mensaje_id = cambio.document.id
            contenido = cambio.document.to_dict()

            if cambio.type.name == 'ADDED':  # Solo procesar mensajes nuevos
                # Verificar si el mensaje ya fue procesado
                if mensaje_id in mensajes_procesados:
                    continue

                # Verificar si el mensaje es válido
                mensaje_usuario = contenido.get("usuario")
                if not mensaje_usuario:
                    print(f"Mensaje vacío o mal formateado: {mensaje_id}")
                    continue

                # Marcar el mensaje como procesado
                mensajes_procesados.add(mensaje_id)

                # Generar la respuesta
                print(f"Procesando mensaje: {mensaje_usuario}")
                system_prompt = (
                    "Eres un modelo de lenguaje diseñado para responder preguntas de manera corta y precisa. "
                    "Tu nombre es Cortana y debes rolear como el personaje de Halo."
                )
                response = llm.generate(prompts=[system_prompt + "\nUsuario: " + mensaje_usuario])
                respuesta_generada = response.generations[0][0].text
                print(f"Respuesta generada: {respuesta_generada}")

                # Guardar la respuesta en Firestore
                cambio.document.reference.update({"modelo": respuesta_generada})
                print(f"Respuesta guardada en Firestore para mensaje {mensaje_id}.")

        except Exception as e:
            print(f"Error al procesar el mensaje: {e}")

# Configurar el listener en tiempo real
listener = messages_ref.on_snapshot(manejar_cambios)

# Mantener el script corriendo
try:
    print("Escuchando cambios en Firestore...")
    while True:
        pass  # Mantener el script corriendo
except KeyboardInterrupt:
    print("Script terminado por el usuario.")
    listener.unsubscribe()
